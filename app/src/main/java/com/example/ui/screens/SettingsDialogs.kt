package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameEntity
import com.example.data.repository.StorageStats
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalAtariColors
import kotlin.math.roundToInt

@Composable
fun VolumeDialog(
    volume: Int,
    isMuted: Boolean,
    strings: AppStrings,
    onVolumeChange: (Int) -> Unit,
    onToggleMute: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAtariColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text(strings.soundVolume, color = colors.cream, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.muteAudio, color = colors.cream)
                    Switch(
                        checked = isMuted,
                        onCheckedChange = { onToggleMute() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colors.orange
                        )
                    )
                }
                Text(
                    text = "${if (isMuted) 0 else volume}%",
                    fontFamily = JetBrainsMonoFamily,
                    color = colors.amber,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = volume.toFloat(),
                    onValueChange = { onVolumeChange(it.roundToInt()) },
                    valueRange = 0f..100f,
                    enabled = !isMuted,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.orange,
                        activeTrackColor = colors.orange
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.done, color = colors.orange, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun ImportGamesDialog(
    strings: AppStrings,
    onImportFiles: () -> Unit,
    onImportFolder: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAtariColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text(strings.addGames, color = colors.cream, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = ".a26 · .bin · .a52 · .a78 · .lnx · .j64",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 12.sp,
                    color = colors.muted
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.card)
                        .clickable(onClick = onImportFiles)
                        .padding(14.dp)
                        .testTag("import_files_option"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.InsertDriveFile, contentDescription = null, tint = colors.orange)
                    Spacer(Modifier.width(12.dp))
                    Text(strings.importFiles, color = colors.cream, fontWeight = FontWeight.Medium)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.card)
                        .clickable(onClick = onImportFolder)
                        .padding(14.dp)
                        .testTag("import_folder_option"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, tint = colors.amber)
                    Spacer(Modifier.width(12.dp))
                    Text(strings.importFolder, color = colors.cream, fontWeight = FontWeight.Medium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = colors.muted)
            }
        }
    )
}

@Composable
fun StorageManagerDialog(
    stats: StorageStats,
    importedGames: List<GameEntity>,
    strings: AppStrings,
    onDeleteGame: (GameEntity) -> Unit,
    onClearAllSaves: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAtariColors.current
    val totalMb = (stats.importedRomsBytes + stats.databaseBytes) / (1024f * 1024f)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text(strings.storageManager, color = colors.cream, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatLine(strings.importedGamesCount, stats.importedGamesCount.toString())
                StatLine(strings.savesCountLabel, stats.saveSlotsCount.toString())
                StatLine(strings.storageUsedLabel, String.format("%.2f MB", totalMb))

                if (importedGames.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(importedGames, key = { it.id }) { g ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.card)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(g.nameEn, color = colors.cream, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onDeleteGame(g) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.delete, tint = colors.orange)
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onClearAllSaves,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.card),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.clearAllSaves, color = colors.orange, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.done, color = colors.amber, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun StatLine(label: String, value: String) {
    val colors = LocalAtariColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = colors.muted, fontSize = 13.sp)
        Text(value, color = colors.amber, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun BackupRestoreDialog(
    strings: AppStrings,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAtariColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text(strings.backupRestore, color = colors.cream, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.card)
                        .clickable(onClick = onExport)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Upload, contentDescription = null, tint = colors.orange)
                    Spacer(Modifier.width(12.dp))
                    Text(strings.exportBackup, color = colors.cream, fontWeight = FontWeight.Medium)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.card)
                        .clickable(onClick = onImport)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, tint = colors.amber)
                    Spacer(Modifier.width(12.dp))
                    Text(strings.importBackup, color = colors.cream, fontWeight = FontWeight.Medium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = colors.muted)
            }
        }
    )
}

@Composable
fun AboutPrivacyDialog(
    strings: AppStrings,
    onDismiss: () -> Unit
) {
    val colors = LocalAtariColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text(strings.aboutApp, color = colors.cream, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${strings.appTitle} v1.0",
                    color = colors.amber,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                )
                Text(strings.aboutBody, color = colors.cream, fontSize = 13.sp, lineHeight = 20.sp)
                Spacer(Modifier.height(4.dp))
                Text(strings.privacyPolicy, color = colors.orange, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(strings.privacyBody, color = colors.muted, fontSize = 13.sp, lineHeight = 20.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.done, color = colors.orange, fontWeight = FontWeight.Bold)
            }
        }
    )
}
