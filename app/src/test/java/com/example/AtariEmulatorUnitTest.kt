package com.example

import com.example.data.model.GameEntity
import com.example.emulator.Atari2600Engine
import com.example.emulator.TiaChip
import com.example.emulator.UnifiedAtariEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AtariEmulatorUnitTest {
    @Test
    fun bundledRomExecutesRendersAndSavesState() {
        val romFile = resolveAssetFile("src/main/assets/roms/rom_0001.a26")
        assertTrue("Expected bundled ROM file at ${romFile.absolutePath}", romFile.exists())

        val engine = Atari2600Engine()
        val loaded = engine.loadRom(romFile.readBytes())
        assertTrue("ROM should load cleanly", loaded.isSuccess)

        val audioBuf = engine.getAudioBuffer()
        repeat(20) { idx ->
            engine.setInputs(
                up = false,
                down = false,
                left = idx % 2 == 0,
                right = idx % 2 == 1,
                fireA = idx == 5,
                actionB = false
            )
            engine.stepFrame(audioBuf)
        }

        val uniqueColors = engine.tia.frameBuffer.toSet().size
        assertTrue("Framebuffer should render multiple distinct NTSC colors, got $uniqueColors", uniqueColors >= 2)
        assertEquals(TiaChip.SCREEN_WIDTH * TiaChip.SCREEN_HEIGHT, engine.tia.frameBuffer.size)

        val snapshot = engine.saveState()
        assertTrue("Save state must be non-empty", snapshot.isNotEmpty())
        assertTrue("Load state must succeed", engine.loadState(snapshot))
        assertTrue("Rewind history should be available after 20 frames", engine.rewindStep())
    }

    @Test
    fun allSixteenArcadeClassicsRenderRespondSaveAndRewind() {
        val engines = listOf(
            "mario", "pacman", "invaders", "kong", "pitfall", "river",
            "breakout", "pong", "frogger", "asteroids", "tetris", "galaga",
            "enduro", "centipede", "missile", "snake"
        )
        val unified = UnifiedAtariEngine()

        for (type in engines) {
            val game = GameEntity(
                id = "test_$type",
                nameAr = type,
                nameEn = type,
                system = "2600",
                category = "أكشن",
                year = 1983,
                romPath = "roms/rom_0001.a26",
                isImported = false,
                engineType = type,
                variantSeed = 0
            )
            val res = unified.loadGame(game, null)
            assertTrue("Engine $type should load cleanly", res.isSuccess)

            val audio = unified.getAudioBuffer()
            repeat(30) { f ->
                unified.setInputs(
                    up = f % 10 == 0,
                    down = false,
                    left = f % 4 == 0,
                    right = f % 4 != 0,
                    fireA = f % 6 == 0,
                    actionB = f == 12
                )
                unified.stepFrame(audio)
            }

            val colors = unified.frameBuffer.toSet().size
            assertTrue("Engine $type should render rich pixels, got $colors colors", colors >= 4)

            val snap = unified.saveState()
            assertTrue("Engine $type save state should not be empty", snap.isNotEmpty())
            assertTrue("Engine $type load state should succeed", unified.loadState(snap))
            assertTrue("Engine $type rewind should succeed", unified.rewindStep())
        }
    }

    @Test
    fun gamesCatalogHas2600EntriesWithSuperMarioAndPacMan() {
        val jsonFile = resolveAssetFile("src/main/assets/games.json")
        assertTrue("games.json must exist", jsonFile.exists())

        val text = jsonFile.readText(Charsets.UTF_8)
        val count = Regex("\"id\":\"atari_\\d{4}\"").findAll(text).count()
        assertEquals(2600, count)
        assertTrue(text.contains("\"id\":\"atari_0001\""))
        assertTrue(text.contains("\"nameEn\":\"Super Mario Bros.\""))
        assertTrue(text.contains("\"engineType\":\"mario\""))
        assertTrue(text.contains("\"id\":\"atari_0002\""))
        assertTrue(text.contains("\"nameEn\":\"Pac-Man\""))
        assertTrue(text.contains("\"engineType\":\"pacman\""))
    }

    private fun resolveAssetFile(rel: String): File {
        val f1 = File(rel)
        return if (f1.exists()) f1 else File("app/$rel")
    }
}
