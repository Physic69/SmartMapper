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
    private var rotationVectorSensor: Sensor? = null // REPLACED Gyroscope

    private val sensorPreprocessor = SensorPreprocessor()
    private val positionEstimator = EnhancedVelocityPositionEstimator()

    // State variables
    private var isCalibrated = false
    private var lastAccelRaw = FloatArray(3)
    private var rotationMatrix = FloatArray(9) // To store orientation

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "MotionSensorServiceChannel"
        val ACTION_SENSOR_DATA_UPDATE = "com.example.try1.SENSOR_DATA_UPDATE"
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

        Log.d("MotionSensorService", "Service created, waiting for calibration")
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, lastAccelRaw, 0, 3)

                // Calibrate with the first accelerometer reading
                if (!isCalibrated) {
                    sensorPreprocessor.calibrate(lastAccelRaw)
                    isCalibrated = true
                    Log.i("MotionSensorService", "Calibration completed with initial stationary readings")
                    return
                }

                // If we have an orientation, process the accelerometer data
                if (rotationMatrix.any { it != 0f }) {
                    val accelFiltered = sensorPreprocessor.preprocess(lastAccelRaw)
                    val (_, position) = positionEstimator.update(accelFiltered, rotationMatrix, event.timestamp)

                    Log.d("MotionSensorService", "Position: x=${position[0]}, y=${position[1]}, z=${position[2]}")
                    sendDataToActivity(position)
                }
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                // Get the rotation matrix from the sensor event
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            }
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

    // --- Unchanged methods from here ---
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
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