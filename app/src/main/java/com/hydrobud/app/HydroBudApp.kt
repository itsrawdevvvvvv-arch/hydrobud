package com.hydrobud.app

import android.app.Application
import android.os.Build
import com.hydrobud.app.notifications.HydrationNotificationHelper
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HydroBudApp : Application() {
    override fun onCreate() {
        super.onCreate()
        installCrashLogger()
        HydrationNotificationHelper.createNotificationChannel(this)
    }

    private fun installCrashLogger() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val report = buildString {
                    appendLine("Time: $timestamp")
                    appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (SDK ${Build.VERSION.SDK_INT})")
                    appendLine("Thread: ${thread.name}")
                    appendLine(sw.toString())
                }
                File(filesDir, CRASH_LOG_FILE).writeText(report)
            } catch (_: Exception) {
                // Never fail inside the crash handler
            } finally {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    companion object {
        const val CRASH_LOG_FILE = "last_crash.txt"
    }
}
