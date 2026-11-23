package com.example.emulator

import java.nio.ByteBuffer
import java.util.ArrayDeque

/**
 * Complete Atari 2600 Machine Engine:
 * - 6502/6507 CPU + TIA + 6532 RIOT
 * - VSYNC-synchronized NTSC frame stepping (prevents rolling/black frames)
 * - Supports 2KB, 4KB, F8 (8KB), F6 (16KB) cartridges and strips .a78/.lnx headers
 * - Full state serialization + circular Rewind buffer
 */
class Atari2600Engine : MemoryBus {
    val tia = TiaChip()
    val riot = Riot6532()
    val cpu = Cpu6502(this)

    private var rom: ByteArray = ByteArray(2048)
    private var currentBank = 0
    private var bankMode = BankMode.PLAIN_2K_4K
    private var frameCounter = 0L

    private val rewindHistory = ArrayDeque<ByteArray>(120)
    private val audioBuffer = ShortArray(AtariAudioPlayer.SAMPLES_PER_FRAME)

    enum class BankMode { PLAIN_2K_4K, F8_8K, F6_16K }

    fun loadRom(rawBytes: ByteArray): Result<Unit> {
        return runCatching {
            val payload = stripKnownHeader(rawBytes)
            require(payload.isNotEmpty()) { "ملف ROM فارغ أو غير صالح" }
            rom = payload
            bankMode = when {
                payload.size <= 4096 -> BankMode.PLAIN_2K_4K
                payload.size <= 8192 -> BankMode.F8_8K
                else -> BankMode.F6_16K
            }
            reset()
        }
    }

    private fun stripKnownHeader(bytes: ByteArray): ByteArray {
        if (bytes.size > 128 && String(bytes, 1, 8, Charsets.US_ASCII) == "ATARI780") {
            return bytes.copyOfRange(128, bytes.size)
        }
        if (bytes.size > 64 && String(bytes, 0, 4, Charsets.US_ASCII) == "LYNX") {
            return bytes.copyOfRange(64, bytes.size)
        }
        return bytes
    }

    fun reset() {
        currentBank = 0
        frameCounter = 0L
        rewindHistory.clear()
        tia.reset()
        riot.reset()
        cpu.reset()
    }

    fun setInputs(
        up: Boolean,
        down: Boolean,
        left: Boolean,
        right: Boolean,
        fireA: Boolean,
        actionB: Boolean,
        resetSwitch: Boolean = false
    ) {
        riot.setJoystick(up, down, left, right)
        riot.setSwitches(resetPressed = resetSwitch, selectPressed = actionB)
        tia.setFireButton(fireA)
    }

    override fun read(addr: Int): Int {
        val masked = addr and 0x1FFF
        if ((masked and 0x1000) != 0) {
            val offset = masked and 0x0FFF
            when (bankMode) {
                BankMode.F8_8K -> {
                    if (offset == 0x0FF8) currentBank = 0
                    else if (offset == 0x0FF9) currentBank = 1
                }
                BankMode.F6_16K -> {
                    if (offset in 0x0FF6..0x0FF9) currentBank = offset - 0x0FF6
                }
                BankMode.PLAIN_2K_4K -> Unit
            }
            val romAddr = when (bankMode) {
                BankMode.PLAIN_2K_4K -> offset % rom.size
                BankMode.F8_8K, BankMode.F6_16K -> (currentBank * 4096 + offset) % rom.size
            }
            return rom[romAddr].toInt() and 0xFF
        }

        if ((masked and 0x0280) == 0x0080) {
            return riot.ram[masked and 0x7F].toInt() and 0xFF
        }

        if ((masked and 0x0280) == 0x0280) {
            return riot.read(masked)
        }

        return tia.read(masked)
    }

    override fun write(addr: Int, value: Int) {
        val masked = addr and 0x1FFF
        if ((masked and 0x1000) != 0) {
            val offset = masked and 0x0FFF
            when (bankMode) {
                BankMode.F8_8K -> {
                    if (offset == 0x0FF8) currentBank = 0
                    else if (offset == 0x0FF9) currentBank = 1
                }
                BankMode.F6_16K -> {
                    if (offset in 0x0FF6..0x0FF9) currentBank = offset - 0x0FF6
                }
                BankMode.PLAIN_2K_4K -> Unit
            }
            return
        }

        if ((masked and 0x0280) == 0x0080) {
            riot.ram[masked and 0x7F] = (value and 0xFF).toByte()
            return
        }

        if ((masked and 0x0280) == 0x0280) {
            riot.write(masked, value)
            return
        }

        tia.write(masked, value)
    }

    /**
     * Executes one NTSC frame synchronized on VSYNC / VBLANK edges so external 6502 ROMs
     * never drift into black/rolling frames even if a custom kernel runs slightly long.
     */
    fun stepFrame(audioOut: ShortArray? = null): Int {
        if (frameCounter % 15L == 0L) {
            if (rewindHistory.size >= 120) rewindHistory.removeFirst()
            rewindHistory.addLast(saveState())
        }
        frameCounter++

        var visibleLine = 0
        var sawVisible = false
        var vsyncWasOn = (tia.vsync and 0x02) != 0

        for (scanline in 0 until 312) {
            tia.scanlineClock = 0
            tia.wsyncTriggered = false

            while (tia.scanlineClock < TiaChip.CLOCKS_PER_LINE) {
                if (tia.wsyncTriggered) {
                    val remainingClocks = TiaChip.CLOCKS_PER_LINE - tia.scanlineClock
                    val remainingCpuCycles = (remainingClocks + 2) / 3
                    riot.tick(remainingCpuCycles)
                    tia.scanlineClock = TiaChip.CLOCKS_PER_LINE
                    tia.wsyncTriggered = false
                    break
                }
                val cpuCycles = cpu.step()
                riot.tick(cpuCycles)
                tia.scanlineClock += cpuCycles * 3
            }

            val vsyncNow = (tia.vsync and 0x02) != 0
            val vblankNow = (tia.vblank and 0x02) != 0

            if (!vblankNow && !vsyncNow && visibleLine < TiaChip.SCREEN_HEIGHT) {
                tia.renderScanline(visibleLine)
                visibleLine++
                sawVisible = true
            } else if (sawVisible && vsyncNow && !vsyncWasOn) {
                // Next frame's VSYNC started cleanly
                break
            }
            vsyncWasOn = vsyncNow
        }

        if (audioOut != null) {
            tia.generateAudioSamples(audioOut, audioOut.size)
            return audioOut.size
        }
        return 0
    }

    fun getAudioBuffer(): ShortArray = audioBuffer

    fun rewindStep(): Boolean {
        val snap = rewindHistory.pollLast() ?: return false
        return loadState(snap)
    }

    fun canRewind(): Boolean = rewindHistory.isNotEmpty()

    fun getCurrentScoreEstimate(): Int {
        val raw = riot.ram[0x08].toInt() and 0xFF
        return raw * 100
    }

    fun saveState(): ByteArray {
        val buf = ByteBuffer.allocate(512)
        buf.putInt(0x41543236)
        buf.putInt(currentBank)
        buf.putLong(frameCounter)
        cpu.writeState(buf)
        riot.writeState(buf)
        tia.writeState(buf)
        val len = buf.position()
        return buf.array().copyOf(len)
    }

    fun loadState(data: ByteArray): Boolean {
        return runCatching {
            val buf = ByteBuffer.wrap(data)
            val magic = buf.int
            if (magic != 0x41543236) return false
            currentBank = buf.int
            frameCounter = buf.long
            cpu.readState(buf)
            riot.readState(buf)
            tia.readState(buf)
            true
        }.getOrDefault(false)
    }
}
