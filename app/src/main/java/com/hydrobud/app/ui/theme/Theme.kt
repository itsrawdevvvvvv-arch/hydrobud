package com.hydrobud.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object HydroColors {
    val AquaPrimary = Color(0xFF00B4D8)
    val OceanDeep = Color(0xFF0077B6)
    val MintSplash = Color(0xFF48CAE4)
    val SkySoft = Color(0xFFCAF0F8)
    val IceBgLight = Color(0xFFF0F9FF)
    val CardLight = Color(0xFFFFFFFF)
    val CoralAccent = Color(0xFFFF758F)
    val GoldSpark = Color(0xFFFFD166)
    val MintGreen = Color(0xFF06D6A0)
    val InkDark = Color(0xFF0B2545)
    val InkMuted = Color(0xFF527999)

    val DeepBgDark = Color(0xFF071526)
    val CardDark = Color(0xFF0F253E)
    val SurfaceDark = Color(0xFF163252)
}

private val LightColorScheme = lightColorScheme(
    primary = HydroColors.AquaPrimary,
    onPrimary = Color.White,
    primaryContainer = HydroColors.SkySoft,
    onPrimaryContainer = HydroColors.InkDark,
    secondary = HydroColors.OceanDeep,
    onSecondary = Color.White,
    tertiary = HydroColors.CoralAccent,
    background = HydroColors.IceBgLight,
    onBackground = HydroColors.InkDark,
    surface = HydroColors.CardLight,
    onSurface = HydroColors.InkDark,
    surfaceVariant = Color(0xFFE3F4FC),
    onSurfaceVariant = HydroColors.InkMuted
)

private val DarkColorScheme = darkColorScheme(
    primary = HydroColors.MintSplash,
    onPrimary = HydroColors.InkDark,
    primaryContainer = HydroColors.SurfaceDark,
    onPrimaryContainer = HydroColors.SkySoft,
    secondary = HydroColors.AquaPrimary,
    onSecondary = HydroColors.InkDark,
    tertiary = HydroColors.CoralAccent,
    background = HydroColors.DeepBgDark,
    onBackground = Color(0xFFEAF6FF),
    surface = HydroColors.CardDark,
    onSurface = Color(0xFFEAF6FF),
    surfaceVariant = HydroColors.SurfaceDark,
    onSurfaceVariant = Color(0xFF9EC5E6)
)

val PillShapes = Shapes(
    extraSmall = RoundedCornerShape(16.dp),
    small = RoundedCornerShape(24.dp),
    medium = RoundedCornerShape(28.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(50)
)

val HydroTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    )
)

@Composable
fun HydroBudTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        shapes = PillShapes,
        typography = HydroTypography,
        content = content
    )
}
