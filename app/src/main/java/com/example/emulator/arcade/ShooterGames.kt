package com.example.emulator.arcade

import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 7. Space Invaders Classic
 */
class SpaceInvadersArcadeGame(private val seed: Int) : ArcadeGame {
    private var playerX = 76f
    private var bulletX = -1f
    private var bulletY = -1f
    private var enemyDir = 0.55f
    private var score = 0
    private var lives = 3
    private var level = 1
    private var tick = 0

    private data class Alien(var x: Float, var y: Float, var alive: Boolean, val row: Int)
    private data class Shot(var x: Float, var y: Float, var active: Boolean)

    private val aliens = mutableListOf<Alien>()
    private val enemyShots = mutableListOf<Shot>()
    private val bunkers = IntArray(4) { 10 }

    private val alienSprite = intArrayOf(
        0b00100100, 0b00011000, 0b00111100, 0b01011010,
        0b11111111, 0b10111101, 0b10100101, 0b00100100
    )

    init { reset() }

    override fun reset() {
        score = 0; lives = 3; level = 1
        initWave()
    }

    private fun initWave() {
        playerX = 76f; bulletY = -1f; enemyDir = 0.45f + level * 0.1f
        aliens.clear(); enemyShots.clear()
        bunkers.fill(10)
        for (r in 0 until 5) {
            for (c in 0 until 8) {
                aliens.add(Alien(18f + c * 15f, 24f + r * 13f, true, r))
            }
        }
    }

    override fun step(input: ArcadeInput, fb: IntArray, audio: ArcadeSoundGen) {
        tick++
        if (input.left) playerX = (playerX - 2f).coerceIn(6f, 144f)
        if (input.right) playerX = (playerX + 2f).coerceIn(6f, 144f)

        if ((input.aJustPressed || input.a) && bulletY < 0f) {
            bulletX = playerX + 4f
            bulletY = 166f
            audio.play(SoundFx.LASER)
        }

        if (bulletY >= 0f) {
            bulletY -= 4f
            if (bulletY < 12f) bulletY = -1f
            for (a in aliens) {
                if (a.alive && abs(bulletX - (a.x + 4)) < 6 && abs(bulletY - (a.y + 4)) < 6) {
                    a.alive = false
                    bulletY = -1f
                    score += (5 - a.row) * 20
                    audio.play(SoundFx.HIT)
                    break
                }
            }
        }

        // Move aliens
        var hitEdge = false
        for (a in aliens) {
            if (!a.alive) continue
            a.x += enemyDir
            if (a.x < 6f || a.x > 144f) hitEdge = true
            if (a.y > 158f) { lives = 0; reset(); return }
        }
        if (hitEdge) {
            enemyDir = -enemyDir
            for (a in aliens) a.y += 5f
        }

        val aliveAliens = aliens.filter { it.alive }
        if (aliveAliens.isEmpty()) {
            level++
            score += 500
            audio.play(SoundFx.CLEAR)
            initWave()
            return
        }

        if (tick % (36 - level * 3).coerceAtLeast(12) == 0 && enemyShots.size < 4) {
            val shooter = aliveAliens[Random(tick + seed).nextInt(aliveAliens.size)]
            enemyShots.add(Shot(shooter.x + 4f, shooter.y + 8f, true))
        }

        val it = enemyShots.iterator()
        while (it.hasNext()) {
            val s = it.next()
            s.y += 2.2f
            if (s.y > 186f) { it.remove(); continue }
            // Bunkers
            for (bi in 0 until 4) {
                val bx = 22f + bi * 34f
                if (bunkers[bi] > 0 && s.x in bx..(bx + 16f) && s.y in 146f..154f) {
                    bunkers[bi]--
                    it.remove()
                    break
                }
            }
            if (abs(s.x - (playerX + 4f)) < 6f && abs(s.y - 170f) < 6f) {
                it.remove()
                lives--
                audio.play(SoundFx.EXPLODE)
                if (lives <= 0) { reset(); return }
            }
        }

        // Render
        ArcadeCanvas.clear(fb, 0xFF07070E.toInt())
        for (a in aliens) {
            if (!a.alive) continue
            val col = when (a.row) {
                0 -> 0xFFE85D1A.toInt()
                1, 2 -> 0xFFF2AA2B.toInt()
                else -> 0xFF38E868.toInt()
            }
            ArcadeCanvas.drawBitmapSprite(fb, a.x.toInt(), a.y.toInt(), alienSprite, 8, col, (tick / 12) % 2 == 0)
        }
        for (bi in 0 until 4) {
            if (bunkers[bi] > 0) {
                ArcadeCanvas.fillRect(fb, 22 + bi * 34, 146, 16, (bunkers[bi] / 2).coerceAtLeast(2), 0xFF38E868.toInt())
            }
        }
        if (bulletY >= 0f) ArcadeCanvas.fillRect(fb, bulletX.toInt(), bulletY.toInt(), 2, 6, 0xFFF4E9D8.toInt())
        for (s in enemyShots) ArcadeCanvas.fillRect(fb, s.x.toInt(), s.y.toInt(), 2, 5, 0xFFFF4242.toInt())
        // Cannon
        ArcadeCanvas.fillRect(fb, playerX.toInt(), 170, 10, 5, 0xFF38E868.toInt())
        ArcadeCanvas.fillRect(fb, playerX.toInt() + 4, 166, 2, 4, 0xFF38E868.toInt())

        ArcadeCanvas.drawHudBar(fb, score, lives, level)
    }

    override fun getScore(): Int = score
    override fun writeState(buf: ByteBuffer) { buf.putFloat(playerX); buf.putInt(score); buf.putInt(lives); buf.putInt(level) }
    override fun readState(buf: ByteBuffer) { playerX = buf.float; score = buf.int; lives = buf.int; level = buf.int }
}

/**
 * 8. Galaga Galaxy Squadron
 */
class GalagaArcadeGame(private val seed: Int) : ArcadeGame {
    private var px = 76f
    private var score = 0
    private var lives = 3
    private var level = 1
    private var tick = 0
    private data class Shot(var x: Float, var y: Float)
    private data class Enemy(var x: Float, var y: Float, var diving: Boolean, var alive: Boolean)
    private val shots = mutableListOf<Shot>()
    private val enemies = mutableListOf<Enemy>()

    init { reset() }
    override fun reset() { score = 0; lives = 3; level = 1; initWave() }

    private fun initWave() {
        px = 76f; shots.clear(); enemies.clear()
        for (r in 0 until 4) for (c in 0 until 6) {
            enemies.add(Enemy(24f + c * 20f, 26f + r * 14f, diving = false, alive = true))
        }
    }

    override fun step(input: ArcadeInput, fb: IntArray, audio: ArcadeSoundGen) {
        tick++
        if (input.left) px = (px - 2.1f).coerceIn(6f, 144f)
        if (input.right) px = (px + 2.1f).coerceIn(6f, 144f)
        if (input.aJustPressed && shots.size < 4) {
            shots.add(Shot(px + 2f, 166f))
            shots.add(Shot(px + 7f, 166f))
            audio.play(SoundFx.LASER)
        }

        val sIt = shots.iterator()
        while (sIt.hasNext()) {
            val s = sIt.next()
            s.y -= 4.2f
            if (s.y < 12f) { sIt.remove(); continue }
            for (e in enemies) {
                if (e.alive && abs(s.x - e.x) < 7 && abs(s.y - e.y) < 7) {
                    e.alive = false
                    sIt.remove()
                    score += if (e.diving) 150 else 80
                    audio.play(SoundFx.HIT)
                    break
                }
            }
        }

        if (tick % 70 == 0) {
            enemies.filter { it.alive && !it.diving }.randomOrNull(Random(tick + seed))?.diving = true
        }
        for (e in enemies) {
            if (!e.alive) continue
            if (e.diving) {
                e.y += 1.7f + level * 0.15f
                e.x += sin(e.y * 0.08f) * 1.4f
                if (e.y > 192f) { e.y = 24f; e.diving = false }
                if (abs(e.x - (px + 4)) < 7 && abs(e.y - 170f) < 7) {
                    lives--; e.alive = false; audio.play(SoundFx.EXPLODE)
                    if (lives <= 0) { reset(); return }
                }
            } else {
                e.x += cos(tick * 0.05f) * 0.35f
            }
        }
        if (enemies.none { it.alive }) { level++; score += 500; audio.play(SoundFx.CLEAR); initWave() }

        ArcadeCanvas.clear(fb, 0xFF040510.toInt())
        for (i in 0 until 20) {
            val sx = (i * 37 + seed) % ArcadeCanvas.W
            val sy = (i * 29 + tick * 2) % (ArcadeCanvas.H - 14) + 14
            fb[sy * ArcadeCanvas.W + sx] = 0xFF666699.toInt()
        }
        for (e in enemies) {
            if (!e.alive) continue
            val col = if (e.diving) 0xFFE85D1A.toInt() else 0xFF42C8F5.toInt()
            ArcadeCanvas.fillRect(fb, e.x.toInt() - 4, e.y.toInt() - 3, 8, 6, col)
            ArcadeCanvas.fillRect(fb, e.x.toInt() - 2, e.y.toInt() - 5, 4, 2, 0xFFF2AA2B.toInt())
        }
        for (s in shots) ArcadeCanvas.fillRect(fb, s.x.toInt(), s.y.toInt(), 2, 5, 0xFFF2AA2B.toInt())
        ArcadeCanvas.fillRect(fb, px.toInt(), 170, 10, 6, 0xFFF4E9D8.toInt())
        ArcadeCanvas.fillRect(fb, px.toInt() + 4, 165, 2, 5, 0xFFE85D1A.toInt())
        ArcadeCanvas.drawHudBar(fb, score, lives, level)
    }

    override fun getScore(): Int = score
    override fun writeState(buf: ByteBuffer) { buf.putFloat(px); buf.putInt(score); buf.putInt(lives); buf.putInt(level) }
    override fun readState(buf: ByteBuffer) { px = buf.float; score = buf.int; lives = buf.int; level = buf.int }
}

/**
 * 9. River Raid Canyon Fighter
 */
class RiverRaidArcadeGame(private val seed: Int) : ArcadeGame {
    private var px = 76f
    private var scroll = 0f
    private var fuel = 100f
    private var score = 0
    private var lives = 3
    private var tick = 0
    private data class Obj(var x: Float, var y: Float, val kind: Int, var alive: Boolean = true) // 0=ship,1=heli,2=fuel
    private val objs = mutableListOf<Obj>()
    private var shotX = -1f
    private var shotY = -1f

    init { reset() }
    override fun reset() { px = 76f; scroll = 0f; fuel = 100f; score = 0; lives = 3; objs.clear() }

    override fun step(input: ArcadeInput, fb: IntArray, audio: ArcadeSoundGen) {
        tick++
        val spd = when {
            input.up -> 2.2f
            input.down -> 0.8f
            else -> 1.4f
        }
        scroll += spd
        fuel -= 0.08f
        if (fuel <= 0f) { lives--; audio.play(SoundFx.EXPLODE); if (lives <= 0) { reset(); return } else fuel = 100f }

        if (input.left) px -= 1.8f
        if (input.right) px += 1.8f
        if (input.aJustPressed && shotY < 0f) { shotX = px + 3f; shotY = 160f; audio.play(SoundFx.LASER) }

        if (shotY >= 0f) {
            shotY -= 4.5f
            if (shotY < 14f) shotY = -1f
        }

        if (tick % 36 == 0) {
            val r = Random(tick + seed)
            val kind = if (tick % 108 == 0) 2 else r.nextInt(2)
            objs.add(Obj(42f + r.nextFloat() * 72f, 14f, kind))
        }

        val it = objs.iterator()
        while (it.hasNext()) {
            val o = it.next()
            o.y += spd
            if (o.kind < 2) o.x += sin(tick * 0.05f + o.y * 0.02f) * 0.4f
            if (o.y > 186f || !o.alive) { it.remove(); continue }

            if (shotY >= 0f && abs(shotX - o.x) < 7 && abs(shotY - o.y) < 7) {
                o.alive = false; shotY = -1f
                score += if (o.kind == 2) 80 else 120
                audio.play(SoundFx.HIT)
            }
            if (abs(px + 4 - o.x) < 7 && abs(166f - o.y) < 7) {
                if (o.kind == 2) {
                    fuel = (fuel + 40f).coerceAtMost(100f)
                    o.alive = false
                    audio.play(SoundFx.POWERUP)
                } else {
                    lives--; o.alive = false; audio.play(SoundFx.EXPLODE)
                    if (lives <= 0) { reset(); return }
                }
            }
        }

        // Canyon bank collision at y = 166
        val bankW = (24 + sin((scroll + 166f) * 0.015f) * 14f).toInt()
        if (px < bankW || px + 8 > ArcadeCanvas.W - bankW) {
            lives--; audio.play(SoundFx.EXPLODE)
            if (lives <= 0) { reset(); return } else px = 76f
        }

        ArcadeCanvas.clear(fb, 0xFF1A46B8.toInt())
        for (y in 12 until 180) {
            val bw = (24 + sin((scroll + (180 - y)) * 0.015f) * 14f).toInt()
            ArcadeCanvas.fillRect(fb, 0, y, bw, 1, 0xFF2C9C38.toInt())
            ArcadeCanvas.fillRect(fb, ArcadeCanvas.W - bw, y, bw, 1, 0xFF2C9C38.toInt())
        }
        for (o in objs) {
            when (o.kind) {
                2 -> {
                    ArcadeCanvas.fillRect(fb, o.x.toInt() - 4, o.y.toInt() - 5, 8, 10, 0xFFE85D1A.toInt())
                    ArcadeCanvas.drawText(fb, o.x.toInt() - 1, o.y.toInt() - 2, "F", 0xFFFFFFFF.toInt(), 1)
                }
                else -> ArcadeCanvas.fillRect(fb, o.x.toInt() - 5, o.y.toInt() - 3, 10, 6, 0xFFF2AA2B.toInt())
            }
        }
        if (shotY >= 0f) ArcadeCanvas.fillRect(fb, shotX.toInt(), shotY.toInt(), 2, 5, 0xFFFFFFFF.toInt())
        // Jet
        ArcadeCanvas.fillRect(fb, px.toInt() + 3, 162, 2, 10, 0xFFF4E9D8.toInt())
        ArcadeCanvas.fillRect(fb, px.toInt(), 166, 8, 3, 0xFFF4E9D8.toInt())
        // Fuel Bar
        ArcadeCanvas.fillRect(fb, 0, 180, ArcadeCanvas.W, 12, 0xFF1A1512.toInt())
        ArcadeCanvas.drawText(fb, 8, 183, "FUEL", 0xFFF2AA2B.toInt(), 1)
        ArcadeCanvas.fillRect(fb, 34, 183, (fuel * 1.1f).toInt(), 5, if (fuel < 25f) 0xFFE85D1A.toInt() else 0xFF38E868.toInt())
        ArcadeCanvas.drawHudBar(fb, score, lives, 1 + score / 1000)
    }

    override fun getScore(): Int = score
    override fun writeState(buf: ByteBuffer) { buf.putFloat(px); buf.putFloat(fuel); buf.putInt(score); buf.putInt(lives) }
    override fun readState(buf: ByteBuffer) { px = buf.float; fuel = buf.float; score = buf.int; lives = buf.int }
}

/**
 * 10. Asteroids 360 Space Shooter
 */
class AsteroidsArcadeGame(private val seed: Int) : ArcadeGame {
    private var sx = 80f; private var sy = 96f
    private var svx = 0f; private var svy = 0f
    private var angle = -1.57f
    private var score = 0; private var lives = 3; private var level = 1; private var tick = 0
    private data class Rock(var x: Float, var y: Float, var vx: Float, var yv: Float, var size: Int, var alive: Boolean = true)
    private data class Laser(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Int)
    private val rocks = mutableListOf<Rock>()
    private val lasers = mutableListOf<Laser>()

    init { reset() }
    override fun reset() { score = 0; lives = 3; level = 1; initWave() }

    private fun initWave() {
        sx = 80f; sy = 96f; svx = 0f; svy = 0f; lasers.clear(); rocks.clear()
        val r = Random(seed + level * 19)
        repeat(3 + level) {
            rocks.add(Rock(r.nextFloat() * 160f, 20f + r.nextFloat() * 40f, (r.nextFloat() - 0.5f) * 1.4f, (r.nextFloat() - 0.5f) * 1.4f, 8))
        }
    }

    override fun step(input: ArcadeInput, fb: IntArray, audio: ArcadeSoundGen) {
        tick++
        if (input.left) angle -= 0.1f
        if (input.right) angle += 0.1f
        if (input.up) {
            svx = (svx + cos(angle) * 0.12f).coerceIn(-2.4f, 2.4f)
            svy = (svy + sin(angle) * 0.12f).coerceIn(-2.4f, 2.4f)
        }
        svx *= 0.99f; svy *= 0.99f
        sx = (sx + svx + 160f) % 160f
        sy = ((sy + svy - 14f + 178f) % 178f) + 14f

        if (input.bJustPressed) {
            sx = Random(tick).nextFloat() * 140f + 10f
            sy = Random(tick + 7).nextFloat() * 140f + 24f
            audio.play(SoundFx.POWERUP)
        }
        if (input.aJustPressed && lasers.size < 5) {
            lasers.add(Laser(sx, sy, cos(angle) * 3.8f, sin(angle) * 3.8f, 36))
            audio.play(SoundFx.LASER)
        }

        val lIt = lasers.iterator()
        while (lIt.hasNext()) {
            val l = lIt.next()
            l.x = (l.x + l.vx + 160f) % 160f
            l.y = ((l.y + l.vy - 14f + 178f) % 178f) + 14f
            l.life--
            if (l.life <= 0) { lIt.remove(); continue }
            for (i in rocks.indices) {
                val rk = rocks[i]
                if (rk.alive && abs(l.x - rk.x) < rk.size + 2 && abs(l.y - rk.y) < rk.size + 2) {
                    rk.alive = false; lIt.remove()
                    score += 100
                    audio.play(SoundFx.HIT)
                    if (rk.size > 4) {
                        rocks.add(Rock(rk.x, rk.y, rk.vx + 0.6f, -rk.yv, rk.size / 2))
                        rocks.add(Rock(rk.x, rk.y, -rk.vx, rk.yv + 0.6f, rk.size / 2))
                    }
                    break
                }
            }
        }

        for (rk in rocks) {
            if (!rk.alive) continue
            rk.x = (rk.x + rk.vx + 160f) % 160f
            rk.y = ((rk.y + rk.yv - 14f + 178f) % 178f) + 14f
            if (abs(sx - rk.x) < rk.size + 2 && abs(sy - rk.y) < rk.size + 2) {
                lives--; audio.play(SoundFx.EXPLODE)
                if (lives <= 0) { reset(); return } else { sx = 80f; sy = 96f; svx = 0f; svy = 0f }
            }
        }
        if (rocks.none { it.alive }) { level++; audio.play(SoundFx.CLEAR); initWave() }

        ArcadeCanvas.clear(fb, 0xFF09090D.toInt())
        for (rk in rocks) {
            if (rk.alive) ArcadeCanvas.fillCircle(fb, rk.x.toInt(), rk.y.toInt(), rk.size, 0xFFA89886.toInt())
        }
        for (l in lasers) ArcadeCanvas.fillRect(fb, l.x.toInt(), l.y.toInt(), 2, 2, 0xFFE85D1A.toInt())
        val nx = (sx + cos(angle) * 6).toInt()
        val ny = (sy + sin(angle) * 6).toInt()
        val lx = (sx + cos(angle + 2.4f) * 5).toInt()
        val ly = (sy + sin(angle + 2.4f) * 5).toInt()
        val rx = (sx + cos(angle - 2.4f) * 5).toInt()
        val ry = (sy + sin(angle - 2.4f) * 5).toInt()
        ArcadeCanvas.drawLine(fb, nx, ny, lx, ly, 0xFFF2AA2B.toInt())
        ArcadeCanvas.drawLine(fb, nx, ny, rx, ry, 0xFFF2AA2B.toInt())
        ArcadeCanvas.drawLine(fb, lx, ly, rx, ry, 0xFFF2AA2B.toInt())
        ArcadeCanvas.drawHudBar(fb, score, lives, level)
    }

    override fun getScore(): Int = score
    override fun writeState(buf: ByteBuffer) { buf.putFloat(sx); buf.putFloat(sy); buf.putInt(score); buf.putInt(lives); buf.putInt(level) }
    override fun readState(buf: ByteBuffer) { sx = buf.float; sy = buf.float; score = buf.int; lives = buf.int; level = buf.int }
}

/**
 * 11. Centipede Classic
 */
class CentipedeArcadeGame(private val seed: Int) : ArcadeGame {
    private var px = 80f; private var py = 170f
    private var shotX = -1f; private var shotY = -1f
    private var score = 0; private var lives = 3; private var level = 1; private var tick = 0
    private data class Seg(var x: Float, var y: Float, var dir: Float, var alive: Boolean = true)
    private val segs = mutableListOf<Seg>()
    private val mushrooms = mutableListOf<Pair<Int, Int>>()

    init { reset() }
    override fun reset() { score = 0; lives = 3; level = 1; initStage() }

    private fun initStage() {
        px = 80f; py = 170f; shotY = -1f
        segs.clear(); mushrooms.clear()
        val r = Random(seed + level)
        repeat(18) { mushrooms.add(r.nextInt(1, 19) * 8 to r.nextInt(3, 18) * 8) }
        for (i in 0 until 10) segs.add(Seg(80f - i * 7f, 20f, 1.1f + level * 0.1f))
    }

    override fun step(input: ArcadeInput, fb: IntArray, audio: ArcadeSoundGen) {
        tick++
        if (input.left) px = (px - 2f).coerceIn(4f, 152f)
        if (input.right) px = (px + 2f).coerceIn(4f, 152f)
        if (input.up) py = (py - 1.8f).coerceIn(136f, 180f)
        if (input.down) py = (py + 1.8f).coerceIn(136f, 180f)
        if ((input.aJustPressed || input.a) && shotY < 0f) { shotX = px + 3f; shotY = py; audio.play(SoundFx.LASER) }

        if (shotY >= 0f) {
            shotY -= 4.5f
            if (shotY < 12f) shotY = -1f
            for (s in segs) {
                if (s.alive && abs(shotX - s.x) < 6 && abs(shotY - s.y) < 6) {
                    s.alive = false; shotY = -1f; score += 100
                    mushrooms.add(((s.x / 8).toInt() * 8) to ((s.y / 8).toInt() * 8))
                    audio.play(SoundFx.HIT)
                    break
                }
            }
            val mIt = mushrooms.iterator()
            while (mIt.hasNext() && shotY >= 0f) {
                val (mx, my) = mIt.next()
                if (abs(shotX - (mx + 3)) < 5 && abs(shotY - (my + 3)) < 5) {
                    mIt.remove(); shotY = -1f; score += 10
                    break
                }
            }
        }

        for (s in segs) {
            if (!s.alive) continue
            s.x += s.dir
            val hitMushroom = mushrooms.any { (mx, my) -> abs(s.x - (mx + 3)) < 5 && abs(s.y - (my + 3)) < 5 }
            if (s.x < 4f || s.x > 152f || hitMushroom) {
                s.dir = -s.dir; s.y += 8f
                if (s.y > 180f) s.y = 20f
            }
            if (abs(s.x - (px + 3)) < 6 && abs(s.y - (py + 3)) < 6) {
                lives--; audio.play(SoundFx.EXPLODE)
                if (lives <= 0) { reset(); return } else { px = 80f; py = 170f }
            }
        }
        if (segs.none { it.alive }) { level++; audio.play(SoundFx.CLEAR); initStage() }

        ArcadeCanvas.clear(fb, 0xFF0A0C08.toInt())
        for ((mx, my) in mushrooms) ArcadeCanvas.fillRect(fb, mx + 1, my + 1, 6, 6, 0xFF38D858.toInt())
        for (s in segs) if (s.alive) ArcadeCanvas.fillCircle(fb, s.x.toInt(), s.y.toInt(), 3, 0xFFE85D1A.toInt())
        if (shotY >= 0f) ArcadeCanvas.fillRect(fb, shotX.toInt(), shotY.toInt(), 2, 5, 0xFFF2AA2B.toInt())
        ArcadeCanvas.fillRect(fb, px.toInt(), py.toInt(), 7, 7, 0xFFF4E9D8.toInt())
        ArcadeCanvas.drawHudBar(fb, score, lives, level)
    }

    override fun getScore(): Int = score
    override fun writeState(buf: ByteBuffer) { buf.putFloat(px); buf.putFloat(py); buf.putInt(score); buf.putInt(lives); buf.putInt(level) }
    override fun readState(buf: ByteBuffer) { px = buf.float; py = buf.float; score = buf.int; lives = buf.int; level = buf.int }
}

/**
 * 12. Missile Command Classic
 */
class MissileCommandArcadeGame(private val seed: Int) : ArcadeGame {
    private var cx = 80f; private var cy = 96f
    private var score = 0; private var cities = 3; private var level = 1; private var tick = 0
    private data class Incoming(var x: Float, var y: Float, val tx: Float, val vx: Float, val vy: Float, var alive: Boolean = true)
    private data class Blast(val x: Float, val y: Float, var r: Int = 2, var life: Int = 24)
    private val incoming = mutableListOf<Incoming>()
    private val blasts = mutableListOf<Blast>()
    private val cityAlive = booleanArrayOf(true, true, true)

    init { reset() }
    override fun reset() { score = 0; cities = 3; level = 1; cityAlive.fill(true); incoming.clear(); blasts.clear() }

    override fun step(input: ArcadeInput, fb: IntArray, audio: ArcadeSoundGen) {
        tick++
        if (input.left) cx = (cx - 2.4f).coerceIn(6f, 154f)
        if (input.right) cx = (cx + 2.4f).coerceIn(6f, 154f)
        if (input.up) cy = (cy - 2.4f).coerceIn(18f, 168f)
        if (input.down) cy = (cy + 2.4f).coerceIn(18f, 168f)

        if (input.aJustPressed && blasts.size < 4) {
            blasts.add(Blast(cx, cy))
            audio.play(SoundFx.LASER)
        }

        if (tick % (50 - level * 5).coerceAtLeast(18) == 0) {
            val r = Random(tick + seed)
            val sx = 10f + r.nextFloat() * 140f
            val targets = listOf(30f, 80f, 130f)
            val tx = targets[r.nextInt(3)]
            val dx = tx - sx
            val dy = 176f - 14f
            val steps = 160f - level * 10f
            incoming.add(Incoming(sx, 14f, tx, dx / steps, dy / steps))
        }

        val bIt = blasts.iterator()
        while (bIt.hasNext()) {
            val b = bIt.next()
            b.life--
            if (b.life > 12) b.r++ else b.r = (b.r - 1).coerceAtLeast(1)
            if (b.life <= 0) bIt.remove()
        }

        val mIt = incoming.iterator()
        while (mIt.hasNext()) {
            val m = mIt.next()
            m.x += m.vx; m.y += m.vy
            if (blasts.any { abs(m.x - it.x) < it.r && abs(m.y - it.y) < it.r }) {
                score += 100
                audio.play(SoundFx.HIT)
                mIt.remove()
                continue
            }
            if (m.y >= 176f) {
                val cityIdx = when {
                    m.tx < 50f -> 0
                    m.tx < 105f -> 1
                    else -> 2
                }
                if (cityAlive[cityIdx]) {
                    cityAlive[cityIdx] = false
                    cities--
                    audio.play(SoundFx.EXPLODE)
                    if (cities <= 0) { reset(); return }
                }
                mIt.remove()
            }
        }
        level = 1 + score / 1000

        ArcadeCanvas.clear(fb, 0xFF0A0812.toInt())
        ArcadeCanvas.fillRect(fb, 0, 178, ArcadeCanvas.W, 14, 0xFFC86818.toInt())
        val cxs = intArrayOf(24, 74, 124)
        for (i in 0..2) {
            if (cityAlive[i]) ArcadeCanvas.fillRect(fb, cxs[i], 168, 12, 10, 0xFF42C8F5.toInt())
        }
        for (m in incoming) {
            ArcadeCanvas.drawLine(fb, m.x.toInt(), m.y.toInt(), (m.x - m.vx * 8).toInt(), (m.y - m.vy * 8).toInt(), 0xFFFF3838.toInt())
            ArcadeCanvas.fillRect(fb, m.x.toInt() - 1, m.y.toInt() - 1, 3, 3, 0xFFF4E9D8.toInt())
        }
        for (b in blasts) ArcadeCanvas.fillCircle(fb, b.x.toInt(), b.y.toInt(), b.r, 0xFFF2AA2B.toInt())
        // Crosshair
        ArcadeCanvas.drawLine(fb, cx.toInt() - 4, cy.toInt(), cx.toInt() + 4, cy.toInt(), 0xFFE85D1A.toInt())
        ArcadeCanvas.drawLine(fb, cx.toInt(), cy.toInt() - 4, cx.toInt(), cy.toInt() + 4, 0xFFE85D1A.toInt())
        ArcadeCanvas.drawHudBar(fb, score, cities, level)
    }

    override fun getScore(): Int = score
    override fun writeState(buf: ByteBuffer) { buf.putFloat(cx); buf.putFloat(cy); buf.putInt(score); buf.putInt(cities) }
    override fun readState(buf: ByteBuffer) { cx = buf.float; cy = buf.float; score = buf.int; cities = buf.int }
}
