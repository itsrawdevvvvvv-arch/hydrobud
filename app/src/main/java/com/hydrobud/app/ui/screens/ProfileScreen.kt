package com.hydrobud.app.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hydrobud.app.data.ActivityLevel
import com.hydrobud.app.data.CharacterMood
import com.hydrobud.app.data.HydroCharacterType
import com.hydrobud.app.data.UserProfile
import com.hydrobud.app.ui.components.BouncyPillButton
import com.hydrobud.app.ui.components.HydroCharacterAvatar
import com.hydrobud.app.ui.theme.HydroColors

@Composable
fun ProfileScreen(
    profile: UserProfile,
    onUpdateProfile: (UserProfile) -> Unit,
    contentPadding: PaddingValues
) {
    var nameInput by remember(profile.name) { mutableStateOf(profile.name) }
    var savedBanner by remember { mutableStateOf(false) }

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
            text = "Your Profile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )

        // Character Selector Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 5.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Choose Your Character",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HydroCharacterType.entries.forEach { charType ->
                        val selected = charType == profile.characterType
                        val scale by animateFloatAsState(
                            targetValue = if (selected) 1.05f else 0.94f,
                            animationSpec = spring(Spring.DampingRatioMediumBouncy),
                            label = "profileCharScale"
                        )
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clip(RoundedCornerShape(26.dp))
                                .clickable {
                                    onUpdateProfile(profile.copy(characterType = charType))
                                },
                            shape = RoundedCornerShape(26.dp),
                            color = if (selected) HydroColors.AquaPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            shadowElevation = if (selected) 6.dp else 0.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(modifier = Modifier.size(58.dp)) {
                                    HydroCharacterAvatar(
                                        characterType = charType,
                                        mood = if (selected) CharacterMood.CELEBRATING else CharacterMood.HAPPY,
                                        isBlinking = false,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = charType.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Personal Details Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Display Name",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = {
                            nameInput = it
                            savedBanner = false
                        },
                        singleLine = true,
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HydroColors.AquaPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    BouncyPillButton(
                        onClick = {
                            onUpdateProfile(profile.copy(name = nameInput.ifBlank { "Buddy" }))
                            savedBanner = true
                        }
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = "Save", modifier = Modifier.size(18.dp))
                    }
                }
                if (savedBanner) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Name updated!",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = HydroColors.MintGreen
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Body Weight",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (profile.weightKg > 35) {
                                            onUpdateProfile(profile.copy(weightKg = profile.weightKg - 1))
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                text = "${profile.weightKg} kg",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                            Surface(
                                shape = CircleShape,
                                color = HydroColors.AquaPrimary,
                                contentColor = Color.White,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (profile.weightKg < 180) {
                                            onUpdateProfile(profile.copy(weightKg = profile.weightKg + 1))
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Activity Level",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActivityLevel.entries.forEach { level ->
                        val active = level == profile.activityLevel
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .clickable {
                                    onUpdateProfile(profile.copy(activityLevel = level))
                                },
                            shape = CircleShape,
                            color = if (active) HydroColors.AquaPrimary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            ) {
                                Text(
                                    text = level.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Smart Goal Apply Pill
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    color = HydroColors.SkySoft
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Smart Goal",
                                fontSize = 11.sp,
                                color = HydroColors.OceanDeep,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${profile.recommendedGoalMl()} ml / day",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = HydroColors.InkDark
                            )
                        }
                        BouncyPillButton(
                            onClick = {
                                onUpdateProfile(profile.copy(dailyGoalMl = profile.recommendedGoalMl()))
                            },
                            containerColor = HydroColors.OceanDeep
                        ) {
                            Text("Apply", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Quick Cup Presets Pill Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Quick Sip Cup Sizes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CupStepperPill(
                        label = "Cup 1",
                        ml = profile.quickCup1Ml,
                        onChange = { onUpdateProfile(profile.copy(quickCup1Ml = it)) },
                        modifier = Modifier.weight(1f)
                    )
                    CupStepperPill(
                        label = "Cup 2",
                        ml = profile.quickCup2Ml,
                        onChange = { onUpdateProfile(profile.copy(quickCup2Ml = it)) },
                        modifier = Modifier.weight(1f)
                    )
                    CupStepperPill(
                        label = "Cup 3",
                        ml = profile.quickCup3Ml,
                        onChange = { onUpdateProfile(profile.copy(quickCup3Ml = it)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CupStepperPill(
    label: String,
    ml: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text("${ml}ml", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { if (ml > 50) onChange(ml - 50) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = HydroColors.AquaPrimary,
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { if (ml < 1000) onChange(ml + 50) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
