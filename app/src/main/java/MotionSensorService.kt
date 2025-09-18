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
    private val positionEstimator = EnhancedVelocityPositionEstimator()

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
                lastAccelRaw = event.values.clone()
            }
            Sensor.TYPE_GYROSCOPE -> {
                lastGyroRaw = event.values.clone()
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

            // Orientation
            val orientation = orientationEstimator.updateOrientation(accelFiltered, gyroFiltered, event.timestamp)

            // Position & step detection
            val result = positionEstimator.update(accelFiltered, orientation, event.timestamp)

            // Logs (everything unified under MotionSensorService tag)
            Log.d("MotionSensorService", "Filtered Accel: ${accelFiltered.joinToString()}")
            Log.d("MotionSensorService", "Filtered Gyro: ${gyroFiltered.joinToString()}")
            Log.d("MotionSensorService",
                "Orientation (deg): roll=${Math.toDegrees(orientation.first.toDouble())}, " +
                        "pitch=${Math.toDegrees(orientation.second.toDouble())}, yaw=${Math.toDegrees(orientation.third.toDouble())}"
            )
            Log.d("MotionSensorService", "Velocity: ${result.velocity.joinToString()}")
            Log.d("MotionSensorService", "Position: ${result.position.joinToString()}")

            // Step detection log (if detected)
            result.stepLength?.let { stepLen ->
                Log.i("MotionSensorService", "Step detected: length=${"%.2f".format(stepLen)} m, " +
                        "Position=(${result.position[0]}, ${result.position[1]})")
            }
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

    override fun onBind(intent: Intent?): IBinder? = null
}
