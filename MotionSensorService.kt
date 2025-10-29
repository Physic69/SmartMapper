package com.example.try1

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
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
    private var rotationVectorSensor: Sensor? = null

    private val sensorPreprocessor = SensorPreprocessor()
    private val positionEstimator = EnhancedVelocityPositionEstimator()

    private var isCalibrating = true
    private val calibrationSamples = mutableListOf<FloatArray>()
    private val requiredSamples = 200

    private var lastAccelRaw = FloatArray(3)
    private var rotationMatrix = FloatArray(9)

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "MotionSensorServiceChannel"
        val ACTION_SENSOR_DATA_UPDATE = "com.example.try1.SENSOR_DATA_UPDATE"
        val ACTION_STATUS_UPDATE = "com.example.try1.STATUS_UPDATE"
        val EXTRA_STATUS_MESSAGE = "com.example.try1.EXTRA_STATUS_MESSAGE"
        val EXTRA_POSITION_X = "com.example.try1.EXTRA_POSITION_X"
        val EXTRA_POSITION_Y = "com.example.try1.EXTRA_POSITION_Y"
        val EXTRA_POSITION_Z = "com.example.try1.EXTRA_POSITION_Z"
        // Constant for broadcasting the variance value
        val EXTRA_VARIANCE = "com.example.try1.EXTRA_VARIANCE"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_FASTEST)

        sendStatusUpdate("Calibrating... Hold Still!")
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                if (isCalibrating) {
                    processCalibration(event.values)
                } else {
                    processMotion(event)
                }
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            }
        }
    }

    // In MotionSensorService.kt

    private fun processCalibration(accelValues: FloatArray) {
        calibrationSamples.add(accelValues.copyOf())

        // --- START OF CALIBRATION FIX ---
        // Send progress updates to the UI
        sendStatusUpdate("Calibrating... (${calibrationSamples.size} / $requiredSamples)")
        // --- END OF CALIBRATION FIX ---

        if (calibrationSamples.size >= requiredSamples) {
            val avgAccel = FloatArray(3)
            for (i in 0..2) {
                avgAccel[i] = calibrationSamples.map { it[i] }.average().toFloat()
            }
            sensorPreprocessor.calibrate(avgAccel)
            isCalibrating = false
            calibrationSamples.clear()
            Log.i("MotionSensorService", "Calibration completed.")
            sendStatusUpdate("Tracking Active")
        }
    }

    private fun processMotion(event: SensorEvent) {
        System.arraycopy(event.values, 0, lastAccelRaw, 0, 3)
        if (rotationMatrix.any { it != 0f }) {
            val accelFiltered = sensorPreprocessor.preprocess(lastAccelRaw)
            val (_, position) = positionEstimator.update(accelFiltered, rotationMatrix, event.timestamp)

            // Get the public variance property from the estimator
            val variance = positionEstimator.lastCalculatedVariance
            sendDataToActivity(position, variance)
        }
    }

    private fun sendStatusUpdate(message: String) {
        val intent = Intent(ACTION_STATUS_UPDATE).apply {
            putExtra(EXTRA_STATUS_MESSAGE, message)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    // Modified to send the variance along with position
    private fun sendDataToActivity(position: FloatArray, variance: Float) {
        val intent = Intent(ACTION_SENSOR_DATA_UPDATE).apply {
            putExtra(EXTRA_POSITION_X, position[0])
            putExtra(EXTRA_POSITION_Y, position[1])
            putExtra(EXTRA_POSITION_Z, position[2])
            putExtra(EXTRA_VARIANCE, variance)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        sendStatusUpdate("Idle")
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "Motion Sensor Service Channel", NotificationManager.IMPORTANCE_DEFAULT)
            getSystemService(NotificationManager::class.java).createNotificationChannel(serviceChannel)
        }
    }
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Motion Tracking Active")
            .setContentText("Tracking your movement in the background.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }
}