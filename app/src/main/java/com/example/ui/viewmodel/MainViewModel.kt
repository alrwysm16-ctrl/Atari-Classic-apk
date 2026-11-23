package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AtariDatabase
import com.example.data.model.AspectRatioMode
import com.example.data.model.AtariSystem
import com.example.data.model.GameEntity
import com.example.data.model.LibrarySection
import com.example.data.model.SaveSlotEntity
import com.example.data.model.ScreenFilterMode
import com.example.data.model.SortMode
import com.example.data.model.ThemeMode
import com.example.data.model.UserSettings
import com.example.data.preferences.SettingsRepository
import com.example.data.repository.AtariRepository
import com.example.data.repository.IndexingState
import com.example.data.repository.StorageStats
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryFilters(
    val system: AtariSystem = AtariSystem.ALL,
    val category: String = "ALL",
    val section: LibrarySection = LibrarySection.ALL,
    val searchQuery: String = "",
    val sortMode: SortMode = SortMode.DEFAULT
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AtariDatabase.getInstance(application)
    private val settingsRepo = SettingsRepository(application)
    val repository = AtariRepository(application, db, settingsRepo)

    private val _indexingState = MutableStateFlow(IndexingState())
    val indexingState: StateFlow<IndexingState> = _indexingState.asStateFlow()

    private val _filters = MutableStateFlow(LibraryFilters())
    val filters: StateFlow<LibraryFilters> = _filters.asStateFlow()

    val settings: StateFlow<UserSettings> = settingsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    val categories: StateFlow<List<String>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val games: StateFlow<List<GameEntity>> = _filters
        .flatMapLatest { f ->
            repository.observeGames(
                system = f.system.code,
                category = f.category,
                section = f.section,
                searchQuery = f.searchQuery,
                sortMode = f.sortMode
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val importedGames: StateFlow<List<GameEntity>> = repository.observeImportedGames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _storageStats = MutableStateFlow(StorageStats(0, 0, 0L, 0L))
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    init {
        startIndexing()
    }

    fun startIndexing() {
        viewModelScope.launch {
            repository.ensureLibraryIndexed { state ->
                _indexingState.value = state
            }
            refreshStorageStats()
        }
    }

    fun setSystemFilter(sys: AtariSystem) {
        _filters.value = _filters.value.copy(system = sys)
    }

    fun setCategoryFilter(cat: String) {
        _filters.value = _filters.value.copy(category = cat)
    }

    fun setSection(section: LibrarySection) {
        _filters.value = _filters.value.copy(section = section)
    }

    fun setSearchQuery(q: String) {
        _filters.value = _filters.value.copy(searchQuery = q)
    }

    fun setSortMode(mode: SortMode) {
        _filters.value = _filters.value.copy(sortMode = mode)
    }

    fun toggleFavorite(game: GameEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(game.id, game.isFavorite)
        }
    }

    fun observeGame(gameId: String) = repository.observeGame(gameId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun observeSaveSlots(gameId: String) = repository.observeSaveSlots(gameId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteSaveSlot(gameId: String, slotIndex: Int) {
        viewModelScope.launch {
            repository.deleteSaveSlot(gameId, slotIndex)
            refreshStorageStats()
        }
    }

    // Settings mutations
    fun setCrtFilter(enabled: Boolean) = viewModelScope.launch { settingsRepo.setCrtFilter(enabled) }
    fun setScanlines(enabled: Boolean) = viewModelScope.launch { settingsRepo.setScanlines(enabled) }
    fun setFilterMode(mode: ScreenFilterMode) = viewModelScope.launch { settingsRepo.setFilterMode(mode) }
    fun setVibration(enabled: Boolean) = viewModelScope.launch { settingsRepo.setVibration(enabled) }
    fun setAutoSave(enabled: Boolean) = viewModelScope.launch { settingsRepo.setAutoSave(enabled) }
    fun setAspectRatio(mode: AspectRatioMode) = viewModelScope.launch { settingsRepo.setAspectRatio(mode) }
    fun setVolume(percent: Int) = viewModelScope.launch { settingsRepo.setVolumePercent(percent) }
    fun setMuted(muted: Boolean) = viewModelScope.launch { settingsRepo.setMuted(muted) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    fun setIsArabic(isAr: Boolean) = viewModelScope.launch { settingsRepo.setIsArabic(isAr) }

    fun saveTouchLayout(
        scale: Float,
        opacity: Float,
        dx: Float,
        dy: Float,
        abX: Float,
        abY: Float,
        swapControls: Boolean,
        showExtraButtons: Boolean,
        aX: Float,
        aY: Float,
        bX: Float,
        bY: Float
    ) {
        viewModelScope.launch {
            settingsRepo.setTouchLayout(
                scale = scale,
                opacity = opacity,
                dpadX = dx,
                dpadY = dy,
                abX = abX,
                abY = abY,
                swapControls = swapControls,
                showExtraButtons = showExtraButtons,
                aX = aX,
                aY = aY,
                bX = bX,
                bY = bY
            )
        }
    }

    fun setSwapControls(swap: Boolean) {
        val cur = settings.value
        saveTouchLayout(
            scale = cur.touchScale,
            opacity = cur.touchOpacity,
            dx = cur.dpadOffsetX,
            dy = cur.dpadOffsetY,
            abX = cur.abOffsetX,
            abY = cur.abOffsetY,
            swapControls = swap,
            showExtraButtons = cur.showExtraButtons,
            aX = cur.aButtonOffsetX,
            aY = cur.aButtonOffsetY,
            bX = cur.bButtonOffsetX,
            bY = cur.bButtonOffsetY
        )
    }

    fun resetTouchLayout() {
        viewModelScope.launch { settingsRepo.resetTouchLayout() }
    }

    // Import ROMs / Folders
    fun importRomFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val isAr = settings.value.isArabic
            repository.importRomUris(uris).fold(
                onSuccess = { count ->
                    refreshStorageStats()
                    val msg = if (isAr) {
                        if (count > 0) "تم استيراد $count لعبة بنجاح" else "لم يتم العثور على ملفات ROM صالحة"
                    } else {
                        if (count > 0) "Imported $count ROM(s)" else "No valid ROM files found"
                    }
                    _messages.tryEmit(msg)
                },
                onFailure = {
                    _messages.tryEmit(if (isAr) "تعذر استيراد الملفات" else "Failed to import ROMs")
                }
            )
        }
    }

    fun importRomFolder(treeUri: Uri) {
        viewModelScope.launch {
            val isAr = settings.value.isArabic
            repository.importRomFolder(treeUri).fold(
                onSuccess = { count ->
                    refreshStorageStats()
                    val msg = if (isAr) {
                        if (count > 0) "تم استيراد $count لعبة من المجلد" else "المجلد لا يحتوي على ملفات ROM مدعومة"
                    } else {
                        if (count > 0) "Imported $count ROM(s) from folder" else "No supported ROMs in folder"
                    }
                    _messages.tryEmit(msg)
                },
                onFailure = {
                    _messages.tryEmit(if (isAr) "تعذر قراءة المجلد المحدد" else "Could not read selected folder")
                }
            )
        }
    }

    fun deleteImportedGame(game: GameEntity) {
        viewModelScope.launch {
            repository.deleteImportedGame(game)
            refreshStorageStats()
            _messages.tryEmit(if (settings.value.isArabic) "تم حذف اللعبة" else "Game removed")
        }
    }

    fun clearAllSaves() {
        viewModelScope.launch {
            repository.clearAllSaves()
            refreshStorageStats()
            _messages.tryEmit(if (settings.value.isArabic) "تم مسح جميع خانات الحفظ" else "All save states cleared")
        }
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            _storageStats.value = repository.getStorageStats()
        }
    }

    suspend fun buildBackupJson(): String = repository.exportBackupJson()

    fun restoreBackupFromJson(jsonText: String) {
        viewModelScope.launch {
            val isAr = settings.value.isArabic
            repository.importBackupJson(jsonText).fold(
                onSuccess = { count ->
                    refreshStorageStats()
                    _messages.tryEmit(
                        if (isAr) "تمت استعادة النسخة الاحتياطية ($count عنصر)"
                        else "Restored backup ($count items)"
                    )
                },
                onFailure = {
                    _messages.tryEmit(if (isAr) "ملف النسخة الاحتياطية غير صالح" else "Invalid backup file")
                }
            )
        }
    }
}
