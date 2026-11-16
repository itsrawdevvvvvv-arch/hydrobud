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

/**
 * Minimal, high-contrast playful palette inspired by the reference UI (262479.jpg):
 * Soft Off-White background, Pure White cards, Cyan Dome, Coral-Orange action pills, and Crisp Ink Black.
 */
object HydroColors {
    val OffWhiteBg = Color(0xFFF5F5F5)
    val PureWhite = Color(0xFFFFFFFF)
    val SoftGrayTrack = Color(0xFFEBEBEB)
    val InkBlack = Color(0xFF141414)
    val InkMuted = Color(0xFF757575)

    val CyanDome = Color(0xFF1ED5E2)
    val CyanDeep = Color(0xFF00B4C5)
    val CoralOrange = Color(0xFFFF6536)
    val CoralSoft = Color(0xFFFFEFEA)
    val FreshGreen = Color(0xFF2ECC71)
    val SunnyAmber = Color(0xFFFFB800)

    // Dark mode equivalents
    val DarkBg = Color(0xFF121212)
    val DarkCard = Color(0xFF1E1E1E)
    val DarkTrack = Color(0xFF2C2C2C)

    // Semantic aliases
    val AquaPrimary = CyanDome
    val OceanDeep = CyanDeep
    val MintSplash = CoralOrange
    val SkySoft = CoralSoft
    val CoralAccent = CoralOrange
    val GoldSpark = SunnyAmber
    val MintGreen = FreshGreen
    val InkDark = InkBlack
}

private val LightColorScheme = lightColorScheme(
    primary = HydroColors.CoralOrange,
    onPrimary = Color.White,
    primaryContainer = HydroColors.CoralSoft,
    onPrimaryContainer = HydroColors.InkBlack,
    secondary = HydroColors.CyanDome,
    onSecondary = HydroColors.InkBlack,
    tertiary = HydroColors.FreshGreen,
    background = HydroColors.OffWhiteBg,
    onBackground = HydroColors.InkBlack,
    surface = HydroColors.PureWhite,
    onSurface = HydroColors.InkBlack,
    surfaceVariant = HydroColors.SoftGrayTrack,
    onSurfaceVariant = HydroColors.InkMuted
)

private val DarkColorScheme = darkColorScheme(
    primary = HydroColors.CoralOrange,
    onPrimary = Color.White,
    primaryContainer = HydroColors.DarkTrack,
    onPrimaryContainer = Color.White,
    secondary = HydroColors.CyanDome,
    onSecondary = HydroColors.InkBlack,
    tertiary = HydroColors.FreshGreen,
    background = HydroColors.DarkBg,
    onBackground = Color.White,
    surface = HydroColors.DarkCard,
    onSurface = Color.White,
    surfaceVariant = HydroColors.DarkTrack,
    onSurfaceVariant = Color(0xFFB0B0B0)
)

val PillShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(20.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(50)
)

val HydroTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 44.sp,
        letterSpacing = (-1.2).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 26.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
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
