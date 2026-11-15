package com.hydrobud.app.steps

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hydrobud.app.MainActivity
import com.hydrobud.app.R
import com.hydrobud.app.data.HydrationRepository
import com.hydrobud.app.widget.HydroWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Always-on background step counter foreground service.
 * Uses hardware Sensor.TYPE_STEP_COUNTER for ultra-low battery usage,
 * with an accelerometer peak fallback for budget devices without a step sensor.
 */
class StepCounterService : Service(), SensorEventListener {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var sensorManager: SensorManager
    private var usingHardwareCounter = false
    private var lastAccelMag = 9.8f
    private var lastStepTimeMs = 0L

    override fun onCreate() {
        super.onCreate()
        createChannel()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepCounter != null) {
            usingHardwareCounter = true
            sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (accel != null) {
                sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildLiveNotification(0, 8000, 0, 2500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        serviceScope.launch {
            val state = HydrationRepository.getInstance(applicationContext).getSnapshot()
            updateNotification(
                state.todaySteps,
                state.profile.dailyStepGoal,
                state.todayTotalMl,
                state.profile.dailyGoalMl
            )
        }
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (usingHardwareCounter && event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSinceBoot = event.values[0].toInt()
            serviceScope.launch {
                val repo = HydrationRepository.getInstance(applicationContext)
                repo.updateHardwareSteps(totalSinceBoot)
                val state = repo.getSnapshot()
                updateNotification(
                    state.todaySteps,
                    state.profile.dailyStepGoal,
                    state.todayTotalMl,
                    state.profile.dailyGoalMl
                )
                HydroWidgetProvider.triggerRefresh(applicationContext)
            }
        } else if (!usingHardwareCounter && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val mag = sqrt(x * x + y * y + z * z)
            val delta = mag - lastAccelMag
            lastAccelMag = mag
            val now = System.currentTimeMillis()
            if (delta > 3.2f && now - lastStepTimeMs > 340L) {
                lastStepTimeMs = now
                serviceScope.launch {
                    val repo = HydrationRepository.getInstance(applicationContext)
                    repo.incrementStepsBy(1)
                    val state = repo.getSnapshot()
                    if (state.todaySteps % 10 == 0) {
                        updateNotification(
                            state.todaySteps,
                            state.profile.dailyStepGoal,
                            state.todayTotalMl,
                            state.profile.dailyGoalMl
                        )
                        HydroWidgetProvider.triggerRefresh(applicationContext)
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Live Step & Hydration Tracker",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps your step counter active even when the app is closed"
            setShowBadge(false)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun updateNotification(steps: Int, stepGoal: Int, waterMl: Int, waterGoal: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildLiveNotification(steps, stepGoal, waterMl, waterGoal))
    }

    private fun buildLiveNotification(
        steps: Int,
        stepGoal: Int,
        waterMl: Int,
        waterGoal: Int
    ): android.app.Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this,
            10,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val addWaterIntent = Intent(this, HydroWidgetProvider::class.java).apply {
            action = HydroWidgetProvider.ACTION_ADD_WATER_250
        }
        val addWaterPending = PendingIntent.getBroadcast(
            this,
            11,
            addWaterIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pct = if (stepGoal > 0) ((steps * 100) / stepGoal).coerceIn(0, 100) else 0
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Walk: $steps / $stepGoal steps  •  Water: $waterMl / $waterGoal ml")
            .setContentText("HydroBud is tracking your steps in the background")
            .setProgress(100, pct, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pending)
            .addAction(0, "+250 ml Sip", addWaterPending)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "hydrobud_live_steps"
        private const val NOTIFICATION_ID = 2002

        fun start(context: Context) {
            val intent = Intent(context, StepCounterService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, StepCounterService::class.java)
            context.stopService(intent)
        }
    }
}
