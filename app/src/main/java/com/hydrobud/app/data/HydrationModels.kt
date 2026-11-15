package com.hydrobud.app.data

enum class HydroCharacterType(
    val id: String,
    val displayName: String,
    val tagline: String
) {
    DROPPY("droppy", "Droppy", "Bouncy Aqua Droplet"),
    BUBBELS("bubbels", "Bubbels", "Cheerful Sea Spirit"),
    CLOUDY("cloudy", "Cloudy", "Fluffy Rain Buddy");

    companion object {
        fun fromId(id: String?): HydroCharacterType =
            entries.find { it.id == id } ?: DROPPY
    }
}

enum class ActivityLevel(
    val id: String,
    val label: String,
    val bonusMl: Int
) {
    LIGHT("light", "Light", 0),
    MODERATE("moderate", "Moderate", 350),
    ACTIVE("active", "Active", 650),
    ATHLETE("athlete", "Athlete", 1000);

    companion object {
        fun fromId(id: String?): ActivityLevel =
            entries.find { it.id == id } ?: MODERATE
    }
}

enum class CharacterMood {
    THIRSTY,    // 0% - 24%
    HAPPY,      // 25% - 69%
    ENERGIZED,  // 70% - 99%
    CELEBRATING // 100%+
}

data class WaterLogEntry(
    val id: String,
    val amountMl: Int,
    val timestampMillis: Long,
    val label: String = "Water"
)

data class DailyHistoryEntry(
    val dateKey: String, // yyyy-MM-dd
    val totalMl: Int,
    val goalMl: Int,
    val steps: Int = 0,
    val stepGoal: Int = 8000
)

data class UserProfile(
    val name: String = "Buddy",
    val weightKg: Int = 65,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val dailyGoalMl: Int = 2500,
    val dailyStepGoal: Int = 8000,
    val walkTrackingEnabled: Boolean = true,
    val wakeHour: Int = 7,
    val sleepHour: Int = 23,
    val characterType: HydroCharacterType = HydroCharacterType.DROPPY,
    val remindersEnabled: Boolean = true,
    val reminderIntervalMinutes: Int = 60,
    val quickCup1Ml: Int = 150,
    val quickCup2Ml: Int = 250,
    val quickCup3Ml: Int = 500,
    val hasCompletedOnboarding: Boolean = false
) {
    fun recommendedGoalMl(): Int {
        val base = (weightKg.coerceIn(30, 180) * 35)
        val raw = base + activityLevel.bonusMl
        return ((raw + 50) / 100) * 100
    }
}

data class HydrationUiState(
    val profile: UserProfile = UserProfile(),
    val todayTotalMl: Int = 0,
    val todaySteps: Int = 0,
    val todayLogs: List<WaterLogEntry> = emptyList(),
    val weeklyHistory: List<DailyHistoryEntry> = emptyList(),
    val streakDays: Int = 0,
    val isLoaded: Boolean = false
) {
    val progress: Float
        get() = if (profile.dailyGoalMl <= 0) 0f
        else (todayTotalMl.toFloat() / profile.dailyGoalMl.toFloat()).coerceIn(0f, 1.5f)

    val clampedProgress: Float
        get() = progress.coerceIn(0f, 1f)

    val percentInt: Int
        get() = if (profile.dailyGoalMl <= 0) 0
        else ((todayTotalMl.toFloat() / profile.dailyGoalMl.toFloat()) * 100f).toInt()

    val remainingMl: Int
        get() = (profile.dailyGoalMl - todayTotalMl).coerceAtLeast(0)

    val stepProgress: Float
        get() = if (profile.dailyStepGoal <= 0) 0f
        else (todaySteps.toFloat() / profile.dailyStepGoal.toFloat()).coerceIn(0f, 1f)

    val stepPercentInt: Int
        get() = if (profile.dailyStepGoal <= 0) 0
        else ((todaySteps.toFloat() / profile.dailyStepGoal.toFloat()) * 100f).toInt()

    val estimatedKm: Float
        get() = (todaySteps * 0.00076f)

    val estimatedKcal: Int
        get() = (todaySteps * 0.04f).toInt()

    val mood: CharacterMood
        get() = when {
            percentInt >= 100 -> CharacterMood.CELEBRATING
            percentInt >= 70 -> CharacterMood.ENERGIZED
            percentInt >= 25 -> CharacterMood.HAPPY
            else -> CharacterMood.THIRSTY
        }
}
