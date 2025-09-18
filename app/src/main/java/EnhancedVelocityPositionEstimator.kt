package com.example.try1

import kotlin.math.*

/**
 * Enhanced motion + position estimator using PDR ideas from "Vector Graph Assisted Pedestrian Dead Reckoning"
 */
class EnhancedVelocityPositionEstimator(
    // weights / params, you may tune these
    private val accelNoiseThreshold: Float = 0.1f,  // m/s² threshold for ignoring small accel
    private val freqWeight: Float = 0.4f,           // α in step-length model
    private val varWeight: Float = 0.2f,            // β
    private val constantSL: Float = 0.3f,           // γ (meters) - baseline step length
    private val minStepInterval: Float = 0.3f,      // seconds between steps (∆t_min)
    private val maxStepInterval: Float = 1.2f,      // seconds between steps (∆t_max)
    private val dynamicThresholdFactor: Float = 1.2f,  // how much above mean magnitude threshold
    private val velocityZeroThreshold: Float = 0.05f  // m/s
) {
    private var velocity = FloatArray(3)
    private var position = FloatArray(3)
    private var lastTimestamp: Long = 0L

    // Buffers for step detection
    private var lastStepTimestamp: Long = 0L
    private val magAccelWindow = mutableListOf<Float>()  // for computing dynamic threshold etc.
    private var lastStepLength: Float = 0f

    private val gravity = 9.81f

    /**
     * Call this as sensor events come in (with filtered accel and orientation)
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

        // Rotate acceleration to world frame
        val accelWorld = rotateToWorld(accelBody, orientation)
        // Subtract gravity
        accelWorld[2] -= gravity

        // Calculate magnitude
        val aMag = sqrt(accelWorld[0]*accelWorld[0] + accelWorld[1]*accelWorld[1] + accelWorld[2]*accelWorld[2])
        // Add to window
        magAccelWindow.add(aMag)
        if (magAccelWindow.size > 50) {  // ~1 second window if sampling ~50Hz; adjust as needed
            magAccelWindow.removeAt(0)
        }
        val meanMag = magAccelWindow.average().toFloat()

        // Dynamic threshold for step detection
        val deltaTh = meanMag * dynamicThresholdFactor

        // Step detection logic
        val stepDetected = detectStep(aMag, deltaTh, timestamp)

        if (stepDetected) {
            // Estimate step frequency: interval between steps
            val stepInterval = (timestamp - lastStepTimestamp) / 1_000_000_000.0f
            lastStepTimestamp = timestamp
            val stepFreq = if (stepInterval > 0f) 1f / stepInterval else 0f

            // Compute acceleration variance over last step: approximate from magAccelWindow
            val varAcc = variance(magAccelWindow)

            // Estimate step length
            val stepLen = freqWeight * stepFreq + varWeight * varAcc + constantSL

            // For simplicity, position update from step-based rather than from continuous integration
            // E.g., move forward by stepLen in the heading direction (we assume heading from orientation.yaw)
            // This is more PDR than double integration.
            position[0] += stepLen * cos(orientation.third)  // east / x
            position[1] += stepLen * sin(orientation.third)  // north / y
            // Z coordinate maybe for stairs or altitude, left out here
        } else {
            // Fallback: continuous integration if no step detection
            // Integrate acceleration → velocity:
            for (i in 0..2) {
                val a = if (abs(accelWorld[i]) < accelNoiseThreshold) 0f else accelWorld[i]
                velocity[i] += a * dt
            }
            // Zero velocity update if velocity small
            val speed = sqrt(velocity[0]*velocity[0] + velocity[1]*velocity[1] + velocity[2]*velocity[2])
            if (speed < velocityZeroThreshold) {
                velocity = FloatArray(3)
            }
            // Position update
            for (i in 0..2) position[i] += velocity[i] * dt
        }

        return Pair(velocity.copyOf(), position.copyOf())
    }

    private fun detectStep(aMag: Float, deltaTh: Float, timestamp: Long): Boolean {
        // simple threshold crossing method
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
        return list.fold(0f) { acc, v -> acc + (v - mean)*(v - mean) } / list.size
    }

    private fun rotateToWorld(accel: FloatArray, orientation: Triple<Float, Float, Float>): FloatArray {
        // same implementation as before (roll, pitch, yaw → rotation matrix)
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
            floatArrayOf(-sinP,         sinR * cosP,                    cosR * cosP)
        )
        val out = FloatArray(3)
        for (i in 0..2) for (j in 0..2) out[i] += rot[i][j] * accel[j]
        return out
    }

    fun reset() {
        velocity = FloatArray(3)
        position = FloatArray(3)
        lastTimestamp = 0L
        lastStepTimestamp = 0L
        magAccelWindow.clear()
    }
}
