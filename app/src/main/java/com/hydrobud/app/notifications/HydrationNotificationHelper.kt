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

    fun showHydrationNotification(
        context: Context,
        userName: String,
        characterName: String,
        todayTotalMl: Int,
        goalMl: Int,
        todaySteps: Int = 0,
        stepGoal: Int = 8000
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(cardBitmap)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(cardBitmap)
                    .setSummaryText(body)
            )
            .setColor(Color.parseColor("#7C76B8"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "+250 ml Sip", add250PendingIntent)
            .addAction(0, "+500 ml Bottle", add500PendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /**
     * Renders a playful neo-brutalist progress card Bitmap using the "Let's stream TV shows now" palette.
     */
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

        val periwinkle = Color.parseColor("#7C76B8")
        val yellow = Color.parseColor("#F5EB3B")
        val peach = Color.parseColor("#F9C8BE")
        val amber = Color.parseColor("#F2BF5E")
        val ink = Color.parseColor("#111111")

        // Background card
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = periwinkle }
        canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), 42f, 42f, bgPaint)

        // Crisp black border
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawRoundRect(RectF(6f, 6f, w - 6f, h - 6f), 40f, 40f, strokePaint)

        // Cute mascot droplet on left
        val dropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = yellow }
        val dropPath = Path().apply {
            moveTo(115f, 42f)
            cubicTo(175f, 105f, 180f, 225f, 115f, 225f)
            cubicTo(50f, 225f, 55f, 105f, 115f, 42f)
            close()
        }
        canvas.drawPath(dropPath, dropPaint)
        canvas.drawPath(dropPath, strokePaint)

        // Cute oval eyes
        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink }
        canvas.drawOval(RectF(86f, 128f, 106f, 156f), whitePaint)
        canvas.drawOval(RectF(86f, 128f, 106f, 156f), strokePaint)
        canvas.drawOval(RectF(124f, 128f, 144f, 156f), whitePaint)
        canvas.drawOval(RectF(124f, 128f, 144f, 156f), strokePaint)
        canvas.drawCircle(98f, 143f, 5f, inkPaint)
        canvas.drawCircle(136f, 143f, 5f, inkPaint)

        // Title pill
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = yellow
            textSize = 32f
            isFakeBoldText = true
        }
        canvas.drawText("$characterName's Live Check-In", 215f, 70f, titlePaint)

        // Water pill bar
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#5D579B") }
        val waterBarRect = RectF(215f, 95f, 670f, 145f)
        canvas.drawRoundRect(waterBarRect, 25f, 25f, trackPaint)
        val waterFillWidth = (waterBarRect.width() * (waterPct / 100f)).coerceAtLeast(36f)
        val peachPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = peach }
        canvas.drawRoundRect(
            RectF(215f, 95f, 215f + waterFillWidth, 145f),
            25f,
            25f,
            peachPaint
        )
        canvas.drawRoundRect(waterBarRect, 25f, 25f, strokePaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textSize = 23f
            isFakeBoldText = true
        }
        canvas.drawText("Water: $todayTotalMl / $goalMl ml ($waterPct%)", 235f, 128f, labelPaint)

        // Walk pill bar
        val walkBarRect = RectF(215f, 170f, 670f, 220f)
        canvas.drawRoundRect(walkBarRect, 25f, 25f, trackPaint)
        val walkFillWidth = (walkBarRect.width() * (stepPct / 100f)).coerceAtLeast(36f)
        val amberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = amber }
        canvas.drawRoundRect(
            RectF(215f, 170f, 215f + walkFillWidth, 220f),
            25f,
            25f,
            amberPaint
        )
        canvas.drawRoundRect(walkBarRect, 25f, 25f, strokePaint)
        canvas.drawText("Walk: $todaySteps / $stepGoal steps ($stepPct%)", 235f, 203f, labelPaint)

        return bmp
    }
}
