package com.example.emulator

/**
 * Authentic 128-color Atari 2600 NTSC palette (bits 7..1 -> index 0..127).
 */
object NtscPalette {
    val COLORS: IntArray = IntArray(128) { idx ->
        val hue = (idx ushr 3) and 0x0F
        val lum = idx and 0x07
        val luma = 0.10 + (lum / 7.0) * 0.85
        if (hue == 0) {
            val g = (luma * 255).toInt().coerceIn(0, 255)
            (0xFF shl 24) or (g shl 16) or (g shl 8) or g
        } else {
            val angle = Math.toRadians((hue - 1) * 24.0 + 35.0)
            val sat = 0.36
            val y = luma
            val u = sat * kotlin.math.cos(angle)
            val v = sat * kotlin.math.sin(angle)
            val r = ((y + 1.13983 * v) * 255).toInt().coerceIn(0, 255)
            val g = ((y - 0.39465 * u - 0.58060 * v) * 255).toInt().coerceIn(0, 255)
            val b = ((y + 2.03211 * u) * 255).toInt().coerceIn(0, 255)
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }

    fun getArgb(tiaColorByte: Int): Int {
        return COLORS[(tiaColorByte ushr 1) and 0x7F]
    }
}
