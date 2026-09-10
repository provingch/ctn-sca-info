package py.edu.ctn.sca.padres.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/*
 * Palette mirrors the web app's CSS custom properties (frontend/src/index.css :root
 * and :root[data-theme="dark"]) so the Android login and parent screens read as the
 * same product as the browser.
 */

// --- Web --accent / --accent-deep / --accent-soft / --accent-strong ---
private val Accent = Color(0xFF5267F7)
private val AccentDeep = Color(0xFF3144CF)
private val AccentStrong = Color(0xFFDCE1FF)
private val AccentSoft = Color(0xFFEEF0FF)

private val AccentDark = Color(0xFF91A0FF)
private val AccentDeepDark = Color(0xFFC6CEFF)
private val AccentStrongDark = Color(0xFF2C3869)
private val AccentSoftDark = Color(0xFF222C53)

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = AccentStrong,
    onPrimaryContainer = Color(0xFF0A1B3D),
    secondary = Color(0xFF28784A),
    onSecondary = Color.White,
    background = Color(0xFFEEF3F7),      // --bg
    onBackground = Color(0xFF14233B),    // --ink
    surface = Color(0xFFFFFFFF),         // --paper
    onSurface = Color(0xFF14233B),       // --ink
    surfaceVariant = Color(0xFFF7F9FC),  // --bg-soft
    onSurfaceVariant = Color(0xFF64748B),// --muted
    outline = Color(0xFFB9C7D6),         // --line-strong
    outlineVariant = Color(0xFFD4DEE9),  // --line
    error = Color(0xFFB63C3C),           // --danger
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = Color(0xFF11162A),
    primaryContainer = AccentStrongDark,
    onPrimaryContainer = Color(0xFFEDF2FB),
    secondary = Color(0xFF65C58D),
    onSecondary = Color(0xFF08281A),
    background = Color(0xFF0D111B),       // --bg
    onBackground = Color(0xFFEDF2FB),     // --ink
    surface = Color(0xFF171E2C),          // --paper
    onSurface = Color(0xFFEDF2FB),        // --ink
    surfaceVariant = Color(0xFF121824),   // --bg-soft
    onSurfaceVariant = Color(0xFFA7B4C6), // --muted
    outline = Color(0xFF405069),          // --line-strong
    outlineVariant = Color(0xFF2C3748),   // --line
    error = Color(0xFFE7A9A6),
    onError = Color(0xFF3A1210),
)

/**
 * Tokens the Material [androidx.compose.material3.ColorScheme] has no slot for but the
 * web design leans on: the grade-chip scale, success/warning accents, the login
 * gradient, and the deep/soft accent tints used for eyebrow labels and selected cards.
 */
@Immutable
data class ScaColors(
    val accentDeep: Color,
    val accentSoft: Color,
    val bgSoft: Color,
    val success: Color,
    val warning: Color,
    val grade1: Color,
    val grade2: Color,
    val grade3: Color,
    val grade4: Color,
    val grade5: Color,
    val authGradient: List<Color>,
) {
    /** Web `.grade-chip--N` background for a normalized grade (1..5). */
    fun grade(nota: Int): Color = when (nota.coerceIn(1, 5)) {
        5 -> grade5
        4 -> grade4
        3 -> grade3
        2 -> grade2
        else -> grade1
    }
}

private val LightScaColors = ScaColors(
    accentDeep = AccentDeep,
    accentSoft = AccentSoft,
    bgSoft = Color(0xFFF7F9FC),
    success = Color(0xFF28784A),          // --success
    warning = Color(0xFFB56B16),          // --warning
    grade1 = Color(0xFFEF4444),
    grade2 = Color(0xFFF97316),
    grade3 = Color(0xFFF59E0B),
    grade4 = Color(0xFF84CC16),
    grade5 = Color(0xFF16A34A),
    authGradient = listOf(Color(0xFFE5EDF6), Color(0xFFF8FAFC), Color(0xFFDBE5F2)),
)

private val DarkScaColors = LightScaColors.copy(
    accentDeep = AccentDeepDark,
    accentSoft = AccentSoftDark,
    bgSoft = Color(0xFF121824),
    success = Color(0xFF65C58D),
    warning = Color(0xFFE4AD5D),
    authGradient = listOf(Color(0xFF101832), Color(0xFF18264A), Color(0xFF131F37)),
)

private val LocalScaColors = staticCompositionLocalOf { LightScaColors }

/** Web-parity tokens with no Material slot. Call inside [ScaPadresTheme]. */
val scaColors: ScaColors
    @Composable
    get() = LocalScaColors.current

private val AppTypography = Typography()

@Composable
fun ScaPadresTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val extras = if (darkTheme) DarkScaColors else LightScaColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // Edge-to-edge is enabled in MainActivity; just match the icon tint.
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    CompositionLocalProvider(LocalScaColors provides extras) {
        MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
    }
}
