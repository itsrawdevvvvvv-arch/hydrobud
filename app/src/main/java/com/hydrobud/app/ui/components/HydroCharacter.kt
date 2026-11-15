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
 * Hardware-accelerated playful neo-brutalist animated liquid tank + expressive character.
 * Styled with the "Let's stream TV shows now" palette & crisp ink outlines.
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
        targetValue = progress.coerceIn(0.06f, 1.0f),
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
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val bubbleCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubbleCycle"
    )

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (mood == CharacterMood.CELEBRATING) 850 else 1600,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )

    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            isBlinking = true
            delay(130)
            isBlinking = false
        }
    }

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
                        tapScaleX.animateTo(1.20f, tween(85))
                        tapScaleX.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
                    }
                    launch {
                        tapScaleY.animateTo(0.82f, tween(85))
                        tapScaleY.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
                    }
                    launch {
                        tapRotation.animateTo(-7f, tween(85))
                        tapRotation.animateTo(7f, tween(105))
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
            val amplitude = (h * 0.035f).coerceAtLeast(6f)

            // Back parallax wave (Warm Amber)
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
                color = HydroColors.BlushPeach.copy(alpha = 0.72f)
            )

            // Front main wave (Periwinkle Purple)
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
                color = HydroColors.PeriwinklePurple.copy(alpha = 0.85f)
            )

            // Rising playful bubbles
            val bubbleSeeds = 7
            for (i in 0 until bubbleSeeds) {
                val offset = ((bubbleCycle + i * 0.143f) % 1f)
                val bx = w * (0.14f + (i * 0.125f) % 0.76f) + sin(offset * 6f + i) * 10f
                val by = h - offset * (h - waterTop + 20f)
                if (by > waterTop + 8f) {
                    val radius = (5.5f + (i % 3) * 3.5f)
                    drawCircle(
                        color = HydroColors.ElectricYellow.copy(alpha = (1f - offset) * 0.75f),
                        radius = radius,
                        center = Offset(bx, by)
                    )
                }
            }

            if (mood == CharacterMood.CELEBRATING) {
                for (s in 0 until 6) {
                    val angle = wavePhase + s * (PI.toFloat() / 3f)
                    val sx = w * 0.5f + cos(angle) * (w * 0.34f)
                    val sy = h * 0.45f + sin(angle * 1.5f) * (h * 0.25f)
                    drawCircle(
                        color = if (s % 2 == 0) HydroColors.ElectricYellow else HydroColors.BrickRed,
                        radius = 6f + (s % 2) * 2f,
                        center = Offset(sx, sy)
                    )
                }
            }
        }

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
    val outlineStroke = (w * 0.028f).coerceAtLeast(4f)
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

    // Crisp neo-brutalist black offset shadow
    drawCircle(
        color = HydroColors.InkBlack.copy(alpha = 0.22f),
        radius = w * 0.27f,
        center = Offset(cx + w * 0.02f, h * 0.89f)
    )

    drawPath(path = bodyPath, color = HydroColors.ElectricYellow)
    drawPath(path = bodyPath, color = HydroColors.InkBlack, style = Stroke(width = outlineStroke))

    // Inner shine
    innerPath.reset()
    innerPath.moveTo(cx - w * 0.14f, h * 0.40f)
    innerPath.quadraticTo(cx - w * 0.22f, h * 0.54f, cx - w * 0.16f, h * 0.64f)
    drawPath(
        path = innerPath,
        color = Color.White.copy(alpha = 0.75f),
        style = Stroke(width = w * 0.04f, cap = StrokeCap.Round)
    )

    if (mood == CharacterMood.CELEBRATING) {
        drawCuteCrown(cx, h * 0.08f, w * 0.17f, outlineStroke)
    }

    drawKawaiiFace(
        cx = cx,
        eyeY = h * 0.55f,
        eyeSpacing = w * 0.11f,
        eyeRadius = w * 0.055f,
        mouthY = h * 0.66f,
        mood = mood,
        isBlinking = isBlinking,
        isWinking = isWinking,
        outlineStroke = outlineStroke * 0.75f
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
    val outlineStroke = (w * 0.028f).coerceAtLeast(4f)
    val finColor = HydroColors.BrickRed

    // Cute ear fins with black outlines
    drawOval(color = finColor, topLeft = Offset(cx - w * 0.42f, cy - h * 0.12f), size = Size(w * 0.16f, h * 0.10f))
    drawOval(color = HydroColors.InkBlack, topLeft = Offset(cx - w * 0.42f, cy - h * 0.12f), size = Size(w * 0.16f, h * 0.10f), style = Stroke(outlineStroke * 0.8f))
    drawOval(color = finColor, topLeft = Offset(cx + w * 0.26f, cy - h * 0.12f), size = Size(w * 0.16f, h * 0.10f))
    drawOval(color = HydroColors.InkBlack, topLeft = Offset(cx + w * 0.26f, cy - h * 0.12f), size = Size(w * 0.16f, h * 0.10f), style = Stroke(outlineStroke * 0.8f))

    // Round squishy blob body in Blush Peach
    drawRoundRect(
        color = HydroColors.BlushPeach,
        topLeft = Offset(cx - w * 0.32f, cy - h * 0.26f),
        size = Size(w * 0.64f, h * 0.56f),
        cornerRadius = CornerRadius(w * 0.30f, w * 0.30f)
    )
    drawRoundRect(
        color = HydroColors.InkBlack,
        topLeft = Offset(cx - w * 0.32f, cy - h * 0.26f),
        size = Size(w * 0.64f, h * 0.56f),
        cornerRadius = CornerRadius(w * 0.30f, w * 0.30f),
        style = Stroke(width = outlineStroke)
    )

    // Belly patch
    drawOval(
        color = HydroColors.ElectricYellow.copy(alpha = 0.7f),
        topLeft = Offset(cx - w * 0.18f, cy + h * 0.04f),
        size = Size(w * 0.36f, h * 0.20f)
    )

    if (mood == CharacterMood.CELEBRATING) {
        drawCuteCrown(cx, cy - h * 0.31f, w * 0.17f, outlineStroke)
    }

    drawKawaiiFace(
        cx = cx,
        eyeY = cy - h * 0.06f,
        eyeSpacing = w * 0.12f,
        eyeRadius = w * 0.055f,
        mouthY = cy + h * 0.05f,
        mood = mood,
        isBlinking = isBlinking,
        isWinking = isWinking,
        outlineStroke = outlineStroke * 0.75f
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
    val outlineStroke = (w * 0.028f).coerceAtLeast(4f)

    // Little dangling feet
    drawRoundRect(
        color = HydroColors.InkBlack,
        topLeft = Offset(cx - w * 0.14f, cy + h * 0.19f),
        size = Size(w * 0.09f, h * 0.11f),
        cornerRadius = CornerRadius(w * 0.045f, w * 0.045f)
    )
    drawRoundRect(
        color = HydroColors.InkBlack,
        topLeft = Offset(cx + w * 0.05f, cy + h * 0.19f),
        size = Size(w * 0.09f, h * 0.11f),
        cornerRadius = CornerRadius(w * 0.045f, w * 0.045f)
    )

    // Fluffy cloud puffs in Warm Amber / Cream
    drawCircle(color = HydroColors.InkBlack, radius = w * 0.19f + outlineStroke * 0.6f, center = Offset(cx - w * 0.18f, cy + h * 0.02f))
    drawCircle(color = HydroColors.InkBlack, radius = w * 0.19f + outlineStroke * 0.6f, center = Offset(cx + w * 0.18f, cy + h * 0.02f))
    drawCircle(color = HydroColors.InkBlack, radius = w * 0.24f + outlineStroke * 0.6f, center = Offset(cx, cy - h * 0.08f))

    drawCircle(color = HydroColors.CreamWhite, radius = w * 0.19f, center = Offset(cx - w * 0.18f, cy + h * 0.02f))
    drawCircle(color = HydroColors.CreamWhite, radius = w * 0.19f, center = Offset(cx + w * 0.18f, cy + h * 0.02f))
    drawCircle(color = HydroColors.CreamWhite, radius = w * 0.24f, center = Offset(cx, cy - h * 0.08f))
    drawRoundRect(
        color = HydroColors.CreamWhite,
        topLeft = Offset(cx - w * 0.30f, cy - h * 0.04f),
        size = Size(w * 0.60f, h * 0.26f),
        cornerRadius = CornerRadius(w * 0.14f, w * 0.14f)
    )

    if (mood == CharacterMood.CELEBRATING) {
        drawCuteCrown(cx, cy - h * 0.35f, w * 0.17f, outlineStroke)
    }

    drawKawaiiFace(
        cx = cx,
        eyeY = cy + h * 0.01f,
        eyeSpacing = w * 0.11f,
        eyeRadius = w * 0.05f,
        mouthY = cy + h * 0.11f,
        mood = mood,
        isBlinking = isBlinking,
        isWinking = isWinking,
        outlineStroke = outlineStroke * 0.75f
    )
}

private fun DrawScope.drawCuteCrown(cx: Float, baseY: Float, width: Float, stroke: Float) {
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
    drawPath(path = crownPath, color = HydroColors.WarmAmber)
    drawPath(path = crownPath, color = HydroColors.InkBlack, style = Stroke(width = stroke * 0.7f))
}

private fun DrawScope.drawKawaiiFace(
    cx: Float,
    eyeY: Float,
    eyeSpacing: Float,
    eyeRadius: Float,
    mouthY: Float,
    mood: CharacterMood,
    isBlinking: Boolean,
    isWinking: Boolean,
    outlineStroke: Float
) {
    val ink = HydroColors.InkBlack

    // Big oval expressive cartoon eyes (inspired by reference icons)
    if (isBlinking || isWinking) {
        drawLine(
            color = ink,
            start = Offset(cx - eyeSpacing - eyeRadius, eyeY),
            end = Offset(cx - eyeSpacing + eyeRadius, eyeY),
            strokeWidth = eyeRadius * 0.6f,
            cap = StrokeCap.Round
        )
    } else {
        drawOval(
            color = Color.White,
            topLeft = Offset(cx - eyeSpacing - eyeRadius * 0.85f, eyeY - eyeRadius * 1.1f),
            size = Size(eyeRadius * 1.7f, eyeRadius * 2.2f)
        )
        drawOval(
            color = ink,
            topLeft = Offset(cx - eyeSpacing - eyeRadius * 0.85f, eyeY - eyeRadius * 1.1f),
            size = Size(eyeRadius * 1.7f, eyeRadius * 2.2f),
            style = Stroke(width = outlineStroke)
        )
        drawCircle(color = ink, radius = eyeRadius * 0.45f, center = Offset(cx - eyeSpacing + eyeRadius * 0.15f, eyeY))
    }

    if (isBlinking) {
        drawLine(
            color = ink,
            start = Offset(cx + eyeSpacing - eyeRadius, eyeY),
            end = Offset(cx + eyeSpacing + eyeRadius, eyeY),
            strokeWidth = eyeRadius * 0.6f,
            cap = StrokeCap.Round
        )
    } else {
        drawOval(
            color = Color.White,
            topLeft = Offset(cx + eyeSpacing - eyeRadius * 0.85f, eyeY - eyeRadius * 1.1f),
            size = Size(eyeRadius * 1.7f, eyeRadius * 2.2f)
        )
        drawOval(
            color = ink,
            topLeft = Offset(cx + eyeSpacing - eyeRadius * 0.85f, eyeY - eyeRadius * 1.1f),
            size = Size(eyeRadius * 1.7f, eyeRadius * 2.2f),
            style = Stroke(width = outlineStroke)
        )
        drawCircle(color = ink, radius = eyeRadius * 0.45f, center = Offset(cx + eyeSpacing + eyeRadius * 0.15f, eyeY))
    }

    // Rosy Cheeks
    val cheekAlpha = if (mood == CharacterMood.THIRSTY) 0.4f else 0.85f
    drawOval(
        color = HydroColors.BrickRed.copy(alpha = cheekAlpha),
        topLeft = Offset(cx - eyeSpacing - eyeRadius * 1.3f, eyeY + eyeRadius * 0.9f),
        size = Size(eyeRadius * 1.6f, eyeRadius * 0.9f)
    )
    drawOval(
        color = HydroColors.BrickRed.copy(alpha = cheekAlpha),
        topLeft = Offset(cx + eyeSpacing - eyeRadius * 0.3f, eyeY + eyeRadius * 0.9f),
        size = Size(eyeRadius * 1.6f, eyeRadius * 0.9f)
    )

    val mouthPath = Path()
    when (mood) {
        CharacterMood.THIRSTY -> {
            mouthPath.moveTo(cx - eyeRadius * 0.9f, mouthY + eyeRadius * 0.25f)
            mouthPath.quadraticTo(cx, mouthY - eyeRadius * 0.35f, cx + eyeRadius * 0.9f, mouthY + eyeRadius * 0.25f)
            drawPath(path = mouthPath, color = ink, style = Stroke(width = eyeRadius * 0.45f, cap = StrokeCap.Round))
        }
        CharacterMood.HAPPY, CharacterMood.ENERGIZED -> {
            mouthPath.moveTo(cx - eyeRadius * 1.0f, mouthY - eyeRadius * 0.1f)
            mouthPath.quadraticTo(cx, mouthY + eyeRadius * 0.9f, cx + eyeRadius * 1.0f, mouthY - eyeRadius * 0.1f)
            drawPath(path = mouthPath, color = ink, style = Stroke(width = eyeRadius * 0.48f, cap = StrokeCap.Round))
        }
        CharacterMood.CELEBRATING -> {
            mouthPath.moveTo(cx - eyeRadius * 1.2f, mouthY - eyeRadius * 0.15f)
            mouthPath.quadraticTo(cx, mouthY + eyeRadius * 1.4f, cx + eyeRadius * 1.2f, mouthY - eyeRadius * 0.15f)
            mouthPath.close()
            drawPath(path = mouthPath, color = HydroColors.BrickRed)
            drawPath(path = mouthPath, color = ink, style = Stroke(width = eyeRadius * 0.38f, cap = StrokeCap.Round))
        }
    }
}
