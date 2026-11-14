package com.hydrobud.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hydrobud.app.data.HydrationRepository
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class HydrationReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repo = HydrationRepository.getInstance(applicationContext)
        repo.ensureDateRolledOver()
        val state = repo.getSnapshot()
        val profile = state.profile

        if (!profile.remindersEnabled) return Result.success()

        val currentHour = LocalTime.now().hour
        val withinActiveHours = if (profile.wakeHour <= profile.sleepHour) {
            currentHour in profile.wakeHour until profile.sleepHour
        } else {
            currentHour >= profile.wakeHour || currentHour < profile.sleepHour
        }

        if (withinActiveHours) {
            HydrationNotificationHelper.showHydrationNotification(
                context = applicationContext,
                userName = profile.name,
                characterName = profile.characterType.displayName,
                todayTotalMl = state.todayTotalMl,
                goalMl = profile.dailyGoalMl
            )
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "hydrobud_periodic_reminder"

        fun schedule(context: Context, enabled: Boolean, intervalMinutes: Int) {
            val workManager = WorkManager.getInstance(context)
            if (!enabled) {
                workManager.cancelUniqueWork(WORK_NAME)
                return
            }

            val safeInterval = intervalMinutes.coerceAtLeast(15).toLong()
            val request = PeriodicWorkRequestBuilder<HydrationReminderWorker>(
                safeInterval,
                TimeUnit.MINUTES
            )
                .setInitialDelay(safeInterval, TimeUnit.MINUTES)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
