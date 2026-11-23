package ch.awakened.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ch.awakened.app.R

object Sys {
    val Navy0 = Color(0xFF040915)
    val Navy1 = Color(0xFF071226)
    val Panel = Color(0xFF0B1C38)
    val PanelHi = Color(0xFF10284D)
    val Grid = Color(0xFF12305A)
    val Cyan = Color(0xFF3DE8FF)
    val Blue = Color(0xFF1E90FF)
    val Text = Color(0xFFE6F7FF)
    val Muted = Color(0xFF9DBCDD)
    val Dim = Color(0xFF55739A)
    val Red = Color(0xFFFF2E4D)
    val RedDeep = Color(0xFF1A0006)
    val Gold = Color(0xFFFFD166)
    val Green = Color(0xFF3DFFA8)
    val Violet = Color(0xFFB57BFF)
}

val Rajdhani = FontFamily(
    Font(R.font.rajdhani_medium, FontWeight.Medium),
    Font(R.font.rajdhani_semibold, FontWeight.SemiBold),
    Font(R.font.rajdhani_bold, FontWeight.Bold),
)

private val typography = Typography(
    displayLarge = TextStyle(fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 52.sp, letterSpacing = 2.sp),
    displayMedium = TextStyle(fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 40.sp, letterSpacing = 2.sp),
    headlineLarge = TextStyle(fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = 1.5.sp),
    headlineMedium = TextStyle(fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = 1.5.sp),
    titleLarge = TextStyle(fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = 1.5.sp),
    titleMedium = TextStyle(fontFamily = Rajdhani, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, letterSpacing = 1.sp),
    titleSmall = TextStyle(fontFamily = Rajdhani, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 1.sp),
    labelLarge = TextStyle(fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 17.sp, letterSpacing = 1.5.sp),
    labelMedium = TextStyle(fontFamily = Rajdhani, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 1.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
)

private val colors = darkColorScheme(
    primary = Sys.Cyan,
    onPrimary = Sys.Navy0,
    primaryContainer = Sys.PanelHi,
    onPrimaryContainer = Sys.Cyan,
    secondary = Sys.Blue,
    onSecondary = Sys.Text,
    tertiary = Sys.Gold,
    background = Sys.Navy0,
    onBackground = Sys.Text,
    surface = Sys.Panel,
    onSurface = Sys.Text,
    surfaceVariant = Sys.PanelHi,
    onSurfaceVariant = Sys.Muted,
    surfaceContainer = Sys.Panel,
    surfaceContainerHigh = Sys.PanelHi,
    surfaceContainerHighest = Sys.PanelHi,
    error = Sys.Red,
    onError = Sys.Text,
    outline = Sys.Cyan.copy(alpha = 0.5f),
    outlineVariant = Sys.Dim,
)

@Composable
fun AwakenedTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
