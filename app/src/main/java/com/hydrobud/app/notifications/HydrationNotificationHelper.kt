package com.hydrobud.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hydrobud.app.MainActivity
import com.hydrobud.app.R
import com.hydrobud.app.widget.HydroWidgetProvider

object HydrationNotificationHelper {

    const val CHANNEL_ID = "hydrobud_reminders"
    private const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        val name = "Hydration Reminders"
        val descriptionText = "Friendly reminders from your HydroBud character to drink water"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            enableVibration(true)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun showHydrationNotification(
        context: Context,
        userName: String,
        characterName: String,
        todayTotalMl: Int,
        goalMl: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val add250Intent = Intent(context, HydroWidgetProvider::class.java).apply {
            action = HydroWidgetProvider.ACTION_ADD_WATER_250
        }
        val add250PendingIntent = PendingIntent.getBroadcast(
            context,
            250,
            add250Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remaining = (goalMl - todayTotalMl).coerceAtLeast(0)
        val title = if (remaining > 0) {
            "$characterName misses you, $userName!"
        } else {
            "$characterName is proud of you, $userName!"
        }

        val body = if (remaining > 0) {
            "Time for a fresh sip! You're at $todayTotalMl / $goalMl ml ($remaining ml to go)."
        } else {
            "You hit your $goalMl ml goal today! Keep glowing!"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .addAction(
                0,
                "+250 ml Sip",
                add250PendingIntent
            )
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
