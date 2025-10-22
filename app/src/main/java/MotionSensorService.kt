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

    // --- START OF CALIBRATION FIX ---
    private var isCalibrating = true
    private val calibrationSamples = mutableListOf<FloatArray>()
    private val requiredSamples = 200 // Collect 200 samples for a stable average
    // --- END OF CALIBRATION FIX ---

    private var lastAccelRaw = FloatArray(3)
    private var rotationMatrix = FloatArray(9)

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "MotionSensorServiceChannel"
        val ACTION_SENSOR_DATA_UPDATE = "com.example.try1.SENSOR_DATA_UPDATE"
        // New constants to send status updates to the UI
        val ACTION_STATUS_UPDATE = "com.example.try1.STATUS_UPDATE"
        val EXTRA_STATUS_MESSAGE = "com.example.try1.EXTRA_STATUS_MESSAGE"
        val EXTRA_POSITION_X = "com.example.try1.EXTRA_POSITION_X"
        val EXTRA_POSITION_Y = "com.example.try1.EXTRA_POSITION_Y"
        val EXTRA_POSITION_Z = "com.example.try1.EXTRA_POSITION_Z"
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

        Log.d("MotionSensorService", "Service created, starting calibration.")
        // Tell the UI that we are now calibrating
        sendStatusUpdate("Calibrating... Hold Still!")
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // --- START OF CALIBRATION FIX ---
                // If we are in calibration mode, collect samples
                if (isCalibrating) {
                    processCalibration(event.values)
                } else {
                    // Otherwise, do the normal motion processing
                    processMotion(event)
                }
                // --- END OF CALIBRATION FIX ---
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            }
        }
    }

    // --- START OF CALIBRATION FIX ---
    /**
     * Collects accelerometer samples. When enough are collected, it computes the
     * average and uses that as the bias.
     */
    private fun processCalibration(accelValues: FloatArray) {
        calibrationSamples.add(accelValues.copyOf())

        // Check if we have enough samples
        if (calibrationSamples.size >= requiredSamples) {
            val avgAccel = FloatArray(3)
            // Calculate the average for each axis
            for (i in 0..2) {
                avgAccel[i] = calibrationSamples.map { it[i] }.average().toFloat()
            }

            // Use the stable average to set the bias
            sensorPreprocessor.calibrate(avgAccel)
            isCalibrating = false
            calibrationSamples.clear() // Free up memory
            Log.i("MotionSensorService", "Calibration completed with bias: ${avgAccel.joinToString()}")
            // Tell the UI that tracking is now active
            sendStatusUpdate("Tracking Active")
        }
    }
    // --- END OF CALIBRATION FIX ---

    /**
     * The original logic that runs AFTER calibration is complete.
     */
    private fun processMotion(event: SensorEvent) {
        System.arraycopy(event.values, 0, lastAccelRaw, 0, 3)
        if (rotationMatrix.any { it != 0f }) {
            val accelFiltered = sensorPreprocessor.preprocess(lastAccelRaw)
            val (_, position) = positionEstimator.update(accelFiltered, rotationMatrix, event.timestamp)
            sendDataToActivity(position)
        }
    }

    /**
     * Sends a status message (e.g., "Calibrating") to the MainActivity.
     */
    private fun sendStatusUpdate(message: String) {
        val intent = Intent(ACTION_STATUS_UPDATE).apply {
            putExtra(EXTRA_STATUS_MESSAGE, message)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendDataToActivity(position: FloatArray) {
        val intent = Intent(ACTION_SENSOR_DATA_UPDATE).apply {
            putExtra(EXTRA_POSITION_X, position[0])
            putExtra(EXTRA_POSITION_Y, position[1])
            putExtra(EXTRA_POSITION_Z, position[2])
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    // --- Unchanged methods from here ---
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