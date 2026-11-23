package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AtariEmblemDark
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.CoverPalettes

/**
 * Renders the retro geometric cover art shown in the style board:
 * vibrant burnt orange / amber / terracotta surface with a bold dark silhouette.
 */
@Composable
fun GameCoverArt(
    coverStyle: String,
    coverChar: String,
    paletteIndex: Int,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    letterSize: TextUnit = 40.sp
) {
    val bgColor = CoverPalettes[paletteIndex.mod(CoverPalettes.size)]
    val emblemColor = AtariEmblemDark

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        if (coverStyle == "letter") {
            Text(
                text = coverChar.take(1).uppercase(),
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = letterSize,
                color = emblemColor
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val unit = minOf(size.width, size.height) * 0.24f

                when (coverStyle) {
                    "pong" -> drawPongEmblem(cx, cy, unit, emblemColor)
                    "castle" -> drawCastleEmblem(cx, cy, unit, emblemColor)
                    "jet" -> drawJetEmblem(cx, cy, unit, emblemColor)
                    "star" -> drawStarEmblem(cx, cy, unit, emblemColor)
                    "skull" -> drawSkullEmblem(cx, cy, unit, emblemColor)
                    "rocket" -> drawRocketEmblem(cx, cy, unit, emblemColor)
                    "brick" -> drawBrickEmblem(cx, cy, unit, emblemColor)
                    "car" -> drawCarEmblem(cx, cy, unit, emblemColor)
                    else -> drawStarEmblem(cx, cy, unit, emblemColor)
                }
            }
        }
    }
}

private fun DrawScope.drawPongEmblem(cx: Float, cy: Float, u: Float, color: Color) {
    val barW = u * 0.42f
    val barH = u * 1.45f
    drawRoundRect(
        color = color,
        topLeft = Offset(cx - u * 0.72f, cy - barH / 2f),
        size = Size(barW, barH),
        cornerRadius = CornerRadius(3f, 3f)
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(cx + u * 0.30f, cy - barH / 2f),
        size = Size(barW, barH),
        cornerRadius = CornerRadius(3f, 3f)
    )
}

private fun DrawScope.drawCastleEmblem(cx: Float, cy: Float, u: Float, color: Color) {
    val w = u * 1.3f
    val h = u * 1.5f
    val left = cx - w / 2f
    val top = cy - h / 2f
    // Battlements
    val toothW = w / 5f
    drawRect(color, Offset(left, top), Size(toothW, u * 0.35f))
    drawRect(color, Offset(left + toothW * 2f, top), Size(toothW, u * 0.35f))
    drawRect(color, Offset(left + toothW * 4f, top), Size(toothW, u * 0.35f))
    // Tower body
    drawRect(color, Offset(left + w * 0.08f, top + u * 0.3f), Size(w * 0.84f, h * 0.58f))
    // Base
    drawRect(color, Offset(left - w * 0.05f, top + h * 0.84f), Size(w * 1.1f, h * 0.16f))
}

private fun DrawScope.drawJetEmblem(cx: Float, cy: Float, u: Float, color: Color) {
    val path = Path().apply {
        moveTo(cx + u * 0.9f, cy)
        lineTo(cx - u * 0.1f, cy - u * 0.25f)
        lineTo(cx - u * 0.35f, cy - u * 0.9f)
        lineTo(cx - u * 0.6f, cy - u * 0.9f)
        lineTo(cx - u * 0.45f, cy - u * 0.22f)
        lineTo(cx - u * 0.9f, cy - u * 0.4f)
        lineTo(cx - u * 0.9f, cy + u * 0.4f)
        lineTo(cx - u * 0.45f, cy + u * 0.22f)
        lineTo(cx - u * 0.6f, cy + u * 0.9f)
        lineTo(cx - u * 0.35f, cy + u * 0.9f)
        lineTo(cx - u * 0.1f, cy + u * 0.25f)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawStarEmblem(cx: Float, cy: Float, u: Float, color: Color) {
    val path = Path().apply {
        moveTo(cx, cy - u * 0.95f)
        lineTo(cx + u * 0.25f, cy - u * 0.25f)
        lineTo(cx + u * 0.95f, cy)
        lineTo(cx + u * 0.25f, cy + u * 0.25f)
        lineTo(cx, cy + u * 0.95f)
        lineTo(cx - u * 0.25f, cy + u * 0.25f)
        lineTo(cx - u * 0.95f, cy)
        lineTo(cx - u * 0.25f, cy - u * 0.25f)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawSkullEmblem(cx: Float, cy: Float, u: Float, color: Color) {
    drawCircle(color, radius = u * 0.68f, center = Offset(cx, cy - u * 0.15f))
    drawRoundRect(
        color = color,
        topLeft = Offset(cx - u * 0.38f, cy + u * 0.25f),
        size = Size(u * 0.76f, u * 0.45f),
        cornerRadius = CornerRadius(4f, 4f)
    )
}

private fun DrawScope.drawRocketEmblem(cx: Float, cy: Float, u: Float, color: Color) {
    val path = Path().apply {
        moveTo(cx, cy - u * 0.95f)
        lineTo(cx + u * 0.42f, cy - u * 0.1f)
        lineTo(cx + u * 0.42f, cy + u * 0.55f)
        lineTo(cx + u * 0.78f, cy + u * 0.88f)
        lineTo(cx + u * 0.22f, cy + u * 0.68f)
        lineTo(cx - u * 0.22f, cy + u * 0.68f)
        lineTo(cx - u * 0.78f, cy + u * 0.88f)
        lineTo(cx - u * 0.42f, cy + u * 0.55f)
        lineTo(cx - u * 0.42f, cy - u * 0.1f)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawBrickEmblem(cx: Float, cy: Float, u: Float, color: Color) {
    val bw = u * 0.65f
    val bh = u * 0.32f
    val gap = u * 0.12f
    drawRoundRect(color, Offset(cx - bw - gap / 2, cy - bh - gap), Size(bw, bh), CornerRadius(3f, 3f))
    drawRoundRect(color, Offset(cx + gap / 2, cy - bh - gap), Size(bw, bh), CornerRadius(3f, 3f))
    drawRoundRect(color, Offset(cx - bw * 0.6f, cy + gap * 0.5f), Size(bw * 1.2f, bh), CornerRadius(3f, 3f))
}

private fun DrawScope.drawCarEmblem(cx: Float, cy: Float, u: Float, color: Color) {
    drawRoundRect(
        color = color,
        topLeft = Offset(cx - u * 0.42f, cy - u * 0.75f),
        size = Size(u * 0.84f, u * 1.5f),
        cornerRadius = CornerRadius(u * 0.22f, u * 0.22f)
    )
    // Wheels
    drawRect(color, Offset(cx - u * 0.66f, cy - u * 0.52f), Size(u * 0.22f, u * 0.42f))
    drawRect(color, Offset(cx + u * 0.44f, cy - u * 0.52f), Size(u * 0.22f, u * 0.42f))
    drawRect(color, Offset(cx - u * 0.66f, cy + u * 0.14f), Size(u * 0.22f, u * 0.42f))
    drawRect(color, Offset(cx + u * 0.44f, cy + u * 0.14f), Size(u * 0.22f, u * 0.42f))
}
