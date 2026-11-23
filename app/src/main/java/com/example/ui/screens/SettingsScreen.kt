package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AspectRatioMode
import com.example.data.model.GameEntity
import com.example.data.model.ScreenFilterMode
import com.example.data.model.ThemeMode
import com.example.data.model.UserSettings
import com.example.data.repository.StorageStats
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalAtariColors

@Composable
fun SettingsScreen(
    settings: UserSettings,
    strings: AppStrings,
    storageStats: StorageStats,
    importedGames: List<GameEntity>,
    onBack: () -> Unit,
    onToggleCrt: (Boolean) -> Unit,
    onToggleScanlines: (Boolean) -> Unit,
    onToggleVibration: (Boolean) -> Unit,
    onToggleAutoSave: (Boolean) -> Unit,
    onCycleAspectRatio: () -> Unit,
    onCycleScreenFilter: () -> Unit,
    onCycleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
    onVolumeChange: (Int) -> Unit,
    onToggleMute: () -> Unit,
    onToggleSwapControls: (Boolean) -> Unit,
    connectedGamepadName: String?,
    onResetTouchLayout: () -> Unit,
    onOpenImportRomFiles: () -> Unit,
    onOpenImportRomFolder: () -> Unit,
    onDeleteImportedGame: (GameEntity) -> Unit,
    onClearAllSaves: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onRefreshStorage: () -> Unit
) {
    val colors = LocalAtariColors.current
    BackHandler(onBack = onBack)

    var showVolumeDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Header (matches style board screen 4!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.amber
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = strings.settingsTitle,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.cream
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = colors.amber,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Switch Rows (exact match to style board!)
            SettingSwitchRow(
                label = strings.crtFilter,
                checked = settings.crtFilter,
                onCheckedChange = onToggleCrt,
                modifier = Modifier.testTag("setting_crt_switch")
            )
            SettingSwitchRow(
                label = strings.scanlines,
                checked = settings.scanlines,
                onCheckedChange = onToggleScanlines,
                modifier = Modifier.testTag("setting_scanlines_switch")
            )
            SettingSwitchRow(
                label = strings.vibration,
                checked = settings.vibration,
                onCheckedChange = onToggleVibration,
                modifier = Modifier.testTag("setting_vibration_switch")
            )
            SettingSwitchRow(
                label = strings.autoSave,
                checked = settings.autoSave,
                onCheckedChange = onToggleAutoSave,
                modifier = Modifier.testTag("setting_autosave_switch")
            )
            SettingSwitchRow(
                label = strings.swapSides,
                checked = settings.swapControls,
                onCheckedChange = onToggleSwapControls,
                modifier = Modifier.testTag("setting_swap_controls_switch")
            )

            // Gamepad status & mapping card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.card)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (connectedGamepadName != null) "${strings.gamepadConnected}: $connectedGamepadName" else strings.gamepadHelp,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (connectedGamepadName != null) Color(0xFF66EE88) else colors.amber
                )
                Text(
                    text = strings.gamepadMappingSummary,
                    fontSize = 12.sp,
                    color = colors.muted
                )
            }

            // Value Rows (amber readout on end)
            SettingValueRow(
                label = strings.aspectRatio,
                value = if (settings.isArabic) settings.aspectRatio.labelAr else settings.aspectRatio.labelEn,
                mono = true,
                onClick = onCycleAspectRatio,
                modifier = Modifier.testTag("setting_aspect_ratio")
            )
            SettingValueRow(
                label = strings.soundVolume,
                value = if (settings.isMuted) "0%" else "${settings.volumePercent}%",
                mono = true,
                onClick = { showVolumeDialog = true },
                modifier = Modifier.testTag("setting_volume")
            )
            SettingValueRow(
                label = strings.screenFilter,
                value = if (settings.isArabic) settings.filterMode.labelAr else settings.filterMode.labelEn,
                mono = false,
                onClick = onCycleScreenFilter,
                modifier = Modifier.testTag("setting_screen_filter")
            )
            SettingValueRow(
                label = strings.themeSetting,
                value = if (settings.isArabic) settings.themeMode.labelAr else settings.themeMode.labelEn,
                mono = false,
                onClick = onCycleTheme,
                modifier = Modifier.testTag("setting_theme")
            )
            SettingValueRow(
                label = strings.languageSetting,
                value = if (settings.isArabic) "العربية" else "English",
                mono = false,
                onClick = onToggleLanguage,
                modifier = Modifier.testTag("setting_language")
            )

            // Action Rows (amber icon on end)
            SettingActionRow(
                label = strings.addGames,
                icon = Icons.Rounded.Add,
                onClick = { showImportDialog = true },
                modifier = Modifier.testTag("setting_add_games")
            )
            SettingActionRow(
                label = strings.resetLayout,
                icon = Icons.Rounded.Tune,
                onClick = onResetTouchLayout,
                modifier = Modifier.testTag("setting_reset_layout")
            )
            SettingActionRow(
                label = strings.storageManager,
                icon = Icons.Rounded.Storage,
                onClick = {
                    onRefreshStorage()
                    showStorageDialog = true
                },
                modifier = Modifier.testTag("setting_storage")
            )
            SettingActionRow(
                label = strings.backupRestore,
                icon = Icons.Rounded.Backup,
                onClick = { showBackupDialog = true },
                modifier = Modifier.testTag("setting_backup")
            )
            SettingActionRow(
                label = strings.aboutApp,
                icon = Icons.Outlined.Info,
                onClick = { showAboutDialog = true },
                modifier = Modifier.testTag("setting_about")
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showVolumeDialog) {
        VolumeDialog(
            volume = settings.volumePercent,
            isMuted = settings.isMuted,
            strings = strings,
            onVolumeChange = onVolumeChange,
            onToggleMute = onToggleMute,
            onDismiss = { showVolumeDialog = false }
        )
    }

    if (showImportDialog) {
        ImportGamesDialog(
            strings = strings,
            onImportFiles = {
                showImportDialog = false
                onOpenImportRomFiles()
            },
            onImportFolder = {
                showImportDialog = false
                onOpenImportRomFolder()
            },
            onDismiss = { showImportDialog = false }
        )
    }

    if (showStorageDialog) {
        StorageManagerDialog(
            stats = storageStats,
            importedGames = importedGames,
            strings = strings,
            onDeleteGame = onDeleteImportedGame,
            onClearAllSaves = onClearAllSaves,
            onDismiss = { showStorageDialog = false }
        )
    }

    if (showBackupDialog) {
        BackupRestoreDialog(
            strings = strings,
            onExport = {
                showBackupDialog = false
                onExportBackup()
            },
            onImport = {
                showBackupDialog = false
                onImportBackup()
            },
            onDismiss = { showBackupDialog = false }
        )
    }

    if (showAboutDialog) {
        AboutPrivacyDialog(
            strings = strings,
            onDismiss = { showAboutDialog = false }
        )
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAtariColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = colors.cream
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.orange,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = colors.switchTrackOff,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun SettingValueRow(
    label: String,
    value: String,
    mono: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAtariColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = colors.cream
        )
        Text(
            text = value,
            fontFamily = if (mono) JetBrainsMonoFamily else com.example.ui.theme.CairoFontFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.amber
        )
    }
}

@Composable
private fun SettingActionRow(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAtariColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = colors.cream
        )
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = colors.amber,
            modifier = Modifier.size(22.dp)
        )
    }
}
