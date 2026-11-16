package com.hydrobud.app

import android.content.Context

/**
 * Watchdog for the startup path.
 *
 * On every cold start the flag is set; it is cleared as soon as the first frame is on
 * screen. If the process dies before that, the next launch comes up in SAFE MODE:
 * no foreground service, no WorkManager scheduling, no widget broadcast, no splash -
 * just the UI. That keeps a bad background start from bricking the whole app.
 */
object CrashGuard {

    private const val PREFS = "hydrobud_crash_guard"
    private const val KEY_UNHEALTHY = "unhealthy_start"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun markStarting(context: Context) {
        prefs(context).edit().putBoolean(KEY_UNHEALTHY, true).apply()
    }

    fun markHealthy(context: Context) {
        prefs(context).edit().putBoolean(KEY_UNHEALTHY, false).apply()
    }

    fun isSafeMode(context: Context): Boolean =
        prefs(context).getBoolean(KEY_UNHEALTHY, false)
}
