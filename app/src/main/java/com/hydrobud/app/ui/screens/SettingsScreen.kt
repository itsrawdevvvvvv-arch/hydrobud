package com.hydrobud.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.hydrobud.app.HydroBudApp
import com.hydrobud.app.R
import com.hydrobud.app.data.HydroCharacterType
import com.hydrobud.app.data.UserProfile
import com.hydrobud.app.ui.components.BouncyPillButton
import com.hydrobud.app.ui.theme.HydroColors
import java.io.File

@Composable
fun SettingsScreen(
    profile: UserProfile,
    onUpdateProfile: (UserProfile) -> Unit,
    onSendTestNotification: () -> Unit,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    var nameInput by remember(profile.name) { mutableStateOf(profile.name) }
    var goalSlider by remember(profile.dailyGoalMl) { mutableFloatStateOf(profile.dailyGoalMl.toFloat()) }

    val crashFile = remember { File(context.filesDir, HydroBudApp.CRASH_LOG_FILE) }
    var hasCrashLog by remember { mutableStateOf(crashFile.exists()) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    val intervals = listOf(30 to "30m", 60 to "1h", 90 to "1.5h", 120 to "2h")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(
                top = contentPadding.calculateTopPadding() + 14.dp,
                bottom = contentPadding.calculateBottomPadding() + 92.dp,
                start = 18.dp,
                end = 18.dp
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "PROFILE &\nSETTINGS",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Your Name", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        singleLine = true,
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HydroColors.CoralOrange,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    BouncyPillButton(
                        onClick = {
                            onUpdateProfile(profile.copy(name = nameInput.ifBlank { "Buddy" }))
                        }
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = "Save", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Daily Water Limit", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = "${((goalSlider.toInt() + 50) / 100) * 100} ml",
                        fontWeight = FontWeight.Black,
                        color = HydroColors.CoralOrange
                    )
                }
                Slider(
                    value = goalSlider,
                    onValueChange = { goalSlider = it },
                    onValueChangeFinished = {
                        val snapped = ((goalSlider.toInt() + 50) / 100) * 100
                        onUpdateProfile(profile.copy(dailyGoalMl = snapped))
                    },
                    valueRange = 1000f..5000f,
                    colors = SliderDefaults.colors(
                        thumbColor = HydroColors.CoralOrange,
                        activeTrackColor = HydroColors.CyanDome
                    )
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Buddy Style", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HydroCharacterType.entries.forEach { charType ->
                        val active = charType == profile.characterType
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .clickable {
                                    onUpdateProfile(profile.copy(characterType = charType))
                                },
                            shape = CircleShape,
                            color = if (active) HydroColors.CoralOrange else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            ) {
                                Text(
                                    text = charType.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Water Reminders", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = "Periodic alerts during active hours",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = profile.remindersEnabled,
                        onCheckedChange = { onUpdateProfile(profile.copy(remindersEnabled = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = HydroColors.CoralOrange
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    intervals.forEach { (mins, label) ->
                        val active = profile.reminderIntervalMinutes == mins
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .clickable {
                                    onUpdateProfile(profile.copy(reminderIntervalMinutes = mins))
                                },
                            shape = CircleShape,
                            color = if (active) HydroColors.CyanDome else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 9.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HydroColors.InkBlack
                                )
                            }
                        }
                    }
                }
            }
        }

        BouncyPillButton(
            onClick = {
                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onSendTestNotification()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_round_notifications_active),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Send Test Notification", fontWeight = FontWeight.Bold)
        }

        // Crash Log Diagnostics Row (R6)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Diagnostics", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = if (hasCrashLog) "Crash log available" else "No crashes recorded",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (hasCrashLog) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BouncyPillButton(
                            onClick = {
                                val text = runCatching { crashFile.readText() }.getOrDefault("")
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "HydroBud Crash Log")
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Crash Log"))
                            },
                            containerColor = HydroColors.CyanDome,
                            contentColor = HydroColors.InkBlack
                        ) {
                            Text("Share Log", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        BouncyPillButton(
                            onClick = {
                                crashFile.delete()
                                hasCrashLog = false
                            },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Text("Clear", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
