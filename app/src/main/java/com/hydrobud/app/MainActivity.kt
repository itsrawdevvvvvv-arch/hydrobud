package com.hydrobud.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hydrobud.app.ui.MainViewModel
import com.hydrobud.app.ui.components.FloatingPillNavBar
import com.hydrobud.app.ui.components.NavTabItem
import com.hydrobud.app.ui.screens.HomeScreen
import com.hydrobud.app.ui.screens.OnboardingScreen
import com.hydrobud.app.ui.screens.ProfileScreen
import com.hydrobud.app.ui.screens.RemindersScreen
import com.hydrobud.app.ui.screens.SplashLoadingScreen
import com.hydrobud.app.ui.screens.StatsScreen
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
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSplash by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = remember {
        listOf(
            NavTabItem("home", "Drink", Icons.Rounded.WaterDrop),
            NavTabItem("stats", "Stats", Icons.Rounded.BarChart),
            NavTabItem("reminders", "Alerts", Icons.Rounded.Notifications),
            NavTabItem("profile", "Profile", Icons.Rounded.Person)
        )
    }

    AnimatedContent(
        targetState = when {
            showSplash || !state.isLoaded -> "splash"
            !state.profile.hasCompletedOnboarding -> "onboarding"
            else -> "main"
        },
        transitionSpec = {
            (fadeIn() + scaleIn(initialScale = 0.96f)) togetherWith fadeOut()
        },
        label = "rootTransition"
    ) { screen ->
        when (screen) {
            "splash" -> {
                SplashLoadingScreen(
                    characterType = state.profile.characterType,
                    onFinished = { showSplash = false }
                )
            }
            "onboarding" -> {
                OnboardingScreen(
                    onComplete = { name, weight, activity, goal, character ->
                        viewModel.completeOnboarding(name, weight, activity, goal, character)
                    }
                )
            }
            else -> {
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
                                1 -> StatsScreen(
                                    state = state,
                                    contentPadding = innerPadding
                                )
                                2 -> RemindersScreen(
                                    profile = state.profile,
                                    onUpdateProfile = { viewModel.updateProfile(it) },
                                    onSendTestNotification = { viewModel.sendTestNotification() },
                                    contentPadding = innerPadding
                                )
                                else -> ProfileScreen(
                                    profile = state.profile,
                                    onUpdateProfile = { viewModel.updateProfile(it) },
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
}
