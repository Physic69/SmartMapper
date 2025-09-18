package com.example.try1

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log

class MotionSensorService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    private val sensorPreprocessor = SensorPreprocessor()
    private val orientationEstimator = OrientationEstimator()
    private val positionEstimator = EnhancedVelocityPositionEstimator() // NEW

    private var lastAccelRaw = FloatArray(3) { 0f }
    private var lastGyroRaw = FloatArray(3) { 0f }
    private var isCalibrated = false

    override fun onCreate() {
        super.onCreate()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
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

    override fun onSensorChanged(event: SensorEvent) {
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

        // Calibrate once assuming device is stationary at start
        if (!isCalibrated && lastAccelRaw.any { it != 0f } && lastGyroRaw.any { it != 0f }) {
            sensorPreprocessor.calibrate(lastAccelRaw, lastGyroRaw)
            isCalibrated = true
            Log.i("MotionSensorService", "Calibration completed with initial stationary readings")
            return
        }

        if (isCalibrated) {
            val (accelFiltered, gyroFiltered) = sensorPreprocessor.preprocess(lastAccelRaw, lastGyroRaw)

            // Step 4: Orientation
            val orientation = orientationEstimator.updateOrientation(accelFiltered, gyroFiltered, event.timestamp)

            // Step 5: Motion integration with drift management (PDR-inspired)
            val (velocity, position) = positionEstimator.update(accelFiltered, orientation, event.timestamp)

            // Log filtered data, orientation, and estimated motion
            Log.d("MotionSensorService",
                "Filtered Accel: x=${accelFiltered[0]}, y=${accelFiltered[1]}, z=${accelFiltered[2]}"
            )
            Log.d("MotionSensorService",
                "Filtered Gyro: x=${gyroFiltered[0]}, y=${gyroFiltered[1]}, z=${gyroFiltered[2]}"
            )
            Log.d("MotionSensorService",
                "Orientation (deg): roll=${Math.toDegrees(orientation.first.toDouble())}, " +
                        "pitch=${Math.toDegrees(orientation.second.toDouble())}, yaw=${Math.toDegrees(orientation.third.toDouble())}"
            )
            Log.d("MotionSensorService",
                "Velocity: x=${velocity[0]}, y=${velocity[1]}, z=${velocity[2]}"
            )
            Log.d("MotionSensorService",
                "Position: x=${position[0]}, y=${position[1]}, z=${position[2]}"
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Optional
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
        Log.d("MotionSensorService", "Service destroyed and listeners unregistered")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
