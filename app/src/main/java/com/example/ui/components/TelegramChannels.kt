package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalAtariColors

data class TelegramChannelInfo(
    val id: String,
    val handle: String,
    val titleAr: String,
    val titleEn: String,
    val url: String
)

val ATARI_TELEGRAM_CHANNELS = listOf(
    TelegramChannelInfo(
        id = "pixels_mods",
        handle = "@pixels_x_mods_apk",
        titleAr = "قناة التطبيقات والألعاب المعدلة",
        titleEn = "Pixels X Mods APK",
        url = "https://t.me/pixels_x_mods_apk"
    ),
    TelegramChannelInfo(
        id = "qwe_bot",
        handle = "@qweqwe2019bot2bot",
        titleAr = "قناة البوتات والتحديثات",
        titleEn = "Bots & Updates Channel",
        url = "https://t.me/qweqwe2019bot2bot"
    ),
    TelegramChannelInfo(
        id = "abu_saleh",
        handle = "@Abu2Saleh",
        titleAr = "قناة أبو صالح",
        titleEn = "Abu2Saleh Official",
        url = "https://t.me/Abu2Saleh"
    )
)

fun openTelegramUrl(context: Context, url: String): Boolean {
    return runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}

@Composable
fun TelegramSubscribeDialog(
    isArabic: Boolean,
    strings: AppStrings,
    showDontAskOption: Boolean = true,
    onDismissSession: () -> Unit,
    onDontShowAgain: () -> Unit
) {
    val colors = LocalAtariColors.current
    val context = LocalContext.current
    val visited = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismissSession,
        containerColor = colors.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colors.orange),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = strings.telegramDialogTitle,
                    color = colors.cream,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.telegramDialogBody,
                    color = colors.muted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )

                ATARI_TELEGRAM_CHANNELS.forEachIndexed { idx, channel ->
                    val isVisited = visited.contains(channel.id)
                    TelegramChannelCard(
                        channel = channel,
                        isArabic = isArabic,
                        actionLabel = if (isVisited) strings.subscribedLabel else strings.subscribeNow,
                        isVisited = isVisited,
                        onClick = {
                            if (!isVisited) visited.add(channel.id)
                            openTelegramUrl(context, channel.url)
                        },
                        modifier = Modifier.testTag("telegram_dialog_channel_$idx")
                    )
                }

                if (showDontAskOption) {
                    TextButton(
                        onClick = onDontShowAgain,
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("telegram_dont_show_again")
                    ) {
                        Text(
                            text = strings.dontShowAgain,
                            color = colors.muted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismissSession,
                colors = ButtonDefaults.buttonColors(containerColor = colors.orange),
                modifier = Modifier.testTag("telegram_dialog_close")
            ) {
                Text(
                    text = strings.continueToApp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
fun TelegramChannelCard(
    channel: TelegramChannelInfo,
    isArabic: Boolean,
    actionLabel: String,
    isVisited: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAtariColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .border(
                width = 1.dp,
                color = if (isVisited) colors.amber else colors.divider,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isArabic) channel.titleAr else channel.titleEn,
                color = colors.cream,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = channel.handle,
                color = colors.amber,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isVisited) Color(0xFF1F4D2E) else colors.orange)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isVisited) Icons.Rounded.CheckCircle else Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
                tint = if (isVisited) Color(0xFF66EE88) else Color.White,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = actionLabel,
                color = if (isVisited) Color(0xFF66EE88) else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
