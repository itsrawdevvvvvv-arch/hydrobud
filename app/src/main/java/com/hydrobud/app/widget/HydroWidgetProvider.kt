package com.hydrobud.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.hydrobud.app.MainActivity
import com.hydrobud.app.R
import com.hydrobud.app.data.HydrationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HydroWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateAllWidgets(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_ADD_WATER_250, ACTION_ADD_WATER_500, ACTION_WIDGET_REFRESH -> {
                val amount = when (intent.action) {
                    ACTION_ADD_WATER_250 -> 250
                    ACTION_ADD_WATER_500 -> 500
                    else -> 0
                }
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = HydrationRepository.getInstance(context)
                        if (amount > 0) {
                            repo.addWater(amount, "Widget Sip")
                        }
                        val manager = AppWidgetManager.getInstance(context)
                        val ids = manager.getAppWidgetIds(
                            ComponentName(context, HydroWidgetProvider::class.java)
                        )
                        updateAllWidgets(context, manager, ids)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_ADD_WATER_250 = "com.hydrobud.app.ACTION_ADD_WATER_250"
        const val ACTION_ADD_WATER_500 = "com.hydrobud.app.ACTION_ADD_WATER_500"
        const val ACTION_WIDGET_REFRESH = "com.hydrobud.app.ACTION_WIDGET_REFRESH"

        fun triggerRefresh(context: Context) {
            val intent = Intent(context, HydroWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_REFRESH
            }
            context.sendBroadcast(intent)
        }

        private suspend fun updateAllWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            if (appWidgetIds.isEmpty()) return
            val repo = HydrationRepository.getInstance(context)
            repo.ensureDateRolledOver()
            val state = repo.getSnapshot()

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPending = PendingIntent.getActivity(
                context,
                1,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val add250Intent = Intent(context, HydroWidgetProvider::class.java).apply {
                action = ACTION_ADD_WATER_250
            }
            val add250Pending = PendingIntent.getBroadcast(
                context,
                2,
                add250Intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val add500Intent = Intent(context, HydroWidgetProvider::class.java).apply {
                action = ACTION_ADD_WATER_500
            }
            val add500Pending = PendingIntent.getBroadcast(
                context,
                3,
                add500Intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val progressPct = state.percentInt.coerceIn(0, 100)
            val statusText = when {
                progressPct >= 100 -> "${state.profile.characterType.displayName}: Goal Reached!"
                progressPct >= 70 -> "${state.profile.characterType.displayName}: Almost there!"
                progressPct >= 25 -> "${state.profile.characterType.displayName}: Keep sipping!"
                else -> "${state.profile.characterType.displayName}: Needs water!"
            }

            appWidgetIds.forEach { widgetId ->
                val views = RemoteViews(context.packageName, R.layout.hydro_widget).apply {
                    setTextViewText(R.id.widget_intake_text, "${state.todayTotalMl} / ${state.profile.dailyGoalMl} ml")
                    setTextViewText(R.id.widget_percent_badge, "$progressPct%")
                    setTextViewText(R.id.widget_status_text, statusText)
                    setProgressBar(R.id.widget_progress_bar, 100, progressPct, false)
                    setOnClickPendingIntent(R.id.widget_root, openAppPending)
                    setOnClickPendingIntent(R.id.widget_btn_250, add250Pending)
                    setOnClickPendingIntent(R.id.widget_btn_500, add500Pending)
                }
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}
