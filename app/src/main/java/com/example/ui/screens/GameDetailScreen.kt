package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import com.example.data.model.SaveSlotEntity
import com.example.ui.components.GameCoverArt
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalAtariColors
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GameDetailScreen(
    game: GameEntity?,
    saveSlots: List<SaveSlotEntity>,
    isArabic: Boolean,
    strings: AppStrings,
    onBack: () -> Unit,
    onToggleFavorite: (GameEntity) -> Unit,
    onPlay: (Int?) -> Unit,
    onDeleteSlot: (Int) -> Unit
) {
    val colors = LocalAtariColors.current
    BackHandler(onBack = onBack)

    if (game == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bg),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "...", color = colors.muted)
        }
        return
    }

    val slotsByIndex = saveSlots.associateBy { it.slotIndex }

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
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top bar: Back + Favorite heart
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("detail_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.amber
                    )
                }

                IconButton(
                    onClick = { onToggleFavorite(game) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("detail_favorite_button")
                ) {
                    Icon(
                        imageVector = if (game.isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = strings.navFavorites,
                        tint = colors.orange
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Large Hero Cover Card (matches style board)
            GameCoverArt(
                coverStyle = game.coverStyle,
                coverChar = game.coverChar,
                paletteIndex = game.paletteIndex,
                cornerRadius = 18.dp,
                letterSize = 72.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.48f)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Title (Arabic + English subtitle)
            Text(
                text = if (isArabic) game.nameAr else game.nameEn,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = colors.cream
            )
            if (isArabic && game.nameEn != game.nameAr) {
                Text(
                    text = game.nameEn,
                    fontSize = 14.sp,
                    color = colors.muted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata Pill Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetadataPill(text = if (isArabic) "أتاري ${game.system}" else "Atari ${game.system}")
                MetadataPill(text = game.year.toString(), mono = true)
                MetadataPill(text = game.category)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Play CTA Button
            Button(
                onClick = { onPlay(null) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.orange),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("detail_play_button")
            ) {
                Text(
                    text = strings.playNow,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Auto-Save Slot (if present)
            val autoSlot = slotsByIndex[0]
            if (autoSlot != null) {
                SaveSlotRow(
                    title = strings.autoSaveSlot,
                    slot = autoSlot,
                    strings = strings,
                    onClick = { onPlay(0) },
                    onDelete = { onDeleteSlot(0) },
                    modifier = Modifier.testTag("save_slot_0")
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Save Slots 1 & 2 (& 3)
            for (slotIdx in 1..3) {
                val slot = slotsByIndex[slotIdx]
                SaveSlotRow(
                    title = "${strings.saveSlot} $slotIdx",
                    slot = slot,
                    strings = strings,
                    onClick = { onPlay(if (slot != null) slotIdx else null) },
                    onDelete = if (slot != null) { { onDeleteSlot(slotIdx) } } else null,
                    modifier = Modifier.testTag("save_slot_$slotIdx")
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // High Score Row (matches style board)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.card)
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.highScore,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.cream
                )
                Text(
                    text = NumberFormat.getIntegerInstance(Locale.US).format(game.highScore),
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.amber
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.card)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.playCountLabel,
                    fontSize = 15.sp,
                    color = colors.cream
                )
                Text(
                    text = game.playCount.toString(),
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.amber
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MetadataPill(text: String, mono: Boolean = false) {
    val colors = LocalAtariColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.chip)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontFamily = if (mono) JetBrainsMonoFamily else LocalAtariColors.current.let { com.example.ui.theme.CairoFontFamily },
            fontSize = 13.sp,
            color = colors.cream
        )
    }
}

@Composable
private fun SaveSlotRow(
    title: String,
    slot: SaveSlotEntity?,
    strings: AppStrings,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = LocalAtariColors.current
    val timeLabel = slot?.let {
        SimpleDateFormat("HH:mm · dd/MM", Locale.getDefault()).format(Date(it.updatedAt))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.cream
            )
            Text(
                text = if (slot != null) "${strings.savedAtLabel} · $timeLabel" else strings.emptySlot,
                fontSize = 12.sp,
                color = if (slot != null) colors.amber else colors.muted
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = strings.delete,
                        tint = colors.muted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Icon(
                imageVector = Icons.Rounded.Download,
                contentDescription = null,
                tint = if (slot != null) colors.amber else colors.muted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
