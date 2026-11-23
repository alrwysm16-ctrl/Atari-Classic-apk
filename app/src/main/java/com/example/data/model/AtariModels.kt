package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AtariSystem(val code: String, val labelAr: String, val labelEn: String) {
    ALL("ALL", "الكل", "All"),
    A2600("2600", "2600", "2600"),
    A5200("5200", "5200", "5200"),
    A7800("7800", "7800", "7800"),
    LYNX("Lynx", "Lynx", "Lynx"),
    JAGUAR("Jaguar", "Jaguar", "Jaguar");

    companion object {
        fun fromCode(code: String): AtariSystem =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: A2600
    }
}

enum class LibrarySection(val labelAr: String, val labelEn: String) {
    ALL("المكتبة", "Library"),
    FAVORITES("المفضلة", "Favorites"),
    RECENT("الأخيرة", "Recent"),
    MOST_PLAYED("الأكثر لعباً", "Top Played")
}

enum class SortMode(val labelAr: String, val labelEn: String) {
    DEFAULT("الافتراضي", "Default"),
    ALPHA("أبجدي", "A-Z"),
    YEAR("السنة", "Year"),
    MOST_PLAYED("الأكثر لعباً", "Most Played")
}

enum class ScreenFilterMode(val labelAr: String, val labelEn: String) {
    CRT("CRT", "CRT"),
    SCANLINES("خطوط المسح", "Scanlines"),
    SMOOTH("ناعم", "Smooth"),
    PIXEL_PERFECT("بكسل دقيق", "Pixel Perfect")
}

enum class AspectRatioMode(val labelAr: String, val labelEn: String, val ratio: Float?) {
    RATIO_4_3("4:3", "4:3", 4f / 3f),
    ORIGINAL("أصلي", "Original", 160f / 192f),
    FULL("ملء", "Fill", null),
    RATIO_16_9("16:9", "16:9", 16f / 9f)
}

enum class ThemeMode(val labelAr: String, val labelEn: String) {
    STYLE("حسب الستايل", "Style Dark"),
    DARK("داكن", "Pure Dark"),
    LIGHT("فاتح", "Warm Light")
}

@Entity(
    tableName = "games",
    indices = [
        Index(value = ["system"]),
        Index(value = ["category"]),
        Index(value = ["isFavorite"]),
        Index(value = ["lastPlayedAt"]),
        Index(value = ["playCount"])
    ]
)
data class GameEntity(
    @PrimaryKey val id: String,
    val nameAr: String,
    val nameEn: String,
    val system: String,
    val category: String,
    val year: Int,
    val romPath: String,
    val isImported: Boolean = false,
    val engineType: String = "rom2600",
    val variantSeed: Int = 0,
    val coverStyle: String = "letter",
    val coverChar: String = "A",
    val paletteIndex: Int = 0,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L,
    val highScore: Int = 0
)

@Entity(
    tableName = "save_slots",
    primaryKeys = ["gameId", "slotIndex"]
)
data class SaveSlotEntity(
    val gameId: String,
    val slotIndex: Int, // 0 = auto-save, 1 = slot 1, 2 = slot 2, 3 = slot 3
    val stateData: ByteArray,
    val updatedAt: Long,
    val scoreSnapshot: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SaveSlotEntity) return false
        return gameId == other.gameId && slotIndex == other.slotIndex && updatedAt == other.updatedAt
    }

    override fun hashCode(): Int {
        return 31 * gameId.hashCode() + slotIndex
    }
}

data class UserSettings(
    val crtFilter: Boolean = true,
    val scanlines: Boolean = false,
    val filterMode: ScreenFilterMode = ScreenFilterMode.CRT,
    val vibration: Boolean = true,
    val autoSave: Boolean = true,
    val aspectRatio: AspectRatioMode = AspectRatioMode.RATIO_4_3,
    val volumePercent: Int = 80,
    val isMuted: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.STYLE,
    val isArabic: Boolean = true,
    val touchScale: Float = 1.0f,
    val touchOpacity: Float = 0.85f,
    val dpadOffsetX: Float = 0f,
    val dpadOffsetY: Float = 0f,
    val abOffsetX: Float = 0f,
    val abOffsetY: Float = 0f,
    val swapControls: Boolean = false,
    val showExtraButtons: Boolean = true,
    val aButtonOffsetX: Float = 0f,
    val aButtonOffsetY: Float = 0f,
    val bButtonOffsetX: Float = 0f,
    val bButtonOffsetY: Float = 0f,
    val showTelegramDialogOnStart: Boolean = true
)
