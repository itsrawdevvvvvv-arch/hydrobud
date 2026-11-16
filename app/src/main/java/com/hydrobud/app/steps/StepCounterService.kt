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
import com.hydrobud.app.notifications.HydrationNotificationHelper
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
 * Always-on step counter foreground service with full Android 14+ / MIUI safety guards:
 * - Never crashes if ACTIVITY_RECOGNITION or POST_NOTIFICATIONS is denied.
 * - Wraps startForeground() and startForegroundService() in try/catch.
 * - Uses monochrome ic_stat_steps notification small icon.
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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasActivityPermission(this)) {
            _sensorStatus.value = "Permission needed"
            _startError.value = "Grant Physical Activity permission to start the step counter."
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildLiveNotification(0, 8000, 0, 2500)
        val started = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            true
        } catch (e: SecurityException) {
            _startError.value = "Permission denied for background step tracking."
            false
        } catch (e: Exception) {
            _startError.value = "Background step service blocked by system (on Xiaomi/MIUI, enable Autostart)."
            false
        }

        if (!started) {
            _sensorStatus.value = "Blocked by system"
            stopSelf()
            return START_NOT_STICKY
        }

        _startError.value = null
        if (intent?.action == ACTION_REREGISTER || _sensorStatus.value == "Inactive" || _sensorStatus.value == "Starting...") {
            registerBestSensor()
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

        if (hasActivityPermission(this)) {
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
        _sensorStatus.value = "Inactive"
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
        if (!HydrationNotificationHelper.hasNotificationPermission(this)) return
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, buildLiveNotification(steps, stepGoal, waterMl, waterGoal))
        } catch (_: SecurityException) {
            // Keep service alive even if notification permission is missing
        }
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
            .setSmallIcon(R.drawable.ic_stat_steps)
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

        private val _sensorStatus = MutableStateFlow("Inactive")
        val sensorStatus: StateFlow<String> = _sensorStatus.asStateFlow()

        private val _startError = MutableStateFlow<String?>(null)
        val startError: StateFlow<String?> = _startError.asStateFlow()

        fun hasActivityPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }

        fun start(context: Context): Boolean {
            if (!hasActivityPermission(context)) {
                _startError.value = "Grant Physical Activity permission to start the step counter."
                return false
            }
            return try {
                val intent = Intent(context, StepCounterService::class.java)
                context.startForegroundService(intent)
                _startError.value = null
                true
            } catch (e: Exception) {
                _startError.value = "Could not start step service in background. Enable Autostart for HydroBud on Xiaomi/MIUI."
                false
            }
        }

        fun reregister(context: Context): Boolean {
            if (!hasActivityPermission(context)) return false
            return try {
                val intent = Intent(context, StepCounterService::class.java).apply {
                    action = ACTION_REREGISTER
                }
                context.startForegroundService(intent)
                _startError.value = null
                true
            } catch (e: Exception) {
                _startError.value = "Could not start step service. Enable Autostart for HydroBud on Xiaomi/MIUI."
                false
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, StepCounterService::class.java)
                context.stopService(intent)
                _startError.value = null
            } catch (_: Exception) {
            }
        }
    }
}
