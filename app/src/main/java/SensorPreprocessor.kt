package com.example.try1

class SensorPreprocessor {

    private val alpha = 0.1f
    private var accelBias = FloatArray(3)
    private var isCalibrated = false
    private var prevAccelFiltered = FloatArray(3)

    fun calibrate(accelReadings: FloatArray) {
        accelBias = accelReadings.copyOf()
        isCalibrated = true
    }

    fun preprocess(accelRaw: FloatArray): FloatArray {
        if (!isCalibrated) {
            return accelRaw
        }

        val accelFiltered = FloatArray(3)

        for (i in 0..2) {
            val accelCorrected = accelRaw[i] - accelBias[i]
            accelFiltered[i] = alpha * accelCorrected + (1 - alpha) * prevAccelFiltered[i]
            prevAccelFiltered[i] = accelFiltered[i]
        }

        return accelFiltered
    }
}