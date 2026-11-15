package com.hydrobud.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

private val Context.hydroDataStore: DataStore<Preferences> by preferencesDataStore(name = "hydrobud_prefs")

class HydrationRepository private constructor(private val context: Context) {

    private object Keys {
        val NAME = stringPreferencesKey("user_name")
        val WEIGHT_KG = intPreferencesKey("weight_kg")
        val ACTIVITY_LEVEL = stringPreferencesKey("activity_level")
        val DAILY_GOAL_ML = intPreferencesKey("daily_goal_ml")
        val DAILY_STEP_GOAL = intPreferencesKey("daily_step_goal")
        val WALK_TRACKING_ENABLED = booleanPreferencesKey("walk_tracking_enabled")
        val WAKE_HOUR = intPreferencesKey("wake_hour")
        val SLEEP_HOUR = intPreferencesKey("sleep_hour")
        val CHARACTER_TYPE = stringPreferencesKey("character_type")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val REMINDER_INTERVAL_MIN = intPreferencesKey("reminder_interval_min")
        val QUICK_CUP_1 = intPreferencesKey("quick_cup_1")
        val QUICK_CUP_2 = intPreferencesKey("quick_cup_2")
        val QUICK_CUP_3 = intPreferencesKey("quick_cup_3")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")

        val LAST_ACTIVE_DATE = stringPreferencesKey("last_active_date")
        val TODAY_LOGS_JSON = stringPreferencesKey("today_logs_json")
        val HISTORY_JSON = stringPreferencesKey("history_json")

        val TODAY_STEPS = intPreferencesKey("today_steps")
        val SENSOR_BASELINE = intPreferencesKey("sensor_baseline")
        val LAST_SENSOR_VALUE = intPreferencesKey("last_sensor_value")
    }

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun todayKey(): String = LocalDate.now().format(dateFormatter)

    val uiStateFlow: Flow<HydrationUiState> = context.hydroDataStore.data.map { prefs ->
        val today = todayKey()
        val storedDate = prefs[Keys.LAST_ACTIVE_DATE] ?: today

        val profile = UserProfile(
            name = prefs[Keys.NAME] ?: "Buddy",
            weightKg = prefs[Keys.WEIGHT_KG] ?: 65,
            activityLevel = ActivityLevel.fromId(prefs[Keys.ACTIVITY_LEVEL]),
            dailyGoalMl = prefs[Keys.DAILY_GOAL_ML] ?: 2500,
            dailyStepGoal = prefs[Keys.DAILY_STEP_GOAL] ?: 8000,
            walkTrackingEnabled = prefs[Keys.WALK_TRACKING_ENABLED] ?: true,
            wakeHour = prefs[Keys.WAKE_HOUR] ?: 7,
            sleepHour = prefs[Keys.SLEEP_HOUR] ?: 23,
            characterType = HydroCharacterType.fromId(prefs[Keys.CHARACTER_TYPE]),
            remindersEnabled = prefs[Keys.REMINDERS_ENABLED] ?: true,
            reminderIntervalMinutes = prefs[Keys.REMINDER_INTERVAL_MIN] ?: 60,
            quickCup1Ml = prefs[Keys.QUICK_CUP_1] ?: 150,
            quickCup2Ml = prefs[Keys.QUICK_CUP_2] ?: 250,
            quickCup3Ml = prefs[Keys.QUICK_CUP_3] ?: 500,
            hasCompletedOnboarding = prefs[Keys.ONBOARDING_DONE] ?: false
        )

        val todayLogs = if (storedDate == today) {
            parseLogs(prefs[Keys.TODAY_LOGS_JSON])
        } else {
            emptyList()
        }

        val todaySteps = if (storedDate == today) (prefs[Keys.TODAY_STEPS] ?: 0) else 0
        val todayTotal = todayLogs.sumOf { it.amountMl }
        val historyMap = parseHistory(prefs[Keys.HISTORY_JSON]).toMutableMap()
        historyMap[today] = DailyHistoryEntry(
            dateKey = today,
            totalMl = todayTotal,
            goalMl = profile.dailyGoalMl,
            steps = todaySteps,
            stepGoal = profile.dailyStepGoal
        )

        val last7Days = (6 downTo 0).map { offset ->
            val d = LocalDate.now().minusDays(offset.toLong()).format(dateFormatter)
            historyMap[d] ?: DailyHistoryEntry(d, 0, profile.dailyGoalMl, 0, profile.dailyStepGoal)
        }

        val streak = calculateStreak(historyMap, profile.dailyGoalMl)

        HydrationUiState(
            profile = profile,
            todayTotalMl = todayTotal,
            todaySteps = todaySteps,
            todayLogs = todayLogs.sortedByDescending { it.timestampMillis },
            weeklyHistory = last7Days,
            streakDays = streak,
            isLoaded = true
        )
    }

    suspend fun getSnapshot(): HydrationUiState = uiStateFlow.first()

    suspend fun ensureDateRolledOver() {
        val today = todayKey()
        context.hydroDataStore.edit { prefs ->
            val storedDate = prefs[Keys.LAST_ACTIVE_DATE]
            if (storedDate != null && storedDate != today) {
                val oldLogs = parseLogs(prefs[Keys.TODAY_LOGS_JSON])
                val oldTotal = oldLogs.sumOf { it.amountMl }
                val oldGoal = prefs[Keys.DAILY_GOAL_ML] ?: 2500
                val oldSteps = prefs[Keys.TODAY_STEPS] ?: 0
                val oldStepGoal = prefs[Keys.DAILY_STEP_GOAL] ?: 8000

                val historyMap = parseHistory(prefs[Keys.HISTORY_JSON]).toMutableMap()
                historyMap[storedDate] = DailyHistoryEntry(
                    dateKey = storedDate,
                    totalMl = oldTotal,
                    goalMl = oldGoal,
                    steps = oldSteps,
                    stepGoal = oldStepGoal
                )
                prefs[Keys.HISTORY_JSON] = serializeHistory(historyMap)
                prefs[Keys.TODAY_LOGS_JSON] = "[]"
                prefs[Keys.TODAY_STEPS] = 0
                val lastSensor = prefs[Keys.LAST_SENSOR_VALUE] ?: -1
                if (lastSensor >= 0) {
                    prefs[Keys.SENSOR_BASELINE] = lastSensor
                }
            }
            prefs[Keys.LAST_ACTIVE_DATE] = today
        }
    }

    suspend fun updateHardwareSteps(totalStepsSinceBoot: Int) {
        if (totalStepsSinceBoot < 0) return
        ensureDateRolledOver()
        val today = todayKey()
        context.hydroDataStore.edit { prefs ->
            val lastSensor = prefs[Keys.LAST_SENSOR_VALUE] ?: -1
            val currentTodaySteps = prefs[Keys.TODAY_STEPS] ?: 0
            var baseline = prefs[Keys.SENSOR_BASELINE] ?: -1

            val newTodaySteps = when {
                baseline < 0 -> {
                    baseline = totalStepsSinceBoot - currentTodaySteps
                    prefs[Keys.SENSOR_BASELINE] = baseline
                    currentTodaySteps
                }
                totalStepsSinceBoot < lastSensor -> {
                    // Device rebooted: hardware counter reset to 0
                    baseline = totalStepsSinceBoot - currentTodaySteps
                    prefs[Keys.SENSOR_BASELINE] = baseline
                    currentTodaySteps + totalStepsSinceBoot
                }
                else -> {
                    (totalStepsSinceBoot - baseline).coerceAtLeast(currentTodaySteps)
                }
            }

            prefs[Keys.LAST_SENSOR_VALUE] = totalStepsSinceBoot
            prefs[Keys.TODAY_STEPS] = newTodaySteps
            prefs[Keys.LAST_ACTIVE_DATE] = today

            val goal = prefs[Keys.DAILY_GOAL_ML] ?: 2500
            val stepGoal = prefs[Keys.DAILY_STEP_GOAL] ?: 8000
            val totalMl = parseLogs(prefs[Keys.TODAY_LOGS_JSON]).sumOf { it.amountMl }
            val historyMap = parseHistory(prefs[Keys.HISTORY_JSON]).toMutableMap()
            historyMap[today] = DailyHistoryEntry(today, totalMl, goal, newTodaySteps, stepGoal)
            prefs[Keys.HISTORY_JSON] = serializeHistory(historyMap)
        }
    }

    suspend fun incrementStepsBy(delta: Int) {
        if (delta <= 0) return
        ensureDateRolledOver()
        val today = todayKey()
        context.hydroDataStore.edit { prefs ->
            val current = prefs[Keys.TODAY_STEPS] ?: 0
            val updated = current + delta
            prefs[Keys.TODAY_STEPS] = updated
            val baseline = prefs[Keys.SENSOR_BASELINE] ?: -1
            if (baseline >= 0) {
                prefs[Keys.SENSOR_BASELINE] = (baseline - delta).coerceAtLeast(0)
            }
            prefs[Keys.LAST_ACTIVE_DATE] = today

            val goal = prefs[Keys.DAILY_GOAL_ML] ?: 2500
            val stepGoal = prefs[Keys.DAILY_STEP_GOAL] ?: 8000
            val totalMl = parseLogs(prefs[Keys.TODAY_LOGS_JSON]).sumOf { it.amountMl }
            val historyMap = parseHistory(prefs[Keys.HISTORY_JSON]).toMutableMap()
            historyMap[today] = DailyHistoryEntry(today, totalMl, goal, updated, stepGoal)
            prefs[Keys.HISTORY_JSON] = serializeHistory(historyMap)
        }
    }

    suspend fun addWater(amountMl: Int, label: String = "Water") {
        if (amountMl <= 0) return
        ensureDateRolledOver()
        val today = todayKey()
        context.hydroDataStore.edit { prefs ->
            val currentLogs = parseLogs(prefs[Keys.TODAY_LOGS_JSON]).toMutableList()
            currentLogs.add(
                WaterLogEntry(
                    id = UUID.randomUUID().toString(),
                    amountMl = amountMl,
                    timestampMillis = System.currentTimeMillis(),
                    label = label
                )
            )
            prefs[Keys.TODAY_LOGS_JSON] = serializeLogs(currentLogs)
            prefs[Keys.LAST_ACTIVE_DATE] = today

            val goal = prefs[Keys.DAILY_GOAL_ML] ?: 2500
            val stepGoal = prefs[Keys.DAILY_STEP_GOAL] ?: 8000
            val steps = prefs[Keys.TODAY_STEPS] ?: 0
            val total = currentLogs.sumOf { it.amountMl }
            val historyMap = parseHistory(prefs[Keys.HISTORY_JSON]).toMutableMap()
            historyMap[today] = DailyHistoryEntry(today, total, goal, steps, stepGoal)
            prefs[Keys.HISTORY_JSON] = serializeHistory(historyMap)
        }
    }

    suspend fun removeLog(logId: String) {
        ensureDateRolledOver()
        val today = todayKey()
        context.hydroDataStore.edit { prefs ->
            val currentLogs = parseLogs(prefs[Keys.TODAY_LOGS_JSON]).filterNot { it.id == logId }
            prefs[Keys.TODAY_LOGS_JSON] = serializeLogs(currentLogs)
            val goal = prefs[Keys.DAILY_GOAL_ML] ?: 2500
            val stepGoal = prefs[Keys.DAILY_STEP_GOAL] ?: 8000
            val steps = prefs[Keys.TODAY_STEPS] ?: 0
            val total = currentLogs.sumOf { it.amountMl }
            val historyMap = parseHistory(prefs[Keys.HISTORY_JSON]).toMutableMap()
            historyMap[today] = DailyHistoryEntry(today, total, goal, steps, stepGoal)
            prefs[Keys.HISTORY_JSON] = serializeHistory(historyMap)
        }
    }

    suspend fun undoLastLog() {
        ensureDateRolledOver()
        val today = todayKey()
        context.hydroDataStore.edit { prefs ->
            val currentLogs = parseLogs(prefs[Keys.TODAY_LOGS_JSON]).sortedByDescending { it.timestampMillis }
            if (currentLogs.isNotEmpty()) {
                val updated = currentLogs.drop(1)
                prefs[Keys.TODAY_LOGS_JSON] = serializeLogs(updated)
                val goal = prefs[Keys.DAILY_GOAL_ML] ?: 2500
                val stepGoal = prefs[Keys.DAILY_STEP_GOAL] ?: 8000
                val steps = prefs[Keys.TODAY_STEPS] ?: 0
                val total = updated.sumOf { it.amountMl }
                val historyMap = parseHistory(prefs[Keys.HISTORY_JSON]).toMutableMap()
                historyMap[today] = DailyHistoryEntry(today, total, goal, steps, stepGoal)
                prefs[Keys.HISTORY_JSON] = serializeHistory(historyMap)
            }
        }
    }

    suspend fun updateProfile(profile: UserProfile) {
        context.hydroDataStore.edit { prefs ->
            prefs[Keys.NAME] = profile.name.ifBlank { "Buddy" }
            prefs[Keys.WEIGHT_KG] = profile.weightKg.coerceIn(30, 200)
            prefs[Keys.ACTIVITY_LEVEL] = profile.activityLevel.id
            prefs[Keys.DAILY_GOAL_ML] = profile.dailyGoalMl.coerceIn(500, 6000)
            prefs[Keys.DAILY_STEP_GOAL] = profile.dailyStepGoal.coerceIn(1000, 30000)
            prefs[Keys.WALK_TRACKING_ENABLED] = profile.walkTrackingEnabled
            prefs[Keys.WAKE_HOUR] = profile.wakeHour.coerceIn(0, 23)
            prefs[Keys.SLEEP_HOUR] = profile.sleepHour.coerceIn(0, 23)
            prefs[Keys.CHARACTER_TYPE] = profile.characterType.id
            prefs[Keys.REMINDERS_ENABLED] = profile.remindersEnabled
            prefs[Keys.REMINDER_INTERVAL_MIN] = profile.reminderIntervalMinutes
            prefs[Keys.QUICK_CUP_1] = profile.quickCup1Ml.coerceIn(50, 1500)
            prefs[Keys.QUICK_CUP_2] = profile.quickCup2Ml.coerceIn(50, 1500)
            prefs[Keys.QUICK_CUP_3] = profile.quickCup3Ml.coerceIn(50, 1500)
            prefs[Keys.ONBOARDING_DONE] = profile.hasCompletedOnboarding
        }
    }

    private fun parseLogs(json: String?): List<WaterLogEntry> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        WaterLogEntry(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            amountMl = obj.optInt("amountMl", 250),
                            timestampMillis = obj.optLong("timestampMillis", System.currentTimeMillis()),
                            label = obj.optString("label", "Water")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeLogs(logs: List<WaterLogEntry>): String {
        val arr = JSONArray()
        logs.forEach { entry ->
            arr.put(
                JSONObject().apply {
                    put("id", entry.id)
                    put("amountMl", entry.amountMl)
                    put("timestampMillis", entry.timestampMillis)
                    put("label", entry.label)
                }
            )
        }
        return arr.toString()
    }

    private fun parseHistory(json: String?): Map<String, DailyHistoryEntry> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val arr = JSONArray(json)
            val map = mutableMapOf<String, DailyHistoryEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val dateKey = obj.optString("dateKey")
                if (dateKey.isNotBlank()) {
                    map[dateKey] = DailyHistoryEntry(
                        dateKey = dateKey,
                        totalMl = obj.optInt("totalMl", 0),
                        goalMl = obj.optInt("goalMl", 2500),
                        steps = obj.optInt("steps", 0),
                        stepGoal = obj.optInt("stepGoal", 8000)
                    )
                }
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun serializeHistory(map: Map<String, DailyHistoryEntry>): String {
        val arr = JSONArray()
        map.values.sortedByDescending { it.dateKey }.take(30).forEach { entry ->
            arr.put(
                JSONObject().apply {
                    put("dateKey", entry.dateKey)
                    put("totalMl", entry.totalMl)
                    put("goalMl", entry.goalMl)
                    put("steps", entry.steps)
                    put("stepGoal", entry.stepGoal)
                }
            )
        }
        return arr.toString()
    }

    private fun calculateStreak(
        historyMap: Map<String, DailyHistoryEntry>,
        currentGoal: Int
    ): Int {
        var streak = 0
        val today = LocalDate.now()
        val todayEntry = historyMap[today.format(dateFormatter)]
        val todayMet = todayEntry != null && todayEntry.totalMl >= (todayEntry.goalMl.takeIf { it > 0 } ?: currentGoal)
        if (todayMet) streak++

        var dayOffset = 1L
        while (dayOffset <= 30L) {
            val key = today.minusDays(dayOffset).format(dateFormatter)
            val entry = historyMap[key]
            if (entry != null && entry.totalMl >= entry.goalMl && entry.goalMl > 0) {
                streak++
                dayOffset++
            } else {
                break
            }
        }
        return streak
    }

    companion object {
        @Volatile
        private var INSTANCE: HydrationRepository? = null

        fun getInstance(context: Context): HydrationRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HydrationRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
