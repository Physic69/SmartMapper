package com.example.try1

import android.util.Log
import kotlin.math.*

/**
 * Enhanced motion + position estimator with step detection and ZUPT
 */
class EnhancedVelocityPositionEstimator(
    private val accelNoiseThreshold: Float = 0.1f,
    private val freqWeight: Float = 0.4f,
    private val varWeight: Float = 0.2f,
    private val constantSL: Float = 0.3f,
    private val minStepInterval: Float = 0.3f,
    private val maxStepInterval: Float = 1.2f,
    private val dynamicThresholdFactor: Float = 1.2f,
    private val velocityZeroThreshold: Float = 0.05f
) {
    private var velocity = FloatArray(3) { 0f }
    private var position = FloatArray(3) { 0f }
    private var lastTimestamp: Long = 0L

    private var lastStepTimestamp: Long = 0L
    private val magAccelWindow = mutableListOf<Float>()
    private var lastStepLength: Float = 0f
    private val gravity = 9.81f

    /**
     * Call this with filtered accel + orientation + timestamp
     * Returns velocity & position pair.
     */
    fun update(
        accelBody: FloatArray,
        orientation: Triple<Float, Float, Float>,
        timestamp: Long
    ): Pair<FloatArray, FloatArray> {
        if (lastTimestamp == 0L) {
            lastTimestamp = timestamp
            return Pair(velocity.copyOf(), position.copyOf())
        }
        val dt = (timestamp - lastTimestamp) / 1_000_000_000.0f
        lastTimestamp = timestamp

        val accelWorld = rotateToWorld(accelBody, orientation)
        accelWorld[2] -= gravity

        val aMag = sqrt(accelWorld[0] * accelWorld[0] + accelWorld[1] * accelWorld[1] + accelWorld[2] * accelWorld[2])
        magAccelWindow.add(aMag)
        if (magAccelWindow.size > 50) {
            magAccelWindow.removeAt(0)
        }
        val meanMag = magAccelWindow.average().toFloat()
        val deltaTh = meanMag * dynamicThresholdFactor

        val stepDetected = detectStep(aMag, deltaTh, timestamp)
        if (stepDetected) {
            val stepInterval = (timestamp - lastStepTimestamp) / 1_000_000_000.0f
            lastStepTimestamp = timestamp
            val stepFreq = if (stepInterval > 0f) 1f / stepInterval else 0f
            val varAcc = variance(magAccelWindow)
            val stepLen = freqWeight * stepFreq + varWeight * varAcc + constantSL

            lastStepLength = stepLen
            position[0] += stepLen * cos(orientation.third)
            position[1] += stepLen * sin(orientation.third)

            // Zero-velocity update: reset velocity on step detected
            velocity = FloatArray(3) { 0f }

            // Log the step detection and step length
            Log.i("StepDetection", "Step detected: length = ${"%.2f".format(stepLen)} m, " +
                    "New Position=(${position[0]}, ${position[1]})")
        } else {
            for (i in 0..2) {
                val a = if (abs(accelWorld[i]) < accelNoiseThreshold) 0f else accelWorld[i]
                velocity[i] += a * dt
            }

            val speed = sqrt(velocity[0] * velocity[0] + velocity[1] * velocity[1] + velocity[2] * velocity[2])
            if (speed < velocityZeroThreshold) {
                velocity = FloatArray(3) { 0f }
            }
            for (i in 0..2) position[i] += velocity[i] * dt
        }
        return Pair(velocity.copyOf(), position.copyOf())
    }

    private fun detectStep(aMag: Float, deltaTh: Float, timestamp: Long): Boolean {
        if (aMag > deltaTh) {
            val timeSinceLast = (timestamp - lastStepTimestamp) / 1_000_000_000.0f
            if (timeSinceLast > minStepInterval && timeSinceLast < maxStepInterval) {
                return true
            }
        }
        return false
    }

    private fun variance(list: List<Float>): Float {
        if (list.isEmpty()) return 0f
        val mean = list.average().toFloat()
        return list.fold(0f) { acc, v -> acc + (v - mean) * (v - mean) } / list.size
    }

    private fun rotateToWorld(accel: FloatArray, orientation: Triple<Float, Float, Float>): FloatArray {
        val (roll, pitch, yaw) = orientation
        val sinR = sin(roll)
        val cosR = cos(roll)
        val sinP = sin(pitch)
        val cosP = cos(pitch)
        val sinY = sin(yaw)
        val cosY = cos(yaw)
        val rot = arrayOf(
            floatArrayOf(cosP * cosY, sinR * sinP * cosY - cosR * sinY, cosR * sinP * cosY + sinR * sinY),
            floatArrayOf(cosP * sinY, sinR * sinP * sinY + cosR * cosY, cosR * sinP * sinY - sinR * cosY),
            floatArrayOf(-sinP, sinR * cosP, cosR * cosP)
        )
        val out = FloatArray(3) { 0f }
        for (i in 0..2) for (j in 0..2) out[i] += rot[i][j] * accel[j]
        return out
    }

    fun reset() {
        velocity = FloatArray(3) { 0f }
        position = FloatArray(3) { 0f }
        lastTimestamp = 0L
        lastStepTimestamp = 0L
        magAccelWindow.clear()
    }
}
