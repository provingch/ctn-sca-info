package py.edu.ctn.sca.padres.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import py.edu.ctn.sca.padres.R
import py.edu.ctn.sca.padres.core.Role
import py.edu.ctn.sca.padres.core.RoleAccent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import py.edu.ctn.sca.padres.core.ThemeMode
import py.edu.ctn.sca.padres.core.ThemePrefs

/** Par fondo/texto de un estado (Tag, chips de nota, avisos). */
@Immutable
data class Tone(val bg: Color, val fg: Color)

/** Tokens sin slot en Material: líneas, muted, tonos de estado, acento suave del rol. */
@Immutable
data class ScaColors(
    val accentSoft: Color,
    val onAccentSoft: Color,
    val line: Color,
    val lineStrong: Color,
    val muted: Color,
    val bgSoft: Color,
    val success: Tone,
    val warning: Tone,
    val danger: Tone,
    val lightGreen: Tone,
    /** info = acento suave del rol / acento del rol */
    val info: Tone,
) {
    /** Nota 1..5: 5,4 verde · 3 ámbar · 2,1 rojo. */
    fun grade(nota: Int): Tone = when (nota.coerceIn(1, 5)) {
        5, 4 -> success
        3 -> warning
        else -> danger
    }

    /** Puntaje de tarea sobre 10: 1–4 rojo · 5–6 ámbar · 7–8 verde claro · 9–10 verde. */
    fun score10(nota: Number): Tone {
        val n = nota.toDouble()
        return when {
            n < 5 -> danger
            n < 7 -> warning
            n < 9 -> lightGreen
            else -> success
        }
    }
}

private val Ink = Color(0xFF172033)
private val InkDark = Color(0xFFEDF2FB)

private fun scheme(accent: RoleAccent, dark: Boolean): ColorScheme {
    val primary = when (accent) {
        RoleAccent.BORDO -> if (dark) Color(0xFFE58C99) else Color(0xFF7A1F2C)
        RoleAccent.AZUL -> if (dark) Color(0xFF91A0FF) else Color(0xFF5267F7)
    }
    val soft = soft(accent, dark)
    return if (dark) darkColorScheme(
        primary = primary, onPrimary = Color(0xFF11162A),
        primaryContainer = soft, onPrimaryContainer = onSoft(accent, true),
        background = Color(0xFF0D111B), onBackground = InkDark,
        surface = Color(0xFF171E2C), onSurface = InkDark,
        surfaceVariant = Color(0xFF121824), onSurfaceVariant = Color(0xFFA7B4C6),
        outline = Color(0xFF405069), outlineVariant = Color(0xFF2C3748),
        error = Color(0xFFE7A9A6), onError = Color(0xFF3A1210),
    ) else lightColorScheme(
        primary = primary, onPrimary = Color.White,
        primaryContainer = soft, onPrimaryContainer = onSoft(accent, false),
        background = Color(0xFFF9FAFB), onBackground = Ink,
        surface = Color.White, onSurface = Ink,
        surfaceVariant = Color(0xFFF2F4F7), onSurfaceVariant = Color(0xFF5C6576),
        outline = Color(0xFFCBD2DC), outlineVariant = Color(0xFFE4E7EC),
        error = Color(0xFFA63B33), onError = Color.White,
    )
}

private fun soft(a: RoleAccent, dark: Boolean) = when (a) {
    RoleAccent.BORDO -> if (dark) Color(0xFF3A1C22) else Color(0xFFF7E8EA)
    RoleAccent.AZUL -> if (dark) Color(0xFF222C53) else Color(0xFFEEF0FF)
}

private fun onSoft(a: RoleAccent, dark: Boolean) = when (a) {
    RoleAccent.BORDO -> if (dark) Color(0xFFE58C99) else Color(0xFF7A1F2C)
    RoleAccent.AZUL -> if (dark) Color(0xFFC6CEFF) else Color(0xFF3144CF)
}

private fun extras(accent: RoleAccent, dark: Boolean, primary: Color): ScaColors {
    val soft = soft(accent, dark)
    return if (dark) ScaColors(
        accentSoft = soft, onAccentSoft = onSoft(accent, true),
        line = Color(0xFF2C3748), lineStrong = Color(0xFF405069),
        muted = Color(0xFFA7B4C6), bgSoft = Color(0xFF121824),
        success = Tone(Color(0xFF12301F), Color(0xFF7FD0A0)),
        warning = Tone(Color(0xFF3A2A12), Color(0xFFE4AD5D)),
        danger = Tone(Color(0xFF3A1B1B), Color(0xFFE7A9A6)),
        lightGreen = Tone(Color(0xFF26331A), Color(0xFFA5D26A)),
        info = Tone(soft, primary),
    ) else ScaColors(
        accentSoft = soft, onAccentSoft = onSoft(accent, false),
        line = Color(0xFFE4E7EC), lineStrong = Color(0xFFCBD2DC),
        muted = Color(0xFF5C6576), bgSoft = Color(0xFFF2F4F7),
        success = Tone(Color(0xFFE6F4EA), Color(0xFF2E7D4F)),
        warning = Tone(Color(0xFFFDF0DC), Color(0xFF9A5B13)),
        danger = Tone(Color(0xFFFBE4E4), Color(0xFFA63B33)),
        lightGreen = Tone(Color(0xFFEEF7E4), Color(0xFF4D7C0F)),
        info = Tone(soft, primary),
    )
}

private val LocalScaColors = staticCompositionLocalOf { extras(RoleAccent.AZUL, false, Color(0xFF5267F7)) }

/** Tokens extendidos. Llamar dentro de [ScaTheme]. */
val scaColors: ScaColors
    @Composable
    get() = LocalScaColors.current

private val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun TextStyle.inter() = copy(fontFamily = Inter)

private val AppTypography = Typography().let { d ->
    Typography(
        displayLarge = d.displayLarge.inter(), displayMedium = d.displayMedium.inter(), displaySmall = d.displaySmall.inter(),
        headlineLarge = d.headlineLarge.inter(),
        headlineMedium = d.headlineMedium.inter().copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = d.headlineSmall.inter().copy(fontWeight = FontWeight.SemiBold),
        titleLarge = d.titleLarge.inter().copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = d.titleMedium.inter().copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
        titleSmall = d.titleSmall.inter().copy(fontWeight = FontWeight.Medium),
        bodyLarge = d.bodyLarge.inter().copy(fontSize = 16.sp),
        bodyMedium = d.bodyMedium.inter().copy(fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = d.bodySmall.inter().copy(fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = d.labelLarge.inter().copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
        labelMedium = d.labelMedium.inter().copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
        labelSmall = d.labelSmall.inter().copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

@Composable
fun ScaTheme(
    accent: RoleAccent = RoleAccent.AZUL,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = scheme(accent, darkTheme)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    CompositionLocalProvider(LocalScaColors provides extras(accent, darkTheme, colors.primary)) {
        MaterialTheme(colorScheme = colors, typography = AppTypography, shapes = AppShapes) {
            // Compose usa negro como color de contenido por defecto, y ninguna pantalla de la app
            // está dentro de un Surface/Scaffold raíz que lo cambie. Sin esto, todo Text/Icon sin
            // color explícito queda negro sobre el fondo oscuro.
            CompositionLocalProvider(LocalContentColor provides colors.onBackground, content = content)
        }
    }
}

@Composable
fun ScaTheme(role: Role, darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) =
    ScaTheme(role.accent, darkTheme, content)

/** Nombre histórico; equivale a [ScaTheme] con acento azul (login). */
@Composable
fun ScaPadresTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) =
    ScaTheme(RoleAccent.AZUL, darkTheme, content)

/** Oscuro según la preferencia del usuario (claro / oscuro / igual que el celular). */
@Composable
fun rememberDarkTheme(prefs: ThemePrefs): Boolean {
    val mode by prefs.mode.collectAsStateWithLifecycle()
    return when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
}
