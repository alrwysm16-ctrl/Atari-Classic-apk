package com.example.emulator

import java.nio.ByteBuffer
import kotlin.math.abs

/**
 * Atari 2600 TIA (Television Interface Adapter) scanline video + 2-channel audio synthesizer.
 */
class TiaChip {
    companion object {
        const val SCREEN_WIDTH = 160
        const val SCREEN_HEIGHT = 192
        const val CLOCKS_PER_LINE = 228
        const val HBLANK_CLOCKS = 68
    }

    val frameBuffer = IntArray(SCREEN_WIDTH * SCREEN_HEIGHT)

    // Video control registers
    var vsync = 0
    var vblank = 0
    var wsyncTriggered = false

    // Colors
    var colup0 = 0
    var colup1 = 0
    var colupf = 0
    var colubk = 0

    // Playfield
    var ctrlpf = 0
    var pf0 = 0
    var pf1 = 0
    var pf2 = 0

    // Player & Missile graphics / positions
    var grp0 = 0
    var grp1 = 0
    var grp0Delayed = 0
    var grp1Delayed = 0
    var vdelp0 = 0
    var vdelp1 = 0
    var vdelbl = 0

    var enam0 = 0
    var enam1 = 0
    var enabl = 0
    var enablDelayed = 0

    var nusiz0 = 0
    var nusiz1 = 0
    var refp0 = 0
    var refp1 = 0

    var posP0 = 0
    var posP1 = 0
    var posM0 = 0
    var posM1 = 0
    var posBL = 0

    var hmp0 = 0
    var hmp1 = 0
    var hmm0 = 0
    var hmm1 = 0
    var hmbl = 0

    var resmp0 = 0
    var resmp1 = 0

    // Collision latches
    var cxm0p = 0
    var cxm1p = 0
    var cxp0fb = 0
    var cxp1fb = 0
    var cxm0fb = 0
    var cxm1fb = 0
    var cxblpf = 0
    var cxppmm = 0

    // Fire button INPT4 (bit 7 = 0 when pressed)
    var inpt4 = 0x80

    // Audio registers
    val audc = IntArray(2)
    val audf = IntArray(2)
    val audv = IntArray(2)
    private val audioPhase = IntArray(2)
    private var lfsr = 0x155

    // Scanline timing state
    var scanlineClock = 0 // 0..227 within current scanline

    fun reset() {
        frameBuffer.fill(0xFF000000.toInt())
        vsync = 0
        vblank = 0
        wsyncTriggered = false
        colup0 = 0; colup1 = 0; colupf = 0; colubk = 0
        ctrlpf = 0; pf0 = 0; pf1 = 0; pf2 = 0
        grp0 = 0; grp1 = 0; grp0Delayed = 0; grp1Delayed = 0
        vdelp0 = 0; vdelp1 = 0; vdelbl = 0
        enam0 = 0; enam1 = 0; enabl = 0; enablDelayed = 0
        nusiz0 = 0; nusiz1 = 0; refp0 = 0; refp1 = 0
        posP0 = 0; posP1 = 0; posM0 = 0; posM1 = 0; posBL = 0
        hmp0 = 0; hmp1 = 0; hmm0 = 0; hmm1 = 0; hmbl = 0
        resmp0 = 0; resmp1 = 0
        clearCollisions()
        inpt4 = 0x80
        audc.fill(0); audf.fill(0); audv.fill(0)
        scanlineClock = 0
    }

    fun setFireButton(pressed: Boolean) {
        inpt4 = if (pressed) 0x00 else 0x80
    }

    fun read(addr: Int): Int {
        return when (addr and 0x0F) {
            0x00 -> cxm0p
            0x01 -> cxm1p
            0x02 -> cxp0fb
            0x03 -> cxp1fb
            0x04 -> cxm0fb
            0x05 -> cxm1fb
            0x06 -> cxblpf
            0x07 -> cxppmm
            0x08, 0x09, 0x0A, 0x0B -> 0x00
            0x0C -> inpt4
            0x0D -> 0x80
            else -> 0x00
        }
    }

    fun write(addr: Int, value: Int) {
        val v = value and 0xFF
        when (addr and 0x3F) {
            0x00 -> vsync = v
            0x01 -> vblank = v
            0x02 -> wsyncTriggered = true
            0x04 -> nusiz0 = v
            0x05 -> nusiz1 = v
            0x06 -> colup0 = v
            0x07 -> colup1 = v
            0x08 -> colupf = v
            0x09 -> colubk = v
            0x0A -> ctrlpf = v
            0x0B -> refp0 = v
            0x0C -> refp1 = v
            0x0D -> pf0 = v
            0x0E -> pf1 = v
            0x0F -> pf2 = v
            0x10 -> posP0 = currentVisibleX()
            0x11 -> posP1 = currentVisibleX()
            0x12 -> posM0 = currentVisibleX()
            0x13 -> posM1 = currentVisibleX()
            0x14 -> posBL = currentVisibleX()
            0x15 -> audc[0] = v and 0x0F
            0x16 -> audc[1] = v and 0x0F
            0x17 -> audf[0] = v and 0x1F
            0x18 -> audf[1] = v and 0x1F
            0x19 -> audv[0] = v and 0x0F
            0x1A -> audv[1] = v and 0x0F
            0x1B -> {
                grp0 = v
                grp1Delayed = grp1
            }
            0x1C -> {
                grp1 = v
                grp0Delayed = grp0
                enablDelayed = enabl
            }
            0x1D -> enam0 = v
            0x1E -> enam1 = v
            0x1F -> enabl = v
            0x20 -> hmp0 = signedNibble(v ushr 4)
            0x21 -> hmp1 = signedNibble(v ushr 4)
            0x22 -> hmm0 = signedNibble(v ushr 4)
            0x23 -> hmm1 = signedNibble(v ushr 4)
            0x24 -> hmbl = signedNibble(v ushr 4)
            0x25 -> vdelp0 = v and 1
            0x26 -> vdelp1 = v and 1
            0x27 -> vdelbl = v and 1
            0x28 -> resmp0 = v
            0x29 -> resmp1 = v
            0x2A -> applyHmove()
            0x2B -> {
                hmp0 = 0; hmp1 = 0; hmm0 = 0; hmm1 = 0; hmbl = 0
            }
            0x2C -> clearCollisions()
        }
    }

    private fun signedNibble(n: Int): Int {
        val v = n and 0x0F
        return if (v >= 8) v - 16 else v
    }

    private fun currentVisibleX(): Int {
        // Sprite positioning appears ~4-5 clocks after RESPx
        val raw = scanlineClock - HBLANK_CLOCKS + 5
        return raw.coerceIn(0, SCREEN_WIDTH - 1)
    }

    private fun applyHmove() {
        posP0 = wrapX(posP0 - hmp0)
        posP1 = wrapX(posP1 - hmp1)
        posM0 = wrapX(posM0 - hmm0)
        posM1 = wrapX(posM1 - hmm1)
        posBL = wrapX(posBL - hmbl)
    }

    private fun wrapX(x: Int): Int {
        var r = x % SCREEN_WIDTH
        if (r < 0) r += SCREEN_WIDTH
        return r
    }

    private fun clearCollisions() {
        cxm0p = 0; cxm1p = 0; cxp0fb = 0; cxp1fb = 0
        cxm0fb = 0; cxm1fb = 0; cxblpf = 0; cxppmm = 0
    }

    /**
     * Render one visible scanline (0..191) into frameBuffer and update hardware collision latches.
     */
    fun renderScanline(visibleY: Int) {
        if (visibleY !in 0 until SCREEN_HEIGHT) return
        val rowOffset = visibleY * SCREEN_WIDTH

        if ((vblank and 0x02) != 0) {
            val black = 0xFF000000.toInt()
            for (x in 0 until SCREEN_WIDTH) {
                frameBuffer[rowOffset + x] = black
            }
            return
        }

        val bgArgb = NtscPalette.getArgb(colubk)
        val pfArgb = NtscPalette.getArgb(colupf)
        val p0Argb = NtscPalette.getArgb(colup0)
        val p1Argb = NtscPalette.getArgb(colup1)

        val p0Gfx = if (vdelp0 != 0) grp0Delayed else grp0
        val p1Gfx = if (vdelp1 != 0) grp1Delayed else grp1
        val blOn = (if (vdelbl != 0) enablDelayed else enabl) and 0x02 != 0
        val m0On = (enam0 and 0x02) != 0 && (resmp0 and 0x02) == 0
        val m1On = (enam1 and 0x02) != 0 && (resmp1 and 0x02) == 0

        val reflectPf = (ctrlpf and 0x01) != 0
        val scoreMode = (ctrlpf and 0x02) != 0
        val pfPriority = (ctrlpf and 0x04) != 0
        val blSize = 1 shl ((ctrlpf ushr 4) and 0x03)

        for (x in 0 until SCREEN_WIDTH) {
            val pfHit = isPlayfieldPixelOn(x, reflectPf)
            val p0Hit = isPlayerPixelOn(x, posP0, p0Gfx, (refp0 and 0x08) != 0, nusiz0)
            val p1Hit = isPlayerPixelOn(x, posP1, p1Gfx, (refp1 and 0x08) != 0, nusiz1)
            val m0Hit = m0On && isPointInWidth(x, posM0, 1 shl ((nusiz0 ushr 4) and 0x03))
            val m1Hit = m1On && isPointInWidth(x, posM1, 1 shl ((nusiz1 ushr 4) and 0x03))
            val blHit = blOn && isPointInWidth(x, posBL, blSize)

            // Update hardware collision flags
            if (m0Hit && p1Hit) cxm0p = cxm0p or 0x80
            if (m0Hit && p0Hit) cxm0p = cxm0p or 0x40
            if (m1Hit && p0Hit) cxm1p = cxm1p or 0x80
            if (m1Hit && p1Hit) cxm1p = cxm1p or 0x40
            if (p0Hit && pfHit) cxp0fb = cxp0fb or 0x80
            if (p0Hit && blHit) cxp0fb = cxp0fb or 0x40
            if (p1Hit && pfHit) cxp1fb = cxp1fb or 0x80
            if (p1Hit && blHit) cxp1fb = cxp1fb or 0x40
            if (m0Hit && pfHit) cxm0fb = cxm0fb or 0x80
            if (m0Hit && blHit) cxm0fb = cxm0fb or 0x40
            if (m1Hit && pfHit) cxm1fb = cxm1fb or 0x80
            if (m1Hit && blHit) cxm1fb = cxm1fb or 0x40
            if (blHit && pfHit) cxblpf = cxblpf or 0x80
            if (p0Hit && p1Hit) cxppmm = cxppmm or 0x80
            if (m0Hit && m1Hit) cxppmm = cxppmm or 0x40

            val pfColor = when {
                scoreMode && x < 80 -> p0Argb
                scoreMode -> p1Argb
                else -> pfArgb
            }

            val color = if (pfPriority) {
                when {
                    pfHit || blHit -> pfColor
                    p0Hit || m0Hit -> p0Argb
                    p1Hit || m1Hit -> p1Argb
                    else -> bgArgb
                }
            } else {
                when {
                    p0Hit || m0Hit -> p0Argb
                    p1Hit || m1Hit -> p1Argb
                    pfHit || blHit -> pfColor
                    else -> bgArgb
                }
            }
            frameBuffer[rowOffset + x] = color
        }
    }

    private fun isPlayfieldPixelOn(x: Int, reflect: Boolean): Boolean {
        val pfPix = x ushr 2 // 0..39 (4 color clocks per playfield bit)
        val idx = if (pfPix < 20) {
            pfPix
        } else if (reflect) {
            39 - pfPix
        } else {
            pfPix - 20
        }
        return when {
            idx < 4 -> (pf0 and (1 shl (4 + idx))) != 0
            idx < 12 -> (pf1 and (1 shl (7 - (idx - 4)))) != 0
            else -> (pf2 and (1 shl (idx - 12))) != 0
        }
    }

    private fun isPlayerPixelOn(
        x: Int,
        pos: Int,
        gfx: Int,
        reflect: Boolean,
        nusiz: Int
    ): Boolean {
        if (gfx == 0) return false
        val mode = nusiz and 0x07
        val scale = when (mode) {
            5 -> 2
            7 -> 4
            else -> 1
        }
        val dx = x - pos
        if (dx < 0 || dx >= 8 * scale) {
            // Check repeated copies if NUSIZ copies enabled
            val copyOffset = when (mode) {
                1 -> if (dx in 16 until 24) dx - 16 else -1
                2 -> if (dx in 32 until 40) dx - 32 else -1
                3 -> when (dx) {
                    in 16 until 24 -> dx - 16
                    in 32 until 40 -> dx - 32
                    else -> -1
                }
                4 -> if (dx in 64 until 72) dx - 64 else -1
                6 -> when (dx) {
                    in 32 until 40 -> dx - 32
                    in 64 until 72 -> dx - 64
                    else -> -1
                }
                else -> -1
            }
            if (copyOffset < 0) return false
            val bitIdx = if (reflect) copyOffset else 7 - copyOffset
            return (gfx and (1 shl bitIdx)) != 0
        }
        val px = dx / scale
        val bitIdx = if (reflect) px else 7 - px
        return (gfx and (1 shl bitIdx)) != 0
    }

    private fun isPointInWidth(x: Int, pos: Int, width: Int): Boolean {
        return x >= pos && x < pos + width
    }

    /**
     * Synthesize PCM audio samples for one frame (22050 Hz / 60 fps = 367 samples).
     */
    fun generateAudioSamples(out: ShortArray, count: Int) {
        for (i in 0 until count) {
            var mixed = 0
            for (ch in 0..1) {
                val vol = audv[ch]
                val ctrl = audc[ch]
                if (vol == 0 || ctrl == 0) continue
                val period = (audf[ch] + 1) * 2
                audioPhase[ch]++
                val waveBit = when (ctrl) {
                    1, 2, 3, 6, 7, 10, 12, 13 -> ((audioPhase[ch] / period) and 1)
                    4, 5 -> if ((audioPhase[ch] / (period * 2)) and 1 == 0) 1 else 0
                    8, 9, 14, 15 -> {
                        if (audioPhase[ch] % period == 0) {
                            val bit = ((lfsr ushr 0) xor (lfsr ushr 2)) and 1
                            lfsr = (lfsr ushr 1) or (bit shl 8)
                        }
                        lfsr and 1
                    }
                    else -> 1
                }
                val amp = if (waveBit != 0) vol * 700 else -vol * 700
                mixed += amp
            }
            out[i] = mixed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    fun writeState(buf: ByteBuffer) {
        buf.putInt(vsync); buf.putInt(vblank)
        buf.putInt(colup0); buf.putInt(colup1); buf.putInt(colupf); buf.putInt(colubk)
        buf.putInt(ctrlpf); buf.putInt(pf0); buf.putInt(pf1); buf.putInt(pf2)
        buf.putInt(grp0); buf.putInt(grp1); buf.putInt(grp0Delayed); buf.putInt(grp1Delayed)
        buf.putInt(vdelp0); buf.putInt(vdelp1); buf.putInt(vdelbl)
        buf.putInt(enam0); buf.putInt(enam1); buf.putInt(enabl); buf.putInt(enablDelayed)
        buf.putInt(nusiz0); buf.putInt(nusiz1); buf.putInt(refp0); buf.putInt(refp1)
        buf.putInt(posP0); buf.putInt(posP1); buf.putInt(posM0); buf.putInt(posM1); buf.putInt(posBL)
        buf.putInt(hmp0); buf.putInt(hmp1); buf.putInt(hmm0); buf.putInt(hmm1); buf.putInt(hmbl)
        buf.putInt(cxm0p); buf.putInt(cxm1p); buf.putInt(cxp0fb); buf.putInt(cxp1fb)
        buf.putInt(cxm0fb); buf.putInt(cxm1fb); buf.putInt(cxblpf); buf.putInt(cxppmm)
        for (i in 0..1) {
            buf.putInt(audc[i]); buf.putInt(audf[i]); buf.putInt(audv[i])
        }
    }

    fun readState(buf: ByteBuffer) {
        vsync = buf.int; vblank = buf.int
        colup0 = buf.int; colup1 = buf.int; colupf = buf.int; colubk = buf.int
        ctrlpf = buf.int; pf0 = buf.int; pf1 = buf.int; pf2 = buf.int
        grp0 = buf.int; grp1 = buf.int; grp0Delayed = buf.int; grp1Delayed = buf.int
        vdelp0 = buf.int; vdelp1 = buf.int; vdelbl = buf.int
        enam0 = buf.int; enam1 = buf.int; enabl = buf.int; enablDelayed = buf.int
        nusiz0 = buf.int; nusiz1 = buf.int; refp0 = buf.int; refp1 = buf.int
        posP0 = buf.int; posP1 = buf.int; posM0 = buf.int; posM1 = buf.int; posBL = buf.int
        hmp0 = buf.int; hmp1 = buf.int; hmm0 = buf.int; hmm1 = buf.int; hmbl = buf.int
        cxm0p = buf.int; cxm1p = buf.int; cxp0fb = buf.int; cxp1fb = buf.int
        cxm0fb = buf.int; cxm1fb = buf.int; cxblpf = buf.int; cxppmm = buf.int
        for (i in 0..1) {
            audc[i] = buf.int; audf[i] = buf.int; audv[i] = buf.int
        }
    }
}
