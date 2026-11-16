package com.hydrobud.app.steps

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.hydrobud.app.MainActivity
import com.hydrobud.app.R
import com.hydrobud.app.data.HydrationRepository
import com.hydrobud.app.widget.HydroWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Reliable always-on step counter foreground service.
 * - Uses Sensor.TYPE_STEP_COUNTER with maxReportLatencyUs = 0 for immediate delivery.
 * - Falls back to Sensor.TYPE_STEP_DETECTOR or a tuned Accelerometer step detector
 *   if permission isn't granted yet or the device lacks a hardware step sensor.
 */
class StepCounterService : Service(), SensorEventListener {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var sensorManager: SensorManager

    private enum class ActiveSensorMode(val label: String) {
        HARDWARE_COUNTER("Hardware Step Sensor"),
        HARDWARE_DETECTOR("Step Detector"),
        ACCELEROMETER("Motion Sensor")
    }

    private var activeMode = ActiveSensorMode.ACCELEROMETER
    private var filteredMag = 9.81f
    private var prevMag = 9.81f
    private var lastStepTimeMs = 0L

    override fun onCreate() {
        super.onCreate()
        createChannel()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        registerBestSensor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_REREGISTER) {
            registerBestSensor()
        }

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

    private fun registerBestSensor() {
        sensorManager.unregisterListener(this)

        val hasActivityPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (hasActivityPermission) {
            val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            if (stepCounter != null && sensorManager.registerListener(
                    this,
                    stepCounter,
                    SensorManager.SENSOR_DELAY_UI,
                    0
                )
            ) {
                activeMode = ActiveSensorMode.HARDWARE_COUNTER
                _sensorStatus.value = activeMode.label
                return
            }

            val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
            if (stepDetector != null && sensorManager.registerListener(
                    this,
                    stepDetector,
                    SensorManager.SENSOR_DELAY_UI,
                    0
                )
            ) {
                activeMode = ActiveSensorMode.HARDWARE_DETECTOR
                _sensorStatus.value = activeMode.label
                return
            }
        }

        // Universal fallback: tuned low-pass/high-pass accelerometer step detector
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accel != null) {
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
            activeMode = ActiveSensorMode.ACCELEROMETER
            _sensorStatus.value = activeMode.label
        } else {
            _sensorStatus.value = "No Motion Sensor"
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
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
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                serviceScope.launch {
                    val repo = HydrationRepository.getInstance(applicationContext)
                    repo.incrementStepsBy(1)
                    val state = repo.getSnapshot()
                    updateNotification(
                        state.todaySteps,
                        state.profile.dailyStepGoal,
                        state.todayTotalMl,
                        state.profile.dailyGoalMl
                    )
                    if (state.todaySteps % 5 == 0) {
                        HydroWidgetProvider.triggerRefresh(applicationContext)
                    }
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                if (activeMode != ActiveSensorMode.ACCELEROMETER) return
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val rawMag = sqrt(x * x + y * y + z * z)
                // Smooth gravity baseline + detect walking peak
                filteredMag = filteredMag * 0.8f + rawMag * 0.2f
                val diff = filteredMag - prevMag
                prevMag = filteredMag

                val now = System.currentTimeMillis()
                if (filteredMag > 11.4f && diff > 0.65f && now - lastStepTimeMs > 320L) {
                    lastStepTimeMs = now
                    serviceScope.launch {
                        val repo = HydrationRepository.getInstance(applicationContext)
                        repo.incrementStepsBy(1)
                        val state = repo.getSnapshot()
                        if (state.todaySteps % 5 == 0) {
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
            .setContentTitle("Steps: $steps / $stepGoal  •  Water: $waterMl / $waterGoal ml")
            .setContentText("Active: ${_sensorStatus.value}")
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
        private const val ACTION_REREGISTER = "com.hydrobud.app.ACTION_REREGISTER_SENSOR"

        private val _sensorStatus = MutableStateFlow("Starting...")
        val sensorStatus: StateFlow<String> = _sensorStatus.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, StepCounterService::class.java)
            context.startForegroundService(intent)
        }

        fun reregister(context: Context) {
            val intent = Intent(context, StepCounterService::class.java).apply {
                action = ACTION_REREGISTER
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, StepCounterService::class.java)
            context.stopService(intent)
        }
    }
}
