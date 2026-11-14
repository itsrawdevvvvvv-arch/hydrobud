package com.hydrobud.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.WaterDrop
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hydrobud.app.data.CharacterMood
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
    var characterQuoteIndex by remember { mutableStateOf(0) }

    val animatedTotalMl by animateIntAsState(
        targetValue = state.todayTotalMl,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessLow
        ),
        label = "totalMlCounter"
    )

    val moodMessage = remember(state.mood, characterQuoteIndex, state.profile.name) {
        val name = state.profile.name
        when (state.mood) {
            CharacterMood.THIRSTY -> listOf(
                "Hey $name, I'm feeling a bit dry!",
                "Tap a pill below for a refreshing sip!"
            )[characterQuoteIndex % 2]
            CharacterMood.HAPPY -> listOf(
                "Nice flow, $name! Keep it up!",
                "Every sip powers up your day!"
            )[characterQuoteIndex % 2]
            CharacterMood.ENERGIZED -> listOf(
                "Woohoo! Almost at your daily limit!",
                "You're glowing today, $name!"
            )[characterQuoteIndex % 2]
            CharacterMood.CELEBRATING -> listOf(
                "100% Goal Crushed! Hydration Hero!",
                "Party mode! You did amazing, $name!"
            )[characterQuoteIndex % 2]
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 92.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header Pills
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WaterDrop,
                            contentDescription = null,
                            tint = HydroColors.AquaPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hi, ${state.profile.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = HydroColors.CoralAccent.copy(alpha = 0.16f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = HydroColors.CoralAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${state.streakDays}d",
                                fontWeight = FontWeight.ExtraBold,
                                color = HydroColors.CoralAccent,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${state.percentInt}%",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Main Animated Liquid Tank + Interactive Character Pill Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp),
                shape = RoundedCornerShape(42.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedLiquidTankWithCharacter(
                        progress = state.clampedProgress,
                        characterType = state.profile.characterType,
                        mood = state.mood,
                        modifier = Modifier.fillMaxSize(),
                        onCharacterTap = { characterQuoteIndex++ }
                    )

                    // Top floating speech bubble pill
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 14.dp, start = 16.dp, end = 16.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                        shadowElevation = 4.dp
                    ) {
                        AnimatedContent(
                            targetState = moodMessage,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "moodBubble"
                        ) { msg ->
                            Text(
                                text = msg,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                            )
                        }
                    }

                    // Bottom floating ml counter pill
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 14.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "$animatedTotalMl",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = HydroColors.OceanDeep
                            )
                            Text(
                                text = " / ${state.profile.dailyGoalMl} ml",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // Quick Add Pill Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BouncyPillButton(
                        onClick = { onAddWater(state.profile.quickCup1Ml, "Small Cup") },
                        modifier = Modifier.weight(1f),
                        containerColor = HydroColors.MintSplash
                    ) {
                        Icon(Icons.Rounded.LocalDrink, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+${state.profile.quickCup1Ml}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    BouncyPillButton(
                        onClick = { onAddWater(state.profile.quickCup2Ml, "Glass") },
                        modifier = Modifier.weight(1f),
                        containerColor = HydroColors.AquaPrimary
                    ) {
                        Icon(Icons.Rounded.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+${state.profile.quickCup2Ml}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    BouncyPillButton(
                        onClick = { onAddWater(state.profile.quickCup3Ml, "Bottle") },
                        modifier = Modifier.weight(1f),
                        containerColor = HydroColors.OceanDeep
                    ) {
                        Icon(Icons.Rounded.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+${state.profile.quickCup3Ml}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BouncyPillButton(
                        onClick = { showCustomDialog = true },
                        modifier = Modifier.weight(1.4f),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = HydroColors.AquaPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Custom Sip", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    BouncyPillButton(
                        onClick = onUndo,
                        modifier = Modifier.weight(1f),
                        enabled = state.todayLogs.isNotEmpty(),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Icon(Icons.Rounded.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Undo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // Today's Logs Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Sips",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (state.remainingMl > 0) "${state.remainingMl} ml left" else "Goal Complete!",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.remainingMl > 0) MaterialTheme.colorScheme.onSurfaceVariant else HydroColors.MintGreen,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }

        if (state.todayLogs.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "No sips logged yet today. Tap a pill button above to hydrate ${state.profile.characterType.displayName}!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
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
                onAddWater(amount, "Custom Sip")
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
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = HydroColors.SkySoft,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.WaterDrop,
                            contentDescription = null,
                            tint = HydroColors.OceanDeep,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "+${entry.amountMl} ml",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
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
                    imageVector = Icons.Rounded.DeleteOutline,
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
        shape = RoundedCornerShape(32.dp),
        title = {
            Text("Custom Water Sip", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = HydroColors.SkySoft
                ) {
                    Text(
                        text = "$amount ml",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HydroColors.OceanDeep,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Slider(
                    value = sliderVal,
                    onValueChange = { sliderVal = it },
                    valueRange = 50f..1000f,
                    colors = SliderDefaults.colors(
                        thumbColor = HydroColors.OceanDeep,
                        activeTrackColor = HydroColors.AquaPrimary
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
                Text("Add Sip", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            BouncyPillButton(
                onClick = onDismiss,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
        }
    )
}
