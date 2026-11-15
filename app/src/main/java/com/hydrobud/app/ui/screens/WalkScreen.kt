package com.hydrobud.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.hydrobud.app.data.HydrationUiState
import com.hydrobud.app.data.UserProfile
import com.hydrobud.app.ui.components.BouncyPillButton
import com.hydrobud.app.ui.components.PillProgressBar
import com.hydrobud.app.ui.theme.HydroColors
import java.text.NumberFormat
import java.util.Locale

@Composable
fun WalkScreen(
    state: HydrationUiState,
    onUpdateProfile: (UserProfile) -> Unit,
    onStartService: () -> Unit,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    var hasActivityPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val activityPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasActivityPermission = granted
        if (granted && state.profile.walkTrackingEnabled) {
            onStartService()
        }
    }

    val animatedSteps by animateIntAsState(
        targetValue = state.todaySteps,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessLow
        ),
        label = "stepsCounter"
    )

    var stepGoalSlider by remember(state.profile.dailyStepGoal) {
        mutableFloatStateOf(state.profile.dailyStepGoal.toFloat())
    }

    val formattedSteps = remember(animatedSteps) {
        NumberFormat.getNumberInstance(Locale.US).format(animatedSteps)
    }

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
        // Header Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Walk & Steps",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = HydroColors.ElectricYellow
            )
            Surface(
                shape = CircleShape,
                color = HydroColors.WarmAmber,
                border = BorderStroke(2.dp, HydroColors.InkBlack)
            ) {
                Text(
                    text = "${state.stepPercentInt}% Goal",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = HydroColors.InkBlack,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }

        if (!hasActivityPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                color = HydroColors.BlushPeach,
                border = BorderStroke(2.dp, HydroColors.InkBlack)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Allow Physical Activity so steps count even when the app is closed!",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = HydroColors.InkBlack,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BouncyPillButton(
                        onClick = {
                            activityPermLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        },
                        containerColor = HydroColors.ElectricYellow
                    ) {
                        Text("Allow", fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // Chunky Step Counter Hero Card (inspired by 262335.jpg + Let's Stream palette)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(36.dp),
            color = HydroColors.CreamWhite,
            border = BorderStroke(2.5.dp, HydroColors.InkBlack),
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formattedSteps,
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Black,
                        color = HydroColors.InkBlack,
                        letterSpacing = (-1.5).sp
                    )
                    Text(
                        text = "steps",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = HydroColors.InkMuted,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Distance & Calorie Pill Chips
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = HydroColors.SoftLavenderBg,
                        border = BorderStroke(1.5.dp, HydroColors.InkBlack)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Route, contentDescription = null, modifier = Modifier.size(16.dp), tint = HydroColors.InkBlack)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = String.format(Locale.US, "%.2f km", state.estimatedKm),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = HydroColors.InkBlack
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = HydroColors.BlushPeach,
                        border = BorderStroke(1.5.dp, HydroColors.InkBlack)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(16.dp), tint = HydroColors.BrickRed)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "~${state.estimatedKcal} kcal",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = HydroColors.InkBlack
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                PillProgressBar(
                    progress = state.stepProgress,
                    height = 20.dp,
                    fillColor = HydroColors.ElectricYellow
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Milestone Markers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("0", "5k", "8k", "10k", "15k").forEach { marker ->
                        Text(
                            text = marker,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HydroColors.InkMuted
                        )
                    }
                }
            }
        }

        // Walk Goal Setter Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = HydroColors.CreamWhite,
            border = BorderStroke(2.dp, HydroColors.InkBlack),
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Walk Goal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = HydroColors.InkBlack
                    )
                    Surface(
                        shape = CircleShape,
                        color = HydroColors.ElectricYellow,
                        border = BorderStroke(1.5.dp, HydroColors.InkBlack)
                    ) {
                        Text(
                            text = "${((stepGoalSlider.toInt() + 250) / 500) * 500} steps",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = HydroColors.InkBlack,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Slider(
                    value = stepGoalSlider,
                    onValueChange = { stepGoalSlider = it },
                    onValueChangeFinished = {
                        val snapped = ((stepGoalSlider.toInt() + 250) / 500) * 500
                        onUpdateProfile(state.profile.copy(dailyStepGoal = snapped))
                    },
                    valueRange = 2000f..20000f,
                    colors = SliderDefaults.colors(
                        thumbColor = HydroColors.InkBlack,
                        activeTrackColor = HydroColors.PeriwinklePurple
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5000, 8000, 10000, 15000).forEach { preset ->
                        val active = state.profile.dailyStepGoal == preset
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .clickable {
                                    stepGoalSlider = preset.toFloat()
                                    onUpdateProfile(state.profile.copy(dailyStepGoal = preset))
                                },
                            shape = CircleShape,
                            color = if (active) HydroColors.WarmAmber else HydroColors.SoftLavenderBg,
                            border = BorderStroke(1.5.dp, HydroColors.InkBlack)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 9.dp)
                            ) {
                                Text(
                                    text = "${preset / 1000}k",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = HydroColors.InkBlack
                                )
                            }
                        }
                    }
                }
            }
        }

        // Always-On Background Step Tracker Toggle Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = HydroColors.WarmAmber,
            border = BorderStroke(2.dp, HydroColors.InkBlack),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = HydroColors.CreamWhite,
                        border = BorderStroke(1.5.dp, HydroColors.InkBlack),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.DirectionsWalk, contentDescription = null, tint = HydroColors.InkBlack)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Always-On Step Counter",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = HydroColors.InkBlack
                        )
                        Text(
                            text = "Counts steps 24/7 even when app is closed",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = HydroColors.InkBlack.copy(alpha = 0.8f)
                        )
                    }
                }
                Switch(
                    checked = state.profile.walkTrackingEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateProfile(state.profile.copy(walkTrackingEnabled = enabled))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = HydroColors.ElectricYellow,
                        checkedTrackColor = HydroColors.InkBlack
                    )
                )
            }
        }
    }
}
