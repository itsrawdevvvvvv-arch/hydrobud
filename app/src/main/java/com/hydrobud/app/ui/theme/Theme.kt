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
 * Color palette directly inspired by the "Let's stream TV shows now" reference:
 * Periwinkle Purple, Electric Yellow, Blush Peach, Warm Amber, Brick Red, and Crisp Ink Black.
 */
object HydroColors {
    val PeriwinklePurple = Color(0xFF7C76B8)
    val PeriwinkleDeep = Color(0xFF5D579B)
    val ElectricYellow = Color(0xFFF5EB3B)
    val BlushPeach = Color(0xFFF9C8BE)
    val WarmAmber = Color(0xFFF2BF5E)
    val BrickRed = Color(0xFFC23F38)
    val InkBlack = Color(0xFF111111)
    val CreamWhite = Color(0xFFFFFDF9)
    val SoftLavenderBg = Color(0xFFF4F2FA)
    val MintPop = Color(0xFF6EE7B7)

    // Dark mode variants preserving the playful pop contrast
    val DarkPeriwinkleBg = Color(0xFF1A182E)
    val DarkCard = Color(0xFF262342)
    val DarkSurfaceVar = Color(0xFF343059)

    // Semantic aliases used across components
    val AquaPrimary = PeriwinklePurple
    val OceanDeep = PeriwinkleDeep
    val MintSplash = ElectricYellow
    val SkySoft = BlushPeach
    val CoralAccent = BrickRed
    val GoldSpark = ElectricYellow
    val MintGreen = MintPop
    val InkDark = InkBlack
    val InkMuted = Color(0xFF5A5678)
}

private val LightColorScheme = lightColorScheme(
    primary = HydroColors.PeriwinklePurple,
    onPrimary = Color.White,
    primaryContainer = HydroColors.BlushPeach,
    onPrimaryContainer = HydroColors.InkBlack,
    secondary = HydroColors.ElectricYellow,
    onSecondary = HydroColors.InkBlack,
    tertiary = HydroColors.WarmAmber,
    onTertiary = HydroColors.InkBlack,
    background = HydroColors.PeriwinklePurple,
    onBackground = HydroColors.InkBlack,
    surface = HydroColors.CreamWhite,
    onSurface = HydroColors.InkBlack,
    surfaceVariant = HydroColors.SoftLavenderBg,
    onSurfaceVariant = HydroColors.InkMuted
)

private val DarkColorScheme = darkColorScheme(
    primary = HydroColors.ElectricYellow,
    onPrimary = HydroColors.InkBlack,
    primaryContainer = HydroColors.DarkSurfaceVar,
    onPrimaryContainer = HydroColors.BlushPeach,
    secondary = HydroColors.PeriwinklePurple,
    onSecondary = Color.White,
    tertiary = HydroColors.WarmAmber,
    background = HydroColors.DarkPeriwinkleBg,
    onBackground = HydroColors.CreamWhite,
    surface = HydroColors.DarkCard,
    onSurface = HydroColors.CreamWhite,
    surfaceVariant = HydroColors.DarkSurfaceVar,
    onSurfaceVariant = Color(0xFFC7C3E6)
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
        fontWeight = FontWeight.Black,
        fontSize = 42.sp,
        letterSpacing = (-1.0).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
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
        fontWeight = FontWeight.ExtraBold,
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
