package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Base64
import com.example.data.local.AtariDatabase
import com.example.data.model.AspectRatioMode
import com.example.data.model.GameEntity
import com.example.data.model.LibrarySection
import com.example.data.model.SaveSlotEntity
import com.example.data.model.ScreenFilterMode
import com.example.data.model.SortMode
import com.example.data.model.ThemeMode
import com.example.data.model.UserSettings
import com.example.data.preferences.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.UUID

data class IndexingState(
    val isReady: Boolean = false,
    val progress: Float = 0f,
    val indexedCount: Int = 0,
    val totalCount: Int = 2600,
    val error: String? = null
)

data class StorageStats(
    val importedGamesCount: Int,
    val saveSlotsCount: Int,
    val importedRomsBytes: Long,
    val databaseBytes: Long
)

class AtariRepository(
    private val context: Context,
    private val db: AtariDatabase,
    private val settingsRepo: SettingsRepository
) {
    private val gameDao = db.gameDao()
    private val saveSlotDao = db.saveSlotDao()

    private val importedRomsDir: File by lazy {
        File(context.filesDir, "imported_roms").apply { mkdirs() }
    }

    companion object {
        private const val CURRENT_CATALOG_VERSION = 2
    }

    suspend fun ensureLibraryIndexed(onProgress: (IndexingState) -> Unit) = withContext(Dispatchers.IO) {
        val existingCount = gameDao.getCount()
        val indexedVersion = settingsRepo.getCatalogVersion()
        if (existingCount >= 2600 && indexedVersion >= CURRENT_CATALOG_VERSION) {
            onProgress(IndexingState(isReady = true, progress = 1f, indexedCount = existingCount, totalCount = existingCount))
            return@withContext
        }

        try {
            val preservedStats = gameDao.getGamesWithUserStats().associateBy { it.id }
            val jsonText = context.assets.open("games.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            val array = JSONArray(jsonText)
            val total = array.length()
            val batch = ArrayList<GameEntity>(300)

            for (i in 0 until total) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val oldStats = preservedStats[id]
                batch.add(
                    GameEntity(
                        id = id,
                        nameAr = obj.getString("nameAr"),
                        nameEn = obj.getString("nameEn"),
                        system = obj.getString("system"),
                        category = obj.getString("category"),
                        year = obj.getInt("year"),
                        romPath = obj.getString("romPath"),
                        isImported = false,
                        engineType = obj.optString("engineType", "rom2600"),
                        variantSeed = obj.optInt("variantSeed", 0),
                        coverStyle = obj.optString("coverStyle", "letter"),
                        coverChar = obj.optString("coverChar", "A"),
                        paletteIndex = obj.optInt("paletteIndex", 0),
                        isFavorite = oldStats?.isFavorite ?: false,
                        playCount = oldStats?.playCount ?: 0,
                        lastPlayedAt = oldStats?.lastPlayedAt ?: 0L,
                        highScore = oldStats?.highScore ?: 0
                    )
                )
                if (batch.size >= 260 || i == total - 1) {
                    gameDao.insertAll(batch)
                    batch.clear()
                    val countNow = i + 1
                    onProgress(
                        IndexingState(
                            isReady = false,
                            progress = countNow.toFloat() / total.coerceAtLeast(1),
                            indexedCount = countNow,
                            totalCount = total
                        )
                    )
                }
            }
            settingsRepo.setCatalogVersion(CURRENT_CATALOG_VERSION)
            onProgress(IndexingState(isReady = true, progress = 1f, indexedCount = total, totalCount = total))
        } catch (e: Exception) {
            onProgress(
                IndexingState(
                    isReady = false,
                    progress = 0f,
                    error = e.localizedMessage ?: "تعذر فهرسة مكتبة الألعاب المحلية"
                )
            )
        }
    }

    fun observeGames(
        system: String,
        category: String,
        section: LibrarySection,
        searchQuery: String,
        sortMode: SortMode
    ): Flow<List<GameEntity>> {
        val favOnly = if (section == LibrarySection.FAVORITES) 1 else 0
        val recentOnly = if (section == LibrarySection.RECENT) 1 else 0
        val mostPlayedOnly = if (section == LibrarySection.MOST_PLAYED) 1 else 0
        val effectiveSort = when {
            section == LibrarySection.MOST_PLAYED && sortMode == SortMode.DEFAULT -> SortMode.MOST_PLAYED.name
            else -> sortMode.name
        }
        return gameDao.observeFilteredGames(
            system = system,
            category = category,
            favoritesOnly = favOnly,
            recentOnly = recentOnly,
            mostPlayedOnly = mostPlayedOnly,
            searchQuery = searchQuery.trim(),
            sortMode = effectiveSort
        )
    }

    fun observeCategories(): Flow<List<String>> = gameDao.observeCategories()

    fun observeGame(id: String): Flow<GameEntity?> = gameDao.observeById(id)

    suspend fun getGame(id: String): GameEntity? = gameDao.getById(id)

    fun observeSaveSlots(gameId: String): Flow<List<SaveSlotEntity>> = saveSlotDao.observeSlotsForGame(gameId)

    suspend fun getSaveSlot(gameId: String, slotIndex: Int): SaveSlotEntity? = saveSlotDao.getSlot(gameId, slotIndex)

    suspend fun saveState(gameId: String, slotIndex: Int, stateData: ByteArray, score: Int) {
        saveSlotDao.upsertSlot(
            SaveSlotEntity(
                gameId = gameId,
                slotIndex = slotIndex,
                stateData = stateData,
                updatedAt = System.currentTimeMillis(),
                scoreSnapshot = score
            )
        )
        if (score > 0) {
            gameDao.updateHighScore(gameId, score)
        }
    }

    suspend fun deleteSaveSlot(gameId: String, slotIndex: Int) {
        saveSlotDao.deleteSlot(gameId, slotIndex)
    }

    suspend fun toggleFavorite(id: String, current: Boolean) {
        gameDao.setFavorite(id, !current)
    }

    suspend fun recordPlay(id: String, score: Int) {
        gameDao.recordPlaySession(id, System.currentTimeMillis(), score)
    }

    suspend fun loadRomBytes(game: GameEntity): ByteArray = withContext(Dispatchers.IO) {
        if (game.isImported) {
            File(game.romPath).readBytes()
        } else {
            context.assets.open(game.romPath).use { it.readBytes() }
        }
    }

    suspend fun importRomUris(uris: List<Uri>): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            var imported = 0
            for (uri in uris) {
                val displayName = resolveDisplayName(uri) ?: "Custom_Atari_${System.currentTimeMillis()}.a26"
                if (!isSupportedRomName(displayName)) continue
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: continue
                if (bytes.isEmpty()) continue
                saveImportedRom(displayName, bytes)
                imported++
            }
            imported
        }
    }

    suspend fun importRomFolder(treeUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val items = collectRomDocuments(treeUri, rootDocId)
            var imported = 0
            for ((docUri, name) in items) {
                val bytes = context.contentResolver.openInputStream(docUri)?.use { it.readBytes() } ?: continue
                if (bytes.isEmpty()) continue
                saveImportedRom(name, bytes)
                imported++
            }
            imported
        }
    }

    private fun collectRomDocuments(treeUri: Uri, parentDocId: String): List<Pair<Uri, String>> {
        val result = mutableListOf<Pair<Uri, String>>()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val docId = cursor.getString(0) ?: continue
                val name = cursor.getString(1) ?: continue
                val mime = cursor.getString(2) ?: ""
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    result.addAll(collectRomDocuments(treeUri, docId))
                } else if (isSupportedRomName(name)) {
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    result.add(docUri to name)
                }
            }
        }
        return result
    }

    private suspend fun saveImportedRom(rawFilename: String, bytes: ByteArray) {
        val cleanName = rawFilename
            .substringBeforeLast('.')
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
            .ifBlank { "Atari ROM" }

        val ext = rawFilename.substringAfterLast('.', "a26").lowercase(Locale.ROOT)
        val system = detectSystem(ext, bytes)
        val id = "imp_${UUID.randomUUID().toString().take(8)}"
        val destFile = File(importedRomsDir, "${id}.$ext")
        destFile.writeBytes(bytes)

        val firstChar = cleanName.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "A"
        val styles = listOf("letter", "pong", "castle", "jet", "star", "skull", "rocket")
        val style = styles[kotlin.math.abs(cleanName.hashCode()) % styles.size]
        val paletteIdx = kotlin.math.abs(cleanName.hashCode()) % 6

        gameDao.insertGame(
            GameEntity(
                id = id,
                nameAr = cleanName,
                nameEn = cleanName,
                system = system,
                category = "مستوردة",
                year = 1983,
                romPath = destFile.absolutePath,
                isImported = true,
                coverStyle = style,
                coverChar = firstChar,
                paletteIndex = paletteIdx
            )
        )
    }

    private fun detectSystem(ext: String, bytes: ByteArray): String {
        if (bytes.size >= 4 && bytes[0] == 'L'.code.toByte() && bytes[1] == 'Y'.code.toByte() &&
            bytes[2] == 'N'.code.toByte() && bytes[3] == 'X'.code.toByte()
        ) return "Lynx"
        if (bytes.size >= 12 && String(bytes, 1, 8, Charsets.US_ASCII) == "ATARI780") return "7800"
        return when (ext) {
            "a52" -> "5200"
            "a78" -> "7800"
            "lnx" -> "Lynx"
            "j64", "jag" -> "Jaguar"
            else -> "2600"
        }
    }

    private fun isSupportedRomName(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return ext in setOf("a26", "bin", "rom", "a52", "a78", "lnx", "j64", "jag")
    }

    private fun resolveDisplayName(uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx)
            }
        }
        return uri.lastPathSegment
    }

    fun observeImportedGames(): Flow<List<GameEntity>> = gameDao.observeImportedGames()

    suspend fun deleteImportedGame(game: GameEntity) = withContext(Dispatchers.IO) {
        if (game.isImported) {
            File(game.romPath).delete()
            gameDao.deleteImportedGame(game.id)
        }
    }

    suspend fun clearAllSaves() = withContext(Dispatchers.IO) {
        saveSlotDao.deleteAllSlots()
    }

    suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val romBytes = importedRomsDir.listFiles()?.sumOf { it.length() } ?: 0L
        val dbFile = context.getDatabasePath("atari_classic.db")
        val dbSize = if (dbFile.exists()) dbFile.length() else 0L
        val importedCount = gameDao.observeImportedGames().first().size
        val saveCount = saveSlotDao.getAllSlots().size
        StorageStats(
            importedGamesCount = importedCount,
            saveSlotsCount = saveCount,
            importedRomsBytes = romBytes,
            databaseBytes = dbSize
        )
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val settings = settingsRepo.settingsFlow.first()
        val stats = gameDao.getGamesWithUserStats()
        val saves = saveSlotDao.getAllSlots()

        val root = JSONObject().apply {
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("settings", JSONObject().apply {
                put("crtFilter", settings.crtFilter)
                put("scanlines", settings.scanlines)
                put("filterMode", settings.filterMode.name)
                put("vibration", settings.vibration)
                put("autoSave", settings.autoSave)
                put("aspectRatio", settings.aspectRatio.name)
                put("volumePercent", settings.volumePercent)
                put("isMuted", settings.isMuted)
                put("themeMode", settings.themeMode.name)
                put("isArabic", settings.isArabic)
                put("touchScale", settings.touchScale.toDouble())
                put("touchOpacity", settings.touchOpacity.toDouble())
                put("swapControls", settings.swapControls)
                put("showExtraButtons", settings.showExtraButtons)
            })
            val statsArr = JSONArray()
            for (g in stats) {
                statsArr.put(JSONObject().apply {
                    put("id", g.id)
                    put("isFavorite", g.isFavorite)
                    put("playCount", g.playCount)
                    put("lastPlayedAt", g.lastPlayedAt)
                    put("highScore", g.highScore)
                })
            }
            put("gameStats", statsArr)
            val savesArr = JSONArray()
            for (s in saves) {
                savesArr.put(JSONObject().apply {
                    put("gameId", s.gameId)
                    put("slotIndex", s.slotIndex)
                    put("updatedAt", s.updatedAt)
                    put("scoreSnapshot", s.scoreSnapshot)
                    put("stateData", Base64.encodeToString(s.stateData, Base64.NO_WRAP))
                })
            }
            put("saveSlots", savesArr)
        }
        root.toString(2)
    }

    suspend fun importBackupJson(jsonText: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject(jsonText)
            val sObj = root.optJSONObject("settings")
            if (sObj != null) {
                settingsRepo.applyBackupSettings(
                    UserSettings(
                        crtFilter = sObj.optBoolean("crtFilter", true),
                        scanlines = sObj.optBoolean("scanlines", false),
                        filterMode = runCatching { ScreenFilterMode.valueOf(sObj.optString("filterMode")) }.getOrDefault(ScreenFilterMode.CRT),
                        vibration = sObj.optBoolean("vibration", true),
                        autoSave = sObj.optBoolean("autoSave", true),
                        aspectRatio = runCatching { AspectRatioMode.valueOf(sObj.optString("aspectRatio")) }.getOrDefault(AspectRatioMode.RATIO_4_3),
                        volumePercent = sObj.optInt("volumePercent", 80),
                        isMuted = sObj.optBoolean("isMuted", false),
                        themeMode = runCatching { ThemeMode.valueOf(sObj.optString("themeMode")) }.getOrDefault(ThemeMode.STYLE),
                        isArabic = sObj.optBoolean("isArabic", true),
                        touchScale = sObj.optDouble("touchScale", 1.0).toFloat(),
                        touchOpacity = sObj.optDouble("touchOpacity", 0.85).toFloat(),
                        swapControls = sObj.optBoolean("swapControls", false),
                        showExtraButtons = sObj.optBoolean("showExtraButtons", true)
                    )
                )
            }
            var restoredCount = 0
            val statsArr = root.optJSONArray("gameStats")
            if (statsArr != null) {
                for (i in 0 until statsArr.length()) {
                    val item = statsArr.getJSONObject(i)
                    gameDao.restoreGameStats(
                        id = item.getString("id"),
                        fav = item.optBoolean("isFavorite", false),
                        plays = item.optInt("playCount", 0),
                        lastPlayed = item.optLong("lastPlayedAt", 0L),
                        highScore = item.optInt("highScore", 0)
                    )
                    restoredCount++
                }
            }
            val savesArr = root.optJSONArray("saveSlots")
            if (savesArr != null) {
                for (i in 0 until savesArr.length()) {
                    val item = savesArr.getJSONObject(i)
                    val bytes = Base64.decode(item.getString("stateData"), Base64.NO_WRAP)
                    saveSlotDao.upsertSlot(
                        SaveSlotEntity(
                            gameId = item.getString("gameId"),
                            slotIndex = item.getInt("slotIndex"),
                            stateData = bytes,
                            updatedAt = item.optLong("updatedAt", System.currentTimeMillis()),
                            scoreSnapshot = item.optInt("scoreSnapshot", 0)
                        )
                    )
                    restoredCount++
                }
            }
            restoredCount
        }
    }
}
