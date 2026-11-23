package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AspectRatioMode
import com.example.data.model.SaveSlotEntity
import com.example.data.model.ScreenFilterMode
import com.example.data.model.UserSettings
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AtariEmblemDark
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalAtariColors
import com.example.ui.viewmodel.EmulatorViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmulatorPlayScreen(
    gameId: String,
    initialSlotIndex: Int?,
    settings: UserSettings,
    strings: AppStrings,
    viewModel: EmulatorViewModel,
    onSetPlayingActive: (Boolean, (() -> Unit)?) -> Unit,
    onSetFilterMode: (ScreenFilterMode) -> Unit,
    onSetAspectRatio: (AspectRatioMode) -> Unit,
    onToggleMute: () -> Unit,
    onSaveTouchLayout: (Float, Float, Float, Float, Float, Float, Boolean, Boolean, Float, Float, Float, Float) -> Unit,
    onResetTouchLayout: () -> Unit,
    onExit: () -> Unit
) {
    val colors = LocalAtariColors.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val connectedGamepad by viewModel.connectedGamepadName.collectAsStateWithLifecycle()
    val slots by viewModel.observeSlotsForCurrentGame(gameId).collectAsStateWithLifecycle(emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    var showMenuSheet by remember { mutableStateOf(false) }
    var isEditingControls by remember { mutableStateOf(false) }

    var editScale by remember(settings.touchScale) { mutableFloatStateOf(settings.touchScale) }
    var editOpacity by remember(settings.touchOpacity) { mutableFloatStateOf(settings.touchOpacity) }
    var dpadOffX by remember(settings.dpadOffsetX) { mutableFloatStateOf(settings.dpadOffsetX) }
    var dpadOffY by remember(settings.dpadOffsetY) { mutableFloatStateOf(settings.dpadOffsetY) }
    var abOffX by remember(settings.abOffsetX) { mutableFloatStateOf(settings.abOffsetX) }
    var abOffY by remember(settings.abOffsetY) { mutableFloatStateOf(settings.abOffsetY) }
    var editSwap by remember(settings.swapControls) { mutableStateOf(settings.swapControls) }
    var editShowExtra by remember(settings.showExtraButtons) { mutableStateOf(settings.showExtraButtons) }
    var aOffX by remember(settings.aButtonOffsetX) { mutableFloatStateOf(settings.aButtonOffsetX) }
    var aOffY by remember(settings.aButtonOffsetY) { mutableFloatStateOf(settings.aButtonOffsetY) }
    var bOffX by remember(settings.bButtonOffsetX) { mutableFloatStateOf(settings.bButtonOffsetX) }
    var bOffY by remember(settings.bButtonOffsetY) { mutableFloatStateOf(settings.bButtonOffsetY) }

    // Visual active press highlights on touch controls
    var activeDir by remember { mutableStateOf(QuadDir()) }
    var activeA by remember { mutableStateOf(false) }
    var activeB by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onSetPlayingActive(true) {
            val nextPause = !showMenuSheet
            showMenuSheet = nextPause
            viewModel.setPaused(nextPause)
        }
        onDispose {
            onSetPlayingActive(false, null)
        }
    }

    LaunchedEffect(gameId, initialSlotIndex) {
        viewModel.startSession(gameId, initialSlotIndex)
    }

    LaunchedEffect(settings.volumePercent, settings.isMuted, settings.autoSave, settings.isArabic) {
        viewModel.updateAudioSettings(settings.volumePercent, settings.isMuted, settings.autoSave, settings.isArabic)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val exitAction = {
        viewModel.exitAndAutoSave(onExit)
    }

    BackHandler {
        if (isEditingControls) {
            isEditingControls = false
            viewModel.setPaused(false)
        } else if (showMenuSheet) {
            showMenuSheet = false
            viewModel.setPaused(false)
        } else {
            exitAction()
        }
    }

    val onDirChange: (Boolean, Boolean, Boolean, Boolean) -> Unit = { u, d, l, r ->
        if (settings.vibration && (u != viewModel.touchUp || d != viewModel.touchDown || l != viewModel.touchLeft || r != viewModel.touchRight)) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        activeDir = QuadDir(u, d, l, r)
        viewModel.touchUp = u
        viewModel.touchDown = d
        viewModel.touchLeft = l
        viewModel.touchRight = r
    }

    val onAChange: (Boolean) -> Unit = { pressed ->
        if (pressed && settings.vibration && !viewModel.touchFireA) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        activeA = pressed
        viewModel.touchFireA = pressed
    }

    val onBChange: (Boolean) -> Unit = { pressed ->
        if (pressed && settings.vibration && !viewModel.touchActionB) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        activeB = pressed
        viewModel.touchActionB = pressed
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isLandscape = maxWidth > maxHeight

            if (isLandscape) {
                Column(modifier = Modifier.fillMaxSize()) {
                    PlayTopBar(
                        fps = uiState.fps,
                        score = uiState.currentScore,
                        speedMultiplier = uiState.speedMultiplier,
                        isRecording = uiState.isRecordingClip,
                        gamepadConnected = connectedGamepad != null,
                        onOpenMenu = {
                            showMenuSheet = true
                            viewModel.setPaused(true)
                        },
                        onEditControls = {
                            isEditingControls = true
                            viewModel.setPaused(true)
                        }
                    )

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (!editSwap) {
                                VirtualDPad(
                                    scale = editScale,
                                    opacity = editOpacity,
                                    offsetX = dpadOffX,
                                    offsetY = dpadOffY,
                                    isEditMode = isEditingControls,
                                    activeDir = activeDir,
                                    onDragOffset = { dx, dy -> dpadOffX += dx; dpadOffY += dy },
                                    onDirectionChanged = onDirChange
                                )
                            } else {
                                VirtualActionButtons(
                                    scale = editScale,
                                    opacity = editOpacity,
                                    groupOffsetX = abOffX,
                                    groupOffsetY = abOffY,
                                    aOffsetX = aOffX,
                                    aOffsetY = aOffY,
                                    bOffsetX = bOffX,
                                    bOffsetY = bOffY,
                                    activeA = activeA,
                                    activeB = activeB,
                                    isEditMode = isEditingControls,
                                    onDragA = { dx, dy -> aOffX += dx; aOffY += dy },
                                    onDragB = { dx, dy -> bOffX += dx; bOffY += dy },
                                    onAChanged = onAChange,
                                    onBChanged = onBChange
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AtariScreenViewport(
                                    viewModel = viewModel,
                                    settings = settings,
                                    isLoading = uiState.isLoading,
                                    errorMessage = uiState.errorMessage,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            if (!editSwap) {
                                VirtualActionButtons(
                                    scale = editScale,
                                    opacity = editOpacity,
                                    groupOffsetX = abOffX,
                                    groupOffsetY = abOffY,
                                    aOffsetX = aOffX,
                                    aOffsetY = aOffY,
                                    bOffsetX = bOffX,
                                    bOffsetY = bOffY,
                                    activeA = activeA,
                                    activeB = activeB,
                                    isEditMode = isEditingControls,
                                    onDragA = { dx, dy -> aOffX += dx; aOffY += dy },
                                    onDragB = { dx, dy -> bOffX += dx; bOffY += dy },
                                    onAChanged = onAChange,
                                    onBChanged = onBChange
                                )
                            } else {
                                VirtualDPad(
                                    scale = editScale,
                                    opacity = editOpacity,
                                    offsetX = dpadOffX,
                                    offsetY = dpadOffY,
                                    isEditMode = isEditingControls,
                                    activeDir = activeDir,
                                    onDragOffset = { dx, dy -> dpadOffX += dx; dpadOffY += dy },
                                    onDirectionChanged = onDirChange
                                )
                            }
                        }
                    }

                    PlayBottomBar(
                        speedMultiplier = uiState.speedMultiplier,
                        isRecording = uiState.isRecordingClip,
                        isRewinding = uiState.isRewinding,
                        onRecordOrSnap = { viewModel.toggleRecordingOrCaptureScreenshot() },
                        onCycleSpeed = { viewModel.cycleSpeed() },
                        onRewind = { viewModel.rewindBurst() },
                        onQuickSave = { viewModel.saveToSlot(1) }
                    )
                }
            } else {
                // Portrait Layout (Screen on top, controls below with physical LTR order)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PlayTopBar(
                        fps = uiState.fps,
                        score = uiState.currentScore,
                        speedMultiplier = uiState.speedMultiplier,
                        isRecording = uiState.isRecordingClip,
                        gamepadConnected = connectedGamepad != null,
                        onOpenMenu = {
                            showMenuSheet = true
                            viewModel.setPaused(true)
                        },
                        onEditControls = {
                            isEditingControls = true
                            viewModel.setPaused(true)
                        }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.12f)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AtariScreenViewport(
                            viewModel = viewModel,
                            settings = settings,
                            isLoading = uiState.isLoading,
                            errorMessage = uiState.errorMessage,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (editShowExtra) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 28.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MiniConsolePill(
                                label = "SELECT",
                                opacity = editOpacity,
                                onClick = { viewModel.triggerQuickAction(isReset = false) },
                                modifier = Modifier.testTag("virtual_select_button")
                            )
                            Spacer(Modifier.width(16.dp))
                            MiniConsolePill(
                                label = "RESET",
                                opacity = editOpacity,
                                onClick = { viewModel.triggerQuickAction(isReset = true) },
                                modifier = Modifier.testTag("virtual_reset_button")
                            )
                        }
                    }

                    // Physical LTR guarantees D-Pad on Left, A/B on Right unless editSwap is toggled
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.92f)
                                .padding(horizontal = 18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!editSwap) {
                                VirtualDPad(
                                    scale = editScale,
                                    opacity = editOpacity,
                                    offsetX = dpadOffX,
                                    offsetY = dpadOffY,
                                    isEditMode = isEditingControls,
                                    activeDir = activeDir,
                                    onDragOffset = { dx, dy -> dpadOffX += dx; dpadOffY += dy },
                                    onDirectionChanged = onDirChange
                                )
                                VirtualActionButtons(
                                    scale = editScale,
                                    opacity = editOpacity,
                                    groupOffsetX = abOffX,
                                    groupOffsetY = abOffY,
                                    aOffsetX = aOffX,
                                    aOffsetY = aOffY,
                                    bOffsetX = bOffX,
                                    bOffsetY = bOffY,
                                    activeA = activeA,
                                    activeB = activeB,
                                    isEditMode = isEditingControls,
                                    onDragA = { dx, dy -> aOffX += dx; aOffY += dy },
                                    onDragB = { dx, dy -> bOffX += dx; bOffY += dy },
                                    onAChanged = onAChange,
                                    onBChanged = onBChange
                                )
                            } else {
                                VirtualActionButtons(
                                    scale = editScale,
                                    opacity = editOpacity,
                                    groupOffsetX = abOffX,
                                    groupOffsetY = abOffY,
                                    aOffsetX = aOffX,
                                    aOffsetY = aOffY,
                                    bOffsetX = bOffX,
                                    bOffsetY = bOffY,
                                    activeA = activeA,
                                    activeB = activeB,
                                    isEditMode = isEditingControls,
                                    onDragA = { dx, dy -> aOffX += dx; aOffY += dy },
                                    onDragB = { dx, dy -> bOffX += dx; bOffY += dy },
                                    onAChanged = onAChange,
                                    onBChanged = onBChange
                                )
                                VirtualDPad(
                                    scale = editScale,
                                    opacity = editOpacity,
                                    offsetX = dpadOffX,
                                    offsetY = dpadOffY,
                                    isEditMode = isEditingControls,
                                    activeDir = activeDir,
                                    onDragOffset = { dx, dy -> dpadOffX += dx; dpadOffY += dy },
                                    onDirectionChanged = onDirChange
                                )
                            }
                        }
                    }

                    PlayBottomBar(
                        speedMultiplier = uiState.speedMultiplier,
                        isRecording = uiState.isRecordingClip,
                        isRewinding = uiState.isRewinding,
                        onRecordOrSnap = { viewModel.toggleRecordingOrCaptureScreenshot() },
                        onCycleSpeed = { viewModel.cycleSpeed() },
                        onRewind = { viewModel.rewindBurst() },
                        onQuickSave = { viewModel.saveToSlot(1) }
                    )
                }
            }

            // Touch Controls Customization Floating Panel
            if (isEditingControls) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 54.dp, start = 14.dp, end = 14.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xF01C1713))
                        .border(1.5.dp, colors.orange, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                        .testTag("controls_editor_panel")
                ) {
                    Text(
                        text = strings.controlsEditHint,
                        fontSize = 12.sp,
                        color = colors.cream
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${strings.scaleLabel}: ${(editScale * 100).roundToInt()}%",
                        fontSize = 12.sp,
                        color = colors.amber
                    )
                    Slider(
                        value = editScale,
                        onValueChange = { editScale = it },
                        valueRange = 0.65f..1.45f,
                        colors = SliderDefaults.colors(thumbColor = colors.orange, activeTrackColor = colors.orange)
                    )
                    Text(
                        text = "${strings.opacityLabel}: ${(editOpacity * 100).roundToInt()}%",
                        fontSize = 12.sp,
                        color = colors.amber
                    )
                    Slider(
                        value = editOpacity,
                        onValueChange = { editOpacity = it },
                        valueRange = 0.25f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = colors.orange, activeTrackColor = colors.orange)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = editSwap,
                            onClick = { editSwap = !editSwap },
                            label = { Text(strings.swapSides, fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.orange,
                                selectedLabelColor = Color.White,
                                containerColor = colors.card,
                                labelColor = colors.cream
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = editShowExtra,
                            onClick = { editShowExtra = !editShowExtra },
                            label = { Text(strings.extraButtonsLabel, fontSize = 11.sp, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.amber,
                                selectedLabelColor = AtariEmblemDark,
                                containerColor = colors.card,
                                labelColor = colors.cream
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = {
                            editScale = 1.0f; editOpacity = 0.85f
                            dpadOffX = 0f; dpadOffY = 0f
                            abOffX = 0f; abOffY = 0f
                            aOffX = 0f; aOffY = 0f
                            bOffX = 0f; bOffY = 0f
                            editSwap = false; editShowExtra = true
                            onResetTouchLayout()
                        }) {
                            Text(strings.resetLayout, color = colors.muted)
                        }
                        Button(
                            onClick = {
                                onSaveTouchLayout(
                                    editScale, editOpacity,
                                    dpadOffX, dpadOffY,
                                    abOffX, abOffY,
                                    editSwap, editShowExtra,
                                    aOffX, aOffY,
                                    bOffX, bOffY
                                )
                                isEditingControls = false
                                viewModel.setPaused(false)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.orange),
                            modifier = Modifier.testTag("controls_editor_done")
                        ) {
                            Text(strings.done, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 68.dp)
        )

        if (showMenuSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showMenuSheet = false
                    viewModel.setPaused(false)
                },
                containerColor = colors.surface
            ) {
                InGameMenuContent(
                    gameTitle = if (settings.isArabic) uiState.game?.nameAr ?: "" else uiState.game?.nameEn ?: "",
                    connectedGamepad = connectedGamepad,
                    settings = settings,
                    slots = slots,
                    strings = strings,
                    onResume = {
                        showMenuSheet = false
                        viewModel.setPaused(false)
                    },
                    onRestart = {
                        showMenuSheet = false
                        viewModel.restartGame()
                    },
                    onSaveSlot = { idx -> viewModel.saveToSlot(idx) },
                    onLoadSlot = { idx ->
                        showMenuSheet = false
                        viewModel.setPaused(false)
                        viewModel.loadFromSlot(idx)
                    },
                    onSelectFilter = onSetFilterMode,
                    onSelectAspect = onSetAspectRatio,
                    onEditControls = {
                        showMenuSheet = false
                        isEditingControls = true
                        viewModel.setPaused(true)
                    },
                    onScreenshot = {
                        showMenuSheet = false
                        viewModel.setPaused(false)
                        viewModel.captureScreenshot()
                    },
                    onToggleMute = onToggleMute,
                    onExit = {
                        showMenuSheet = false
                        exitAction()
                    }
                )
            }
        }
    }
}

private data class QuadDir(
    val up: Boolean = false,
    val down: Boolean = false,
    val left: Boolean = false,
    val right: Boolean = false
)

@Composable
private fun PlayTopBar(
    fps: Int,
    score: Int,
    speedMultiplier: Float,
    isRecording: Boolean,
    gamepadConnected: Boolean,
    onOpenMenu: () -> Unit,
    onEditControls: () -> Unit
) {
    val colors = LocalAtariColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onOpenMenu,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("play_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = "Menu",
                    tint = colors.amber
                )
            }
            IconButton(
                onClick = onEditControls,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("play_customize_controls_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "Customize Touch Controls",
                    tint = colors.cream
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (gamepadConnected) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1F4D2E))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .testTag("gamepad_connected_badge")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SportsEsports,
                        contentDescription = "Gamepad Connected",
                        tint = Color(0xFF66EE88),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            if (isRecording) {
                Icon(
                    imageVector = Icons.Rounded.FiberManualRecord,
                    contentDescription = null,
                    tint = colors.orange,
                    modifier = Modifier.size(12.dp)
                )
            }
            if (speedMultiplier != 1.0f) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.orange)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${speedMultiplier}x",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            if (score > 0) {
                Text(
                    text = score.toString(),
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.amber
                )
            }
            Text(
                text = "FPS $fps",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 12.sp,
                color = colors.muted
            )
        }
    }
}

@Composable
private fun AtariScreenViewport(
    viewModel: EmulatorViewModel,
    settings: UserSettings,
    isLoading: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    val colors = LocalAtariColors.current
    val ratio = settings.aspectRatio.ratio
    val frameTick = viewModel.frameTick.intValue
    val frameImg = viewModel.currentFrameImage.value

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val screenBoxModifier = if (ratio != null && maxWidth > 0.dp && maxHeight > 0.dp) {
            val containerRatio = maxWidth / maxHeight
            if (containerRatio > ratio) {
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(ratio)
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio)
            }
        } else {
            Modifier.fillMaxSize()
        }

        Box(
            modifier = screenBoxModifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black)
                .border(1.dp, colors.divider, RoundedCornerShape(14.dp))
                .testTag("atari_screen_canvas"),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularProgressIndicator(color = colors.orange)
                errorMessage != null -> Text(
                    text = errorMessage,
                    color = colors.orange,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
                else -> {
                    val filterQuality = when (settings.filterMode) {
                        ScreenFilterMode.SMOOTH -> FilterQuality.Medium
                        else -> FilterQuality.None
                    }
                    val showScanlines = settings.scanlines ||
                        settings.filterMode == ScreenFilterMode.SCANLINES ||
                        settings.filterMode == ScreenFilterMode.CRT
                    val showCrtVignette = settings.crtFilter || settings.filterMode == ScreenFilterMode.CRT

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Consume frameTick to redraw reliably each frame
                        val _tick = frameTick
                        if (_tick >= 0 && frameImg != null) {
                            drawImage(
                                image = frameImg,
                                srcOffset = IntOffset.Zero,
                                srcSize = IntSize(frameImg.width, frameImg.height),
                                dstOffset = IntOffset.Zero,
                                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                                filterQuality = filterQuality
                            )
                        }

                        if (showScanlines) {
                            val step = (size.height / 96f).coerceAtLeast(3f)
                            var y = 0f
                            while (y < size.height) {
                                drawLine(
                                    color = Color(0x30000000),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1.2f
                                )
                                y += step
                            }
                        }

                        if (showCrtVignette) {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color(0x44000000)
                                    ),
                                    radius = size.maxDimension * 0.76f
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniConsolePill(
    label: String,
    opacity: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAtariColors.current
    Box(
        modifier = modifier
            .alpha(opacity)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC241E18))
            .border(1.dp, colors.divider, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = colors.muted
        )
    }
}

@Composable
private fun VirtualDPad(
    scale: Float,
    opacity: Float,
    offsetX: Float,
    offsetY: Float,
    isEditMode: Boolean,
    activeDir: QuadDir,
    onDragOffset: (Float, Float) -> Unit,
    onDirectionChanged: (Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    val colors = LocalAtariColors.current
    val padSize = (132 * scale).dp

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(padSize)
            .alpha(opacity)
            .clip(CircleShape)
            .background(Color(0xCC241E18))
            .then(
                if (isEditMode) {
                    Modifier
                        .border(2.dp, colors.amber, CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onDragOffset(dragAmount.x, dragAmount.y)
                            }
                        }
                } else {
                    Modifier
                        .border(1.dp, colors.divider, CircleShape)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                fun updateFromPos(pos: Offset) {
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    val dx = pos.x - cx
                                    val dy = pos.y - cy
                                    val deadzone = size.width * 0.14f
                                    onDirectionChanged(
                                        dy < -deadzone,
                                        dy > deadzone,
                                        dx < -deadzone,
                                        dx > deadzone
                                    )
                                }
                                updateFromPos(down.position)
                                do {
                                    val event = awaitPointerEvent()
                                    val pointer = event.changes.firstOrNull { it.pressed }
                                    if (pointer != null) {
                                        updateFromPos(pointer.position)
                                    }
                                } while (event.changes.any { it.pressed })
                                onDirectionChanged(false, false, false, false)
                            }
                        }
                }
            )
            .testTag("virtual_dpad"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowUp,
            contentDescription = null,
            tint = if (activeDir.up) colors.orange else colors.muted,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp).size((26 * scale).dp)
        )
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            tint = if (activeDir.down) colors.orange else colors.muted,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).size((26 * scale).dp)
        )
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowLeft,
            contentDescription = null,
            tint = if (activeDir.left) colors.orange else colors.muted,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp).size((26 * scale).dp)
        )
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = if (activeDir.right) colors.orange else colors.muted,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).size((26 * scale).dp)
        )
        Icon(
            imageVector = Icons.Rounded.Gamepad,
            contentDescription = "D-Pad",
            tint = colors.muted.copy(alpha = 0.6f),
            modifier = Modifier.size((24 * scale).dp)
        )
    }
}

@Composable
private fun VirtualActionButtons(
    scale: Float,
    opacity: Float,
    groupOffsetX: Float,
    groupOffsetY: Float,
    aOffsetX: Float,
    aOffsetY: Float,
    bOffsetX: Float,
    bOffsetY: Float,
    activeA: Boolean,
    activeB: Boolean,
    isEditMode: Boolean,
    onDragA: (Float, Float) -> Unit,
    onDragB: (Float, Float) -> Unit,
    onAChanged: (Boolean) -> Unit,
    onBChanged: (Boolean) -> Unit
) {
    val colors = LocalAtariColors.current
    val btnSize = (64 * scale).dp

    Row(
        modifier = Modifier
            .offset { IntOffset(groupOffsetX.roundToInt(), groupOffsetY.roundToInt()) }
            .alpha(opacity),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // B Button (Amber, independently draggable in edit mode)
        Box(
            modifier = Modifier
                .offset { IntOffset(bOffsetX.roundToInt(), bOffsetY.roundToInt()) }
                .padding(top = 26.dp)
                .size(btnSize)
                .clip(CircleShape)
                .background(if (activeB) colors.cream else colors.amber)
                .then(
                    if (isEditMode) {
                        Modifier
                            .border(2.dp, Color.White, CircleShape)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    onDragB(dragAmount.x, dragAmount.y)
                                }
                            }
                    } else {
                        Modifier.pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                onBChanged(true)
                                do {
                                    val ev = awaitPointerEvent()
                                } while (ev.changes.any { it.pressed })
                                onBChanged(false)
                            }
                        }
                    }
                )
                .testTag("virtual_button_b"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "B",
                fontWeight = FontWeight.Bold,
                fontSize = (22 * scale).sp,
                color = AtariEmblemDark
            )
        }

        // A Button (Burnt Orange, independently draggable in edit mode)
        Box(
            modifier = Modifier
                .offset { IntOffset(aOffsetX.roundToInt(), aOffsetY.roundToInt()) }
                .padding(bottom = 18.dp)
                .size(btnSize)
                .clip(CircleShape)
                .background(if (activeA) colors.amber else colors.orange)
                .then(
                    if (isEditMode) {
                        Modifier
                            .border(2.dp, Color.White, CircleShape)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    onDragA(dragAmount.x, dragAmount.y)
                                }
                            }
                    } else {
                        Modifier.pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                onAChanged(true)
                                do {
                                    val ev = awaitPointerEvent()
                                } while (ev.changes.any { it.pressed })
                                onAChanged(false)
                            }
                        }
                    }
                )
                .testTag("virtual_button_a"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "A",
                fontWeight = FontWeight.Bold,
                fontSize = (22 * scale).sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun PlayBottomBar(
    speedMultiplier: Float,
    isRecording: Boolean,
    isRewinding: Boolean,
    onRecordOrSnap: () -> Unit,
    onCycleSpeed: () -> Unit,
    onRewind: () -> Unit,
    onQuickSave: () -> Unit
) {
    val colors = LocalAtariColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = colors.divider, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bg)
                .padding(vertical = 8.dp, horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onRecordOrSnap,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("play_record_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.FiberManualRecord,
                    contentDescription = "Record / Capture",
                    tint = if (isRecording) colors.orange else colors.muted,
                    modifier = Modifier.size(22.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (speedMultiplier != 1.0f) colors.orange else colors.amber)
                    .clickable(onClick = onCycleSpeed)
                    .testTag("play_speed_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.FastForward,
                    contentDescription = "Fast Forward",
                    tint = if (speedMultiplier != 1.0f) Color.White else AtariEmblemDark,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = onRewind,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("play_rewind_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = "Rewind",
                    tint = if (isRewinding) colors.orange else colors.muted,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = onQuickSave,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("play_quicksave_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Save,
                    contentDescription = "Quick Save",
                    tint = colors.cream,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun InGameMenuContent(
    gameTitle: String,
    connectedGamepad: String?,
    settings: UserSettings,
    slots: List<SaveSlotEntity>,
    strings: AppStrings,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onSaveSlot: (Int) -> Unit,
    onLoadSlot: (Int) -> Unit,
    onSelectFilter: (ScreenFilterMode) -> Unit,
    onSelectAspect: (AspectRatioMode) -> Unit,
    onEditControls: () -> Unit,
    onScreenshot: () -> Unit,
    onToggleMute: () -> Unit,
    onExit: () -> Unit
) {
    val colors = LocalAtariColors.current
    val slotsMap = slots.associateBy { it.slotIndex }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = gameTitle,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = colors.cream
        )

        // Gamepad Status Banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.card)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Rounded.SportsEsports,
                    contentDescription = null,
                    tint = if (connectedGamepad != null) Color(0xFF66EE88) else colors.amber,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (connectedGamepad != null) "${strings.gamepadConnected}: $connectedGamepad" else strings.gamepadNone,
                    color = if (connectedGamepad != null) Color(0xFF66EE88) else colors.cream,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = strings.gamepadMappingSummary,
                color = colors.muted,
                fontSize = 11.sp
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onResume,
                colors = ButtonDefaults.buttonColors(containerColor = colors.orange),
                modifier = Modifier.weight(1f).testTag("menu_resume_button")
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(4.dp))
                Text(strings.resumeGame, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = colors.card),
                modifier = Modifier.weight(1f).testTag("menu_restart_button")
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, tint = colors.amber)
                Spacer(Modifier.width(4.dp))
                Text(strings.restartGame, color = colors.cream)
            }
        }

        for (idx in 1..3) {
            val hasData = slotsMap.containsKey(idx)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.card)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.saveSlot} $idx" + if (hasData) " ✓" else "",
                    color = colors.cream,
                    fontSize = 14.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onSaveSlot(idx) }) {
                        Text(strings.saveQuick, color = colors.orange, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { onLoadSlot(idx) },
                        enabled = hasData
                    ) {
                        Text(strings.loadQuick, color = if (hasData) colors.amber else colors.muted)
                    }
                }
            }
        }

        Text(strings.screenFilter, color = colors.muted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ScreenFilterMode.entries.forEach { mode ->
                val sel = settings.filterMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (sel) colors.orange else colors.card)
                        .clickable { onSelectFilter(mode) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (settings.isArabic) mode.labelAr else mode.labelEn,
                        fontSize = 11.sp,
                        color = if (sel) Color.White else colors.cream,
                        maxLines = 1
                    )
                }
            }
        }

        Text(strings.aspectRatio, color = colors.muted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            AspectRatioMode.entries.forEach { mode ->
                val sel = settings.aspectRatio == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (sel) colors.orange else colors.card)
                        .clickable { onSelectAspect(mode) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (settings.isArabic) mode.labelAr else mode.labelEn,
                        fontSize = 12.sp,
                        fontFamily = JetBrainsMonoFamily,
                        color = if (sel) Color.White else colors.cream
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onEditControls,
                colors = ButtonDefaults.buttonColors(containerColor = colors.card),
                modifier = Modifier.weight(1f).testTag("menu_edit_controls_button")
            ) {
                Icon(Icons.Rounded.Tune, contentDescription = null, tint = colors.amber)
                Spacer(Modifier.width(4.dp))
                Text(strings.controlsOption, color = colors.cream, fontSize = 12.sp)
            }
            Button(
                onClick = onScreenshot,
                colors = ButtonDefaults.buttonColors(containerColor = colors.card),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = colors.amber)
                Spacer(Modifier.width(4.dp))
                Text(strings.screenshot, color = colors.cream, fontSize = 12.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onToggleMute,
                colors = ButtonDefaults.buttonColors(containerColor = colors.card),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (settings.isMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                    contentDescription = null,
                    tint = colors.amber
                )
                Spacer(Modifier.width(4.dp))
                Text(strings.muteAudio, color = colors.cream, fontSize = 12.sp)
            }
            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF421C14)),
                modifier = Modifier.weight(1f).testTag("menu_exit_button")
            ) {
                Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(4.dp))
                Text(strings.exitGame, color = Color.White, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
