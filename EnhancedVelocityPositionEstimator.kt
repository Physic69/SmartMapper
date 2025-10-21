package com.example.try1

import android.util.Log
import kotlin.math.*

class EnhancedVelocityPositionEstimator(
    private val accelNoiseThreshold: Float = 0.05f,
    private val stillnessVarianceThreshold: Float = 0.01f
) {
    private var velocity = FloatArray(3) { 0f }
    private var position = FloatArray(3) { 0f }
    private var lastTimestamp: Long = 0L

    private val worldAccelWindow = mutableListOf<FloatArray>()

    // NEW: Use a low-pass filter to get a stable estimate of gravity's direction.
    private var gravityEstimate = FloatArray(3) { 0f }
    private val gravityFilterAlpha = 0.98f // A high alpha means the filter adapts very slowly.

    fun update(
        accelBody: FloatArray,
        orientation: Triple<Float, Float, Float>,
        timestamp: Long
    ): Pair<FloatArray, FloatArray> {
        if (lastTimestamp == 0L) {
            lastTimestamp = timestamp
            // Initialize gravity estimate with the first reading
            gravityEstimate = rotateToWorld(accelBody, orientation)
            return Pair(velocity.copyOf(), position.copyOf())
        }
        val dt = (timestamp - lastTimestamp) / 1_000_000_000.0f
        lastTimestamp = timestamp

        // Rotate acceleration from phone's coordinate system to the world's
        val accelWorld = rotateToWorld(accelBody, orientation)

        // --- NEW: Isolate Gravity from Linear Acceleration ---
        // 1. Update our stable gravity estimate using the low-pass filter.
        for (i in 0..2) {
            gravityEstimate[i] = gravityFilterAlpha * gravityEstimate[i] + (1 - gravityFilterAlpha) * accelWorld[i]
        }

        // 2. Subtract the stable gravity estimate to get the user's actual movement (linear acceleration).
        val linearAccel = FloatArray(3)
        for (i in 0..2) {
            linearAccel[i] = accelWorld[i] - gravityEstimate[i]
        }

        // --- Use the clean linearAccel for all further calculations ---
        worldAccelWindow.add(linearAccel.copyOf())
        if (worldAccelWindow.size > 15) {
            worldAccelWindow.removeAt(0)
        }

        val isStill = isDeviceStationary()

        if (isStill) {
            // If the device is still, force velocity to zero to stop drift.
            velocity = FloatArray(3) { 0f }
        } else {
            // If moving, integrate the clean linear acceleration to get velocity
            for (i in 0..2) {
                // Apply a noise threshold to avoid integrating tiny values
                val a = if (abs(linearAccel[i]) < accelNoiseThreshold) 0f else linearAccel[i]
                velocity[i] += a * dt
            }
        }

        // Always update position from velocity
        for (i in 0..2) {
            position[i] += velocity[i] * dt
        }

        return Pair(velocity.copyOf(), position.copyOf())
    }

    private fun isDeviceStationary(): Boolean {
        if (worldAccelWindow.size < 15) return false

        val xVar = variance(worldAccelWindow.map { it[0] })
        val yVar = variance(worldAccelWindow.map { it[1] })
        val zVar = variance(worldAccelWindow.map { it[2] })

        return (xVar < stillnessVarianceThreshold &&
                yVar < stillnessVarianceThreshold &&
                zVar < stillnessVarianceThreshold)
    }

    private fun variance(list: List<Float>): Float {
        if (list.size < 2) return 0f
        val mean = list.average().toFloat()
        return list.fold(0f) { acc, v -> acc + (v - mean) * (v - mean) } / (list.size - 1)
    }

    private fun rotateToWorld(accel: FloatArray, orientation: Triple<Float, Float, Float>): FloatArray {
        val (roll, pitch, yaw) = orientation
        val sinR = sin(roll); val cosR = cos(roll)
        val sinP = sin(pitch); val cosP = cos(pitch)
        val sinY = sin(yaw); val cosY = cos(yaw)
        val rot = arrayOf(
            floatArrayOf(cosP * cosY, sinR * sinP * cosY - cosR * sinY, cosR * sinP * cosY + sinR * sinY),
            floatArrayOf(cosP * sinY, sinR * sinP * sinY + cosR * cosY, cosR * sinP * sinY - sinR * cosY),
            floatArrayOf(-sinP, sinR * cosP, cosR * cosP)
        )
        val out = FloatArray(3) { 0f }
        for (i in 0..2) for (j in 0..2) out[i] += rot[i][j] * accel[j]
        return out
    }
}