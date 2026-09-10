package py.edu.ctn.sca.padres.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val Brand = Color(0xFF1F5FA8)
private val BrandDark = Color(0xFF9DC5F2)

private val LightColors = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E7FA),
    onPrimaryContainer = Color(0xFF0A2A4D),
    secondary = Color(0xFF2E7D6B),
    background = Color(0xFFF7F8FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFEDF1F6),
    onSurfaceVariant = Color(0xFF44515F),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = BrandDark,
    onPrimary = Color(0xFF07243F),
    primaryContainer = Color(0xFF204974),
    onPrimaryContainer = Color(0xFFD8E7FA),
    secondary = Color(0xFF8FD3C2),
    background = Color(0xFF101418),
    surface = Color(0xFF171C21),
    surfaceVariant = Color(0xFF2A313A),
    onSurfaceVariant = Color(0xFFC0C8D2),
    error = Color(0xFFF2B8B5),
)

private val AppTypography = Typography()

@Composable
fun ScaPadresTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // Edge-to-edge is enabled in MainActivity; just match the icon tint.
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}
