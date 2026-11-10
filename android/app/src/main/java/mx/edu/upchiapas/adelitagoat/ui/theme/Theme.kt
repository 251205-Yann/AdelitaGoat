package mx.edu.upchiapas.adelitagoat.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Paleta oficial de Figma y Documentación Técnica (Sección 9.1)
val AdelitaNavy = Color(0xFF2F3C7E)
val AdelitaNavyDark = Color(0xFF1F2859)
val AdelitaCoral = Color(0xFFF96167)
val AdelitaCoralLight = Color(0xFFFFECEC)
val AdelitaGold = Color(0xFFF9E795)
val AdelitaGoldDark = Color(0xFFD49B00)
val AdelitaSurfaceGray = Color(0xFFF7F7F7)
val AdelitaTextDark = Color(0xFF333333)
val AdelitaTextMuted = Color(0xFF6E717C)
val AdelitaSuccessGreen = Color(0xFF1E8E5A)
val AdelitaSuccessBg = Color(0xFFE7F6EE)
val AdelitaWarningBg = Color(0xFFFFF8DB)

private val LightColors = lightColorScheme(
    primary = AdelitaNavy,
    onPrimary = Color.White,
    primaryContainer = AdelitaGold,
    onPrimaryContainer = AdelitaNavy,
    secondary = AdelitaCoral,
    onSecondary = Color.White,
    secondaryContainer = AdelitaCoralLight,
    onSecondaryContainer = AdelitaCoral,
    tertiary = AdelitaGold,
    onTertiary = AdelitaTextDark,
    background = Color.White,
    onBackground = AdelitaTextDark,
    surface = Color.White,
    onSurface = AdelitaTextDark,
    surfaceVariant = AdelitaSurfaceGray,
    onSurfaceVariant = AdelitaTextMuted,
    error = Color(0xFFD32F2F),
    onError = Color.White
)

val AdelitaTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        color = AdelitaNavy
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = AdelitaNavy
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = AdelitaTextDark
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = AdelitaTextDark
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = AdelitaTextDark
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = AdelitaTextMuted
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        color = AdelitaTextMuted
    )
)

val AdelitaShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun AdelitaGOATTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AdelitaTypography,
        shapes = AdelitaShapes,
        content = content
    )
}
