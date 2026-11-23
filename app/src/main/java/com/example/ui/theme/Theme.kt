package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.model.ThemeMode

@Immutable
data class AtariExtendedColors(
    val bg: Color,
    val surface: Color,
    val card: Color,
    val chip: Color,
    val divider: Color,
    val switchTrackOff: Color,
    val orange: Color,
    val amber: Color,
    val cream: Color,
    val muted: Color,
    val emblemDark: Color,
    val isDark: Boolean
)

private val WarmStyleColors = AtariExtendedColors(
    bg = AtariBgDark,
    surface = AtariSurfaceDark,
    card = AtariCardDark,
    chip = AtariChipDark,
    divider = AtariDividerDark,
    switchTrackOff = AtariSwitchTrackOff,
    orange = AtariOrange,
    amber = AtariAmber,
    cream = AtariCream,
    muted = AtariMutedCream,
    emblemDark = AtariEmblemDark,
    isDark = true
)

private val PureDarkColors = WarmStyleColors.copy(
    bg = AtariPureBg,
    surface = AtariPureCard,
    card = AtariPureCard
)

private val WarmLightColors = AtariExtendedColors(
    bg = AtariBgLight,
    surface = AtariSurfaceLight,
    card = AtariCardLight,
    chip = AtariChipLight,
    divider = AtariDividerLight,
    switchTrackOff = Color(0xFFBEB09E),
    orange = AtariOrange,
    amber = Color(0xFFD48806),
    cream = AtariTextLight,
    muted = AtariMutedTextLight,
    emblemDark = AtariEmblemDark,
    isDark = false
)

val LocalAtariColors = staticCompositionLocalOf { WarmStyleColors }

@Composable
fun AtariClassicTheme(
    themeMode: ThemeMode = ThemeMode.STYLE,
    isArabic: Boolean = true,
    content: @Composable () -> Unit
) {
    val extended = when (themeMode) {
        ThemeMode.STYLE -> WarmStyleColors
        ThemeMode.DARK -> PureDarkColors
        ThemeMode.LIGHT -> WarmLightColors
    }

    val colorScheme = if (extended.isDark) {
        darkColorScheme(
            primary = extended.orange,
            onPrimary = Color.White,
            secondary = extended.amber,
            onSecondary = AtariEmblemDark,
            background = extended.bg,
            onBackground = extended.cream,
            surface = extended.surface,
            onSurface = extended.cream,
            surfaceVariant = extended.card,
            onSurfaceVariant = extended.muted,
            outline = extended.divider
        )
    } else {
        lightColorScheme(
            primary = extended.orange,
            onPrimary = Color.White,
            secondary = extended.amber,
            onSecondary = AtariEmblemDark,
            background = extended.bg,
            onBackground = extended.cream,
            surface = extended.surface,
            onSurface = extended.cream,
            surfaceVariant = extended.card,
            onSurfaceVariant = extended.muted,
            outline = extended.divider
        )
    }

    val direction = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalAtariColors provides extended,
        LocalLayoutDirection provides direction
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AtariTypography,
            content = content
        )
    }
}
