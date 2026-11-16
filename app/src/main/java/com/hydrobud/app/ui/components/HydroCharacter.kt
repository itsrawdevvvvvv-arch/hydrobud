package com.hydrobud.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.hydrobud.app.data.CharacterMood
import com.hydrobud.app.data.HydroCharacterType
import com.hydrobud.app.ui.theme.HydroColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * 60fps GPU-friendly rising dome character directly inspired by the middle screen of 262479.jpg.
 * Single Canvas, minimal allocations, smooth spring physics on tap and water fill.
 */
@Composable
fun AnimatedLiquidTankWithCharacter(
    progress: Float,
    characterType: HydroCharacterType,
    mood: CharacterMood,
    modifier: Modifier = Modifier,
    onCharacterTap: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0.18f, 1.0f),
        animationSpec = spring(
            dampingRatio = 0.68f,
            stiffness = Spring.StiffnessLow
        ),
        label = "domeProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "domeLoops")

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (mood == CharacterMood.CELEBRATING) 850 else 1700,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )

    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3100)
            isBlinking = true
            delay(120)
            isBlinking = false
        }
    }

    val tapScaleX = remember { Animatable(1f) }
    val tapScaleY = remember { Animatable(1f) }
    var isWinking by remember { mutableStateOf(false) }
    val domePath = remember { Path() }
    val outlinePath = remember { Path() }

    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCharacterTap()
                scope.launch {
                    isWinking = true
                    launch {
                        tapScaleX.animateTo(1.08f, tween(80))
                        tapScaleX.animateTo(1f, spring(Spring.DampingRatioMediumBouncy))
                    }
                    launch {
                        tapScaleY.animateTo(0.92f, tween(80))
                        tapScaleY.animateTo(1f, spring(Spring.DampingRatioMediumBouncy))
                    }
                    delay(400)
                    isWinking = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = tapScaleX.value
                    scaleY = tapScaleY.value
                }
        ) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f

            // Vertical minimal track line (262479.jpg style)
            drawLine(
                color = HydroColors.SoftGrayTrack,
                start = Offset(cx, h * 0.08f),
                end = Offset(cx, h * 0.92f),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )

            // Rising Cyan Dome
            val domeTop = h * (1f - animatedProgress * 0.78f)
            val edgeY = (domeTop + h * 0.24f).coerceAtMost(h * 0.92f)
            val waveDip = sin(wavePhase) * 6f

            domePath.reset()
            domePath.moveTo(0f, h)
            domePath.lineTo(0f, edgeY)
            domePath.quadraticTo(cx, domeTop - h * 0.14f + waveDip, w, edgeY)
            domePath.lineTo(w, h)
            domePath.close()

            val domeColor = when (characterType) {
                HydroCharacterType.DROPPY -> HydroColors.CyanDome
                HydroCharacterType.BUBBELS -> Color(0xFF38E5B3)
                HydroCharacterType.CLOUDY -> Color(0xFF70D6FF)
            }
            drawPath(path = domePath, color = domeColor)

            // Crisp dark top arc outline
            outlinePath.reset()
            outlinePath.moveTo(0f, edgeY)
            outlinePath.quadraticTo(cx, domeTop - h * 0.14f + waveDip, w, edgeY)
            drawPath(
                path = outlinePath,
                color = HydroColors.InkBlack,
                style = Stroke(width = 5.5f, cap = StrokeCap.Round)
            )

            // Track knob sitting on top of the dome
            val knobY = (domeTop + (edgeY - domeTop) * 0.22f + waveDip * 0.5f).coerceIn(h * 0.14f, h * 0.85f)
            drawCircle(
                color = Color.White,
                radius = 22f,
                center = Offset(cx, knobY)
            )
            drawCircle(
                color = HydroColors.InkBlack,
                radius = 22f,
                center = Offset(cx, knobY),
                style = Stroke(width = 3.5f)
            )
            // Tiny smiley inside the knob
            drawCircle(color = HydroColors.InkBlack, radius = 2.6f, center = Offset(cx - 6f, knobY - 3f))
            drawCircle(color = HydroColors.InkBlack, radius = 2.6f, center = Offset(cx + 6f, knobY - 3f))
            drawArc(
                color = HydroColors.InkBlack,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(cx - 7f, knobY - 4f),
                size = Size(14f, 12f),
                style = Stroke(width = 2.6f, cap = StrokeCap.Round)
            )

            // Expressive Cartoon Face inside the Dome
            val faceCenterY = ((knobY + h) * 0.52f + floatOffset).coerceIn(knobY + 55f, h - 48f)
            drawDomeFace(
                cx = cx,
                cy = faceCenterY,
                mood = mood,
                isBlinking = isBlinking,
                isWinking = isWinking
            )
        }
    }
}

private fun DrawScope.drawDomeFace(
    cx: Float,
    cy: Float,
    mood: CharacterMood,
    isBlinking: Boolean,
    isWinking: Boolean
) {
    val ink = HydroColors.InkBlack
    val eyeSpan = 56f

    when {
        isBlinking || isWinking -> {
            // Squint > < eyes just like 262479.jpg!
            drawLine(ink, Offset(cx - eyeSpan - 16f, cy - 12f), Offset(cx - eyeSpan + 8f, cy), 8f, StrokeCap.Round)
            drawLine(ink, Offset(cx - eyeSpan - 16f, cy + 12f), Offset(cx - eyeSpan + 8f, cy), 8f, StrokeCap.Round)
            drawLine(ink, Offset(cx + eyeSpan + 16f, cy - 12f), Offset(cx + eyeSpan - 8f, cy), 8f, StrokeCap.Round)
            drawLine(ink, Offset(cx + eyeSpan + 16f, cy + 12f), Offset(cx + eyeSpan - 8f, cy), 8f, StrokeCap.Round)
        }
        mood == CharacterMood.THIRSTY -> {
            // Cute > < expressive eyes from 262479.jpg
            drawLine(ink, Offset(cx - eyeSpan - 16f, cy - 12f), Offset(cx - eyeSpan + 8f, cy), 8f, StrokeCap.Round)
            drawLine(ink, Offset(cx - eyeSpan - 16f, cy + 12f), Offset(cx - eyeSpan + 8f, cy), 8f, StrokeCap.Round)
            drawLine(ink, Offset(cx + eyeSpan + 16f, cy - 12f), Offset(cx + eyeSpan - 8f, cy), 8f, StrokeCap.Round)
            drawLine(ink, Offset(cx + eyeSpan + 16f, cy + 12f), Offset(cx + eyeSpan - 8f, cy), 8f, StrokeCap.Round)
        }
        else -> {
            // Happy round eyes
            drawCircle(color = ink, radius = 12f, center = Offset(cx - eyeSpan, cy))
            drawCircle(color = Color.White, radius = 4f, center = Offset(cx - eyeSpan - 3f, cy - 4f))
            drawCircle(color = ink, radius = 12f, center = Offset(cx + eyeSpan, cy))
            drawCircle(color = Color.White, radius = 4f, center = Offset(cx + eyeSpan - 3f, cy - 4f))
        }
    }

    // Rosy cheeks
    drawOval(
        color = HydroColors.CoralOrange.copy(alpha = 0.55f),
        topLeft = Offset(cx - eyeSpan - 26f, cy + 18f),
        size = Size(28f, 14f)
    )
    drawOval(
        color = HydroColors.CoralOrange.copy(alpha = 0.55f),
        topLeft = Offset(cx + eyeSpan - 2f, cy + 18f),
        size = Size(28f, 14f)
    )

    // Mouth
    when (mood) {
        CharacterMood.THIRSTY -> {
            // Big cute O mouth from 262479.jpg
            drawOval(
                color = ink,
                topLeft = Offset(cx - 16f, cy + 20f),
                size = Size(32f, 40f)
            )
        }
        CharacterMood.CELEBRATING -> {
            drawArc(
                color = ink,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(cx - 24f, cy + 14f),
                size = Size(48f, 36f)
            )
        }
        else -> {
            drawArc(
                color = ink,
                startAngle = 15f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(cx - 20f, cy + 10f),
                size = Size(40f, 28f),
                style = Stroke(width = 7f, cap = StrokeCap.Round)
            )
        }
    }
}
