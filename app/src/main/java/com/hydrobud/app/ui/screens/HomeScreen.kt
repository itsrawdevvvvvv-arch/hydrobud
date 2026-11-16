package com.hydrobud.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hydrobud.app.R
import com.hydrobud.app.data.HydrationUiState
import com.hydrobud.app.data.WaterLogEntry
import com.hydrobud.app.ui.components.AnimatedLiquidTankWithCharacter
import com.hydrobud.app.ui.components.BouncyPillButton
import com.hydrobud.app.ui.theme.HydroColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    state: HydrationUiState,
    onAddWater: (Int, String) -> Unit,
    onUndo: () -> Unit,
    onDeleteLog: (String) -> Unit,
    contentPadding: PaddingValues
) {
    var showCustomDialog by remember { mutableStateOf(false) }

    val animatedTotalMl by animateIntAsState(
        targetValue = state.todayTotalMl,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessLow
        ),
        label = "totalMlCounter"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 14.dp,
            bottom = contentPadding.calculateBottomPadding() + 92.dp,
            start = 18.dp,
            end = 18.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "HOW MUCH WATER\nTODAY?",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "$animatedTotalMl / ${state.profile.dailyGoalMl} ml  •  ${state.percentInt}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = HydroColors.InkMuted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(275.dp),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                AnimatedLiquidTankWithCharacter(
                    progress = state.clampedProgress,
                    characterType = state.profile.characterType,
                    mood = state.mood,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BouncyPillButton(
                        onClick = { onAddWater(state.profile.quickCup1Ml, "Cup") },
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Text("+${state.profile.quickCup1Ml} ml", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    BouncyPillButton(
                        onClick = { onAddWater(state.profile.quickCup2Ml, "Glass") },
                        modifier = Modifier.weight(1f),
                        containerColor = HydroColors.CoralOrange,
                        contentColor = Color.White
                    ) {
                        Text("+${state.profile.quickCup2Ml} ml", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    BouncyPillButton(
                        onClick = { onAddWater(state.profile.quickCup3Ml, "Bottle") },
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Text("+${state.profile.quickCup3Ml} ml", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BouncyPillButton(
                        onClick = { showCustomDialog = true },
                        modifier = Modifier.weight(1.4f),
                        containerColor = HydroColors.CyanDome,
                        contentColor = HydroColors.InkBlack
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Custom Sip", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    BouncyPillButton(
                        onClick = onUndo,
                        modifier = Modifier.weight(1f),
                        enabled = state.todayLogs.isNotEmpty(),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_round_undo),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Undo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        if (state.todayLogs.isNotEmpty()) {
            item {
                Text(
                    text = "Today's Sips",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(state.todayLogs, key = { it.id }) { entry ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { it / 3 }
                ) {
                    WaterLogPillRow(entry = entry, onDelete = { onDeleteLog(entry.id) })
                }
            }
        }
    }

    if (showCustomDialog) {
        CustomWaterDialog(
            onDismiss = { showCustomDialog = false },
            onConfirm = { amount ->
                onAddWater(amount, "Custom")
                showCustomDialog = false
            }
        )
    }
}

@Composable
private fun WaterLogPillRow(
    entry: WaterLogEntry,
    onDelete: () -> Unit
) {
    val timeText = remember(entry.timestampMillis) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(entry.timestampMillis))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = HydroColors.CyanDome.copy(alpha = 0.16f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_round_water_drop),
                            contentDescription = null,
                            tint = HydroColors.CyanDeep,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "+${entry.amountMl} ml",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${entry.label} • $timeText",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.ic_round_delete_outline),
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CustomWaterDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var sliderVal by remember { mutableFloatStateOf(300f) }
    val amount = ((sliderVal.toInt() + 25) / 50) * 50

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Custom Sip", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$amount ml",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = HydroColors.CoralOrange
                )
                Spacer(modifier = Modifier.height(10.dp))
                Slider(
                    value = sliderVal,
                    onValueChange = { sliderVal = it },
                    valueRange = 50f..1000f,
                    colors = SliderDefaults.colors(
                        thumbColor = HydroColors.CoralOrange,
                        activeTrackColor = HydroColors.CyanDome
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(100, 350, 750).forEach { preset ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .clickable { sliderVal = preset.toFloat() },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text("${preset}ml", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            BouncyPillButton(onClick = { onConfirm(amount) }) {
                Text("Add", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            BouncyPillButton(
                onClick = onDismiss,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
        }
    )
}
