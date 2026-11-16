package com.hydrobud.app.ui

import android.app.Application
import com.hydrobud.app.CrashGuard
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hydrobud.app.data.HydrationRepository
import com.hydrobud.app.data.HydrationUiState
import com.hydrobud.app.data.UserProfile
import com.hydrobud.app.notifications.HydrationNotificationHelper
import com.hydrobud.app.notifications.HydrationReminderWorker
import com.hydrobud.app.steps.StepCounterService
import com.hydrobud.app.widget.HydroWidgetProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = HydrationRepository.getInstance(application)

    val uiState: StateFlow<HydrationUiState> = repo.uiStateFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HydrationUiState()
    )

    val sensorStatus: StateFlow<String> = StepCounterService.sensorStatus
    val serviceError: StateFlow<String?> = StepCounterService.startError

    init {
        viewModelScope.launch {
            // If the previous cold start died before the first frame, skip every
            // background side effect this time so the UI always comes up.
            if (CrashGuard.isSafeMode(getApplication())) {
                android.util.Log.w("HydroBud", "SAFE MODE: skipping background startup work")
                return@launch
            }
            val snapshot = runCatching {
                repo.ensureDateRolledOver()
                repo.getSnapshot()
            }.onFailure {
                android.util.Log.e("HydroBud", "snapshot failed at startup", it)
            }.getOrNull() ?: return@launch

            runCatching {
                HydrationReminderWorker.schedule(
                    context = getApplication(),
                    enabled = snapshot.profile.remindersEnabled,
                    intervalMinutes = snapshot.profile.reminderIntervalMinutes
                )
            }.onFailure { android.util.Log.e("HydroBud", "reminder scheduling failed", it) }

            runCatching {
                if (snapshot.profile.walkTrackingEnabled &&
                    StepCounterService.hasActivityPermission(getApplication())
                ) {
                    StepCounterService.start(getApplication())
                }
            }.onFailure { android.util.Log.e("HydroBud", "step service start failed", it) }

            runCatching {
                HydroWidgetProvider.triggerRefresh(getApplication())
            }.onFailure { android.util.Log.e("HydroBud", "widget refresh failed", it) }
        }
    }

    fun addWater(amountMl: Int, label: String = "Water") {
        viewModelScope.launch {
            repo.addWater(amountMl, label)
            HydroWidgetProvider.triggerRefresh(getApplication())
        }
    }

    fun undoLastLog() {
        viewModelScope.launch {
            repo.undoLastLog()
            HydroWidgetProvider.triggerRefresh(getApplication())
        }
    }

    fun removeLog(logId: String) {
        viewModelScope.launch {
            repo.removeLog(logId)
            HydroWidgetProvider.triggerRefresh(getApplication())
        }
    }

    fun updateProfile(updated: UserProfile) {
        viewModelScope.launch {
            repo.updateProfile(updated)
            HydrationReminderWorker.schedule(
                context = getApplication(),
                enabled = updated.remindersEnabled,
                intervalMinutes = updated.reminderIntervalMinutes
            )
            if (updated.walkTrackingEnabled) {
                if (StepCounterService.hasActivityPermission(getApplication())) {
                    StepCounterService.start(getApplication())
                }
            } else {
                StepCounterService.stop(getApplication())
            }
            HydroWidgetProvider.triggerRefresh(getApplication())
        }
    }

    fun reregisterStepSensor() {
        if (uiState.value.profile.walkTrackingEnabled &&
            StepCounterService.hasActivityPermission(getApplication())
        ) {
            StepCounterService.reregister(getApplication())
        }
    }

    fun sendTestNotification() {
        val state = uiState.value
        HydrationNotificationHelper.showHydrationNotification(
            context = getApplication(),
            userName = state.profile.name,
            characterName = state.profile.characterType.displayName,
            todayTotalMl = state.todayTotalMl,
            goalMl = state.profile.dailyGoalMl,
            todaySteps = state.todaySteps,
            stepGoal = state.profile.dailyStepGoal
        )
    }
}
