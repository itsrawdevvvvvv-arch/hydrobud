package com.hydrobud.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
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
        val descriptionText = "Playful reminders from your HydroBud mascot to drink water and walk"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            enableVibration(true)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showHydrationNotification(
        context: Context,
        userName: String,
        characterName: String,
        todayTotalMl: Int,
        goalMl: Int,
        todaySteps: Int = 0,
        stepGoal: Int = 8000
    ) {
        if (!hasNotificationPermission(context)) return

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

        val add500Intent = Intent(context, HydroWidgetProvider::class.java).apply {
            action = HydroWidgetProvider.ACTION_ADD_WATER_500
        }
        val add500PendingIntent = PendingIntent.getBroadcast(
            context,
            500,
            add500Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remaining = (goalMl - todayTotalMl).coerceAtLeast(0)
        val waterPct = if (goalMl > 0) ((todayTotalMl * 100) / goalMl).coerceIn(0, 100) else 0
        val stepPct = if (stepGoal > 0) ((todaySteps * 100) / stepGoal).coerceIn(0, 100) else 0

        val title = if (remaining > 0) {
            "$characterName says: Sip time, $userName!"
        } else {
            "$characterName is cheering for you, $userName!"
        }

        val body = if (remaining > 0) {
            "Water: $todayTotalMl/$goalMl ml ($waterPct%) • Walk: $todaySteps/$stepGoal steps ($stepPct%)"
        } else {
            "Daily water goal crushed! Walk progress: $todaySteps/$stepGoal steps."
        }

        val cardBitmap = renderPlayfulBannerBitmap(
            characterName = characterName,
            waterPct = waterPct,
            stepPct = stepPct,
            todayTotalMl = todayTotalMl,
            goalMl = goalMl,
            todaySteps = todaySteps,
            stepGoal = stepGoal
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_steps)
            .setLargeIcon(cardBitmap)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(cardBitmap)
                    .setSummaryText(body)
            )
            .setColor(Color.parseColor("#FF6536"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "+250 ml Sip", add250PendingIntent)
            .addAction(0, "+500 ml Bottle", add500PendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission revoked concurrently; ignore safely
        }
    }

    private fun renderPlayfulBannerBitmap(
        characterName: String,
        waterPct: Int,
        stepPct: Int,
        todayTotalMl: Int,
        goalMl: Int,
        todaySteps: Int,
        stepGoal: Int
    ): Bitmap {
        val w = 720
        val h = 280
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val offWhite = Color.parseColor("#F5F5F5")
        val cyan = Color.parseColor("#1ED5E2")
        val coral = Color.parseColor("#FF6536")
        val ink = Color.parseColor("#141414")

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = offWhite }
        canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), 42f, 42f, bgPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }

        // Cute cyan dome mascot on left
        val domePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cyan }
        val domePath = Path().apply {
            moveTo(30f, 240f)
            lineTo(30f, 130f)
            quadTo(115f, 30f, 200f, 130f)
            lineTo(200f, 240f)
            close()
        }
        canvas.drawPath(domePath, domePaint)
        canvas.drawPath(domePath, strokePaint)

        val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink }
        canvas.drawCircle(90f, 155f, 8f, inkPaint)
        canvas.drawCircle(140f, 155f, 8f, inkPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textSize = 30f
            isFakeBoldText = true
        }
        canvas.drawText("$characterName's Live Check-In", 225f, 70f, titlePaint)

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E0E0E0") }
        val waterBarRect = RectF(225f, 95f, 675f, 145f)
        canvas.drawRoundRect(waterBarRect, 25f, 25f, trackPaint)
        val waterFillWidth = (waterBarRect.width() * (waterPct / 100f)).coerceAtLeast(36f)
        canvas.drawRoundRect(RectF(225f, 95f, 225f + waterFillWidth, 145f), 25f, 25f, domePaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textSize = 22f
            isFakeBoldText = true
        }
        canvas.drawText("Water: $todayTotalMl / $goalMl ml ($waterPct%)", 245f, 128f, labelPaint)

        val walkBarRect = RectF(225f, 170f, 675f, 220f)
        canvas.drawRoundRect(walkBarRect, 25f, 25f, trackPaint)
        val walkFillWidth = (walkBarRect.width() * (stepPct / 100f)).coerceAtLeast(36f)
        val coralPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = coral }
        canvas.drawRoundRect(RectF(225f, 170f, 225f + walkFillWidth, 220f), 25f, 25f, coralPaint)
        canvas.drawText("Walk: $todaySteps / $stepGoal steps ($stepPct%)", 245f, 203f, labelPaint)

        return bmp
    }
}
