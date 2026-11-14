package com.hydrobud.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hydrobud.app.data.ActivityLevel
import com.hydrobud.app.data.HydroCharacterType
import com.hydrobud.app.data.HydrationRepository
import com.hydrobud.app.data.HydrationUiState
import com.hydrobud.app.data.UserProfile
import com.hydrobud.app.notifications.HydrationNotificationHelper
import com.hydrobud.app.notifications.HydrationReminderWorker
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

    init {
        viewModelScope.launch {
            repo.ensureDateRolledOver()
            val snapshot = repo.getSnapshot()
            HydrationReminderWorker.schedule(
                context = getApplication(),
                enabled = snapshot.profile.remindersEnabled,
                intervalMinutes = snapshot.profile.reminderIntervalMinutes
            )
            HydroWidgetProvider.triggerRefresh(getApplication())
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
            HydroWidgetProvider.triggerRefresh(getApplication())
        }
    }

    fun completeOnboarding(
        name: String,
        weightKg: Int,
        activityLevel: ActivityLevel,
        dailyGoalMl: Int,
        characterType: HydroCharacterType
    ) {
        val current = uiState.value.profile
        updateProfile(
            current.copy(
                name = name.ifBlank { "Buddy" },
                weightKg = weightKg,
                activityLevel = activityLevel,
                dailyGoalMl = dailyGoalMl,
                characterType = characterType,
                hasCompletedOnboarding = true
            )
        )
    }

    fun sendTestNotification() {
        val state = uiState.value
        HydrationNotificationHelper.showHydrationNotification(
            context = getApplication(),
            userName = state.profile.name,
            characterName = state.profile.characterType.displayName,
            todayTotalMl = state.todayTotalMl,
            goalMl = state.profile.dailyGoalMl
        )
    }
}
