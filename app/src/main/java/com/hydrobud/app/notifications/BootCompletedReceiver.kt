package com.hydrobud.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hydrobud.app.data.HydrationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val state = HydrationRepository.getInstance(context).getSnapshot()
                    HydrationReminderWorker.schedule(
                        context = context,
                        enabled = state.profile.remindersEnabled,
                        intervalMinutes = state.profile.reminderIntervalMinutes
                    )
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
