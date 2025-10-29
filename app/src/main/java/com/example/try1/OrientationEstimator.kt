package com.example.try1

import kotlin.math.*

class OrientationEstimator {

    // Filter coefficient: 0 < alpha < 1 (higher = more gyro trust)
    private val alpha = 0.98f

    // Orientation angles in radians
    private var roll: Float = 0f
    private var pitch: Float = 0f
    private var yaw: Float = 0f

    // Timestamp of last gyroscope update (nanoseconds)
    private var lastTimestamp: Long = 0

    /**
     * Update orientation estimate based on accelerometer + gyroscope data.
     * @param accelData [x, y, z] in m/s²
     * @param gyroData [x, y, z] in rad/s
     * @param timestamp Event timestamp in nanoseconds
     * @return Triple(roll, pitch, yaw) in radians
     */
    fun updateOrientation(accelData: FloatArray, gyroData: FloatArray, timestamp: Long): Triple<Float, Float, Float> {
        val epsilon = 1e-6f

        if (lastTimestamp == 0L) {
            lastTimestamp = timestamp
            // Initialize roll and pitch from accelerometer
            roll = atan2(accelData[1], accelData[2])
            pitch = atan(-accelData[0] / (sqrt(accelData[1]*accelData[1] + accelData[2]*accelData[2]) + epsilon))
            // yaw cannot be obtained from accelerometer; leave as 0
            return Triple(roll, pitch, yaw)
        }

        val dt = (timestamp - lastTimestamp) / 1_000_000_000.0f // ns → s
        lastTimestamp = timestamp

        // Integrate gyro data
        roll += gyroData[0] * dt
        pitch += gyroData[1] * dt
        yaw += gyroData[2] * dt

        // Compute roll & pitch from accelerometer
        val rollAccel = atan2(accelData[1], accelData[2])
        val pitchAccel = atan(-accelData[0] / (sqrt(accelData[1]*accelData[1] + accelData[2]*accelData[2]) + epsilon))

        // Complementary filter
        roll = alpha * roll + (1 - alpha) * rollAccel
        pitch = alpha * pitch + (1 - alpha) * pitchAccel
        // yaw left as is (no accel correction)

        return Triple(roll, pitch, yaw)
    }

    /** Orientation in degrees */
    fun getOrientationDegrees(): Triple<Float, Float, Float> {
        return Triple(
            Math.toDegrees(roll.toDouble()).toFloat(),
            Math.toDegrees(pitch.toDouble()).toFloat(),
            Math.toDegrees(yaw.toDouble()).toFloat()
        )
    }

    /** Reset orientation state */
    fun reset() {
        roll = 0f
        pitch = 0f
        yaw = 0f
        lastTimestamp = 0L
    }
}
