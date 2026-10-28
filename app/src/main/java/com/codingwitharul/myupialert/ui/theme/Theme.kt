package com.codingwitharul.myupialert.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppPalette(
    val isDark: Boolean,
    val background: Color,
    val backgroundGradient: List<Color>,
    val surface: Color,
    val surfaceVariant: Color,
    val glassSurface: Color,
    val glassCard: Color,
    val glassBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val primary: Color = Primary,
    val primaryGradient: List<Color> = listOf(Primary, AccentViolet),
    val success: Color = Success,
    val successDark: Color = SuccessDark,
    val warning: Color = Warning,
    val error: Color = Error
)

val DarkPalette = AppPalette(
    isDark = true,
    background = Color(0xFF090D16),
    backgroundGradient = listOf(Color(0xFF090D16), Color(0xFF0F172A), Color(0xFF090D16)),
    surface = Color(0xFF131B2E),
    surfaceVariant = Color(0xFF1E293B),
    glassSurface = Color(0x1AFFFFFF),
    glassCard = Color(0x14FFFFFF),
    glassBorder = Color(0x26FFFFFF),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B)
)

val LightPalette = AppPalette(
    isDark = false,
    background = Color(0xFFF1F5F9), // Crisp, modern slate off-white for Day Mode
    backgroundGradient = listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC), Color(0xFFEDF2F7)),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF8FAFC),
    glassSurface = Color(0xF2FFFFFF),
    glassCard = Color(0xFFFFFFFF),
    glassBorder = Color(0x1F0F172A),
    textPrimary = Color(0xFF0F172A), // High contrast deep slate
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF94A3B8)
)

val LocalAppPalette = staticCompositionLocalOf { DarkPalette }

val MaterialTheme.appPalette: AppPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalAppPalette.current

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = TextPrimary,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = TextPrimary,
    secondary = Success,
    onSecondary = TextPrimary,
    secondaryContainer = SuccessDark,
    onSecondaryContainer = TextPrimary,
    tertiary = AccentCyan,
    onTertiary = TextPrimary,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    outlineVariant = GlassBorderHighlight,
    error = Error,
    onError = TextPrimary,
    errorContainer = ErrorGlow,
    onErrorContainer = Error
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = PrimaryDark,
    secondary = Success,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = SuccessDark,
    tertiary = AccentCyan,
    onTertiary = Color.White,
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0x1F0F172A),
    outlineVariant = Color(0x0F0F172A),
    error = Error,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = ErrorDark
)

@Composable
fun MyUPIAlertTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

