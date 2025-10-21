package com.example.try1

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
class MotionSensorService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    private val sensorPreprocessor = SensorPreprocessor()
    private val orientationEstimator = OrientationEstimator()
    private val positionEstimator = EnhancedVelocityPositionEstimator()

    private var lastAccelRaw = FloatArray(3)
    private var lastGyroRaw = FloatArray(3)
    private var isCalibrated = false

    // Companion object for constants

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "MotionSensorServiceChannel"

        // NEW: Add constants for broadcasting
        val ACTION_SENSOR_DATA_UPDATE = "com.example.try1.SENSOR_DATA_UPDATE"
        val EXTRA_POSITION_X = "com.example.try1.EXTRA_POSITION_X"
        val EXTRA_POSITION_Y = "com.example.try1.EXTRA_POSITION_Y"
        val EXTRA_POSITION_Z = "com.example.try1.EXTRA_POSITION_Z"
    }

    override fun onCreate() {
        super.onCreate()

        // Create notification channel and start service in foreground
        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        } ?: run {
            Log.w("MotionSensorService", "Accelerometer not available")
        }

        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        } ?: run {
            Log.w("MotionSensorService", "Gyroscope not available")
        }

        Log.d("MotionSensorService", "Service created, waiting for calibration")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Motion Sensor Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Motion Tracking Active")
            .setContentText("Tracking your movement in the background.")
            // Use a proper icon for your app
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    override fun onSensorChanged(event: SensorEvent) {
        // This logic remains the same
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                lastAccelRaw[0] = event.values[0]
                lastAccelRaw[1] = event.values[1]
                lastAccelRaw[2] = event.values[2]
            }
            Sensor.TYPE_GYROSCOPE -> {
                lastGyroRaw[0] = event.values[0]
                lastGyroRaw[1] = event.values[1]
                lastGyroRaw[2] = event.values[2]
            }
        }

        if (!isCalibrated && lastAccelRaw.any { it != 0f } && lastGyroRaw.any { it != 0f }) {
            sensorPreprocessor.calibrate(lastAccelRaw, lastGyroRaw)
            isCalibrated = true
            Log.i("MotionSensorService", "Calibration completed with initial stationary readings")
            return
        }

        if (isCalibrated) {
            val (accelFiltered, gyroFiltered) = sensorPreprocessor.preprocess(lastAccelRaw, lastGyroRaw)
            val orientation = orientationEstimator.updateOrientation(accelFiltered, gyroFiltered, event.timestamp)
            val (velocity, position) = positionEstimator.update(accelFiltered, orientation, event.timestamp)

            Log.d("MotionSensorService", "Position: x=${position[0]}, y=${position[1]}, z=${position[2]}")

            // NEW: Call the function to broadcast the data
            sendDataToActivity(position)
        }
    }
    private fun sendDataToActivity(position: FloatArray) {
        val intent = Intent(ACTION_SENSOR_DATA_UPDATE).apply {
            putExtra(EXTRA_POSITION_X, position[0])
            putExtra(EXTRA_POSITION_Y, position[1])
            putExtra(EXTRA_POSITION_Z, position[2])
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
        Log.d("MotionSensorService", "Service destroyed and listeners unregistered")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}