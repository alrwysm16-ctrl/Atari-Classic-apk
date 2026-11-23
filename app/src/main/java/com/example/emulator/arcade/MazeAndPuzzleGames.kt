package com.example.emulator.arcade

import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.random.Random

/**
 * 4. Pac-Man Classic Maze Game
 */
class PacManArcadeGame(private val seed: Int) : ArcadeGame {
    companion object {
        const val COLS = 19
        const val ROWS = 21
        const val CELL = 8
        const val OX = 4
        const val OY = 16

        // 1=Wall, 0=Pellet, 2=Power Pellet, 3=Empty
        private val MAZE_TEMPLATE = arrayOf(
            "1111111111111111111",
            "1000000001000000001",
            "1211011101011101121",
            "1000000000000000001",
            "1011010111110101101",
            "1000010001000100001",
            "1111011131311101111",
            "3331013333333101333",
            "1111013113113101111",
            "3333033133313303333",
            "1111013111113101111",
            "3331013333333101333",
            "1111013111113101111",
            "1000000001000000001",
            "1011011101011101101",
            "1201000003000001021",
            "1101010111110101011",
            "1000010001000100001",
            "1011111101011111101",
            "1000000000000000001",
            "1111111111111111111"
        )
    }

    private val grid = Array(ROWS) { IntArray(COLS) }
    private var px = 9f
    private var py = 15f
    private var dirX = 0
    private var dirY = 0
    private var wantX = 0
    private var wantY = 0
    private var frightenedTicks = 0
    private var score = 0
    private var lives = 3
    private var level = 1
    private var tick = 0

    private data class Ghost(var x: Float, var y: Float, var dx: Int, var dy: Int, val color: Int)
    private val ghosts = mutableListOf<Ghost>()

    init { reset() }

    override fun reset() {
        score = 0; lives = 3; level = 1
        loadMaze()
    }

    private fun loadMaze() {
        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                grid[r][c] = MAZE_TEMPLATE[r][c].digitToInt()
            }
        }
        resetPositions()
    }

    private fun resetPositions() {
        px = 9f; py = 15f; dirX = 0; dirY = 0; wantX = 0; wantY = 0; frightenedTicks = 0
        ghosts.clear()
        ghosts.add(Ghost(9f, 7f, 1, 0, 0xFFFF2424.toInt()))
        ghosts.add(Ghost(8f, 9f, 0, -1, 0xFFFF88C8.toInt()))
        ghosts.add(Ghost(10f, 9f, -1, 0, 0xFF38E4FF.toInt()))
        ghosts.add(Ghost(9f, 9f, 1, 0, 0xFFF2AA2B.toInt()))
    }

    private fun isWall(col: Int, row: Int): Boolean {
        if (row !in 0 until ROWS) return true
        val wc = ((col % COLS) + COLS) % COLS
        return grid[row][wc] == 1
    }

    override fun step(input: ArcadeInput, fb: IntArray, audio: ArcadeSoundGen) {
        tick++
        if (frightenedTicks > 0) frightenedTicks--

        if (input.left) { wantX = -1; wantY = 0 }
        else if (input.right) { wantX = 1; wantY = 0 }
        else if (input.up) { wantX = 0; wantY = -1 }
        else if (input.down) { wantX = 0; wantY = 1 }

        val speed = 0.14f
        // Align to grid center when turning
        val nearCol = (px + 0.5f).toInt()
        val nearRow = (py + 0.5f).toInt()
        if (abs(px - nearCol) < 0.16f && abs(py - nearRow) < 0.16f) {
            if ((wantX != 0 || wantY != 0) && !isWall(nearCol + wantX, nearRow + wantY)) {
                px = nearCol.toFloat(); py = nearRow.toFloat()
                dirX = wantX; dirY = wantY
            }
            if (isWall(nearCol + dirX, nearRow + dirY)) {
                px = nearCol.toFloat(); py = nearRow.toFloat()
                dirX = 0; dirY = 0
            }
        }

        px += dirX * speed
        py += dirY * speed
        if (px < -0.5f) px = COLS - 0.6f
        if (px > COLS - 0.5f) px = -0.4f

        // Eat pellets
        val curC = (px + 0.5f).toInt().coerceIn(0, COLS - 1)
        val curR = (py + 0.5f).toInt().coerceIn(0, ROWS - 1)
        when (grid[curR][curC]) {
            0 -> {
                grid[curR][curC] = 3
                score += 10
                audio.play(SoundFx.EAT)
            }
            2 -> {
                grid[curR][curC] = 3
                score += 50
                frightenedTicks = 300
                audio.play(SoundFx.POWERUP)
            }
        }

        // Check level clear
        var pelletsLeft = 0
        for (r in 0 until ROWS) for (c in 0 until COLS) if (grid[r][c] == 0 || grid[r][c] == 2) pelletsLeft++
        if (pelletsLeft == 0) {
            level++
            score += 500
            audio.play(SoundFx.CLEAR)
            loadMaze()
            return
        }

        // Update Ghosts
        val gSpeed = if (frightenedTicks > 0) 0.065f else (0.09f + level * 0.01f).coerceAtMost(0.13f)
        for ((gi, g) in ghosts.withIndex()) {
            val gc = (g.x + 0.5f).toInt()
            val gr = (g.y + 0.5f).toInt()
            if (abs(g.x - gc) < 0.14f && abs(g.y - gr) < 0.14f) {
                g.x = gc.toFloat(); g.y = gr.toFloat()
                val dirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
                    .filter { (dx, dy) -> !isWall(gc + dx, gr + dy) && !(dx == -g.dx && dy == -g.dy) }
                val choices = dirs.ifEmpty { listOf(-g.dx to -g.dy) }
                val best = if (frightenedTicks == 0 && (tick + gi * 7) % 3 != 0) {
                    choices.minByOrNull { (dx, dy) -> abs((gc + dx) - px) + abs((gr + dy) - py) } ?: choices.first()
                } else {
                    choices[Random(tick + gi * 31 + seed).nextInt(choices.size)]
                }
                g.dx = best.first; g.dy = best.second
            }
            g.x += g.dx * gSpeed
            g.y += g.dy * gSpeed
            if (g.x < 0f) g.x = (COLS - 1).toFloat()
            if (g.x > COLS - 1f) g.x = 0f

            if (abs(px - g.x) < 0.7f && abs(py - g.y) < 0.7f) {
                if (frightenedTicks > 0) {
                    score += 200
                    g.x = 9f; g.y = 9f
                    audio.play(SoundFx.COIN)
                } else {
                    lives--
                    audio.play(SoundFx.EXPLODE)
                    if (lives <= 0) reset() else resetPositions()
                    return
                }
            }
        }

        // Render
        ArcadeCanvas.clear(fb, 0xFF060612.toInt())
        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                val sx = OX + c * CELL
                val sy = OY + r * CELL
                when (grid[r][c]) {
                    1 -> {
                        ArcadeCanvas.fillRect(fb, sx + 1, sy + 1, CELL - 2, CELL - 2, 0xFF1A38B8.toInt())
                        ArcadeCanvas.strokeRect(fb, sx + 1, sy + 1, CELL - 2, CELL - 2, 0xFF3A6BFF.toInt())
                    }
                    0 -> ArcadeCanvas.fillRect(fb, sx + 3, sy + 3, 2, 2, 0xFFF4D098.toInt())
                    2 -> if ((tick / 8) % 2 == 0) ArcadeCanvas.fillCircle(fb, sx + 4, sy + 4, 3, 0xFFF2AA2B.toInt())
                }
            }
        }

        // Ghosts
        for (g in ghosts) {
            val gx = OX + (g.x * CELL).toInt() + 4
            val gy = OY + (g.y * CELL).toInt() + 4
            val gCol = if (frightenedTicks > 0) 0xFF2858FF.toInt() else g.color
            ArcadeCanvas.fillCircle(fb, gx, gy, 3, gCol)
            ArcadeCanvas.fillRect(fb, gx - 3, gy, 7, 4, gCol)
            ArcadeCanvas.fillRect(fb, gx - 2, gy - 1, 1, 1, 0xFFFFFFFF.toInt())
            ArcadeCanvas.fillRect(fb, gx + 1, gy - 1, 1, 1, 0xFFFFFFFF.toInt())
        }

        // Pac-Man
        val pacX = OX + (px * CELL).toInt() + 4
        val pacY = OY + (py * CELL).toInt() + 4
        ArcadeCanvas.fillCircle(fb, pacX, pacY, 4, 0xFFFFD818.toInt())
        if ((tick / 4) % 2 == 0) {
            val mx = pacX + dirX * 2
            val my = pacY + dirY * 2
            ArcadeCanvas.fillRect(fb, mx - 1, my - 1, 3, 3, 0xFF060612.toInt())
        }

        ArcadeCanvas.drawHudBar(fb, score, lives, level)
    }

    override fun getScore(): Int = score
    override fun writeState(buf: ByteBuffer) {
        buf.putFloat(px); buf.putFloat(py); buf.putInt(score); buf.putInt(lives); buf.putInt(level)
        for (r in 0 until ROWS) for (c in 0 until COLS) buf.put(grid[r][c].toByte())
    }
    override fun readState(buf: ByteBuffer) {
        px = buf.float; py = buf.float; score = buf.int; lives = buf.int; level = buf.int
        for (r in 0 until ROWS) for (c in 0 until COLS) grid[r][c] = buf.get().toInt()
    }
}

/**
 * 5. Tetris Classic
 */
class TetrisArcadeGame(private val seed: Int) : ArcadeGame {
    companion object {
        const val COLS = 10
        const val ROWS = 20
        const val CELL = 8
        const val OX = 40
        const val OY = 20
    }

    private val board = Array(ROWS) { IntArray(COLS) }
    private val colors = intArrayOf(
        0xFF00E5FF.toInt(), 0xFFFFD400.toInt(), 0xFFB838FF.toInt(),
        0xFF38E848.toInt(), 0xFFFF3838.toInt(), 0xFF3878FF.toInt(), 0xFFE85D1A.toInt()
    )
    private val shapes = arrayOf(
        arrayOf(intArrayOf(1, 1, 1, 1)),
        arrayOf(intArrayOf(1, 1), intArrayOf(1, 1)),
        arrayOf(intArrayOf(0, 1, 0), intArrayOf(1, 1, 1)),
        arrayOf(intArrayOf(0, 1, 1), intArrayOf(1, 1, 0)),
        arrayOf(intArrayOf(1, 1, 0), intArrayOf(0, 1, 1)),
        arrayOf(intArrayOf(1, 0, 0), intArrayOf(1, 1, 1)),
        arrayOf(intArrayOf(0, 0, 1), intArrayOf(1, 1, 1))
    )

    private var curType = 0
    private var curRot = 0
    private var curX = 3
    private var curY = 0
    private var nextType = 1
    private var score = 0
    private var lines = 0
    private var level = 1
    private var tick = 0
    private var moveCooldown = 0
    private var rng = Random(seed + 42)

    init { reset() }

    override fun reset() {
        for (r in 0 until ROWS) board[r].fill(0)
        score = 0; lines = 0; level = 1; tick = 0
        rng = Random(seed + 42)
        nextType = rng.nextInt(shapes.size)
        spawnPiece()
    }

    private fun spawnPiece() {
        curType = nextType
        nextType = rng.nextInt(shapes.size)
        curRot = 0
        curX = 3
        curY = 0
        if (!fits(curX, curY, getMatrix(curType, curRot))) {
            reset()
        }
    }

    private fun getMatrix(type: Int, rot: Int): Array<IntArray> {
        var m = shapes[type]
        repeat(rot % 4) {
            val h = m.size
            val w = m[0].size
            m = Array(w) { r -> IntArray(h) { c -> m[h - 1 - c][r] } }
        }
        return m
    }

    private fun fits(tx: Int, ty: Int, m: Array<IntArray>): Boolean {
        for (r in m.indices) {
            for (c in m[r].indices) {
                if (m[r][c] == 0) continue
                val bx = tx + c
                val by = ty + r
                if (bx !in 0 until COLS || by >= ROWS) return false
                if (by >= 0 && board[by][bx] != 0) return false
            }
        }
        return true
    }

    override fun step(input: ArcadeInput, fb: IntArray, audio: ArcadeSoundGen) {
        tick++
        if (moveCooldown > 0) moveCooldown--

        val mat = getMatrix(curType, curRot)
        if (moveCooldown == 0) {
            if (input.left && fits(curX - 1, curY, mat)) { curX--; moveCooldown = 6 }
            else if (input.right && fits(curX + 1, curY, mat)) { curX++; moveCooldown = 6 }
        }

        if (input.aJustPressed || (input.up && moveCooldown == 0)) {
            val nextRot = (curRot + 1) % 4
            if (fits(curX, curY, getMatrix(curType, nextRot))) {
                curRot = nextRot
                audio.play(SoundFx.BOUNCE)
            }
            if (input.up) moveCooldown = 10
        }

        if (input.bJustPressed) {
            while (fits(curX, curY + 1, getMatrix(curType, curRot))) curY++
            lockPiece(audio)
        } else {
            val dropEvery = if (input.down) 3 else (28 - level * 2).coerceAtLeast(5)
            if (tick % dropEvery == 0) {
                val curM = getMatrix(curType, curRot)
                if (fits(curX, curY + 1, curM)) {
                    curY++
                } else {
                    lockPiece(audio)
                }
            }
        }

        // Render
        ArcadeCanvas.clear(fb, 0xFF100E14.toInt())
        ArcadeCanvas.fillRect(fb, OX - 2, OY - 2, COLS * CELL + 4, ROWS * CELL + 4, 0xFF1C1822.toInt())
        ArcadeCanvas.strokeRect(fb, OX - 2, OY - 2, COLS * CELL + 4, ROWS * CELL + 4, 0xFFE85D1A.toInt())

        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                val cellVal = board[r][c]
                if (cellVal > 0) {
                    val col = colors[(cellVal - 1) % colors.size]
                    ArcadeCanvas.fillRect(fb, OX + c * CELL, OY + r * CELL, CELL - 1, CELL - 1, col)
                }
            }
        }

        // Draw active piece
        val activeMat = getMatrix(curType, curRot)
        val pCol = colors[curType % colors.size]
        for (r in activeMat.indices) {
            for (c in activeMat[r].indices) {
                if (activeMat[r][c] != 0 && curY + r >= 0) {
                    ArcadeCanvas.fillRect(fb, OX + (curX + c) * CELL, OY + (curY + r) * CELL, CELL - 1, CELL - 1, pCol)
                }
            }
        }

        // Next piece preview box
        ArcadeCanvas.fillRect(fb, 126, 28, 28, 28, 0xFF1C1822.toInt())
        ArcadeCanvas.strokeRect(fb, 126, 28, 28, 28, 0xFFF2AA2B.toInt())
        val nMat = shapes[nextType]
        val nCol = colors[nextType % colors.size]
        for (r in nMat.indices) {
            for (c in nMat[r].indices) {
                if (nMat[r][c] != 0) {
                    ArcadeCanvas.fillRect(fb, 130 + c * 5, 34 + r * 5, 4, 4, nCol)
                }
            }
        }

        ArcadeCanvas.drawHudBar(fb, score, 1, level)
    }

    private fun lockPiece(audio: ArcadeSoundGen) {
        val mat = getMatrix(curType, curRot)
        for (r in mat.indices) {
            for (c in mat[r].indices) {
                if (mat[r][c] != 0 && curY + r in 0 until ROWS) {
                    board[curY + r][curX + c] = curType + 1
                }
            }
        }
        var cleared = 0
        var r = ROWS - 1
        while (r >= 0) {
            if (board[r].all { it != 0 }) {
                for (rr in r downTo 1) board[rr] = board[rr - 1].clone()
                board[0].fill(0)
                cleared++
            } else {
                r--
            }
        }
        if (cleared > 0) {
            lines += cleared
            score += cleared * cleared * 100
            level = 1 + lines / 8
            audio.play(SoundFx.CLEAR)
        } else {
            score += 10
            audio.play(SoundFx.EAT)
        }
        spawnPiece()
    }

    override fun getScore(): Int = score
    override fun writeState(buf: ByteBuffer) {
        buf.putInt(curType); buf.putInt(curRot); buf.putInt(curX); buf.putInt(curY); buf.putInt(score); buf.putInt(level)
        for (r in 0 until ROWS) for (c in 0 until COLS) buf.put(board[r][c].toByte())
    }
    override fun readState(buf: ByteBuffer) {
        curType = buf.int; curRot = buf.int; curX = buf.int; curY = buf.int; score = buf.int; level = buf.int
        for (r in 0 until ROWS) for (c in 0 until COLS) board[r][c] = buf.get().toInt()
    }
}

/**
 * 6. Snake Classic Arcade
 */
class SnakeArcadeGame(private val seed: Int) : ArcadeGame {
    companion object {
        const val COLS = 20
        const val ROWS = 22
        const val CELL = 8
    }

    private val snakeX = ArrayList<Int>()
    private val snakeY = ArrayList<Int>()
    private var dx = 1
    private var dy = 0
    private var foodX = 14
    private var foodY = 10
    private var score = 0
    private var tick = 0
    private var rng = Random(seed + 11)

    init { reset() }

    override fun reset() {
        snakeX.clear(); snakeY.clear()
        for (i in 0 until 4) { snakeX.add(8 - i); snakeY.add(11) }
        dx = 1; dy = 0; score = 0
        spawnFood()
    }

    private fun spawnFood() {
        foodX = rng.nextInt(1, COLS - 1)
        foodY = rng.nextInt(2, ROWS - 1)
    }

    override fun step(input: ArcadeInput, fb: IntArray, audio: ArcadeSoundGen) {
        tick++
        if (input.left && dx != 1) { dx = -1; dy = 0 }
        else if (input.right && dx != -1) { dx = 1; dy = 0 }
        else if (input.up && dy != 1) { dx = 0; dy = -1 }
        else if (input.down && dy != -1) { dx = 0; dy = 1 }

        val stepEvery = if (input.a) 4 else 8
        if (tick % stepEvery == 0) {
            val nx = (snakeX[0] + dx + COLS) % COLS
            val ny = ((snakeY[0] + dy - 2 + (ROWS - 2)) % (ROWS - 2)) + 2

            for (i in 0 until snakeX.size) {
                if (snakeX[i] == nx && snakeY[i] == ny) {
                    audio.play(SoundFx.EXPLODE)
                    reset()
                    return
                }
            }
            snakeX.add(0, nx); snakeY.add(0, ny)
            if (nx == foodX && ny == foodY) {
                score += 100
                audio.play(SoundFx.COIN)
                spawnFood()
            } else {
                snakeX.removeAt(snakeX.lastIndex)
                snakeY.removeAt(snakeY.lastIndex)
            }
        }

        ArcadeCanvas.clear(fb, 0xFF121810.toInt())
        ArcadeCanvas.strokeRect(fb, 0, 14, ArcadeCanvas.W, ArcadeCanvas.H - 14, 0xFF3E7028.toInt())
        ArcadeCanvas.fillCircle(fb, foodX * CELL + 4, foodY * CELL + 4, 3, 0xFFE85D1A.toInt())
        for (i in snakeX.indices) {
            val col = if (i == 0) 0xFFF2AA2B.toInt() else 0xFF56D844.toInt()
            ArcadeCanvas.fillRect(fb, snakeX[i] * CELL + 1, snakeY[i] * CELL + 1, CELL - 2, CELL - 2, col)
        }
        ArcadeCanvas.drawHudBar(fb, score, 1, 1 + score / 500)
    }

    override fun getScore(): Int = score
    override fun writeState(buf: ByteBuffer) { buf.putInt(score); buf.putInt(dx); buf.putInt(dy); buf.putInt(foodX); buf.putInt(foodY) }
    override fun readState(buf: ByteBuffer) { score = buf.int; dx = buf.int; dy = buf.int; foodX = buf.int; foodY = buf.int }
}
