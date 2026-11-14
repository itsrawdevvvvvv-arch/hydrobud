package com.hydrobud.app.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hydrobud.app.data.HydrationUiState
import com.hydrobud.app.ui.theme.HydroColors
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatsScreen(
    state: HydrationUiState,
    contentPadding: PaddingValues
) {
    val weeklyTotalMl = state.weeklyHistory.sumOf { it.totalMl }
    val weeklyAvgMl = if (state.weeklyHistory.isNotEmpty()) weeklyTotalMl / state.weeklyHistory.size else 0
    val bestDayMl = state.weeklyHistory.maxOfOrNull { it.totalMl } ?: 0
    val goalsMetCount = state.weeklyHistory.count { it.goalMl > 0 && it.totalMl >= it.goalMl }
    val maxChartMl = maxOf(state.profile.dailyGoalMl, bestDayMl, 1000)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 92.dp,
                start = 16.dp,
                end = 16.dp
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Hydration Stats",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )

        // 7-Day Animated Pill Bar Chart
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(36.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Last 7 Days",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = CircleShape,
                        color = HydroColors.SkySoft
                    ) {
                        Text(
                            text = "Goal: ${state.profile.dailyGoalMl} ml",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = HydroColors.OceanDeep,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    state.weeklyHistory.forEach { entry ->
                        val ratio = (entry.totalMl.toFloat() / maxChartMl.toFloat()).coerceIn(0.06f, 1f)
                        val animatedRatio by animateFloatAsState(
                            targetValue = ratio,
                            animationSpec = spring(
                                dampingRatio = 0.7f,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "bar_${entry.dateKey}"
                        )
                        val dayLabel = remember(entry.dateKey) {
                            try {
                                LocalDate.parse(entry.dateKey).dayOfWeek
                                    .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            } catch (_: Exception) {
                                "Day"
                            }
                        }
                        val metGoal = entry.goalMl > 0 && entry.totalMl >= entry.goalMl

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = if (entry.totalMl >= 1000) {
                                    String.format(Locale.US, "%.1fL", entry.totalMl / 1000f)
                                } else {
                                    "${entry.totalMl}"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .weight(1f)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(animatedRatio)
                                        .clip(CircleShape)
                                        .background(
                                            if (metGoal) {
                                                Brush.verticalGradient(
                                                    listOf(HydroColors.MintGreen, HydroColors.AquaPrimary)
                                                )
                                            } else {
                                                Brush.verticalGradient(
                                                    listOf(HydroColors.MintSplash, HydroColors.OceanDeep)
                                                )
                                            }
                                        )
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = dayLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 2x2 Summary Pill Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatPillCard(
                title = "Daily Average",
                value = "$weeklyAvgMl ml",
                accent = HydroColors.AquaPrimary,
                modifier = Modifier.weight(1f)
            )
            StatPillCard(
                title = "Current Streak",
                value = "${state.streakDays} Days",
                accent = HydroColors.CoralAccent,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatPillCard(
                title = "7-Day Total",
                value = String.format(Locale.US, "%.1f L", weeklyTotalMl / 1000f),
                accent = HydroColors.OceanDeep,
                modifier = Modifier.weight(1f)
            )
            StatPillCard(
                title = "Goals Hit",
                value = "$goalsMetCount / 7",
                accent = HydroColors.MintGreen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatPillCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.16f)
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
