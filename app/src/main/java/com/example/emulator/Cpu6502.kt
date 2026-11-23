package com.example.emulator

import java.nio.ByteBuffer

interface MemoryBus {
    fun read(addr: Int): Int
    fun write(addr: Int, value: Int)
}

/**
 * Complete MOS 6502 / 6507 CPU core for Atari 2600 emulation.
 */
class Cpu6502(private val bus: MemoryBus) {
    var a: Int = 0
    var x: Int = 0
    var y: Int = 0
    var sp: Int = 0xFD
    var pc: Int = 0
    var p: Int = 0x24 // Status flags NV-BDIZC

    companion object {
        const val FLAG_C = 0x01
        const val FLAG_Z = 0x02
        const val FLAG_I = 0x04
        const val FLAG_D = 0x08
        const val FLAG_B = 0x10
        const val FLAG_U = 0x20
        const val FLAG_V = 0x40
        const val FLAG_N = 0x80
    }

    fun reset() {
        a = 0; x = 0; y = 0; sp = 0xFD; p = 0x24
        val lo = bus.read(0xFFFC)
        val hi = bus.read(0xFFFD)
        pc = ((hi shl 8) or lo) and 0xFFFF
    }

    private fun readByte(addr: Int): Int = bus.read(addr and 0xFFFF) and 0xFF
    private fun writeByte(addr: Int, v: Int) = bus.write(addr and 0xFFFF, v and 0xFF)

    private fun fetchByte(): Int {
        val v = readByte(pc)
        pc = (pc + 1) and 0xFFFF
        return v
    }

    private fun fetchWord(): Int {
        val lo = fetchByte()
        val hi = fetchByte()
        return (hi shl 8) or lo
    }

    private fun push(v: Int) {
        writeByte(0x0100 or (sp and 0xFF), v)
        sp = (sp - 1) and 0xFF
    }

    private fun pop(): Int {
        sp = (sp + 1) and 0xFF
        return readByte(0x0100 or (sp and 0xFF))
    }

    private fun setFlag(flag: Int, cond: Boolean) {
        p = if (cond) (p or flag) else (p and flag.inv())
    }

    private fun getFlag(flag: Int): Boolean = (p and flag) != 0

    private fun setZN(v: Int): Int {
        val b = v and 0xFF
        setFlag(FLAG_Z, b == 0)
        setFlag(FLAG_N, (b and 0x80) != 0)
        return b
    }

    private fun addrZp(): Int = fetchByte()
    private fun addrZpx(): Int = (fetchByte() + x) and 0xFF
    private fun addrZpy(): Int = (fetchByte() + y) and 0xFF
    private fun addrAbs(): Int = fetchWord()
    private fun addrAbsX(): Int = (fetchWord() + x) and 0xFFFF
    private fun addrAbsY(): Int = (fetchWord() + y) and 0xFFFF

    private fun addrIndX(): Int {
        val zp = (fetchByte() + x) and 0xFF
        val lo = readByte(zp)
        val hi = readByte((zp + 1) and 0xFF)
        return (hi shl 8) or lo
    }

    private fun addrIndY(): Int {
        val zp = fetchByte()
        val lo = readByte(zp)
        val hi = readByte((zp + 1) and 0xFF)
        return (((hi shl 8) or lo) + y) and 0xFFFF
    }

    private fun branch(cond: Boolean): Int {
        val offset = fetchByte().toByte().toInt()
        if (!cond) return 2
        val oldPc = pc
        pc = (pc + offset) and 0xFFFF
        return if ((oldPc and 0xFF00) != (pc and 0xFF00)) 4 else 3
    }

    private fun adc(value: Int) {
        val carry = if (getFlag(FLAG_C)) 1 else 0
        if (getFlag(FLAG_D)) {
            var lo = (a and 0x0F) + (value and 0x0F) + carry
            if (lo > 9) lo += 6
            var hi = (a ushr 4) + (value ushr 4) + (if (lo > 15) 1 else 0)
            if (hi > 9) hi += 6
            val result = ((hi and 0x0F) shl 4) or (lo and 0x0F)
            setFlag(FLAG_C, hi > 15)
            setFlag(FLAG_V, false)
            a = setZN(result)
        } else {
            val sum = a + value + carry
            setFlag(FLAG_C, sum > 0xFF)
            setFlag(FLAG_V, ((a xor sum) and (value xor sum) and 0x80) != 0)
            a = setZN(sum)
        }
    }

    private fun sbc(value: Int) {
        val borrow = if (getFlag(FLAG_C)) 0 else 1
        if (getFlag(FLAG_D)) {
            var lo = (a and 0x0F) - (value and 0x0F) - borrow
            var hiBorrow = 0
            if (lo < 0) { lo -= 6; hiBorrow = 1 }
            var hi = (a ushr 4) - (value ushr 4) - hiBorrow
            setFlag(FLAG_C, hi >= 0)
            if (hi < 0) hi -= 6
            val result = ((hi and 0x0F) shl 4) or (lo and 0x0F)
            setFlag(FLAG_V, false)
            a = setZN(result)
        } else {
            adc(value xor 0xFF)
        }
    }

    private fun cmp(reg: Int, value: Int) {
        val diff = reg - value
        setFlag(FLAG_C, reg >= value)
        setZN(diff)
    }

    fun step(): Int {
        val op = fetchByte()
        return when (op) {
            // LDA
            0xA9 -> { a = setZN(fetchByte()); 2 }
            0xA5 -> { a = setZN(readByte(addrZp())); 3 }
            0xB5 -> { a = setZN(readByte(addrZpx())); 4 }
            0xAD -> { a = setZN(readByte(addrAbs())); 4 }
            0xBD -> { a = setZN(readByte(addrAbsX())); 4 }
            0xB9 -> { a = setZN(readByte(addrAbsY())); 4 }
            0xA1 -> { a = setZN(readByte(addrIndX())); 6 }
            0xB1 -> { a = setZN(readByte(addrIndY())); 5 }

            // LDX
            0xA2 -> { x = setZN(fetchByte()); 2 }
            0xA6 -> { x = setZN(readByte(addrZp())); 3 }
            0xB6 -> { x = setZN(readByte(addrZpy())); 4 }
            0xAE -> { x = setZN(readByte(addrAbs())); 4 }
            0xBE -> { x = setZN(readByte(addrAbsY())); 4 }

            // LDY
            0xA0 -> { y = setZN(fetchByte()); 2 }
            0xA4 -> { y = setZN(readByte(addrZp())); 3 }
            0xB4 -> { y = setZN(readByte(addrZpx())); 4 }
            0xAC -> { y = setZN(readByte(addrAbs())); 4 }
            0xBC -> { y = setZN(readByte(addrAbsX())); 4 }

            // STA
            0x85 -> { writeByte(addrZp(), a); 3 }
            0x95 -> { writeByte(addrZpx(), a); 4 }
            0x8D -> { writeByte(addrAbs(), a); 4 }
            0x9D -> { writeByte(addrAbsX(), a); 5 }
            0x99 -> { writeByte(addrAbsY(), a); 5 }
            0x81 -> { writeByte(addrIndX(), a); 6 }
            0x91 -> { writeByte(addrIndY(), a); 6 }

            // STX
            0x86 -> { writeByte(addrZp(), x); 3 }
            0x96 -> { writeByte(addrZpy(), x); 4 }
            0x8E -> { writeByte(addrAbs(), x); 4 }

            // STY
            0x84 -> { writeByte(addrZp(), y); 3 }
            0x94 -> { writeByte(addrZpx(), y); 4 }
            0x8C -> { writeByte(addrAbs(), y); 4 }

            // Transfers
            0xAA -> { x = setZN(a); 2 }
            0xA8 -> { y = setZN(a); 2 }
            0x8A -> { a = setZN(x); 2 }
            0x98 -> { a = setZN(y); 2 }
            0xBA -> { x = setZN(sp); 2 }
            0x9A -> { sp = x; 2 }

            // Stack
            0x48 -> { push(a); 3 }
            0x68 -> { a = setZN(pop()); 4 }
            0x08 -> { push(p or FLAG_B or FLAG_U); 3 }
            0x28 -> { p = (pop() and FLAG_B.inv()) or FLAG_U; 4 }

            // Logical AND
            0x29 -> { a = setZN(a and fetchByte()); 2 }
            0x25 -> { a = setZN(a and readByte(addrZp())); 3 }
            0x35 -> { a = setZN(a and readByte(addrZpx())); 4 }
            0x2D -> { a = setZN(a and readByte(addrAbs())); 4 }
            0x3D -> { a = setZN(a and readByte(addrAbsX())); 4 }
            0x39 -> { a = setZN(a and readByte(addrAbsY())); 4 }
            0x21 -> { a = setZN(a and readByte(addrIndX())); 6 }
            0x31 -> { a = setZN(a and readByte(addrIndY())); 5 }

            // Logical ORA
            0x09 -> { a = setZN(a or fetchByte()); 2 }
            0x05 -> { a = setZN(a or readByte(addrZp())); 3 }
            0x15 -> { a = setZN(a or readByte(addrZpx())); 4 }
            0x0D -> { a = setZN(a or readByte(addrAbs())); 4 }
            0x1D -> { a = setZN(a or readByte(addrAbsX())); 4 }
            0x19 -> { a = setZN(a or readByte(addrAbsY())); 4 }
            0x01 -> { a = setZN(a or readByte(addrIndX())); 6 }
            0x11 -> { a = setZN(a or readByte(addrIndY())); 5 }

            // Logical EOR
            0x49 -> { a = setZN(a xor fetchByte()); 2 }
            0x45 -> { a = setZN(a xor readByte(addrZp())); 3 }
            0x55 -> { a = setZN(a xor readByte(addrZpx())); 4 }
            0x4D -> { a = setZN(a xor readByte(addrAbs())); 4 }
            0x5D -> { a = setZN(a xor readByte(addrAbsX())); 4 }
            0x59 -> { a = setZN(a xor readByte(addrAbsY())); 4 }
            0x41 -> { a = setZN(a xor readByte(addrIndX())); 6 }
            0x51 -> { a = setZN(a xor readByte(addrIndY())); 5 }

            // BIT
            0x24 -> {
                val m = readByte(addrZp())
                setFlag(FLAG_Z, (a and m) == 0)
                setFlag(FLAG_V, (m and 0x40) != 0)
                setFlag(FLAG_N, (m and 0x80) != 0)
                3
            }
            0x2C -> {
                val m = readByte(addrAbs())
                setFlag(FLAG_Z, (a and m) == 0)
                setFlag(FLAG_V, (m and 0x40) != 0)
                setFlag(FLAG_N, (m and 0x80) != 0)
                4
            }

            // ADC
            0x69 -> { adc(fetchByte()); 2 }
            0x65 -> { adc(readByte(addrZp())); 3 }
            0x75 -> { adc(readByte(addrZpx())); 4 }
            0x6D -> { adc(readByte(addrAbs())); 4 }
            0x7D -> { adc(readByte(addrAbsX())); 4 }
            0x79 -> { adc(readByte(addrAbsY())); 4 }
            0x61 -> { adc(readByte(addrIndX())); 6 }
            0x71 -> { adc(readByte(addrIndY())); 5 }

            // SBC
            0xE9 -> { sbc(fetchByte()); 2 }
            0xE5 -> { sbc(readByte(addrZp())); 3 }
            0xF5 -> { sbc(readByte(addrZpx())); 4 }
            0xED -> { sbc(readByte(addrAbs())); 4 }
            0xFD -> { sbc(readByte(addrAbsX())); 4 }
            0xF9 -> { sbc(readByte(addrAbsY())); 4 }
            0xE1 -> { sbc(readByte(addrIndX())); 6 }
            0xF1 -> { sbc(readByte(addrIndY())); 5 }

            // CMP
            0xC9 -> { cmp(a, fetchByte()); 2 }
            0xC5 -> { cmp(a, readByte(addrZp())); 3 }
            0xD5 -> { cmp(a, readByte(addrZpx())); 4 }
            0xCD -> { cmp(a, readByte(addrAbs())); 4 }
            0xDD -> { cmp(a, readByte(addrAbsX())); 4 }
            0xD9 -> { cmp(a, readByte(addrAbsY())); 4 }
            0xC1 -> { cmp(a, readByte(addrIndX())); 6 }
            0xD1 -> { cmp(a, readByte(addrIndY())); 5 }

            // CPX
            0xE0 -> { cmp(x, fetchByte()); 2 }
            0xE4 -> { cmp(x, readByte(addrZp())); 3 }
            0xEC -> { cmp(x, readByte(addrAbs())); 4 }

            // CPY
            0xC0 -> { cmp(y, fetchByte()); 2 }
            0xC4 -> { cmp(y, readByte(addrZp())); 3 }
            0xCC -> { cmp(y, readByte(addrAbs())); 4 }

            // INC / DEC / INX / DEX / INY / DEY
            0xE6 -> { val ad = addrZp(); writeByte(ad, setZN(readByte(ad) + 1)); 5 }
            0xF6 -> { val ad = addrZpx(); writeByte(ad, setZN(readByte(ad) + 1)); 6 }
            0xEE -> { val ad = addrAbs(); writeByte(ad, setZN(readByte(ad) + 1)); 6 }
            0xFE -> { val ad = addrAbsX(); writeByte(ad, setZN(readByte(ad) + 1)); 7 }
            0xC6 -> { val ad = addrZp(); writeByte(ad, setZN(readByte(ad) - 1)); 5 }
            0xD6 -> { val ad = addrZpx(); writeByte(ad, setZN(readByte(ad) - 1)); 6 }
            0xCE -> { val ad = addrAbs(); writeByte(ad, setZN(readByte(ad) - 1)); 6 }
            0xDE -> { val ad = addrAbsX(); writeByte(ad, setZN(readByte(ad) - 1)); 7 }
            0xE8 -> { x = setZN(x + 1); 2 }
            0xCA -> { x = setZN(x - 1); 2 }
            0xC8 -> { y = setZN(y + 1); 2 }
            0x88 -> { y = setZN(y - 1); 2 }

            // Shifts & Rotates
            0x0A -> { setFlag(FLAG_C, (a and 0x80) != 0); a = setZN(a shl 1); 2 }
            0x06 -> { val ad = addrZp(); val m = readByte(ad); setFlag(FLAG_C, (m and 0x80) != 0); writeByte(ad, setZN(m shl 1)); 5 }
            0x16 -> { val ad = addrZpx(); val m = readByte(ad); setFlag(FLAG_C, (m and 0x80) != 0); writeByte(ad, setZN(m shl 1)); 6 }
            0x0E -> { val ad = addrAbs(); val m = readByte(ad); setFlag(FLAG_C, (m and 0x80) != 0); writeByte(ad, setZN(m shl 1)); 6 }
            0x1E -> { val ad = addrAbsX(); val m = readByte(ad); setFlag(FLAG_C, (m and 0x80) != 0); writeByte(ad, setZN(m shl 1)); 7 }

            0x4A -> { setFlag(FLAG_C, (a and 1) != 0); a = setZN(a ushr 1); 2 }
            0x46 -> { val ad = addrZp(); val m = readByte(ad); setFlag(FLAG_C, (m and 1) != 0); writeByte(ad, setZN(m ushr 1)); 5 }
            0x56 -> { val ad = addrZpx(); val m = readByte(ad); setFlag(FLAG_C, (m and 1) != 0); writeByte(ad, setZN(m ushr 1)); 6 }
            0x4E -> { val ad = addrAbs(); val m = readByte(ad); setFlag(FLAG_C, (m and 1) != 0); writeByte(ad, setZN(m ushr 1)); 6 }
            0x5E -> { val ad = addrAbsX(); val m = readByte(ad); setFlag(FLAG_C, (m and 1) != 0); writeByte(ad, setZN(m ushr 1)); 7 }

            0x2A -> {
                val c = if (getFlag(FLAG_C)) 1 else 0
                setFlag(FLAG_C, (a and 0x80) != 0)
                a = setZN((a shl 1) or c); 2
            }
            0x26 -> {
                val ad = addrZp(); val m = readByte(ad); val c = if (getFlag(FLAG_C)) 1 else 0
                setFlag(FLAG_C, (m and 0x80) != 0); writeByte(ad, setZN((m shl 1) or c)); 5
            }
            0x36 -> {
                val ad = addrZpx(); val m = readByte(ad); val c = if (getFlag(FLAG_C)) 1 else 0
                setFlag(FLAG_C, (m and 0x80) != 0); writeByte(ad, setZN((m shl 1) or c)); 6
            }
            0x2E -> {
                val ad = addrAbs(); val m = readByte(ad); val c = if (getFlag(FLAG_C)) 1 else 0
                setFlag(FLAG_C, (m and 0x80) != 0); writeByte(ad, setZN((m shl 1) or c)); 6
            }
            0x3E -> {
                val ad = addrAbsX(); val m = readByte(ad); val c = if (getFlag(FLAG_C)) 1 else 0
                setFlag(FLAG_C, (m and 0x80) != 0); writeByte(ad, setZN((m shl 1) or c)); 7
            }

            0x6A -> {
                val c = if (getFlag(FLAG_C)) 0x80 else 0
                setFlag(FLAG_C, (a and 1) != 0)
                a = setZN((a ushr 1) or c); 2
            }
            0x66 -> {
                val ad = addrZp(); val m = readByte(ad); val c = if (getFlag(FLAG_C)) 0x80 else 0
                setFlag(FLAG_C, (m and 1) != 0); writeByte(ad, setZN((m ushr 1) or c)); 5
            }
            0x76 -> {
                val ad = addrZpx(); val m = readByte(ad); val c = if (getFlag(FLAG_C)) 0x80 else 0
                setFlag(FLAG_C, (m and 1) != 0); writeByte(ad, setZN((m ushr 1) or c)); 6
            }
            0x6E -> {
                val ad = addrAbs(); val m = readByte(ad); val c = if (getFlag(FLAG_C)) 0x80 else 0
                setFlag(FLAG_C, (m and 1) != 0); writeByte(ad, setZN((m ushr 1) or c)); 6
            }
            0x7E -> {
                val ad = addrAbsX(); val m = readByte(ad); val c = if (getFlag(FLAG_C)) 0x80 else 0
                setFlag(FLAG_C, (m and 1) != 0); writeByte(ad, setZN((m ushr 1) or c)); 7
            }

            // Jumps & Calls
            0x4C -> { pc = fetchWord(); 3 }
            0x6C -> {
                val ptr = fetchWord()
                val lo = readByte(ptr)
                val hi = readByte((ptr and 0xFF00) or ((ptr + 1) and 0x00FF))
                pc = (hi shl 8) or lo
                5
            }
            0x20 -> {
                val target = fetchWord()
                val ret = (pc - 1) and 0xFFFF
                push(ret ushr 8)
                push(ret and 0xFF)
                pc = target
                6
            }
            0x60 -> {
                val lo = pop()
                val hi = pop()
                pc = (((hi shl 8) or lo) + 1) and 0xFFFF
                6
            }
            0x40 -> {
                p = (pop() and FLAG_B.inv()) or FLAG_U
                val lo = pop()
                val hi = pop()
                pc = (hi shl 8) or lo
                6
            }
            0x00 -> {
                pc = (pc + 1) and 0xFFFF
                push(pc ushr 8)
                push(pc and 0xFF)
                push(p or FLAG_B or FLAG_U)
                setFlag(FLAG_I, true)
                pc = (readByte(0xFFFF) shl 8) or readByte(0xFFFE)
                7
            }

            // Branches
            0x90 -> branch(!getFlag(FLAG_C)) // BCC
            0xB0 -> branch(getFlag(FLAG_C))  // BCS
            0xF0 -> branch(getFlag(FLAG_Z))  // BEQ
            0xD0 -> branch(!getFlag(FLAG_Z)) // BNE
            0x30 -> branch(getFlag(FLAG_N))  // BMI
            0x10 -> branch(!getFlag(FLAG_N)) // BPL
            0x50 -> branch(!getFlag(FLAG_V)) // BVC
            0x70 -> branch(getFlag(FLAG_V))  // BVS

            // Flags & NOP
            0x18 -> { setFlag(FLAG_C, false); 2 }
            0x38 -> { setFlag(FLAG_C, true); 2 }
            0xD8 -> { setFlag(FLAG_D, false); 2 }
            0xF8 -> { setFlag(FLAG_D, true); 2 }
            0x58 -> { setFlag(FLAG_I, false); 2 }
            0x78 -> { setFlag(FLAG_I, true); 2 }
            0xB8 -> { setFlag(FLAG_V, false); 2 }
            0xEA -> 2

            else -> 2 // Unofficial NOP fallback
        }
    }

    fun writeState(buf: ByteBuffer) {
        buf.putInt(a); buf.putInt(x); buf.putInt(y); buf.putInt(sp); buf.putInt(pc); buf.putInt(p)
    }

    fun readState(buf: ByteBuffer) {
        a = buf.int; x = buf.int; y = buf.int; sp = buf.int; pc = buf.int; p = buf.int
    }
}
