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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hardware-accelerated, low-end-device-friendly animated liquid tank + expressive character.
 * Uses a single Canvas with minimal allocations so it runs at 60fps even on Android 9 budget phones.
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
        targetValue = progress.coerceIn(0.04f, 1.0f),
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessLow
        ),
        label = "tankProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "tankLoops")

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val bubbleCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubbleCycle"
    )

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (mood == CharacterMood.CELEBRATING) 900 else 1700,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )

    // Periodic eye blink
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3200)
            isBlinking = true
            delay(130)
            isBlinking = false
        }
    }

    // Interactive squish & bounce on tap
    val tapScaleX = remember { Animatable(1f) }
    val tapScaleY = remember { Animatable(1f) }
    val tapRotation = remember { Animatable(0f) }
    var isWinking by remember { mutableStateOf(false) }

    val wavePath1 = remember { Path() }
    val wavePath2 = remember { Path() }

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
                        tapScaleX.animateTo(1.18f, tween(90))
                        tapScaleX.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
                    }
                    launch {
                        tapScaleY.animateTo(0.84f, tween(90))
                        tapScaleY.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
                    }
                    launch {
                        tapRotation.animateTo(-6f, tween(90))
                        tapRotation.animateTo(6f, tween(110))
                        tapRotation.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
                    }
                    delay(450)
                    isWinking = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val waterTop = h * (1f - animatedProgress)
            val amplitude = (h * 0.032f).coerceAtLeast(6f)

            // Back parallax wave
            wavePath2.reset()
            wavePath2.moveTo(0f, h)
            wavePath2.lineTo(0f, waterTop)
            val step = (w / 24f).coerceAtLeast(10f)
            var x = 0f
            while (x <= w + step) {
                val y = waterTop + sin((x / w) * 2f * PI.toFloat() + wavePhase + 1.8f) * amplitude * 0.85f
                wavePath2.lineTo(x, y)
                x += step
            }
            wavePath2.lineTo(w, h)
            wavePath2.close()

            drawPath(
                path = wavePath2,
                color = HydroColors.MintSplash.copy(alpha = 0.35f)
            )

            // Front main wave
            wavePath1.reset()
            wavePath1.moveTo(0f, h)
            wavePath1.lineTo(0f, waterTop)
            x = 0f
            while (x <= w + step) {
                val y = waterTop + sin((x / w) * 2.2f * PI.toFloat() - wavePhase) * amplitude
                wavePath1.lineTo(x, y)
                x += step
            }
            wavePath1.lineTo(w, h)
            wavePath1.close()

            drawPath(
                path = wavePath1,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        HydroColors.AquaPrimary.copy(alpha = 0.52f),
                        HydroColors.OceanDeep.copy(alpha = 0.68f)
                    ),
                    startY = waterTop,
                    endY = h
                )
            )

            // Lightweight rising bubbles inside water
            val bubbleSeeds = 7
            for (i in 0 until bubbleSeeds) {
                val offset = ((bubbleCycle + i * 0.143f) % 1f)
                val bx = w * (0.14f + (i * 0.125f) % 0.76f) + sin(offset * 6f + i) * 10f
                val by = h - offset * (h - waterTop + 20f)
                if (by > waterTop + 8f) {
                    val radius = (5f + (i % 3) * 3.5f)
                    drawCircle(
                        color = Color.White.copy(alpha = (1f - offset) * 0.45f),
                        radius = radius,
                        center = Offset(bx, by)
                    )
                }
            }

            // Celebration sparkles when goal reached
            if (mood == CharacterMood.CELEBRATING) {
                for (s in 0 until 6) {
                    val angle = wavePhase + s * (PI.toFloat() / 3f)
                    val sx = w * 0.5f + cos(angle) * (w * 0.33f)
                    val sy = h * 0.45f + sin(angle * 1.5f) * (h * 0.25f)
                    drawCircle(
                        color = if (s % 2 == 0) HydroColors.GoldSpark else HydroColors.CoralAccent,
                        radius = 5f + (s % 2) * 2f,
                        center = Offset(sx, sy)
                    )
                }
            }
        }

        // Character Layer with hardware-accelerated graphicsLayer transform
        HydroCharacterAvatar(
            characterType = characterType,
            mood = mood,
            isBlinking = isBlinking,
            isWinking = isWinking,
            modifier = Modifier
                .fillMaxSize(0.66f)
                .graphicsLayer {
                    translationY = floatOffset * density
                    scaleX = tapScaleX.value
                    scaleY = tapScaleY.value
                    rotationZ = tapRotation.value
                }
        )
    }
}

@Composable
fun HydroCharacterAvatar(
    characterType: HydroCharacterType,
    mood: CharacterMood,
    isBlinking: Boolean,
    isWinking: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bodyPath = remember { Path() }
    val innerPath = remember { Path() }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        when (characterType) {
            HydroCharacterType.DROPPY -> drawDroppy(w, h, cx, cy, bodyPath, innerPath, mood, isBlinking, isWinking)
            HydroCharacterType.BUBBELS -> drawBubbels(w, h, cx, cy, bodyPath, mood, isBlinking, isWinking)
            HydroCharacterType.CLOUDY -> drawCloudy(w, h, cx, cy, mood, isBlinking, isWinking)
        }
    }
}

private fun DrawScope.drawDroppy(
    w: Float,
    h: Float,
    cx: Float,
    cy: Float,
    bodyPath: Path,
    innerPath: Path,
    mood: CharacterMood,
    isBlinking: Boolean,
    isWinking: Boolean
) {
    bodyPath.reset()
    bodyPath.moveTo(cx, h * 0.12f)
    bodyPath.cubicTo(
        cx + w * 0.36f, h * 0.38f,
        cx + w * 0.38f, h * 0.86f,
        cx, h * 0.88f
    )
    bodyPath.cubicTo(
        cx - w * 0.38f, h * 0.86f,
        cx - w * 0.36f, h * 0.38f,
        cx, h * 0.12f
    )
    bodyPath.close()

    // Soft shadow
    drawCircle(
        color = HydroColors.OceanDeep.copy(alpha = 0.14f),
        radius = w * 0.28f,
        center = Offset(cx, h * 0.90f)
    )

    drawPath(
        path = bodyPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF56E0F5), HydroColors.AquaPrimary, HydroColors.OceanDeep)
        )
    )

    // Inner glossy highlight
    innerPath.reset()
    innerPath.moveTo(cx - w * 0.14f, h * 0.40f)
    innerPath.quadraticTo(cx - w * 0.22f, h * 0.54f, cx - w * 0.16f, h * 0.64f)
    drawPath(
        path = innerPath,
        color = Color.White.copy(alpha = 0.55f),
        style = Stroke(width = w * 0.045f, cap = StrokeCap.Round)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.65f),
        radius = w * 0.025f,
        center = Offset(cx - w * 0.10f, h * 0.32f)
    )

    if (mood == CharacterMood.CELEBRATING) {
        drawCuteCrown(cx, h * 0.09f, w * 0.16f)
    }

    drawKawaiiFace(
        cx = cx,
        eyeY = h * 0.56f,
        eyeSpacing = w * 0.11f,
        eyeRadius = w * 0.042f,
        mouthY = h * 0.65f,
        mood = mood,
        isBlinking = isBlinking,
        isWinking = isWinking
    )
}

private fun DrawScope.drawBubbels(
    w: Float,
    h: Float,
    cx: Float,
    cy: Float,
    bodyPath: Path,
    mood: CharacterMood,
    isBlinking: Boolean,
    isWinking: Boolean
) {
    // Cute little ear fins (axolotl/sea-spirit style)
    val finColor = Color(0xFFFF8FA3)
    drawOval(
        color = finColor,
        topLeft = Offset(cx - w * 0.42f, cy - h * 0.12f),
        size = Size(w * 0.16f, h * 0.10f)
    )
    drawOval(
        color = finColor,
        topLeft = Offset(cx - w * 0.40f, cy - h * 0.01f),
        size = Size(w * 0.14f, h * 0.09f)
    )
    drawOval(
        color = finColor,
        topLeft = Offset(cx + w * 0.26f, cy - h * 0.12f),
        size = Size(w * 0.16f, h * 0.10f)
    )
    drawOval(
        color = finColor,
        topLeft = Offset(cx + w * 0.26f, cy - h * 0.01f),
        size = Size(w * 0.14f, h * 0.09f)
    )

    // Round squishy blob body
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF72EFDD), Color(0xFF48CAE4), Color(0xFF0096C7))
        ),
        topLeft = Offset(cx - w * 0.32f, cy - h * 0.26f),
        size = Size(w * 0.64f, h * 0.56f),
        cornerRadius = CornerRadius(w * 0.30f, w * 0.30f)
    )

    // Belly patch
    drawOval(
        color = Color.White.copy(alpha = 0.35f),
        topLeft = Offset(cx - w * 0.19f, cy + h * 0.03f),
        size = Size(w * 0.38f, h * 0.22f)
    )

    if (mood == CharacterMood.CELEBRATING) {
        drawCuteCrown(cx, cy - h * 0.29f, w * 0.16f)
    }

    drawKawaiiFace(
        cx = cx,
        eyeY = cy - h * 0.06f,
        eyeSpacing = w * 0.12f,
        eyeRadius = w * 0.042f,
        mouthY = cy + h * 0.04f,
        mood = mood,
        isBlinking = isBlinking,
        isWinking = isWinking
    )
}

private fun DrawScope.drawCloudy(
    w: Float,
    h: Float,
    cx: Float,
    cy: Float,
    mood: CharacterMood,
    isBlinking: Boolean,
    isWinking: Boolean
) {
    // Little dangling feet
    drawRoundRect(
        color = HydroColors.OceanDeep,
        topLeft = Offset(cx - w * 0.14f, cy + h * 0.19f),
        size = Size(w * 0.08f, h * 0.10f),
        cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
    )
    drawRoundRect(
        color = HydroColors.OceanDeep,
        topLeft = Offset(cx + w * 0.06f, cy + h * 0.19f),
        size = Size(w * 0.08f, h * 0.10f),
        cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
    )

    // Fluffy cloud puffs
    val cloudBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFFFFF), Color(0xFFD8F3FF))
    )
    drawCircle(brush = cloudBrush, radius = w * 0.19f, center = Offset(cx - w * 0.18f, cy + h * 0.02f))
    drawCircle(brush = cloudBrush, radius = w * 0.19f, center = Offset(cx + w * 0.18f, cy + h * 0.02f))
    drawCircle(brush = cloudBrush, radius = w * 0.24f, center = Offset(cx, cy - h * 0.08f))
    drawRoundRect(
        brush = cloudBrush,
        topLeft = Offset(cx - w * 0.30f, cy - h * 0.04f),
        size = Size(w * 0.60f, h * 0.26f),
        cornerRadius = CornerRadius(w * 0.14f, w * 0.14f)
    )

    if (mood == CharacterMood.CELEBRATING) {
        drawCuteCrown(cx, cy - h * 0.34f, w * 0.16f)
    }

    drawKawaiiFace(
        cx = cx,
        eyeY = cy + h * 0.01f,
        eyeSpacing = w * 0.11f,
        eyeRadius = w * 0.038f,
        mouthY = cy + h * 0.10f,
        mood = mood,
        isBlinking = isBlinking,
        isWinking = isWinking
    )
}

private fun DrawScope.drawCuteCrown(cx: Float, baseY: Float, width: Float) {
    val crownPath = Path().apply {
        moveTo(cx - width * 0.5f, baseY + width * 0.35f)
        lineTo(cx - width * 0.55f, baseY)
        lineTo(cx - width * 0.20f, baseY + width * 0.18f)
        lineTo(cx, baseY - width * 0.12f)
        lineTo(cx + width * 0.20f, baseY + width * 0.18f)
        lineTo(cx + width * 0.55f, baseY)
        lineTo(cx + width * 0.5f, baseY + width * 0.35f)
        close()
    }
    drawPath(path = crownPath, color = HydroColors.GoldSpark)
}

private fun DrawScope.drawKawaiiFace(
    cx: Float,
    eyeY: Float,
    eyeSpacing: Float,
    eyeRadius: Float,
    mouthY: Float,
    mood: CharacterMood,
    isBlinking: Boolean,
    isWinking: Boolean
) {
    val ink = HydroColors.InkDark

    // Left Eye
    if (isBlinking || isWinking) {
        drawLine(
            color = ink,
            start = Offset(cx - eyeSpacing - eyeRadius, eyeY),
            end = Offset(cx - eyeSpacing + eyeRadius, eyeY),
            strokeWidth = eyeRadius * 0.7f,
            cap = StrokeCap.Round
        )
    } else {
        drawCircle(color = ink, radius = eyeRadius, center = Offset(cx - eyeSpacing, eyeY))
        drawCircle(
            color = Color.White,
            radius = eyeRadius * 0.38f,
            center = Offset(cx - eyeSpacing - eyeRadius * 0.25f, eyeY - eyeRadius * 0.25f)
        )
    }

    // Right Eye
    if (isBlinking) {
        drawLine(
            color = ink,
            start = Offset(cx + eyeSpacing - eyeRadius, eyeY),
            end = Offset(cx + eyeSpacing + eyeRadius, eyeY),
            strokeWidth = eyeRadius * 0.7f,
            cap = StrokeCap.Round
        )
    } else {
        drawCircle(color = ink, radius = eyeRadius, center = Offset(cx + eyeSpacing, eyeY))
        drawCircle(
            color = Color.White,
            radius = eyeRadius * 0.38f,
            center = Offset(cx + eyeSpacing - eyeRadius * 0.25f, eyeY - eyeRadius * 0.25f)
        )
    }

    // Rosy Cheeks
    val cheekAlpha = if (mood == CharacterMood.THIRSTY) 0.35f else 0.75f
    drawOval(
        color = HydroColors.CoralAccent.copy(alpha = cheekAlpha),
        topLeft = Offset(cx - eyeSpacing - eyeRadius * 1.4f, eyeY + eyeRadius * 0.8f),
        size = Size(eyeRadius * 1.8f, eyeRadius * 1.0f)
    )
    drawOval(
        color = HydroColors.CoralAccent.copy(alpha = cheekAlpha),
        topLeft = Offset(cx + eyeSpacing - eyeRadius * 0.4f, eyeY + eyeRadius * 0.8f),
        size = Size(eyeRadius * 1.8f, eyeRadius * 1.0f)
    )

    // Mouth based on Mood
    val mouthPath = Path()
    when (mood) {
        CharacterMood.THIRSTY -> {
            // Cute wavy/pouty "need water" mouth
            mouthPath.moveTo(cx - eyeRadius * 1.1f, mouthY + eyeRadius * 0.3f)
            mouthPath.quadraticTo(cx, mouthY - eyeRadius * 0.4f, cx + eyeRadius * 1.1f, mouthY + eyeRadius * 0.3f)
            drawPath(
                path = mouthPath,
                color = ink,
                style = Stroke(width = eyeRadius * 0.55f, cap = StrokeCap.Round)
            )
        }
        CharacterMood.HAPPY, CharacterMood.ENERGIZED -> {
            mouthPath.moveTo(cx - eyeRadius * 1.2f, mouthY - eyeRadius * 0.1f)
            mouthPath.quadraticTo(cx, mouthY + eyeRadius * 1.1f, cx + eyeRadius * 1.2f, mouthY - eyeRadius * 0.1f)
            drawPath(
                path = mouthPath,
                color = ink,
                style = Stroke(width = eyeRadius * 0.6f, cap = StrokeCap.Round)
            )
        }
        CharacterMood.CELEBRATING -> {
            // Big open joyful smile
            mouthPath.moveTo(cx - eyeRadius * 1.4f, mouthY - eyeRadius * 0.2f)
            mouthPath.quadraticTo(cx, mouthY + eyeRadius * 1.7f, cx + eyeRadius * 1.4f, mouthY - eyeRadius * 0.2f)
            mouthPath.close()
            drawPath(path = mouthPath, color = HydroColors.CoralAccent)
            drawPath(
                path = mouthPath,
                color = ink,
                style = Stroke(width = eyeRadius * 0.45f, cap = StrokeCap.Round)
            )
        }
    }
}
