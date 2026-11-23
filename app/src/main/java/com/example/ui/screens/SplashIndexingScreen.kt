package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.IndexingState
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AtariEmblemDark
import com.example.ui.theme.AtariOrange
import com.example.ui.theme.AtariRust
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalAtariColors

@Composable
fun SplashIndexingScreen(
    indexingState: IndexingState,
    strings: AppStrings,
    onRetry: () -> Unit
) {
    val colors = LocalAtariColors.current
    val animatedProgress by animateFloatAsState(
        targetValue = indexingState.progress.coerceIn(0f, 1f),
        label = "indexing_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Branded retro arcade joystick medallion
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(colors.amber, AtariOrange, AtariRust)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(64.dp)) {
                    val w = size.width
                    val h = size.height
                    // Base
                    drawRoundRect(
                        color = AtariEmblemDark,
                        topLeft = Offset(w * 0.12f, h * 0.54f),
                        size = Size(w * 0.76f, h * 0.36f),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                    // Inner Amber Ring
                    drawCircle(
                        color = colors.amber,
                        radius = w * 0.15f,
                        center = Offset(w * 0.5f, h * 0.70f)
                    )
                    // Stem
                    drawRect(
                        color = AtariEmblemDark,
                        topLeft = Offset(w * 0.44f, h * 0.24f),
                        size = Size(w * 0.12f, h * 0.44f)
                    )
                    // Top Knob
                    drawCircle(
                        color = AtariEmblemDark,
                        radius = w * 0.13f,
                        center = Offset(w * 0.5f, h * 0.20f)
                    )
                    // Fire button
                    drawCircle(
                        color = AtariOrange,
                        radius = w * 0.065f,
                        center = Offset(w * 0.24f, h * 0.64f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = strings.appTitle,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = colors.amber
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = strings.offlineNotice,
                fontSize = 13.sp,
                color = colors.muted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (indexingState.error != null) {
                Text(
                    text = indexingState.error,
                    color = colors.orange,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.orange),
                    modifier = Modifier.testTag("retry_indexing_button")
                ) {
                    Text(text = "إعادة المحاولة", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = strings.indexingTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.cream
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = strings.indexingSubtitle,
                    fontSize = 12.sp,
                    color = colors.muted
                )
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .testTag("indexing_progress_bar"),
                    color = colors.orange,
                    trackColor = colors.card
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${indexingState.indexedCount} / ${indexingState.totalCount}",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 13.sp,
                    color = colors.amber
                )
            }
        }
    }
}
