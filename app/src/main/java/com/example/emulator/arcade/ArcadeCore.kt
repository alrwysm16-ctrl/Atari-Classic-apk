package com.example.emulator.arcade

import java.nio.ByteBuffer
import kotlin.math.abs

data class ArcadeInput(
    val up: Boolean = false,
    val down: Boolean = false,
    val left: Boolean = false,
    val right: Boolean = false,
    val a: Boolean = false,
    val b: Boolean = false,
    val aJustPressed: Boolean = false,
    val bJustPressed: Boolean = false
)

enum class SoundFx {
    NONE, JUMP, COIN, EAT, LASER, EXPLODE, BOUNCE, POWERUP, CLEAR, HIT
}

class ArcadeSoundGen {
    private var freq = 0f
    private var freqEnd = 0f
    private var framesLeft = 0
    private var totalFrames = 1
    private var noiseMode = false
    private var phase = 0f
    private var lfsr = 0xACE1

    fun play(fx: SoundFx) {
        when (fx) {
            SoundFx.NONE -> Unit
            SoundFx.JUMP -> start(260f, 620f, 9, false)
            SoundFx.COIN -> start(988f, 1318f, 8, false)
            SoundFx.EAT -> start(440f, 660f, 4, false)
            SoundFx.LASER -> start(900f, 220f, 7, false)
            SoundFx.EXPLODE -> start(160f, 55f, 16, true)
            SoundFx.BOUNCE -> start(520f, 520f, 4, false)
            SoundFx.POWERUP -> start(330f, 990f, 16, false)
            SoundFx.CLEAR -> start(523f, 1046f, 18, false)
            SoundFx.HIT -> start(220f, 90f, 12, true)
        }
    }

    private fun start(f0: Float, f1: Float, frames: Int, noise: Boolean) {
        freq = f0
        freqEnd = f1
        framesLeft = frames
        totalFrames = frames.coerceAtLeast(1)
        noiseMode = noise
    }

    fun fillSamples(out: ShortArray, count: Int) {
        if (framesLeft <= 0) {
            out.fill(0, 0, count)
            return
        }
        val progress = 1f - (framesLeft.toFloat() / totalFrames.toFloat())
        val curFreq = freq + (freqEnd - freq) * progress
        val env = (framesLeft.toFloat() / totalFrames.toFloat()).coerceIn(0f, 1f)
        val amp = (5500f * env).toInt()
        val inc = curFreq / 22050f

        for (i in 0 until count) {
            val sample = if (noiseMode) {
                if (i % 6 == 0) {
                    val bit = ((lfsr ushr 0) xor (lfsr ushr 2) xor (lfsr ushr 3) xor (lfsr ushr 5)) and 1
                    lfsr = (lfsr ushr 1) or (bit shl 15)
                }
                if ((lfsr and 1) != 0) amp else -amp
            } else {
                phase += inc
                if (phase >= 1f) phase -= 1f
                if (phase < 0.5f) amp else -amp
            }
            out[i] = sample.toShort()
        }
        framesLeft--
    }
}

object ArcadeCanvas {
    const val W = 160
    const val H = 192

    fun clear(fb: IntArray, color: Int) {
        fb.fill(color)
    }

    fun fillRect(fb: IntArray, x: Int, y: Int, w: Int, h: Int, color: Int) {
        val x0 = x.coerceIn(0, W)
        val y0 = y.coerceIn(0, H)
        val x1 = (x + w).coerceIn(0, W)
        val y1 = (y + h).coerceIn(0, H)
        if (x0 >= x1 || y0 >= y1) return
        for (py in y0 until y1) {
            val row = py * W
            for (px in x0 until x1) {
                fb[row + px] = color
            }
        }
    }

    fun strokeRect(fb: IntArray, x: Int, y: Int, w: Int, h: Int, color: Int) {
        fillRect(fb, x, y, w, 1, color)
        fillRect(fb, x, y + h - 1, w, 1, color)
        fillRect(fb, x, y, 1, h, color)
        fillRect(fb, x + w - 1, y, 1, h, color)
    }

    fun fillCircle(fb: IntArray, cx: Int, cy: Int, r: Int, color: Int) {
        val r2 = r * r
        for (dy in -r..r) {
            val py = cy + dy
            if (py !in 0 until H) continue
            val row = py * W
            for (dx in -r..r) {
                val px = cx + dx
                if (px in 0 until W && dx * dx + dy * dy <= r2) {
                    fb[row + px] = color
                }
            }
        }
    }

    fun drawLine(fb: IntArray, x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
        var cx = x0
        var cy = y0
        val dx = abs(x1 - x0)
        val dy = -abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        while (true) {
            if (cx in 0 until W && cy in 0 until H) {
                fb[cy * W + cx] = color
            }
            if (cx == x1 && cy == y1) break
            val e2 = 2 * err
            if (e2 >= dy) { err += dy; cx += sx }
            if (e2 <= dx) { err += dx; cy += sy }
        }
    }

    fun drawBitmapSprite(
        fb: IntArray,
        x: Int,
        y: Int,
        rows: IntArray,
        width: Int,
        color: Int,
        flipX: Boolean = false
    ) {
        for (r in rows.indices) {
            val py = y + r
            if (py !in 0 until H) continue
            val bits = rows[r]
            val rowOffset = py * W
            for (c in 0 until width) {
                val bitIdx = if (flipX) c else (width - 1 - c)
                if ((bits and (1 shl bitIdx)) != 0) {
                    val px = x + c
                    if (px in 0 until W) {
                        fb[rowOffset + px] = color
                    }
                }
            }
        }
    }

    private val DIGITS = arrayOf(
        intArrayOf(0b111, 0b101, 0b101, 0b101, 0b111), // 0
        intArrayOf(0b010, 0b110, 0b010, 0b010, 0b111), // 1
        intArrayOf(0b111, 0b001, 0b111, 0b100, 0b111), // 2
        intArrayOf(0b111, 0b001, 0b111, 0b001, 0b111), // 3
        intArrayOf(0b101, 0b101, 0b111, 0b001, 0b001), // 4
        intArrayOf(0b111, 0b100, 0b111, 0b001, 0b111), // 5
        intArrayOf(0b111, 0b100, 0b111, 0b101, 0b111), // 6
        intArrayOf(0b111, 0b001, 0b001, 0b001, 0b001), // 7
        intArrayOf(0b111, 0b101, 0b111, 0b101, 0b111), // 8
        intArrayOf(0b111, 0b101, 0b111, 0b001, 0b111)  // 9
    )

    private val LETTERS = mapOf(
        'S' to intArrayOf(0b111, 0b100, 0b111, 0b001, 0b111),
        'C' to intArrayOf(0b111, 0b100, 0b100, 0b100, 0b111),
        'L' to intArrayOf(0b100, 0b100, 0b100, 0b100, 0b111),
        'V' to intArrayOf(0b101, 0b101, 0b101, 0b101, 0b010),
        'P' to intArrayOf(0b111, 0b101, 0b111, 0b100, 0b100),
        'F' to intArrayOf(0b111, 0b100, 0b110, 0b100, 0b100),
        'U' to intArrayOf(0b101, 0b101, 0b101, 0b101, 0b111),
        'E' to intArrayOf(0b111, 0b100, 0b110, 0b100, 0b111),
        'G' to intArrayOf(0b111, 0b100, 0b101, 0b101, 0b111),
        'A' to intArrayOf(0b010, 0b101, 0b111, 0b101, 0b101),
        'M' to intArrayOf(0b101, 0b111, 0b101, 0b101, 0b101),
        'O' to intArrayOf(0b111, 0b101, 0b101, 0b101, 0b111),
        'R' to intArrayOf(0b110, 0b101, 0b110, 0b101, 0b101),
        'T' to intArrayOf(0b111, 0b010, 0b010, 0b010, 0b010),
        'N' to intArrayOf(0b101, 0b111, 0b111, 0b101, 0b101),
        '-' to intArrayOf(0b000, 0b000, 0b111, 0b000, 0b000),
        ':' to intArrayOf(0b000, 0b010, 0b000, 0b010, 0b000)
    )

    fun drawText(fb: IntArray, x: Int, y: Int, text: String, color: Int, scale: Int = 1) {
        var curX = x
        for (ch in text.uppercase()) {
            if (ch == ' ') {
                curX += 4 * scale
                continue
            }
            val glyph = when {
                ch in '0'..'9' -> DIGITS[ch - '0']
                LETTERS.containsKey(ch) -> LETTERS[ch]!!
                else -> null
            }
            if (glyph != null) {
                for (r in 0 until 5) {
                    val bits = glyph[r]
                    for (c in 0 until 3) {
                        if ((bits and (1 shl (2 - c))) != 0) {
                            fillRect(fb, curX + c * scale, y + r * scale, scale, scale, color)
                        }
                    }
                }
            }
            curX += 4 * scale
        }
    }

    fun drawHudBar(fb: IntArray, score: Int, lives: Int, level: Int, bgColor: Int = 0xFF0E0B09.toInt()) {
        fillRect(fb, 0, 0, W, 12, bgColor)
        drawText(fb, 4, 3, "S:$score", 0xFFF2AA2B.toInt(), 1)
        drawText(fb, 76, 3, "LV:$level", 0xFFF4E9D8.toInt(), 1)
        for (i in 0 until lives.coerceIn(0, 5)) {
            fillRect(fb, 126 + i * 6, 4, 4, 4, 0xFFE85D1A.toInt())
        }
    }
}

interface ArcadeGame {
    fun reset()
    fun step(input: ArcadeInput, fb: IntArray, audio: ArcadeSoundGen)
    fun getScore(): Int
    fun writeState(buf: ByteBuffer)
    fun readState(buf: ByteBuffer)
}
