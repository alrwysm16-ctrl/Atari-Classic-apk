package com.example

import android.content.Context
import android.hardware.input.InputManager
import android.net.Uri
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.AspectRatioMode
import com.example.data.model.ScreenFilterMode
import com.example.data.model.ThemeMode
import com.example.ui.components.TelegramSubscribeDialog
import com.example.ui.i18n.getStrings
import com.example.ui.screens.EmulatorPlayScreen
import com.example.ui.screens.GameDetailScreen
import com.example.ui.screens.ImportGamesDialog
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashIndexingScreen
import com.example.ui.theme.AtariClassicTheme
import com.example.ui.theme.LocalAtariColors
import com.example.ui.viewmodel.EmulatorViewModel
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    var activeEmulatorVm: EmulatorViewModel? = null
    var isPlayScreenActive: Boolean = false
    var onGamepadStartAction: (() -> Unit)? = null

    private var inputManager: InputManager? = null
    private val inputDeviceListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) = refreshConnectedGamepad()
        override fun onInputDeviceRemoved(deviceId: Int) = refreshConnectedGamepad()
        override fun onInputDeviceChanged(deviceId: Int) = refreshConnectedGamepad()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        inputManager = getSystemService(Context.INPUT_SERVICE) as? InputManager

        setContent {
            val mainVm: MainViewModel = viewModel()
            val emulatorVm: EmulatorViewModel = viewModel()
            activeEmulatorVm = emulatorVm

            LaunchedEffect(Unit) {
                refreshConnectedGamepad()
            }

            AtariClassicRoot(
                mainVm = mainVm,
                emulatorVm = emulatorVm,
                onSetPlayingActive = { active, onStart ->
                    isPlayScreenActive = active
                    onGamepadStartAction = onStart
                },
                onWriteUriText = { uri, text ->
                    withContext(Dispatchers.IO) {
                        contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                            it.write(text)
                        }
                    }
                },
                onReadUriText = { uri ->
                    withContext(Dispatchers.IO) {
                        contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                            it.readText()
                        } ?: ""
                    }
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        inputManager?.registerInputDeviceListener(inputDeviceListener, null)
        refreshConnectedGamepad()
    }

    override fun onPause() {
        super.onPause()
        inputManager?.unregisterInputDeviceListener(inputDeviceListener)
    }

    private fun refreshConnectedGamepad() {
        val vm = activeEmulatorVm ?: return
        val ids = InputDevice.getDeviceIds()
        var foundName: String? = null
        for (id in ids) {
            val dev = InputDevice.getDevice(id) ?: continue
            val src = dev.sources
            val isGamepad = (src and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
            val isJoystick = (src and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
            if ((isGamepad || isJoystick) && !dev.isVirtual) {
                foundName = dev.name
                break
            }
        }
        vm.updateConnectedGamepad(foundName)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val vm = activeEmulatorVm
        if (vm != null && isPlayScreenActive) {
            val code = event.keyCode
            if (code == KeyEvent.KEYCODE_BACK ||
                code == KeyEvent.KEYCODE_VOLUME_UP ||
                code == KeyEvent.KEYCODE_VOLUME_DOWN ||
                code == KeyEvent.KEYCODE_VOLUME_MUTE
            ) {
                return super.dispatchKeyEvent(event)
            }

            val isDown = event.action == KeyEvent.ACTION_DOWN
            val isUp = event.action == KeyEvent.ACTION_UP
            if (isDown || isUp) {
                when (code) {
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W -> { vm.padUp = isDown; return true }
                    KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S -> { vm.padDown = isDown; return true }
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> { vm.padLeft = isDown; return true }
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> { vm.padRight = isDown; return true }
                    KeyEvent.KEYCODE_BUTTON_A,
                    KeyEvent.KEYCODE_BUTTON_X,
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_SPACE,
                    KeyEvent.KEYCODE_Z,
                    KeyEvent.KEYCODE_ENTER -> {
                        vm.padFireA = isDown
                        return true
                    }
                    KeyEvent.KEYCODE_BUTTON_B,
                    KeyEvent.KEYCODE_BUTTON_Y,
                    KeyEvent.KEYCODE_BUTTON_R1,
                    KeyEvent.KEYCODE_X -> {
                        vm.padActionB = isDown
                        return true
                    }
                    KeyEvent.KEYCODE_BUTTON_SELECT,
                    KeyEvent.KEYCODE_BUTTON_L1 -> {
                        if (isDown && event.repeatCount == 0) {
                            vm.padReset = true
                        }
                        return true
                    }
                    KeyEvent.KEYCODE_BUTTON_START,
                    KeyEvent.KEYCODE_MENU -> {
                        if (isDown && event.repeatCount == 0) {
                            onGamepadStartAction?.invoke()
                        }
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        val ev = event ?: return super.onGenericMotionEvent(event)
        val vm = activeEmulatorVm
        val isAnalog = (ev.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK ||
            (ev.source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
        if (vm != null && isPlayScreenActive && isAnalog && ev.action == MotionEvent.ACTION_MOVE) {
            val x = ev.getAxisValue(MotionEvent.AXIS_X)
            val y = ev.getAxisValue(MotionEvent.AXIS_Y)
            val hatX = ev.getAxisValue(MotionEvent.AXIS_HAT_X)
            val hatY = ev.getAxisValue(MotionEvent.AXIS_HAT_Y)

            val effX = if (abs(hatX) > 0.25f) hatX else x
            val effY = if (abs(hatY) > 0.25f) hatY else y

            vm.padLeft = effX < -0.30f
            vm.padRight = effX > 0.30f
            vm.padUp = effY < -0.30f
            vm.padDown = effY > 0.30f

            // Support analog triggers on modern controllers as A / B fire buttons
            val rTrigger = ev.getAxisValue(MotionEvent.AXIS_RTRIGGER)
            val lTrigger = ev.getAxisValue(MotionEvent.AXIS_LTRIGGER)
            if (rTrigger > 0.45f) vm.padFireA = true
            else if (rTrigger in 0f..0.15f && ev.device != null && ev.device.getMotionRange(MotionEvent.AXIS_RTRIGGER) != null) {
                vm.padFireA = false
            }
            if (lTrigger > 0.45f) vm.padActionB = true
            else if (lTrigger in 0f..0.15f && ev.device != null && ev.device.getMotionRange(MotionEvent.AXIS_LTRIGGER) != null) {
                vm.padActionB = false
            }
            return true
        }
        return super.onGenericMotionEvent(event)
    }
}

@Composable
private fun AtariClassicRoot(
    mainVm: MainViewModel,
    emulatorVm: EmulatorViewModel,
    onSetPlayingActive: (Boolean, (() -> Unit)?) -> Unit,
    onWriteUriText: suspend (Uri, String) -> Unit,
    onReadUriText: suspend (Uri) -> String
) {
    val indexingState by mainVm.indexingState.collectAsStateWithLifecycle()
    val settings by mainVm.settings.collectAsStateWithLifecycle()
    val filters by mainVm.filters.collectAsStateWithLifecycle()
    val games by mainVm.games.collectAsStateWithLifecycle()
    val categories by mainVm.categories.collectAsStateWithLifecycle()
    val importedGames by mainVm.importedGames.collectAsStateWithLifecycle()
    val storageStats by mainVm.storageStats.collectAsStateWithLifecycle()
    val connectedGamepad by emulatorVm.connectedGamepadName.collectAsStateWithLifecycle()

    val strings = getStrings(settings.isArabic)
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showLibraryImportDialog by remember { mutableStateOf(false) }
    var startupTelegramDismissed by rememberSaveable { mutableStateOf(false) }
    var showTelegramManualDialog by remember { mutableStateOf(false) }

    val importFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        mainVm.importRomFiles(uris)
    }

    val importFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            mainVm.importRomFolder(uri)
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = mainVm.buildBackupJson()
                onWriteUriText(uri, json)
                snackbarHostState.showSnackbar(
                    if (settings.isArabic) "تم تصدير النسخة الاحتياطية بنجاح" else "Backup exported"
                )
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = onReadUriText(uri)
                mainVm.restoreBackupFromJson(text)
            }
        }
    }

    LaunchedEffect(Unit) {
        mainVm.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    AtariClassicTheme(
        themeMode = settings.themeMode,
        isArabic = settings.isArabic
    ) {
        val colors = LocalAtariColors.current
        Scaffold(
            containerColor = colors.bg,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (!indexingState.isReady) {
                    SplashIndexingScreen(
                        indexingState = indexingState,
                        strings = strings,
                        onRetry = { mainVm.startIndexing() }
                    )
                } else {
                    NavHost(
                        navController = navController,
                        startDestination = "library",
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None },
                        popEnterTransition = { EnterTransition.None },
                        popExitTransition = { ExitTransition.None }
                    ) {
                        composable("library") {
                            LibraryScreen(
                                games = games,
                                categories = categories,
                                filters = filters,
                                isArabic = settings.isArabic,
                                strings = strings,
                                onSystemSelected = mainVm::setSystemFilter,
                                onCategorySelected = mainVm::setCategoryFilter,
                                onSectionSelected = mainVm::setSection,
                                onSearchChanged = mainVm::setSearchQuery,
                                onSortChanged = mainVm::setSortMode,
                                onOpenGameDetail = { game ->
                                    mainVm.selectGame(game)
                                    navController.navigate("detail/${game.id}")
                                },
                                onToggleFavorite = mainVm::toggleFavorite,
                                onOpenImportSheet = { showLibraryImportDialog = true },
                                onOpenTelegram = { showTelegramManualDialog = true },
                                onNavigateSettings = {
                                    navController.navigate("settings")
                                }
                            )
                        }

                        composable(
                            route = "detail/{gameId}",
                            arguments = listOf(navArgument("gameId") { type = NavType.StringType })
                        ) { entry ->
                            val gameId = entry.arguments?.getString("gameId") ?: return@composable
                            val gameFlow = remember(gameId) { mainVm.observeGame(gameId) }
                            val slotsFlow = remember(gameId) { mainVm.observeSaveSlots(gameId) }
                            val game by gameFlow.collectAsStateWithLifecycle()
                            val slots by slotsFlow.collectAsStateWithLifecycle()

                            GameDetailScreen(
                                game = game,
                                saveSlots = slots,
                                isArabic = settings.isArabic,
                                strings = strings,
                                onBack = { navController.popBackStack() },
                                onToggleFavorite = mainVm::toggleFavorite,
                                onPlay = { slotIdx ->
                                    val route = if (slotIdx != null) {
                                        "play/$gameId?slot=$slotIdx"
                                    } else {
                                        "play/$gameId"
                                    }
                                    navController.navigate(route)
                                },
                                onDeleteSlot = { slotIdx ->
                                    mainVm.deleteSaveSlot(gameId, slotIdx)
                                }
                            )
                        }

                        composable(
                            route = "play/{gameId}?slot={slot}",
                            arguments = listOf(
                                navArgument("gameId") { type = NavType.StringType },
                                navArgument("slot") {
                                    type = NavType.IntType
                                    defaultValue = -1
                                }
                            )
                        ) { entry ->
                            val gameId = entry.arguments?.getString("gameId") ?: return@composable
                            val slotArg = entry.arguments?.getInt("slot") ?: -1
                            val preloaded = remember(gameId) { mainVm.getCachedGame(gameId) }

                            EmulatorPlayScreen(
                                gameId = gameId,
                                initialSlotIndex = if (slotArg >= 0) slotArg else null,
                                preloadedGame = preloaded,
                                settings = settings,
                                strings = strings,
                                viewModel = emulatorVm,
                                onSetPlayingActive = onSetPlayingActive,
                                onSetFilterMode = mainVm::setFilterMode,
                                onSetAspectRatio = mainVm::setAspectRatio,
                                onToggleMute = { mainVm.setMuted(!settings.isMuted) },
                                onSaveTouchLayout = mainVm::saveTouchLayout,
                                onResetTouchLayout = mainVm::resetTouchLayout,
                                onExit = { navController.popBackStack() }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                settings = settings,
                                strings = strings,
                                storageStats = storageStats,
                                importedGames = importedGames,
                                onBack = { navController.popBackStack() },
                                onToggleCrt = mainVm::setCrtFilter,
                                onToggleScanlines = mainVm::setScanlines,
                                onToggleVibration = mainVm::setVibration,
                                onToggleAutoSave = mainVm::setAutoSave,
                                onCycleAspectRatio = {
                                    val entries = AspectRatioMode.entries
                                    val next = entries[(entries.indexOf(settings.aspectRatio) + 1) % entries.size]
                                    mainVm.setAspectRatio(next)
                                },
                                onCycleScreenFilter = {
                                    val entries = ScreenFilterMode.entries
                                    val next = entries[(entries.indexOf(settings.filterMode) + 1) % entries.size]
                                    mainVm.setFilterMode(next)
                                },
                                onCycleTheme = {
                                    val entries = ThemeMode.entries
                                    val next = entries[(entries.indexOf(settings.themeMode) + 1) % entries.size]
                                    mainVm.setThemeMode(next)
                                },
                                onToggleLanguage = { mainVm.setIsArabic(!settings.isArabic) },
                                onVolumeChange = mainVm::setVolume,
                                onToggleMute = { mainVm.setMuted(!settings.isMuted) },
                                onToggleSwapControls = mainVm::setSwapControls,
                                onToggleTelegramPrompt = mainVm::setShowTelegramPrompt,
                                connectedGamepadName = connectedGamepad,
                                onResetTouchLayout = {
                                    mainVm.resetTouchLayout()
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (settings.isArabic) "تمت استعادة تخطيط الأزرار الافتراضي"
                                            else "Touch layout reset"
                                        )
                                    }
                                },
                                onOpenImportRomFiles = {
                                    importFilesLauncher.launch(arrayOf("*/*"))
                                },
                                onOpenImportRomFolder = {
                                    importFolderLauncher.launch(null)
                                },
                                onDeleteImportedGame = mainVm::deleteImportedGame,
                                onClearAllSaves = mainVm::clearAllSaves,
                                onExportBackup = {
                                    exportBackupLauncher.launch("atari_classic_backup.json")
                                },
                                onImportBackup = {
                                    restoreBackupLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                onRefreshStorage = mainVm::refreshStorageStats
                            )
                        }
                    }
                }

                if (showLibraryImportDialog) {
                    ImportGamesDialog(
                        strings = strings,
                        onImportFiles = {
                            showLibraryImportDialog = false
                            importFilesLauncher.launch(arrayOf("*/*"))
                        },
                        onImportFolder = {
                            showLibraryImportDialog = false
                            importFolderLauncher.launch(null)
                        },
                        onDismiss = { showLibraryImportDialog = false }
                    )
                }

                val shouldShowStartupTelegram = indexingState.isReady &&
                    settings.showTelegramDialogOnStart &&
                    !startupTelegramDismissed

                if (shouldShowStartupTelegram || showTelegramManualDialog) {
                    TelegramSubscribeDialog(
                        isArabic = settings.isArabic,
                        strings = strings,
                        showDontAskOption = shouldShowStartupTelegram && !showTelegramManualDialog,
                        onDismissSession = {
                            startupTelegramDismissed = true
                            showTelegramManualDialog = false
                        },
                        onDontShowAgain = {
                            startupTelegramDismissed = true
                            showTelegramManualDialog = false
                            mainVm.setShowTelegramPrompt(false)
                        }
                    )
                }
            }
        }
    }
}
