package com.example.emulator

import com.example.data.model.GameEntity
import com.example.emulator.arcade.ArcadeCanvas
import com.example.emulator.arcade.ArcadeGame
import com.example.emulator.arcade.ArcadeInput
import com.example.emulator.arcade.ArcadeSoundGen
import com.example.emulator.arcade.AsteroidsArcadeGame
import com.example.emulator.arcade.BarrelKongArcadeGame
import com.example.emulator.arcade.BreakoutArcadeGame
import com.example.emulator.arcade.CentipedeArcadeGame
import com.example.emulator.arcade.EnduroRacerArcadeGame
import com.example.emulator.arcade.FroggerArcadeGame
import com.example.emulator.arcade.GalagaArcadeGame
import com.example.emulator.arcade.MissileCommandArcadeGame
import com.example.emulator.arcade.PacManArcadeGame
import com.example.emulator.arcade.PitfallJungleArcadeGame
import com.example.emulator.arcade.PongArcadeGame
import com.example.emulator.arcade.RiverRaidArcadeGame
import com.example.emulator.arcade.SnakeArcadeGame
import com.example.emulator.arcade.SpaceInvadersArcadeGame
import com.example.emulator.arcade.SuperMarioArcadeGame
import com.example.emulator.arcade.TetrisArcadeGame
import java.nio.ByteBuffer
import java.util.ArrayDeque

/**
 * Unified Emulator Core dispatching between:
 * 1) 16 dedicated native 60fps 160x192 retro arcade game engines (Mario, Pac-Man, Space Invaders, etc.)
 * 2) Full 6502 + TIA + RIOT 6532 Atari 2600 hardware emulator for external/imported ROM files
 */
class UnifiedAtariEngine {
    companion object {
        const val SCREEN_WIDTH = ArcadeCanvas.W
        const val SCREEN_HEIGHT = ArcadeCanvas.H
        private const val ARCADE_MAGIC = 0x41524331 // "ARC1"
    }

    private val romEngine = Atari2600Engine()
    private var arcadeGame: ArcadeGame? = null
    private var currentEngineType: String = "rom2600"
    private var currentSeed: Int = 0

    private val arcadeFrameBuffer = IntArray(SCREEN_WIDTH * SCREEN_HEIGHT)
    private val arcadeSound = ArcadeSoundGen()
    private val audioBuffer = ShortArray(AtariAudioPlayer.SAMPLES_PER_FRAME)
    private val arcadeRewind = ArrayDeque<ByteArray>(120)
    private var arcadeFrameCount = 0L

    private var prevA = false
    private var prevB = false
    private var inputUp = false
    private var inputDown = false
    private var inputLeft = false
    private var inputRight = false
    private var inputA = false
    private var inputB = false
    private var inputReset = false

    val frameBuffer: IntArray
        get() = if (arcadeGame != null) arcadeFrameBuffer else romEngine.tia.frameBuffer

    fun loadGame(game: GameEntity, romBytes: ByteArray?): Result<Unit> {
        prevA = false
        prevB = false
        arcadeRewind.clear()
        arcadeFrameCount = 0L

        if (!game.isImported && game.engineType != "rom2600") {
            currentEngineType = game.engineType
            currentSeed = game.variantSeed
            val instance = createArcadeGame(game.engineType, game.variantSeed)
            arcadeGame = instance
            instance.reset()
            // Render initial frame immediately so first draw is crisp, never black
            instance.step(ArcadeInput(), arcadeFrameBuffer, arcadeSound)
            return Result.success(Unit)
        }

        arcadeGame = null
        currentEngineType = "rom2600"
        val bytes = romBytes ?: return Result.failure(IllegalArgumentException("ROM bytes missing"))
        return romEngine.loadRom(bytes).also { res ->
            if (res.isSuccess) {
                // Step 2 warmup frames so TV VSYNC locks and initial screen shows immediately
                romEngine.stepFrame(null)
                romEngine.stepFrame(null)
            }
        }
    }

    fun loadRawRom(romBytes: ByteArray): Result<Unit> {
        arcadeGame = null
        currentEngineType = "rom2600"
        return romEngine.loadRom(romBytes)
    }

    private fun createArcadeGame(type: String, seed: Int): ArcadeGame = when (type.lowercase()) {
        "mario" -> SuperMarioArcadeGame(seed)
        "kong" -> BarrelKongArcadeGame(seed)
        "pitfall" -> PitfallJungleArcadeGame(seed)
        "pacman" -> PacManArcadeGame(seed)
        "tetris" -> TetrisArcadeGame(seed)
        "snake" -> SnakeArcadeGame(seed)
        "invaders" -> SpaceInvadersArcadeGame(seed)
        "galaga" -> GalagaArcadeGame(seed)
        "river" -> RiverRaidArcadeGame(seed)
        "asteroids" -> AsteroidsArcadeGame(seed)
        "centipede" -> CentipedeArcadeGame(seed)
        "missile" -> MissileCommandArcadeGame(seed)
        "breakout" -> BreakoutArcadeGame(seed)
        "pong" -> PongArcadeGame(seed)
        "frogger" -> FroggerArcadeGame(seed)
        "enduro" -> EnduroRacerArcadeGame(seed)
        else -> SuperMarioArcadeGame(seed)
    }

    fun setInputs(
        up: Boolean,
        down: Boolean,
        left: Boolean,
        right: Boolean,
        fireA: Boolean,
        actionB: Boolean,
        resetPressed: Boolean = false
    ) {
        inputUp = up
        inputDown = down
        inputLeft = left
        inputRight = right
        inputA = fireA
        inputB = actionB
        inputReset = resetPressed
        if (arcadeGame == null) {
            romEngine.setInputs(up, down, left, right, fireA, actionB, resetPressed)
        }
    }

    fun stepFrame(audioOut: ShortArray? = null): Int {
        val g = arcadeGame
        if (g == null) {
            return romEngine.stepFrame(audioOut)
        }

        if (inputReset) {
            g.reset()
        }

        if (arcadeFrameCount % 15L == 0L) {
            if (arcadeRewind.size >= 120) arcadeRewind.removeFirst()
            arcadeRewind.addLast(saveState())
        }
        arcadeFrameCount++

        val aJust = inputA && !prevA
        val bJust = inputB && !prevB
        prevA = inputA
        prevB = inputB

        val inp = ArcadeInput(
            up = inputUp,
            down = inputDown,
            left = inputLeft,
            right = inputRight,
            a = inputA,
            b = inputB,
            aJustPressed = aJust,
            bJustPressed = bJust
        )
        g.step(inp, arcadeFrameBuffer, arcadeSound)

        if (audioOut != null) {
            arcadeSound.fillSamples(audioOut, audioOut.size)
            return audioOut.size
        }
        return 0
    }

    fun getAudioBuffer(): ShortArray = if (arcadeGame != null) audioBuffer else romEngine.getAudioBuffer()

    fun reset() {
        prevA = false
        prevB = false
        arcadeRewind.clear()
        arcadeFrameCount = 0L
        val g = arcadeGame
        if (g != null) {
            g.reset()
            g.step(ArcadeInput(), arcadeFrameBuffer, arcadeSound)
        } else {
            romEngine.reset()
        }
    }

    fun canRewind(): Boolean = if (arcadeGame != null) arcadeRewind.isNotEmpty() else romEngine.canRewind()

    fun rewindStep(): Boolean {
        val g = arcadeGame ?: return romEngine.rewindStep()
        val snap = arcadeRewind.pollLast() ?: return false
        val ok = loadState(snap)
        if (ok) {
            g.step(ArcadeInput(), arcadeFrameBuffer, arcadeSound)
        }
        return ok
    }

    fun getCurrentScoreEstimate(): Int {
        val g = arcadeGame
        return g?.getScore() ?: romEngine.getCurrentScoreEstimate()
    }

    fun saveState(): ByteArray {
        val g = arcadeGame ?: return romEngine.saveState()
        val buf = ByteBuffer.allocate(2048)
        buf.putInt(ARCADE_MAGIC)
        buf.putLong(arcadeFrameCount)
        g.writeState(buf)
        val len = buf.position()
        return buf.array().copyOf(len)
    }

    fun loadState(data: ByteArray): Boolean {
        val g = arcadeGame ?: return romEngine.loadState(data)
        return runCatching {
            val buf = ByteBuffer.wrap(data)
            val magic = buf.int
            if (magic != ARCADE_MAGIC) return false
            arcadeFrameCount = buf.long
            g.readState(buf)
            g.step(ArcadeInput(), arcadeFrameBuffer, arcadeSound)
            true
        }.getOrDefault(false)
    }
}
