package com.example.emulator

import java.nio.ByteBuffer

/**
 * 6532 RIOT (RAM, I/O, Timer) for Atari 2600.
 */
class Riot6532 {
    val ram = ByteArray(128)
    var swcha: Int = 0xFF // Player 0/1 directions (active-low)
    var swchb: Int = 0x0B // Console switches (bit0=Reset, bit1=Select, bit3=Color, bits6-7=Diff)

    private var timerValue: Int = 0
    private var timerInterval: Int = 1024
    private var timerCycles: Int = 0
    private var timerUnderflow: Boolean = false

    fun reset() {
        ram.fill(0)
        swcha = 0xFF
        swchb = 0x0B
        timerValue = 0xFF
        timerInterval = 1024
        timerCycles = 0
        timerUnderflow = false
    }

    fun setJoystick(up: Boolean, down: Boolean, left: Boolean, right: Boolean) {
        var v = 0xFF
        if (up) v = v and 0xEF    // bit 4
        if (down) v = v and 0xDF  // bit 5
        if (left) v = v and 0xBF  // bit 6
        if (right) v = v and 0x7F // bit 7
        swcha = v
    }

    fun setSwitches(resetPressed: Boolean, selectPressed: Boolean) {
        var v = 0x0B
        if (resetPressed) v = v and 0xFE  // bit 0 active low
        if (selectPressed) v = v and 0xFD // bit 1 active low
        swchb = v
    }

    fun tick(cpuCycles: Int) {
        timerCycles -= cpuCycles
        while (timerCycles <= 0) {
            if (timerValue == 0) {
                timerUnderflow = true
                timerValue = 0xFF
                timerInterval = 1 // After underflow, decrements every cycle
            } else {
                timerValue--
            }
            timerCycles += timerInterval
        }
    }

    fun read(addr: Int): Int {
        return when (addr and 0x2FF) {
            0x280 -> swcha
            0x282 -> swchb
            0x284, 0x286 -> {
                timerUnderflow = false
                timerValue and 0xFF
            }
            0x285, 0x287 -> if (timerUnderflow) 0x80 else 0x00
            else -> 0
        }
    }

    fun write(addr: Int, value: Int) {
        val reg = addr and 0x29F
        when (reg) {
            0x294 -> startTimer(value, 1)
            0x295 -> startTimer(value, 8)
            0x296 -> startTimer(value, 64)
            0x297 -> startTimer(value, 1024)
            0x29C -> startTimer(value, 1)
            0x29D -> startTimer(value, 8)
            0x29E -> startTimer(value, 64)
            0x29F -> startTimer(value, 1024)
        }
    }

    private fun startTimer(value: Int, interval: Int) {
        timerValue = value and 0xFF
        timerInterval = interval
        timerCycles = interval
        timerUnderflow = false
    }

    fun writeState(buf: ByteBuffer) {
        buf.put(ram)
        buf.putInt(swcha)
        buf.putInt(swchb)
        buf.putInt(timerValue)
        buf.putInt(timerInterval)
        buf.putInt(timerCycles)
        buf.put(if (timerUnderflow) 1 else 0)
    }

    fun readState(buf: ByteBuffer) {
        buf.get(ram)
        swcha = buf.int
        swchb = buf.int
        timerValue = buf.int
        timerInterval = buf.int
        timerCycles = buf.int
        timerUnderflow = buf.get() != 0.toByte()
    }
}
