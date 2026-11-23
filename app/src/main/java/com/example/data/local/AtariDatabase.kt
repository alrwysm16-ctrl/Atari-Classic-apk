package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.GameEntity
import com.example.data.model.SaveSlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT COUNT(*) FROM games")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM games WHERE isImported = 1")
    fun observeImportedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(games: List<GameEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    @Query("SELECT * FROM games WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): GameEntity?

    @Query("SELECT * FROM games WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<GameEntity?>

    @Query(
        """
        SELECT * FROM games
        WHERE (:system = 'ALL' OR system = :system)
          AND (:category = 'ALL' OR category = :category)
          AND (:favoritesOnly = 0 OR isFavorite = 1)
          AND (:recentOnly = 0 OR lastPlayedAt > 0)
          AND (:mostPlayedOnly = 0 OR playCount > 0)
          AND (
              :searchQuery = ''
              OR nameAr LIKE '%' || :searchQuery || '%'
              OR nameEn LIKE '%' || :searchQuery || '%'
              OR category LIKE '%' || :searchQuery || '%'
          )
        ORDER BY
          CASE WHEN :sortMode = 'ALPHA' THEN nameEn END ASC,
          CASE WHEN :sortMode = 'YEAR' THEN year END DESC,
          CASE WHEN :sortMode = 'MOST_PLAYED' THEN playCount END DESC,
          CASE WHEN :recentOnly = 1 THEN lastPlayedAt END DESC,
          id ASC
        """
    )
    fun observeFilteredGames(
        system: String,
        category: String,
        favoritesOnly: Int,
        recentOnly: Int,
        mostPlayedOnly: Int,
        searchQuery: String,
        sortMode: String
    ): Flow<List<GameEntity>>

    @Query("SELECT DISTINCT category FROM games ORDER BY category ASC")
    fun observeCategories(): Flow<List<String>>

    @Query("UPDATE games SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query(
        """
        UPDATE games
        SET playCount = playCount + 1,
            lastPlayedAt = :timestamp,
            highScore = CASE WHEN :score > highScore THEN :score ELSE highScore END
        WHERE id = :id
        """
    )
    suspend fun recordPlaySession(id: String, timestamp: Long, score: Int)

    @Query("UPDATE games SET highScore = CASE WHEN :score > highScore THEN :score ELSE highScore END WHERE id = :id")
    suspend fun updateHighScore(id: String, score: Int)

    @Query("SELECT * FROM games WHERE isImported = 1 ORDER BY id DESC")
    fun observeImportedGames(): Flow<List<GameEntity>>

    @Query("DELETE FROM games WHERE id = :id AND isImported = 1")
    suspend fun deleteImportedGame(id: String)

    @Query("SELECT * FROM games WHERE isFavorite = 1 OR playCount > 0 OR highScore > 0")
    suspend fun getGamesWithUserStats(): List<GameEntity>

    @Query("UPDATE games SET isFavorite = :fav, playCount = :plays, lastPlayedAt = :lastPlayed, highScore = :highScore WHERE id = :id")
    suspend fun restoreGameStats(id: String, fav: Boolean, plays: Int, lastPlayed: Long, highScore: Int)
}

@Dao
interface SaveSlotDao {
    @Query("SELECT * FROM save_slots WHERE gameId = :gameId ORDER BY slotIndex ASC")
    fun observeSlotsForGame(gameId: String): Flow<List<SaveSlotEntity>>

    @Query("SELECT * FROM save_slots WHERE gameId = :gameId AND slotIndex = :slotIndex LIMIT 1")
    suspend fun getSlot(gameId: String, slotIndex: Int): SaveSlotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSlot(slot: SaveSlotEntity)

    @Query("DELETE FROM save_slots WHERE gameId = :gameId AND slotIndex = :slotIndex")
    suspend fun deleteSlot(gameId: String, slotIndex: Int)

    @Query("DELETE FROM save_slots")
    suspend fun deleteAllSlots()

    @Query("SELECT * FROM save_slots")
    suspend fun getAllSlots(): List<SaveSlotEntity>

    @Query("SELECT COUNT(*) FROM save_slots")
    fun observeSaveCount(): Flow<Int>
}

@Database(
    entities = [GameEntity::class, SaveSlotEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AtariDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun saveSlotDao(): SaveSlotDao

    companion object {
        @Volatile
        private var INSTANCE: AtariDatabase? = null

        fun getInstance(context: Context): AtariDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AtariDatabase::class.java,
                    "atari_classic.db"
                ).fallbackToDestructiveMigration(true)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
