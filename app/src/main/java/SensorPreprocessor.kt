package com.example.try1  // <- your package here

class SensorPreprocessor {

    // Low-pass filter constant (alpha): 0 < alpha < 1 (smaller = smoother)
    private val alpha = 0.1f

    // Bias offsets for accelerometer and gyroscope
    private var accelBias = FloatArray(3)
    private var gyroBias = FloatArray(3)

    // Flag to indicate if calibration is done
    private var isCalibrated = false

    // Previous filtered values for smoothing
    private var prevAccelFiltered = FloatArray(3)
    private var prevGyroFiltered = FloatArray(3)
    /**
     * Calibrate sensors by saving current stationary readings as bias.
     * Should be called when device is stationary.
     */
    fun calibrate(accelReadings: FloatArray, gyroReadings: FloatArray) {
        for (i in 0..2) {
            accelBias[i] = accelReadings[i]
            gyroBias[i] = gyroReadings[i]
        }
        isCalibrated = true
    }

    /**
     * Applies low-pass filter and bias correction to raw sensor data.
     * Returns a Pair of filtered accel and gyro arrays.
     */
    fun preprocess(accelRaw: FloatArray, gyroRaw: FloatArray): Pair<FloatArray, FloatArray> {
        if (!isCalibrated) {
            // If not calibrated, just return raw data
            return Pair(accelRaw, gyroRaw)
        }

        val accelFiltered = FloatArray(3)
        val gyroFiltered = FloatArray(3)

        for (i in 0..2) {
            // Remove bias
            val accelCorrected = accelRaw[i] - accelBias[i]
            val gyroCorrected = gyroRaw[i] - gyroBias[i]

            // Low-pass filtering
            accelFiltered[i] = alpha * accelCorrected + (1 - alpha) * prevAccelFiltered[i]
            gyroFiltered[i] = alpha * gyroCorrected + (1 - alpha) * prevGyroFiltered[i]

            // Store for next iteration
            prevAccelFiltered[i] = accelFiltered[i]
            prevGyroFiltered[i] = gyroFiltered[i]
        }

        return Pair(accelFiltered, gyroFiltered)
    }
}
