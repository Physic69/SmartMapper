package com.example.try1

class SensorPreprocessor {

    private var accelBias = FloatArray(3)
    private var isCalibrated = false

    fun calibrate(accelReadings: FloatArray) {
        accelBias = accelReadings.copyOf()
        isCalibrated = true
    }

    /**
     * Applies bias correction to raw sensor data.
     * The low-pass filter has been removed to prevent double-filtering.
     */
    fun preprocess(accelRaw: FloatArray): FloatArray {
        if (!isCalibrated) {
            return accelRaw
        }

        val accelCorrected = FloatArray(3)
        for (i in 0..2) {
            // Only remove the bias. No more filtering here.
            accelCorrected[i] = accelRaw[i] - accelBias[i]
        }
        return accelCorrected
    }
}