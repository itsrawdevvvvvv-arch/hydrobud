package com.hydrobud.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hydrobud.app.ui.MainViewModel
import com.hydrobud.app.ui.components.FloatingPillNavBar
import com.hydrobud.app.ui.components.NavTabItem
import com.hydrobud.app.ui.screens.HomeScreen
import com.hydrobud.app.ui.screens.SettingsScreen
import com.hydrobud.app.ui.screens.SplashLoadingScreen
import com.hydrobud.app.ui.screens.WalkScreen
import com.hydrobud.app.ui.theme.HydroBudTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HydroBudTheme {
                HydroBudRoot()
            }
        }
    }
}

@Composable
fun HydroBudRoot(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sensorStatus by viewModel.sensorStatus.collectAsStateWithLifecycle()
    var showSplash by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val activityGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            results[Manifest.permission.ACTIVITY_RECOGNITION] == true
        } else {
            true
        }
        if (activityGranted) {
            viewModel.reregisterStepSensor()
        }
    }

    LaunchedEffect(showSplash) {
        if (!showSplash) {
            val needed = buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    add(Manifest.permission.ACTIVITY_RECOGNITION)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            if (needed.isNotEmpty()) {
                permissionLauncher.launch(needed.toTypedArray())
            }
        }
    }

    val tabs = remember {
        listOf(
            NavTabItem("water", "Water", Icons.Rounded.WaterDrop),
            NavTabItem("steps", "Steps", Icons.Rounded.DirectionsWalk),
            NavTabItem("settings", "Settings", Icons.Rounded.Settings)
        )
    }

    AnimatedContent(
        targetState = showSplash || !state.isLoaded,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "rootTransition"
    ) { isSplash ->
        if (isSplash) {
            SplashLoadingScreen(
                characterType = state.profile.characterType,
                onFinished = { showSplash = false }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "tabTransition"
                    ) { tabIndex ->
                        when (tabIndex) {
                            0 -> HomeScreen(
                                state = state,
                                onAddWater = { ml, label -> viewModel.addWater(ml, label) },
                                onUndo = { viewModel.undoLastLog() },
                                onDeleteLog = { id -> viewModel.removeLog(id) },
                                contentPadding = innerPadding
                            )
                            1 -> WalkScreen(
                                state = state,
                                sensorStatus = sensorStatus,
                                onUpdateProfile = { viewModel.updateProfile(it) },
                                onPermissionGranted = { viewModel.reregisterStepSensor() },
                                contentPadding = innerPadding
                            )
                            else -> SettingsScreen(
                                profile = state.profile,
                                onUpdateProfile = { viewModel.updateProfile(it) },
                                onSendTestNotification = { viewModel.sendTestNotification() },
                                contentPadding = innerPadding
                            )
                        }
                    }
                }

                FloatingPillNavBar(
                    tabs = tabs,
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                )
            }
        }
    }
}
