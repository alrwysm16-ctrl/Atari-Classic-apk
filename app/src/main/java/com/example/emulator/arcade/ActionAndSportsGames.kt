package com.example.emulator.arcade

import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * 13. Breakout Classic Brick Smash
 */
class BreakoutArcadeGame(private val seed: Int) : ArcadeGame {
    private var paddleX = 64f
    private var bx = 80f; private var by = 130f
    private var bvx = 1.4f; private var bvy = -1.6f
    private var score = 0; private var lives = 3; private var level = 1
    private val bricks = Array(6) { BooleanArray(10) { true } }
    private val rowColors = intArrayOf(
        0xFFE83030.toInt(), 0xFFE85D1A.toInt(), 0xFFF2AA2B.toInt(),
        0xFFE8D828.toInt(), 0xFF38D858.toInt(), 0xFF3898FF.toInt()
    )

    init { reset() }
    override fun reset() { score = 0; lives = 3; level = 1; resetBricks() }

    private fun resetBricks() {
        for (r in 0 until 6) bricks[r].fill(true)
        bx = 80f; by = 130f; bvx = 1.4f; bvy = -1.5f - level * 0.15f
    }

    override fun step(input: ArcadeInput, fb: IntArray, audio: ArcadeSoundGen) {
        val spd = if (input.b) 3.4f else 2.4f
        if (input.left) paddleX = (paddleX - spd).coerceIn(4f, 128f)
        if (input.right) paddleX = (paddleX + spd).coerceIn(4f, 128f)

        bx += bvx; by += bvy
        if (bx <= 4f || bx >= 156f) { bvx = -bvx; audio.play(SoundFx.BOUNCE) }
        if (by <= 16f) { bvy = abs(bvy); audio.play(SoundFx.BOUNCE) }

        if (by >= 170f && by <= 176f && bx in (paddleX - 2f)..(paddleX + 30f) && bvy > 0) {
            val hit = (bx - (paddleX + 14f)) / 14f
            bvx = (hit * 2.2f).coerceIn(-2.4f, 2.4f)
            bvy = -abs(bvy)
            audio.play(SoundFx.BOUNCE)
        }

        if (by > 192f) {
            lives--; audio.play(SoundFx.EXPLODE)
            if (lives <= 0) { reset(); return }
            bx = paddleX + 14f; by = 150f; bvy = -1.6f
        }

        // Bricks
        outer@ for (r in 0 until 6) {
            for (c in 0 until 10) {
                if (!bricks[r][c]) continue
                val rx = 6 + c * 15
                val ry = 24 + r * 8
                if (bx in rx.toFloat()..(rx + 14f) && by in ry.toFloat()..(ry + 7f)) {
                    bricks[r][c] = false
                    bvy = -bvy
                    score += (6 - r) * 20
                    audio.play(SoundFx.COIN)
                    break@outer
                }
            }
        }
        if (bricks.all { row -> row.all { !it } }) { level++; audio.play(SoundFx.CLEAR); resetBricks() }

        ArcadeCanvas.clear(fb, 0xFF100C0A.toInt())
        for (r in 0 until 6) {
            for (c in 0 until 10) {
                if (bricks[r][c]) ArcadeCanvas.fillRect(fb, 6 + c * 15, 24 + r * 8, 13, 6, rowColors[r])
            }
        }
        ArcadeCanvas.fillRect(fb, paddleX.toInt(), 172, 28, 5, 0xFFE85D1A.toInt())
        ArcadeCanvas.fillCircle(fb, bx.toInt(), by.toInt(), 3, 0xFFF4E9D8.toInt())
        ArcadeCanvas.drawHudBar(fb, score, lives, level)
    }

    override fun getScore(): Int = score
    override fun writeState(buf: ByteBuffer) { buf.putFloat(paddleX); buf.putFloat(bx); buf.putFloat(by); buf.putInt(score); buf.putInt(lives) }
    override fun readState(buf: ByteBuffer) { paddleX = buf.float; bx = buf.float; by = buf.float; score = buf.int; lives = buf.int }
}

/**
 * 14. Pong Classic
 */
class PongArcadeGame(private val seed: Int) : ArcadeGame {
    private var p1Y = 84f; private var p2Y = 84f
    private var bx = 80f; private var by = 96f
    private var bvx = 1.8f; private var bvy = 1.1f
    private var p1Score = 0; private var p2Score = 0

    init { reset() }
    override fun reset() { p1Y = 84f; p2Y = 84f; p1Score = 0; p2Score = 0; serve(1f) }
    private fun serve(dir: Float) { bx = 80f; by = 96f; bvx = 1.8f * dir; bvy = 1.0f }

    override fun step(input: ArcadeInput, fb: IntArray, audio: ArcadeSoundGen) {
        if (input.up) p1Y = (p1Y - 2.3f).coerceIn(16f, 160f)
        if (input.down) p1Y = (p1Y + 2.3f).coerceIn(16f, 160f)

        val aiSpeed = 1.55f + (seed % 3) * 0.15f
        if (p2Y + 12f < by) p2Y = (p2Y + aiSpeed).coerceIn(16f, 160f)
        else if (p2Y + 12f > by) p2Y = (p2Y - aiSpeed).coerceIn(16f, 160f)

        bx += bvx; by += bvy
        if (by <= 16f || by >= 186f) { bvy = -bvy; audio.play(SoundFx.BOUNCE) }

        if (bx in 12f..18f && by in (p1Y - 2f)..(p1Y + 26f) && bvx < 0) {
            bvx = abs(bvx) * 1.04f
            bvy = ((by - (p1Y + 12f)) / 12f) * 2.2f
            audio.play(SoundFx.BOUNCE)
        }
        if (bx in 142f..148f && by in (p2Y - 2f)..(p2Y + 26f) && bvx > 0) {
            bvx = -abs(bvx) * 1.04f
            bvy = ((by - (p2Y + 12f)) / 12f) * 2.2f
            audio.play(SoundFx.BOUNCE)
        }
        if (bx < 2f) { p2Score++; audio.play(SoundFx.HIT); serve(1f) }
        if (bx > 158f) { p1Score++; audio.play(SoundFx.COIN); serve(-1f) }

        ArcadeCanvas.clear(fb, 0xFF0E0B09.toInt())
        for (y in 18 until 188 step 10) ArcadeCanvas.fillRect(fb, 79, y, 2, 5, 0xFF3E342A.toInt())
        ArcadeCanvas.fillRect(fb, 12, p1Y.toInt(), 5, 24, 0xFFE85D1A.toInt())
        ArcadeCanvas.fillRect(fb, 143, p2Y.toInt(), 5, 24, 0xFFF2AA2B.toInt())
        ArcadeCanvas.fillRect(fb, bx.toInt() - 2, by.toInt() - 2, 4, 4, 0xFFF4E9D8.toInt())
        ArcadeCanvas.drawText(fb, 54, 4, "$p1Score", 0xFFE85D1A.toInt(), 2)
        ArcadeCanvas.drawText(fb, 98, 4, "$p2Score", 0xFFF2AA2B.toInt(), 2)
    }

    override fun getScore(): Int = p1Score * 100
    override fun writeState(buf: ByteBuffer) { buf.putFloat(p1Y); buf.putFloat(p2Y); buf.putFloat(bx); buf.putFloat(by); buf.putInt(p1Score); buf.putInt(p2Score) }
    override fun readState(buf: ByteBuffer) { p1Y = buf.float; p2Y = buf.float; bx = buf.float; by = buf.float; p1Score = buf.int; p2Score = buf.int }
}

/**
 * 15. Frogger River & Highway Crossing
 */
class FroggerArcadeGame(private val seed: Int) : ArcadeGame {
    private var fx = 76; private var fy = 172
    private var score = 0; private var lives = 3; private var level = 1
    private var moveCooldown = 0; private var tick = 0
    private val laneSpeeds = floatArrayOf(1.0f, -1.2f, 1.4f, -0.9f, 0f, 1.1f, -1.3f, 0.9f, -1.5f)
    private val laneOffsets = FloatArray(9) { it * 22f }
    private val padsReached = BooleanArray(5)

    init { reset() }
    override fun reset() { score = 0; lives = 3; level = 1; padsReached.fill(false); resetFrog() }
    private fun resetFrog() { fx = 76; fy = 172 }

    override fun step(input: ArcadeInput, fb: IntArray, audio: ArcadeSoundGen) {
        tick++
        if (moveCooldown > 0) moveCooldown--
        if (moveCooldown == 0) {
            when {
                input.up || input.aJustPressed -> { fy -= 14; score += 10; moveCooldown = 9; audio.play(SoundFx.JUMP) }
                input.down -> { fy = (fy + 14).coerceAtMost(172); moveCooldown = 9; audio.play(SoundFx.JUMP) }
                input.left -> { fx = (fx - 12).coerceAtLeast(6); moveCooldown = 8; audio.play(SoundFx.JUMP) }
                input.right -> { fx = (fx + 12).coerceAtMost(146); moveCooldown = 8; audio.play(SoundFx.JUMP) }
            }
        }

        for (i in laneOffsets.indices) laneOffsets[i] = (laneOffsets[i] + laneSpeeds[i] * (1f + level * 0.1f) + 160f) % 160f

        // Check home pads at top
        if (fy <= 28) {
            val padIdx = ((fx - 8) / 30).coerceIn(0, 4)
            padsReached[padIdx] = true
            score += 200
            audio.play(SoundFx.COIN)
            if (padsReached.all { it }) { level++; padsReached.fill(false); audio.play(SoundFx.CLEAR) }
            resetFrog()
        } else if (fy in 98..154) {
            // Road lanes
            val lane = ((fy - 98) / 14).coerceIn(0, 3) + 5
            val ox = laneOffsets[lane]
            for (c in 0 until 3) {
                val cx = (ox + c * 54f) % 160f
                if (abs(fx - cx) < 12) {
                    lives--; audio.play(SoundFx.EXPLODE)
                    if (lives <= 0) { reset(); return } else resetFrog()
                }
            }
        } else if (fy in 34..82) {
            // River logs
            val lane = ((fy - 34) / 14).coerceIn(0, 3)
            val ox = laneOffsets[lane]
            var onLog = false
            for (c in 0 until 3) {
                val lx = (ox + c * 56f) % 160f
                if (fx in (lx.toInt() - 4)..(lx.toInt() + 26)) {
                    onLog = true
                    fx = (fx + laneSpeeds[lane].toInt()).coerceIn(4, 150)
                }
            }
            if (!onLog) {
                lives--; audio.play(SoundFx.EXPLODE)
                if (lives <= 0) { reset(); return } else resetFrog()
            }
        }

        ArcadeCanvas.clear(fb, 0xFF141418.toInt())
        ArcadeCanvas.fillRect(fb, 0, 14, ArcadeCanvas.W, 20, 0xFF1E6E2E.toInt())
        ArcadeCanvas.fillRect(fb, 0, 34, ArcadeCanvas.W, 56, 0xFF1848A8.toInt())
        ArcadeCanvas.fillRect(fb, 0, 90, ArcadeCanvas.W, 12, 0xFF6B389C.toInt()) // safe median
        ArcadeCanvas.fillRect(fb, 0, 168, ArcadeCanvas.W, 14, 0xFF6B389C.toInt()) // start

        for (i in 0 until 5) {
            val px = 14 + i * 30
            ArcadeCanvas.fillRect(fb, px, 20, 12, 10, if (padsReached[i]) 0xFF38E858.toInt() else 0xFF0A2A12.toInt())
        }
        // Logs
        for (l in 0..3) {
            for (c in 0 until 3) {
                val lx = ((laneOffsets[l] + c * 56f) % 160f).toInt()
                ArcadeCanvas.fillRect(fb, lx, 36 + l * 14, 24, 10, 0xFF8C4818.toInt())
            }
        }
        // Cars
        for (l in 5..8) {
            for (c in 0 until 3) {
                val cx = ((laneOffsets[l] + c * 54f) % 160f).toInt()
                val col = if (l % 2 == 0) 0xFFE85D1A.toInt() else 0xFFF2AA2B.toInt()
                ArcadeCanvas.fillRect(fb, cx, 100 + (l - 5) * 14, 14, 10, col)
            }
        }
        ArcadeCanvas.fillRect(fb, fx, fy, 8, 8, 0xFF38F058.toInt())
        ArcadeCanvas.drawHudBar(fb, score, lives, level)
    }

    override fun getScore(): Int = score
    override fun writeState(buf: ByteBuffer) { buf.putInt(fx); buf.putInt(fy); buf.putInt(score); buf.putInt(lives); buf.putInt(level) }
    override fun readState(buf: ByteBuffer) { fx = buf.int; fy = buf.int; score = buf.int; lives = buf.int; level = buf.int }
}

/**
 * 16. Enduro Highway Racer
 */
class EnduroRacerArcadeGame(private val seed: Int) : ArcadeGame {
    private var px = 80f
    private var speed = 2.0f
    private var dist = 0f
    private var score = 0
    private var lives = 3
    private var tick = 0
    private data class Rival(var laneOffset: Float, var z: Float, val color: Int)
    private val rivals = mutableListOf<Rival>()

    init { reset() }
    override fun reset() { px = 80f; speed = 2.0f; dist = 0f; score = 0; lives = 3; rivals.clear() }

    override fun step(input: ArcadeInput, fb: IntArray, audio: ArcadeSoundGen) {
        tick++
        speed = when {
            input.a || input.up -> (speed + 0.08f).coerceAtMost(4.5f)
            input.b || input.down -> (speed - 0.12f).coerceAtLeast(0.9f)
            else -> (speed * 0.99f).coerceAtLeast(1.8f)
        }
        if (input.left) px = (px - 2.3f).coerceIn(22f, 138f)
        if (input.right) px = (px + 2.3f).coerceIn(22f, 138f)

        dist += speed
        score = (dist / 4f).toInt()
        val curve = sin(dist * 0.008f + seed) * 20f

        if (tick % 40 == 0 && rivals.size < 4) {
            val cols = intArrayOf(0xFF3898FF.toInt(), 0xFFF2AA2B.toInt(), 0xFF38E858.toInt())
            val r = Random(tick + seed)
            rivals.add(Rival((r.nextFloat() - 0.5f) * 0.8f, 0.05f, cols[r.nextInt(3)]))
        }

        val it = rivals.iterator()
        while (it.hasNext()) {
            val rv = it.next()
            rv.z += 0.012f * speed
            if (rv.z > 1.05f) { it.remove(); continue }
            val ry = 66f + rv.z * 116f
            val roadHalf = 14f + rv.z * 58f
            val rx = 80f + curve * (1f - rv.z) + rv.laneOffset * roadHalf
            if (ry in 154f..176f && abs(px - rx) < 11f) {
                lives--; speed = 1.0f; audio.play(SoundFx.EXPLODE); it.remove()
                if (lives <= 0) { reset(); return }
            }
        }

        // Render Sky + Mountains + Perspective Road
        ArcadeCanvas.clear(fb, 0xFF1A3A78.toInt())
        ArcadeCanvas.fillCircle(fb, 80, 48, 12, 0xFFF2AA2B.toInt())
        ArcadeCanvas.fillRect(fb, 0, 64, ArcadeCanvas.W, 128, 0xFF2C7828.toInt())
        for (y in 66 until 192) {
            val z = (y - 66f) / 126f
            val halfW = (14f + z * 58f).toInt()
            val center = (80f + curve * (1f - z)).toInt()
            val roadCol = 0xFF282628.toInt()
            val curbCol = if (((y + dist.toInt()) / 8) % 2 == 0) 0xFFE85D1A.toInt() else 0xFFF4E9D8.toInt()
            ArcadeCanvas.fillRect(fb, center - halfW - 3, y, 3, 1, curbCol)
            ArcadeCanvas.fillRect(fb, center + halfW, y, 3, 1, curbCol)
            ArcadeCanvas.fillRect(fb, center - halfW, y, halfW * 2, 1, roadCol)
            if (((y + dist.toInt()) / 10) % 2 == 0) {
                ArcadeCanvas.fillRect(fb, center - 1, y, 2, 1, 0xFFF2AA2B.toInt())
            }
        }
        for (rv in rivals) {
            val ry = (66f + rv.z * 116f).toInt()
            val roadHalf = 14f + rv.z * 58f
            val rx = (80f + curve * (1f - rv.z) + rv.laneOffset * roadHalf).toInt()
            val w = (4 + rv.z * 10).toInt()
            ArcadeCanvas.fillRect(fb, rx - w / 2, ry - w / 2, w, (w * 0.7f).toInt().coerceAtLeast(3), rv.color)
        }
        // Player Car
        ArcadeCanvas.fillRect(fb, px.toInt() - 7, 164, 14, 10, 0xFFE85D1A.toInt())
        ArcadeCanvas.fillRect(fb, px.toInt() - 5, 160, 10, 5, 0xFFF2AA2B.toInt())
        ArcadeCanvas.drawHudBar(fb, score, lives, 1 + score / 500)
    }

    override fun getScore(): Int = score
    override fun writeState(buf: ByteBuffer) { buf.putFloat(px); buf.putFloat(speed); buf.putFloat(dist); buf.putInt(score); buf.putInt(lives) }
    override fun readState(buf: ByteBuffer) { px = buf.float; speed = buf.float; dist = buf.float; score = buf.int; lives = buf.int }
}
