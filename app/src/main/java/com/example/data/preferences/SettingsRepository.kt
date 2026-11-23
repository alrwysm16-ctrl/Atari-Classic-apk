package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AspectRatioMode
import com.example.data.model.ScreenFilterMode
import com.example.data.model.ThemeMode
import com.example.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "atari_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val CRT_FILTER = booleanPreferencesKey("crt_filter")
        val SCANLINES = booleanPreferencesKey("scanlines")
        val FILTER_MODE = stringPreferencesKey("filter_mode")
        val VIBRATION = booleanPreferencesKey("vibration")
        val AUTO_SAVE = booleanPreferencesKey("auto_save")
        val ASPECT_RATIO = stringPreferencesKey("aspect_ratio")
        val VOLUME_PERCENT = intPreferencesKey("volume_percent")
        val IS_MUTED = booleanPreferencesKey("is_muted")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val IS_ARABIC = booleanPreferencesKey("is_arabic")
        val TOUCH_SCALE = floatPreferencesKey("touch_scale")
        val TOUCH_OPACITY = floatPreferencesKey("touch_opacity")
        val DPAD_OFFSET_X = floatPreferencesKey("dpad_offset_x")
        val DPAD_OFFSET_Y = floatPreferencesKey("dpad_offset_y")
        val AB_OFFSET_X = floatPreferencesKey("ab_offset_x")
        val AB_OFFSET_Y = floatPreferencesKey("ab_offset_y")
        val SWAP_CONTROLS = booleanPreferencesKey("swap_controls")
        val SHOW_EXTRA_BUTTONS = booleanPreferencesKey("show_extra_buttons")
        val A_OFFSET_X = floatPreferencesKey("a_offset_x")
        val A_OFFSET_Y = floatPreferencesKey("a_offset_y")
        val B_OFFSET_X = floatPreferencesKey("b_offset_x")
        val B_OFFSET_Y = floatPreferencesKey("b_offset_y")
        val CATALOG_VERSION = intPreferencesKey("catalog_version")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val crt = prefs[Keys.CRT_FILTER] ?: true
        val scan = prefs[Keys.SCANLINES] ?: false
        val filterStr = prefs[Keys.FILTER_MODE] ?: if (crt) ScreenFilterMode.CRT.name else ScreenFilterMode.PIXEL_PERFECT.name
        val aspectStr = prefs[Keys.ASPECT_RATIO] ?: AspectRatioMode.RATIO_4_3.name
        val themeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.STYLE.name

        UserSettings(
            crtFilter = crt,
            scanlines = scan,
            filterMode = runCatching { ScreenFilterMode.valueOf(filterStr) }.getOrDefault(ScreenFilterMode.CRT),
            vibration = prefs[Keys.VIBRATION] ?: true,
            autoSave = prefs[Keys.AUTO_SAVE] ?: true,
            aspectRatio = runCatching { AspectRatioMode.valueOf(aspectStr) }.getOrDefault(AspectRatioMode.RATIO_4_3),
            volumePercent = prefs[Keys.VOLUME_PERCENT] ?: 80,
            isMuted = prefs[Keys.IS_MUTED] ?: false,
            themeMode = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.STYLE),
            isArabic = prefs[Keys.IS_ARABIC] ?: true,
            touchScale = prefs[Keys.TOUCH_SCALE] ?: 1.0f,
            touchOpacity = prefs[Keys.TOUCH_OPACITY] ?: 0.85f,
            dpadOffsetX = prefs[Keys.DPAD_OFFSET_X] ?: 0f,
            dpadOffsetY = prefs[Keys.DPAD_OFFSET_Y] ?: 0f,
            abOffsetX = prefs[Keys.AB_OFFSET_X] ?: 0f,
            abOffsetY = prefs[Keys.AB_OFFSET_Y] ?: 0f,
            swapControls = prefs[Keys.SWAP_CONTROLS] ?: false,
            showExtraButtons = prefs[Keys.SHOW_EXTRA_BUTTONS] ?: true,
            aButtonOffsetX = prefs[Keys.A_OFFSET_X] ?: 0f,
            aButtonOffsetY = prefs[Keys.A_OFFSET_Y] ?: 0f,
            bButtonOffsetX = prefs[Keys.B_OFFSET_X] ?: 0f,
            bButtonOffsetY = prefs[Keys.B_OFFSET_Y] ?: 0f
        )
    }

    suspend fun getCatalogVersion(): Int = context.dataStore.data.first()[Keys.CATALOG_VERSION] ?: 0

    suspend fun setCatalogVersion(version: Int) {
        context.dataStore.edit { it[Keys.CATALOG_VERSION] = version }
    }

    suspend fun setCrtFilter(enabled: Boolean) {
        context.dataStore.edit {
            it[Keys.CRT_FILTER] = enabled
            if (enabled) {
                it[Keys.FILTER_MODE] = ScreenFilterMode.CRT.name
            } else if (it[Keys.SCANLINES] == true) {
                it[Keys.FILTER_MODE] = ScreenFilterMode.SCANLINES.name
            } else {
                it[Keys.FILTER_MODE] = ScreenFilterMode.PIXEL_PERFECT.name
            }
        }
    }

    suspend fun setScanlines(enabled: Boolean) {
        context.dataStore.edit {
            it[Keys.SCANLINES] = enabled
            if (enabled && it[Keys.CRT_FILTER] != true) {
                it[Keys.FILTER_MODE] = ScreenFilterMode.SCANLINES.name
            }
        }
    }

    suspend fun setFilterMode(mode: ScreenFilterMode) {
        context.dataStore.edit {
            it[Keys.FILTER_MODE] = mode.name
            it[Keys.CRT_FILTER] = (mode == ScreenFilterMode.CRT)
            it[Keys.SCANLINES] = (mode == ScreenFilterMode.SCANLINES || mode == ScreenFilterMode.CRT)
        }
    }

    suspend fun setVibration(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION] = enabled }
    }

    suspend fun setAutoSave(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SAVE] = enabled }
    }

    suspend fun setAspectRatio(mode: AspectRatioMode) {
        context.dataStore.edit { it[Keys.ASPECT_RATIO] = mode.name }
    }

    suspend fun setVolumePercent(percent: Int) {
        context.dataStore.edit { it[Keys.VOLUME_PERCENT] = percent.coerceIn(0, 100) }
    }

    suspend fun setMuted(muted: Boolean) {
        context.dataStore.edit { it[Keys.IS_MUTED] = muted }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setIsArabic(isArabic: Boolean) {
        context.dataStore.edit { it[Keys.IS_ARABIC] = isArabic }
    }

    suspend fun setTouchLayout(
        scale: Float,
        opacity: Float,
        dpadX: Float,
        dpadY: Float,
        abX: Float,
        abY: Float,
        swapControls: Boolean,
        showExtraButtons: Boolean,
        aX: Float = 0f,
        aY: Float = 0f,
        bX: Float = 0f,
        bY: Float = 0f
    ) {
        context.dataStore.edit {
            it[Keys.TOUCH_SCALE] = scale.coerceIn(0.65f, 1.45f)
            it[Keys.TOUCH_OPACITY] = opacity.coerceIn(0.25f, 1.0f)
            it[Keys.DPAD_OFFSET_X] = dpadX
            it[Keys.DPAD_OFFSET_Y] = dpadY
            it[Keys.AB_OFFSET_X] = abX
            it[Keys.AB_OFFSET_Y] = abY
            it[Keys.SWAP_CONTROLS] = swapControls
            it[Keys.SHOW_EXTRA_BUTTONS] = showExtraButtons
            it[Keys.A_OFFSET_X] = aX
            it[Keys.A_OFFSET_Y] = aY
            it[Keys.B_OFFSET_X] = bX
            it[Keys.B_OFFSET_Y] = bY
        }
    }

    suspend fun resetTouchLayout() {
        setTouchLayout(
            scale = 1.0f,
            opacity = 0.85f,
            dpadX = 0f,
            dpadY = 0f,
            abX = 0f,
            abY = 0f,
            swapControls = false,
            showExtraButtons = true,
            aX = 0f,
            aY = 0f,
            bX = 0f,
            bY = 0f
        )
    }

    suspend fun applyBackupSettings(settings: UserSettings) {
        context.dataStore.edit {
            it[Keys.CRT_FILTER] = settings.crtFilter
            it[Keys.SCANLINES] = settings.scanlines
            it[Keys.FILTER_MODE] = settings.filterMode.name
            it[Keys.VIBRATION] = settings.vibration
            it[Keys.AUTO_SAVE] = settings.autoSave
            it[Keys.ASPECT_RATIO] = settings.aspectRatio.name
            it[Keys.VOLUME_PERCENT] = settings.volumePercent
            it[Keys.IS_MUTED] = settings.isMuted
            it[Keys.THEME_MODE] = settings.themeMode.name
            it[Keys.IS_ARABIC] = settings.isArabic
            it[Keys.TOUCH_SCALE] = settings.touchScale
            it[Keys.TOUCH_OPACITY] = settings.touchOpacity
            it[Keys.SWAP_CONTROLS] = settings.swapControls
            it[Keys.SHOW_EXTRA_BUTTONS] = settings.showExtraButtons
        }
    }
}
