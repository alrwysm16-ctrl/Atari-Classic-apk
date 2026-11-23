package com.example.ui.viewmodel

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AtariDatabase
import com.example.data.model.GameEntity
import com.example.data.preferences.SettingsRepository
import com.example.data.repository.AtariRepository
import com.example.emulator.AtariAudioPlayer
import com.example.emulator.UnifiedAtariEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class EmulatorUiState(
    val game: GameEntity? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isPaused: Boolean = false,
    val fps: Int = 60,
    val speedMultiplier: Float = 1.0f, // 0.5f, 1.0f, 2.0f
    val isRewinding: Boolean = false,
    val isRecordingClip: Boolean = false,
    val recordedFramesCount: Int = 0,
    val currentScore: Int = 0
)

class EmulatorViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AtariDatabase.getInstance(application)
    private val settingsRepo = SettingsRepository(application)
    private val repository = AtariRepository(application, db, settingsRepo)

    private val engine = UnifiedAtariEngine()
    private val audioPlayer = AtariAudioPlayer()

    private val _uiState = MutableStateFlow(EmulatorUiState())
    val uiState: StateFlow<EmulatorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    private val _connectedGamepadName = MutableStateFlow<String?>(null)
    val connectedGamepadName: StateFlow<String?> = _connectedGamepadName.asStateFlow()

    // Double-buffered bitmaps so Compose Hardware Canvas always renders fresh GPU textures cleanly
    private val bitmapA: Bitmap = Bitmap.createBitmap(
        UnifiedAtariEngine.SCREEN_WIDTH,
        UnifiedAtariEngine.SCREEN_HEIGHT,
        Bitmap.Config.ARGB_8888
    )
    private val bitmapB: Bitmap = Bitmap.createBitmap(
        UnifiedAtariEngine.SCREEN_WIDTH,
        UnifiedAtariEngine.SCREEN_HEIGHT,
        Bitmap.Config.ARGB_8888
    )
    private var useBufferA = true
    val frameTick = mutableIntStateOf(0)
    val currentFrameImage = mutableStateOf<ImageBitmap?>(null)

    private var loopJob: Job? = null
    private var currentGameId: String? = null
    private var autoSaveOnExit: Boolean = true
    private var isArabic: Boolean = true

    // Live input state (touch + hardware gamepad combined)
    @Volatile var touchUp = false
    @Volatile var touchDown = false
    @Volatile var touchLeft = false
    @Volatile var touchRight = false
    @Volatile var touchFireA = false
    @Volatile var touchActionB = false
    @Volatile var touchReset = false

    @Volatile var padUp = false
    @Volatile var padDown = false
    @Volatile var padLeft = false
    @Volatile var padRight = false
    @Volatile var padFireA = false
    @Volatile var padActionB = false
    @Volatile var padReset = false

    fun updateConnectedGamepad(name: String?) {
        _connectedGamepadName.value = name
    }

    fun startSession(gameId: String, initialSlotIndex: Int?, preloadedGame: GameEntity? = null) {
        if (currentGameId == gameId && loopJob?.isActive == true) return
        stopLoop()
        clearAllInputs()
        currentGameId = gameId

        viewModelScope.launch {
            _uiState.value = EmulatorUiState(game = preloadedGame, isLoading = true)
            val settings = settingsRepo.settingsFlow.first()
            autoSaveOnExit = settings.autoSave
            isArabic = settings.isArabic
            audioPlayer.setVolume(settings.volumePercent, settings.isMuted)

            val game = preloadedGame ?: repository.getGame(gameId)
            if (game == null) {
                _uiState.value = EmulatorUiState(
                    isLoading = false,
                    errorMessage = if (isArabic) "تعذر العثور على اللعبة المطلوبة" else "Game not found"
                )
                return@launch
            }

            val romLoad = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = if (game.isImported || game.engineType == "rom2600") {
                        repository.loadRomBytes(game)
                    } else null
                    engine.loadGame(game, bytes).getOrThrow()
                }
            }

            if (romLoad.isFailure) {
                _uiState.value = EmulatorUiState(
                    game = game,
                    isLoading = false,
                    errorMessage = if (isArabic) "تعذر تشغيل ملف اللعبة محلياً" else "Unable to boot game locally"
                )
                return@launch
            }

            if (initialSlotIndex != null) {
                val slot = repository.getSaveSlot(gameId, initialSlotIndex)
                if (slot != null) {
                    engine.loadState(slot.stateData)
                }
            }

            syncFrameBitmap()
            repository.recordPlay(game.id, 0)
            _uiState.value = EmulatorUiState(
                game = game,
                isLoading = false,
                isPaused = false,
                currentScore = engine.getCurrentScoreEstimate()
            )
            audioPlayer.start()
            startEmulationLoop()
        }
    }

    private fun clearAllInputs() {
        touchUp = false; touchDown = false; touchLeft = false; touchRight = false
        touchFireA = false; touchActionB = false; touchReset = false
        padUp = false; padDown = false; padLeft = false; padRight = false
        padFireA = false; padActionB = false; padReset = false
    }

    fun updateAudioSettings(volumePercent: Int, isMuted: Boolean, autoSave: Boolean, arabic: Boolean) {
        audioPlayer.setVolume(volumePercent, isMuted)
        autoSaveOnExit = autoSave
        isArabic = arabic
    }

    private fun startEmulationLoop() {
        loopJob?.cancel()
        loopJob = viewModelScope.launch(Dispatchers.Default) {
            var fpsFrames = 0
            var fpsWindowStart = System.nanoTime()

            while (isActive) {
                val state = _uiState.value
                if (state.isPaused) {
                    delay(32)
                    continue
                }

                val frameStartNs = System.nanoTime()

                if (state.isRewinding) {
                    val rewound = engine.rewindStep()
                    if (!rewound) {
                        _uiState.value = _uiState.value.copy(isRewinding = false)
                    }
                    syncFrameBitmap()
                    delay(35)
                } else {
                    engine.setInputs(
                        up = touchUp || padUp,
                        down = touchDown || padDown,
                        left = touchLeft || padLeft,
                        right = touchRight || padRight,
                        fireA = touchFireA || padFireA,
                        actionB = touchActionB || padActionB,
                        resetPressed = touchReset || padReset
                    )
                    touchReset = false
                    padReset = false

                    val steps = when {
                        state.speedMultiplier >= 1.9f -> 2
                        else -> 1
                    }

                    val audioBuf = engine.getAudioBuffer()
                    for (s in 0 until steps) {
                        val samples = engine.stepFrame(if (s == 0) audioBuf else null)
                        if (samples > 0) {
                            audioPlayer.writeSamples(audioBuf, samples)
                        }
                    }

                    syncFrameBitmap()

                    if (state.isRecordingClip) {
                        _uiState.value = _uiState.value.copy(
                            recordedFramesCount = _uiState.value.recordedFramesCount + 1
                        )
                    }
                }

                fpsFrames++
                val now = System.nanoTime()
                if (now - fpsWindowStart >= 500_000_000L) {
                    val measured = (fpsFrames * 1_000_000_000L / (now - fpsWindowStart)).toInt().coerceIn(1, 120)
                    val score = engine.getCurrentScoreEstimate()
                    _uiState.value = _uiState.value.copy(fps = measured, currentScore = score)
                    fpsFrames = 0
                    fpsWindowStart = now
                }

                val targetFrameNs = when {
                    _uiState.value.speedMultiplier <= 0.6f -> 33_333_333L // 0.5x
                    _uiState.value.speedMultiplier >= 1.9f -> 8_333_333L  // 2x
                    else -> 16_666_666L                                    // 60fps
                }
                val elapsedNs = System.nanoTime() - frameStartNs
                val sleepMs = (targetFrameNs - elapsedNs) / 1_000_000L
                if (sleepMs > 1L) {
                    delay(sleepMs)
                }
            }
        }
    }

    private fun syncFrameBitmap() {
        val target = if (useBufferA) bitmapA else bitmapB
        target.setPixels(
            engine.frameBuffer,
            0,
            UnifiedAtariEngine.SCREEN_WIDTH,
            0,
            0,
            UnifiedAtariEngine.SCREEN_WIDTH,
            UnifiedAtariEngine.SCREEN_HEIGHT
        )
        useBufferA = !useBufferA
        val img = target.asImageBitmap()
        currentFrameImage.value = img
        frameTick.intValue = frameTick.intValue + 1
    }

    fun setPaused(paused: Boolean) {
        _uiState.value = _uiState.value.copy(isPaused = paused)
    }

    fun restartGame() {
        engine.reset()
        syncFrameBitmap()
        _uiState.value = _uiState.value.copy(isPaused = false, isRewinding = false, currentScore = 0)
        _events.tryEmit(if (isArabic) "تمت إعادة تشغيل اللعبة" else "Game restarted")
    }

    fun triggerQuickAction(isReset: Boolean) {
        if (isReset) {
            restartGame()
        } else {
            touchActionB = true
            viewModelScope.launch {
                delay(120)
                touchActionB = false
            }
        }
    }

    fun cycleSpeed() {
        val next = when (_uiState.value.speedMultiplier) {
            1.0f -> 2.0f
            2.0f -> 0.5f
            else -> 1.0f
        }
        _uiState.value = _uiState.value.copy(speedMultiplier = next)
        val label = if (isArabic) "السرعة: ${next}x" else "Speed: ${next}x"
        _events.tryEmit(label)
    }

    fun rewindBurst() {
        if (!engine.canRewind()) {
            _events.tryEmit(if (isArabic) "لا توجد لحظات سابقة للإرجاع" else "No rewind history yet")
            return
        }
        var steps = 0
        while (steps < 6 && engine.rewindStep()) {
            steps++
        }
        syncFrameBitmap()
        _events.tryEmit(if (isArabic) "تم إرجاع الزمن" else "Rewound")
    }

    fun saveToSlot(slotIndex: Int) {
        val game = _uiState.value.game ?: return
        viewModelScope.launch {
            val stateBytes = engine.saveState()
            val score = engine.getCurrentScoreEstimate()
            repository.saveState(game.id, slotIndex, stateBytes, score)
            val msg = if (isArabic) {
                if (slotIndex == 0) "تم الحفظ التلقائي" else "تم الحفظ في الخانة $slotIndex"
            } else {
                if (slotIndex == 0) "Auto-saved" else "Saved to Slot $slotIndex"
            }
            _events.tryEmit(msg)
        }
    }

    fun loadFromSlot(slotIndex: Int) {
        val game = _uiState.value.game ?: return
        viewModelScope.launch {
            val slot = repository.getSaveSlot(game.id, slotIndex)
            if (slot == null) {
                _events.tryEmit(if (isArabic) "الخانة $slotIndex فارغة" else "Slot $slotIndex is empty")
                return@launch
            }
            if (engine.loadState(slot.stateData)) {
                syncFrameBitmap()
                _events.tryEmit(if (isArabic) "تم تحميل الخانة $slotIndex" else "Loaded Slot $slotIndex")
            }
        }
    }

    fun observeSlotsForCurrentGame(gameId: String) = repository.observeSaveSlots(gameId)

    fun toggleRecordingOrCaptureScreenshot() {
        val currentlyRecording = _uiState.value.isRecordingClip
        if (!currentlyRecording) {
            _uiState.value = _uiState.value.copy(isRecordingClip = true, recordedFramesCount = 0)
            _events.tryEmit(if (isArabic) "بدء تسجيل اللقطة..." else "Recording started...")
        } else {
            _uiState.value = _uiState.value.copy(isRecordingClip = false)
            captureScreenshot()
        }
    }

    fun captureScreenshot() {
        val game = _uiState.value.game ?: return
        val latest = if (useBufferA) bitmapB else bitmapA
        val copy = latest.copy(Bitmap.Config.ARGB_8888, false)
        viewModelScope.launch(Dispatchers.IO) {
            val saved = saveBitmapToGalleryOrApp(copy, game.nameEn)
            copy.recycle()
            _events.tryEmit(
                if (saved) {
                    if (isArabic) "تم حفظ لقطة الشاشة" else "Screenshot saved"
                } else {
                    if (isArabic) "تعذر حفظ لقطة الشاشة" else "Screenshot failed"
                }
            )
        }
    }

    private fun saveBitmapToGalleryOrApp(bitmap: Bitmap, title: String): Boolean {
        val fileName = "Atari_${title.replace(" ", "_")}_${System.currentTimeMillis()}.png"
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AtariClassic")
                }
                val resolver = getApplication<Application>().contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    return true
                }
            }
            val dir = File(getApplication<Application>().filesDir, "screenshots").apply { mkdirs() }
            FileOutputStream(File(dir, fileName)).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            true
        }.getOrDefault(false)
    }

    fun exitAndAutoSave(onDone: () -> Unit) {
        val game = _uiState.value.game
        val score = engine.getCurrentScoreEstimate()
        val stateBytes = if (game != null && autoSaveOnExit) engine.saveState() else null
        stopLoop()
        clearAllInputs()
        if (game != null) {
            viewModelScope.launch {
                if (stateBytes != null) {
                    repository.saveState(game.id, 0, stateBytes, score)
                } else if (score > 0) {
                    repository.recordPlay(game.id, score)
                }
                onDone()
            }
        } else {
            onDone()
        }
    }

    private fun stopLoop() {
        loopJob?.cancel()
        loopJob = null
        audioPlayer.stop()
    }

    override fun onCleared() {
        super.onCleared()
        stopLoop()
    }
}
