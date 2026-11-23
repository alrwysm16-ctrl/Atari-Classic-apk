package com.example.emulator.arcade

import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 1. Super Mario / Plumber Bros Classic (Side-Scrolling Platformer)
 */
class SuperMarioArcadeGame(private val seed: Int) : ArcadeGame {
    private var px = 28f
    private var py = 144f
    private var vx = 0f
    private var vy = 0f
    private var facingRight = true
    private var onGround = true
    private var camX = 0f
    private var score = 0
    private var lives = 3
    private var level = 1
    private var tick = 0
    private var invuln = 0

    // World objects (x in 0..960)
    private data class Block(val x: Int, val y: Int, val isMystery: Boolean, var hit: Boolean = false)
    private data class Coin(val x: Int, val y: Int, var taken: Boolean = false)
    private data class Enemy(var x: Float, val y: Int, var dir: Float, var alive: Boolean = true)
    private data class Fireball(var x: Float, var y: Float, var vx: Float, var vy: Float, var active: Boolean = true)

    private val blocks = mutableListOf<Block>()
    private val coins = mutableListOf<Coin>()
    private val enemies = mutableListOf<Enemy>()
    private val fireballs = mutableListOf<Fireball>()
    private val pipes = listOf(120, 260, 420, 580, 740)
    private val pits = listOf(330..362, 650..682)

    private val skyPalette = listOf(
        0xFF5C94FC.toInt(),
        0xFF2848A8.toInt(),
        0xFF1A1E3D.toInt(),
        0xFF4284E8.toInt()
    )[abs(seed) % 4]

    private val marioStand = intArrayOf(
        0b00111100, 0b01111110, 0b01101100, 0b01111100,
        0b00111000, 0b01111110, 0b10111101, 0b00111000,
        0b00100100, 0b01100110
    )
    private val goombaSprite = intArrayOf(
        0b00111100, 0b01111110, 0b11011011, 0b11111111,
        0b01111110, 0b00111100, 0b01100110, 0b11000011
    )

    init { reset() }

    override fun reset() {
        score = 0; lives = 3; level = 1
        initStage()
    }

    private fun initStage() {
        px = 24f; py = 148f; vx = 0f; vy = 0f; camX = 0f; onGround = true; invuln = 30
        blocks.clear(); coins.clear(); enemies.clear(); fireballs.clear()
        val bx = listOf(80, 96, 112, 180, 196, 300, 316, 480, 496, 512, 700, 716)
        for ((i, x) in bx.withIndex()) {
            blocks.add(Block(x, if (i % 3 == 1) 96 else 118, isMystery = (i % 2 == 1)))
        }
        for (i in 0 until 14) {
            coins.add(Coin(68 + i * 58 + (seed % 9), if (i % 2 == 0) 136 else 92))
        }
        val ex = listOf(160f, 230f, 385f, 460f, 540f, 620f, 790f, 840f)
        for ((i, x) in ex.withIndex()) {
            enemies.add(Enemy(x, 152, if (i % 2 == 0) -0.45f - level * 0.08f else 0.45f + level * 0.08f))
        }
    }

    override fun step(input: ArcadeInput, fb: IntArray, audio: ArcadeSoundGen) {
        tick++
        if (invuln > 0) invuln--

        val speed = if (input.b) 2.2f else 1.55f
        vx = when {
            input.left -> { facingRight = false; -speed }
            input.right -> { facingRight = true; speed }
            else -> 0f
        }
        px = (px + vx).coerceIn(4f, 920f)

        // Pipe horizontal block
        for (pipeX in pipes) {
            if (py + 10 > 138 && px + 8 > pipeX && px < pipeX + 18) {
                if (vx > 0) px = (pipeX - 8).toFloat()
                else if (vx < 0) px = (pipeX + 18).toFloat()
            }
        }

        // Jump
        if ((input.aJustPressed || input.up) && onGround) {
            vy = -4.2f
            onGround = false
            audio.play(SoundFx.JUMP)
        }

        // Fireball with B press
        if (input.bJustPressed && fireballs.count { it.active } < 2) {
            fireballs.add(Fireball(px + 4, py + 4, if (facingRight) 2.8f else -2.8f, 0f))
            audio.play(SoundFx.LASER)
        }

        vy = (vy + 0.22f).coerceAtMost(4.5f)
        val prevY = py
        py += vy

        // Ground & Pit check
        val overPit = pits.any { px + 4 in it.first.toFloat()..it.last.toFloat() }
        if (!overPit && py >= 150f) {
            py = 150f
            vy = 0f
            onGround = true
        } else {
            onGround = false
        }

        // Pipe top landing
        for (pipeX in pipes) {
            if (px + 7 > pipeX && px + 1 < pipeX + 18 && prevY + 10 <= 139 && py + 10 >= 138 && vy >= 0) {
                py = 128f; vy = 0f; onGround = true
            }
        }

        // Block collisions
        for (b in blocks) {
            if (px + 7 > b.x && px + 1 < b.x + 12) {
                if (vy < 0 && prevY >= b.y + 10 && py <= b.y + 10) {
                    py = (b.y + 10).toFloat()
                    vy = 0.5f
                    if (b.isMystery && !b.hit) {
                        b.hit = true
                        score += 200
                        audio.play(SoundFx.COIN)
                    } else {
                        audio.play(SoundFx.BOUNCE)
                    }
                } else if (vy >= 0 && prevY + 10 <= b.y && py + 10 >= b.y) {
                    py = (b.y - 10).toFloat()
                    vy = 0f
                    onGround = true
                }
            }
        }

        // Pit fall
        if (py > 194f) {
            loseLife(audio)
        }

        // Coins
        for (c in coins) {
            if (!c.taken && abs(px + 4 - (c.x + 3)) < 8 && abs(py + 5 - (c.y + 4)) < 8) {
                c.taken = true
                score += 100
                audio.play(SoundFx.COIN)
            }
        }

        // Fireballs
        for (f in fireballs) {
            if (!f.active) continue
            f.x += f.vx
            f.vy += 0.18f
            f.y += f.vy
            if (f.y >= 156f) { f.y = 156f; f.vy = -2.2f }
            if (f.x < camX - 10 || f.x > camX + 170) f.active = false
            for (e in enemies) {
                if (e.alive && abs(f.x - e.x) < 9 && abs(f.y - e.y) < 9) {
                    e.alive = false
                    f.active = false
                    score += 150
                    audio.play(SoundFx.HIT)
                }
            }
        }

        // Enemies
        for (e in enemies) {
            if (!e.alive) continue
            e.x += e.dir
            if (e.x < 40f || e.x > 870f) e.dir = -e.dir
            for (pipeX in pipes) {
                if (e.x + 8 > pipeX && e.x < pipeX + 18) e.dir = -e.dir
            }
            if (abs(px - e.x) < 8 && abs(py - e.y) < 9) {
                if (vy > 0 && prevY + 10 <= e.y + 4) {
                    e.alive = false
                    vy = -3.1f
                    score += 200
                    audio.play(SoundFx.HIT)
                } else if (invuln == 0) {
                    loseLife(audio)
                }
            }
        }

        // Goal flagpole at x = 895
        if (px >= 890f) {
            score += 1000
            level++
            audio.play(SoundFx.CLEAR)
            initStage()
        }

        camX = (px - 60f).coerceIn(0f, 780f)

        // Render
        ArcadeCanvas.clear(fb, skyPalette)
        // Clouds & Hills
        for (i in 0..6) {
            val hx = (i * 150 - (camX * 0.4f).toInt())
            ArcadeCanvas.fillCircle(fb, hx + 40, 160, 28, 0xFF28963C.toInt())
            val cx = (i * 130 + 30 - (camX * 0.25f).toInt())
            ArcadeCanvas.fillCircle(fb, cx, 38, 10, 0xFFF4F4F4.toInt())
            ArcadeCanvas.fillCircle(fb, cx + 10, 36, 12, 0xFFF4F4F4.toInt())
        }

        // Ground
        val camI = camX.toInt()
        for (sx in 0 until ArcadeCanvas.W step 8) {
            val wx = camI + sx
            val inPit = pits.any { wx + 4 in it }
            if (!inPit) {
                ArcadeCanvas.fillRect(fb, sx, 160, 8, 32, 0xFFC84C0C.toInt())
                ArcadeCanvas.fillRect(fb, sx, 160, 8, 3, 0xFF44B834.toInt())
                ArcadeCanvas.strokeRect(fb, sx, 163, 8, 10, 0xFF7C2E04.toInt())
            }
        }

        // Pipes
        for (pipeX in pipes) {
            val sx = pipeX - camI
            if (sx in -20..170) {
                ArcadeCanvas.fillRect(fb, sx + 2, 142, 14, 18, 0xFF1E9E2E.toInt())
                ArcadeCanvas.fillRect(fb, sx, 138, 18, 5, 0xFF2CD840.toInt())
            }
        }

        // Blocks
        for (b in blocks) {
            val sx = b.x - camI
            if (sx in -14..170) {
                val col = when {
                    b.hit -> 0xFF6B533E.toInt()
                    b.isMystery -> 0xFFF2AA2B.toInt()
                    else -> 0xFFC85A24.toInt()
                }
                ArcadeCanvas.fillRect(fb, sx, b.y, 12, 10, col)
                ArcadeCanvas.strokeRect(fb, sx, b.y, 12, 10, 0xFF3E1C0A.toInt())
            }
        }

        // Coins
        for (c in coins) {
            if (!c.taken) {
                val sx = c.x - camI
                if (sx in -10..170) {
                    val pulse = if ((tick / 8) % 2 == 0) 4 else 2
                    ArcadeCanvas.fillRect(fb, sx + (4 - pulse / 2), c.y, pulse, 7, 0xFFFFD426.toInt())
                }
            }
        }

        // Flagpole
        val flagScreenX = 895 - camI
        if (flagScreenX in -10..170) {
            ArcadeCanvas.fillRect(fb, flagScreenX, 68, 2, 92, 0xFFEAEAEA.toInt())
            ArcadeCanvas.fillRect(fb, flagScreenX - 12, 72, 12, 9, 0xFFE85D1A.toInt())
            ArcadeCanvas.fillCircle(fb, flagScreenX + 1, 66, 3, 0xFFF2AA2B.toInt())
        }

        // Enemies
        for (e in enemies) {
            if (!e.alive) continue
            val sx = e.x.toInt() - camI
            if (sx in -10..170) {
                ArcadeCanvas.drawBitmapSprite(fb, sx, e.y, goombaSprite, 8, 0xFF7B3814.toInt(), (tick / 10) % 2 == 0)
            }
        }

        // Fireballs
        for (f in fireballs) {
            if (f.active) {
                ArcadeCanvas.fillCircle(fb, f.x.toInt() - camI, f.y.toInt(), 3, 0xFFFF7A18.toInt())
            }
        }

        // Player Mario
        if (invuln == 0 || (tick / 3) % 2 == 0) {
            val psx = px.toInt() - camI
            ArcadeCanvas.drawBitmapSprite(fb, psx, py.toInt(), marioStand, 8, 0xFFE8281A.toInt(), !facingRight)
            // Blue overalls tint band
            ArcadeCanvas.fillRect(fb, psx + 1, py.toInt() + 6, 6, 3, 0xFF1E50D8.toInt())
        }

        ArcadeCanvas.drawHudBar(fb, score, lives, level)
    }

    private fun loseLife(audio: ArcadeSoundGen) {
        lives--
        audio.play(SoundFx.EXPLODE)
        if (lives <= 0) {
            reset()
        } else {
            px = 24f; py = 148f; vy = 0f; camX = 0f; invuln = 45
        }
    }

    override fun getScore(): Int = score

    override fun writeState(buf: ByteBuffer) {
        buf.putFloat(px); buf.putFloat(py); buf.putFloat(vx); buf.putFloat(vy); buf.putFloat(camX)
        buf.putInt(score); buf.putInt(lives); buf.putInt(level)
    }

    override fun readState(buf: ByteBuffer) {
        px = buf.float; py = buf.float; vx = buf.float; vy = buf.float; camX = buf.float
        score = buf.int; lives = buf.int; level = buf.int
    }
}

/**
 * 2. Donkey Kong / Barrel Kong Classic
 */
class BarrelKongArcadeGame(private val seed: Int) : ArcadeGame {
    private var px = 20f
    private var py = 168f
    private var vy = 0f
    private var onGround = true
    private var climbing = false
    private var score = 0
    private var lives = 3
    private var level = 1
    private var tick = 0

    private val platforms = intArrayOf(176, 138, 100, 62, 30)
    private val ladders = listOf(124 to 138, 36 to 100, 128 to 62, 76 to 30)
    private data class Barrel(var x: Float, var y: Float, var dir: Float, var active: Boolean = true, var scored: Boolean = false)
    private val barrels = mutableListOf<Barrel>()

    init { reset() }

    override fun reset() {
        score = 0; lives = 3; level = 1
        resetStage()
    }

    private fun resetStage() {
        px = 20f; py = 166f; vy = 0f; onGround = true; climbing = false
        barrels.clear()
    }

    override fun step(input: ArcadeInput, fb: IntArray, audio: ArcadeSoundGen) {
        tick++
        val spawnRate = (95 - level * 8).coerceAtLeast(40)
        if (tick % spawnRate == 0) {
            barrels.add(Barrel(36f, 54f, 1.0f + level * 0.1f))
        }

        if (input.left && !climbing) px = (px - 1.4f).coerceIn(10f, 142f)
        if (input.right && !climbing) px = (px + 1.4f).coerceIn(10f, 142f)

        // Check ladder climb
        val nearLadder = ladders.firstOrNull { (lx, topY) ->
            abs(px - lx) < 8 && py + 10 >= topY && py <= topY + 38
        }
        if (nearLadder != null && (input.up || input.down)) {
            climbing = true
            vy = 0f
            if (input.up) py = (py - 1.2f).coerceAtLeast(nearLadder.second - 10f)
            if (input.down) py = (py + 1.2f).coerceAtMost(nearLadder.second + 28f)
        } else {
            climbing = false
        }

        if (!climbing) {
            if (input.aJustPressed && onGround) {
                vy = -3.7f
                onGround = false
                audio.play(SoundFx.JUMP)
            }
            vy = (vy + 0.22f).coerceAtMost(4f)
            val prevY = py
            py += vy
            onGround = false
            for (platY in platforms) {
                val minX = if (platY == 30) 60f else 8f
                val maxX = if (platY == 30) 104f else 152f
                if (px + 4 in minX..maxX && prevY + 10 <= platY + 2 && py + 10 >= platY && vy >= 0) {
                    py = (platY - 10).toFloat()
                    vy = 0f
                    onGround = true
                }
            }
        }

        // Win reach top platform
        if (py <= 22f && px in 60f..100f) {
            score += 1000
            level++
            audio.play(SoundFx.CLEAR)
            resetStage()
        }

        // Update barrels
        for (b in barrels) {
            if (!b.active) continue
            b.x += b.dir
            if (b.x <= 12f) { b.y += 38f; b.dir = abs(b.dir) }
            else if (b.x >= 144f) { b.y += 38f; b.dir = -abs(b.dir) }
            if (b.y > 180f) { b.active = false; continue }

            // Jump over barrel bonus
            if (!b.scored && abs(px - b.x) < 6 && py + 10 < b.y && b.y - py < 22) {
                b.scored = true
                score += 100
                audio.play(SoundFx.COIN)
            }
            if (abs(px - b.x) < 6 && abs(py + 4 - b.y) < 7) {
                lives--
                audio.play(SoundFx.EXPLODE)
                if (lives <= 0) reset() else resetStage()
                return
            }
        }

        // Render
        ArcadeCanvas.clear(fb, 0xFF0A0812.toInt())
        for (platY in platforms) {
            val minX = if (platY == 30) 60 else 8
            val w = if (platY == 30) 44 else 144
            ArcadeCanvas.fillRect(fb, minX, platY, w, 5, 0xFFD83838.toInt())
            for (gx in minX until minX + w step 8) {
                ArcadeCanvas.fillRect(fb, gx, platY + 1, 4, 3, 0xFF7C1A1A.toInt())
            }
        }
        // Ladders
        for ((lx, topY) in ladders) {
            ArcadeCanvas.fillRect(fb, lx, topY, 2, 38, 0xFF42C8F5.toInt())
            ArcadeCanvas.fillRect(fb, lx + 8, topY, 2, 38, 0xFF42C8F5.toInt())
            for (ry in topY + 4 until topY + 38 step 6) {
                ArcadeCanvas.fillRect(fb, lx, ry, 10, 2, 0xFF42C8F5.toInt())
            }
        }
        // Kong & Goal icon
        ArcadeCanvas.fillRect(fb, 16, 44, 16, 18, 0xFF9C5220.toInt())
        ArcadeCanvas.fillRect(fb, 76, 18, 8, 12, 0xFFF2AA2B.toInt())

        // Barrels
        for (b in barrels) {
            if (b.active) {
                ArcadeCanvas.fillCircle(fb, b.x.toInt(), b.y.toInt(), 4, 0xFFE07A22.toInt())
                ArcadeCanvas.fillCircle(fb, b.x.toInt(), b.y.toInt(), 2, 0xFFF2AA2B.toInt())
            }
        }
        // Player
        ArcadeCanvas.fillRect(fb, px.toInt(), py.toInt(), 8, 10, 0xFFE85D1A.toInt())
        ArcadeCanvas.fillRect(fb, px.toInt() + 1, py.toInt(), 6, 3, 0xFFF2AA2B.toInt())

        ArcadeCanvas.drawHudBar(fb, score, lives, level)
    }

    override fun getScore(): Int = score
    override fun writeState(buf: ByteBuffer) { buf.putFloat(px); buf.putFloat(py); buf.putInt(score); buf.putInt(lives); buf.putInt(level) }
    override fun readState(buf: ByteBuffer) { px = buf.float; py = buf.float; score = buf.int; lives = buf.int; level = buf.int }
}

/**
 * 3. Pitfall Jungle Classic
 */
class PitfallJungleArcadeGame(private val seed: Int) : ArcadeGame {
    private var px = 28f
    private var py = 122f
    private var vy = 0f
    private var onGround = true
    private var screenIdx = 0
    private var score = 2000
    private var lives = 3
    private var tick = 0
    private var logX = 140f
    private var treasureTaken = false

    init { reset() }
    override fun reset() {
        px = 28f; py = 122f; vy = 0f; onGround = true; screenIdx = 0; score = 2000; lives = 3; logX = 140f; treasureTaken = false
    }

    override fun step(input: ArcadeInput, fb: IntArray, audio: ArcadeSoundGen) {
        tick++
        if (input.left) px -= 1.6f
        if (input.right) px += 1.6f
        if (px > 152f) { px = 8f; screenIdx++; treasureTaken = false; logX = 145f }
        if (px < 4f) { px = 148f; if (screenIdx > 0) screenIdx-- }

        // Swinging vine grab in screens with pond
        val hasPond = (screenIdx % 2 == 1)
        val vineX = 80 + (sin(tick * 0.06f) * 34f).toInt()
        val vineY = 92 + (cos(tick * 0.06f) * 12f).toInt()
        if (hasPond && !onGround && abs(px + 4 - vineX) < 10 && abs(py - vineY) < 14 && (input.up || input.a)) {
            px = vineX.toFloat() - 4f
            py = vineY.toFloat()
            vy = 0f
        } else {
            if (input.aJustPressed && onGround) {
                vy = -3.8f; onGround = false; audio.play(SoundFx.JUMP)
            }
            vy = (vy + 0.22f).coerceAtMost(4f)
            py += vy
            val overPond = hasPond && px + 4 in 52f..108f
            if (!overPond && py >= 122f) {
                py = 122f; vy = 0f; onGround = true
            } else if (overPond && py > 136f) {
                lives--; audio.play(SoundFx.EXPLODE)
                if (lives <= 0) reset() else { px = 24f; py = 122f; vy = 0f }
                return
            }
        }

        // Rolling log
        logX -= (1.1f + (screenIdx % 3) * 0.25f)
        if (logX < -8f) logX = 160f
        if (abs(px - logX) < 7 && abs(py + 4 - 126f) < 7) {
            score = (score - 100).coerceAtLeast(0)
            audio.play(SoundFx.HIT)
            logX = 160f
        }

        // Treasure pickup
        if (!treasureTaken && abs(px - 124f) < 9 && abs(py - 122f) < 10) {
            treasureTaken = true
            score += 500
            audio.play(SoundFx.COIN)
        }

        // Render Jungle
        ArcadeCanvas.clear(fb, 0xFF1F6B28.toInt()) // Foliage
        ArcadeCanvas.fillRect(fb, 0, 56, ArcadeCanvas.W, 76, 0xFF4CA832.toInt()) // Mid jungle
        // Tree trunks
        for (tx in listOf(22, 68, 114, 146)) {
            ArcadeCanvas.fillRect(fb, tx, 20, 8, 112, 0xFF5A3418.toInt())
        }
        // Ground & underground cave
        ArcadeCanvas.fillRect(fb, 0, 132, ArcadeCanvas.W, 12, 0xFFD9B24C.toInt())
        ArcadeCanvas.fillRect(fb, 0, 144, ArcadeCanvas.W, 48, 0xFF1A1410.toInt())
        ArcadeCanvas.fillRect(fb, 0, 178, ArcadeCanvas.W, 14, 0xFF5A3418.toInt())

        if (hasPond) {
            ArcadeCanvas.fillRect(fb, 52, 132, 56, 12, 0xFF1C56B8.toInt())
            ArcadeCanvas.drawLine(fb, 80, 34, vineX, vineY, 0xFF98DC38.toInt())
        }
        if (!treasureTaken) {
            ArcadeCanvas.fillRect(fb, 124, 124, 8, 8, 0xFFF2AA2B.toInt())
        }
        // Rolling log
        ArcadeCanvas.fillCircle(fb, logX.toInt(), 128, 4, 0xFF7C4018.toInt())
        // Harry Explorer
        ArcadeCanvas.fillRect(fb, px.toInt(), py.toInt(), 7, 10, 0xFF28B838.toInt())
        ArcadeCanvas.fillRect(fb, px.toInt() + 1, py.toInt(), 5, 4, 0xFFF4C89A.toInt())

        ArcadeCanvas.drawHudBar(fb, score, lives, screenIdx + 1)
    }

    override fun getScore(): Int = score
    override fun writeState(buf: ByteBuffer) { buf.putFloat(px); buf.putFloat(py); buf.putInt(screenIdx); buf.putInt(score); buf.putInt(lives) }
    override fun readState(buf: ByteBuffer) { px = buf.float; py = buf.float; screenIdx = buf.int; score = buf.int; lives = buf.int }
}
