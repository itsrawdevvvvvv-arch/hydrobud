package com.hydrobud.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hydrobud.app.data.HydroCharacterType
import com.hydrobud.app.ui.components.PillProgressBar
import com.hydrobud.app.ui.theme.HydroColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Dynamic, GPU-accelerated opening animation inspired by 262479.jpg.
 * Uses only graphicsLayer transforms + a single Canvas with zero heavy allocations for 60fps on low-end devices.
 */
@Composable
fun SplashLoadingScreen(
    characterType: HydroCharacterType,
    onFinished: () -> Unit
) {
    val domeRise = remember { Animatable(1f) }      // 1f = below screen, 0f = resting position
    val domeSquish = remember { Animatable(0.85f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(40f) }
    val eyeOpen = remember { Animatable(0f) }
    val loadProgress = remember { Animatable(0f) }
    val exitAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Phase 1: Rising dome springs up + title slides in
        launch {
            domeRise.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.62f,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            delay(120)
            domeSquish.animateTo(1.06f, tween(220, easing = FastOutSlowInEasing))
            domeSquish.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
        }
        launch {
            delay(180)
            titleAlpha.animateTo(1f, tween(360))
        }
        launch {
            delay(180)
            titleOffsetY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        }
        launch {
            delay(320)
            eyeOpen.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
        }
        // Phase 2: Sleek progress pill fills smoothly
        loadProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1250, easing = FastOutSlowInEasing)
        )
        delay(160)
        exitAlpha.animateTo(0f, tween(220))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .graphicsLayer { alpha = exitAlpha.value }
    ) {
        // Rising Cyan Dome Character at bottom (inspired by 262479.jpg middle screen)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    translationY = domeRise.value * 420f
                    scaleY = domeSquish.value
                }
        ) {
            val w = size.width
            val h = size.height
            val domePath = Path().apply {
                moveTo(0f, h)
                lineTo(0f, h * 0.42f)
                quadraticTo(w * 0.5f, -h * 0.14f, w, h * 0.42f)
                lineTo(w, h)
                close()
            }
            drawPath(path = domePath, color = HydroColors.CyanDome)
            drawPath(
                path = Path().apply {
                    moveTo(0f, h * 0.42f)
                    quadraticTo(w * 0.5f, -h * 0.14f, w, h * 0.42f)
                },
                color = HydroColors.InkBlack,
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )

            // Expressive cartoon eyes & O-mouth popping in
            val e = eyeOpen.value
            if (e > 0.05f) {
                val eyeY = h * 0.52f
                val cx = w * 0.5f
                val eyeSpan = w * 0.14f
                val eyeSize = 26f * e

                // Left > eye
                drawLine(
                    color = HydroColors.InkBlack,
                    start = Offset(cx - eyeSpan - eyeSize, eyeY - eyeSize * 0.6f),
                    end = Offset(cx - eyeSpan + eyeSize * 0.4f, eyeY),
                    strokeWidth = 9f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = HydroColors.InkBlack,
                    start = Offset(cx - eyeSpan - eyeSize, eyeY + eyeSize * 0.6f),
                    end = Offset(cx - eyeSpan + eyeSize * 0.4f, eyeY),
                    strokeWidth = 9f,
                    cap = StrokeCap.Round
                )

                // Right < eye
                drawLine(
                    color = HydroColors.InkBlack,
                    start = Offset(cx + eyeSpan + eyeSize, eyeY - eyeSize * 0.6f),
                    end = Offset(cx + eyeSpan - eyeSize * 0.4f, eyeY),
                    strokeWidth = 9f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = HydroColors.InkBlack,
                    start = Offset(cx + eyeSpan + eyeSize, eyeY + eyeSize * 0.6f),
                    end = Offset(cx + eyeSpan - eyeSize * 0.4f, eyeY),
                    strokeWidth = 9f,
                    cap = StrokeCap.Round
                )

                // Cute oval mouth
                drawOval(
                    color = HydroColors.InkBlack,
                    topLeft = Offset(cx - 20f * e, eyeY + 34f),
                    size = androidx.compose.ui.geometry.Size(40f * e, 52f * e)
                )
            }
        }

        // Top Minimal Headline + Progress Pill
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 120.dp, start = 28.dp, end = 28.dp)
                .graphicsLayer {
                    alpha = titleAlpha.value
                    translationY = titleOffsetY.value
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = HydroColors.CoralOrange
            ) {
                Text(
                    text = "HYDROBUD",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SIP WATER.\nKEEP MOVING.",
                fontSize = 36.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .width(200.dp)
                    .clip(CircleShape)
            ) {
                PillProgressBar(
                    progress = loadProgress.value,
                    height = 12.dp,
                    fillColor = HydroColors.CoralOrange
                )
            }
        }
    }
}
