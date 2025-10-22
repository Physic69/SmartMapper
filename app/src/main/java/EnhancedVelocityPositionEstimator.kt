package com.example.try1

import android.hardware.SensorManager
import kotlin.math.abs

class EnhancedVelocityPositionEstimator(
    private val accelNoiseThreshold: Float = 0.05f,
    private val stillnessVarianceThreshold: Float = 0.01f
) {
    private var velocity = FloatArray(3) { 0f }
    private var position = FloatArray(3) { 0f }
    private var lastTimestamp: Long = 0L

    private val worldAccelWindow = mutableListOf<FloatArray>()
    private var gravityEstimate = FloatArray(3) { 0f }
    // We keep the alpha but will only use it when the device is still
    private val gravityFilterAlpha = 0.8f

    fun update(
        accelBody: FloatArray,
        rotationMatrix: FloatArray,
        timestamp: Long
    ): Pair<FloatArray, FloatArray> {
        if (lastTimestamp == 0L) {
            lastTimestamp = timestamp
            gravityEstimate = rotateToWorld(accelBody, rotationMatrix)
            return Pair(velocity.copyOf(), position.copyOf())
        }
        val dt = (timestamp - lastTimestamp) / 1_000_000_000.0f
        if (dt > 0.5f) { // Reset if there's a large gap in sensor readings
            lastTimestamp = timestamp
            return Pair(velocity.copyOf(), position.copyOf())
        }
        lastTimestamp = timestamp

        val accelWorld = rotateToWorld(accelBody, rotationMatrix)

        // Add current acceleration to the window for stillness detection
        worldAccelWindow.add(accelWorld.copyOf())
        if (worldAccelWindow.size > 15) {
            worldAccelWindow.removeAt(0)
        }

        val isStill = isDeviceStationary()

        // --- START OF GRAVITY FILTER FIX ---
        // Only update the gravity estimate if the device is stationary.
        // This prevents sustained linear acceleration from corrupting the gravity estimate.
        if (isStill) {
            for (i in 0..2) {
                gravityEstimate[i] = gravityFilterAlpha * gravityEstimate[i] + (1 - gravityFilterAlpha) * accelWorld[i]
            }
        }
        // --- END OF GRAVITY FILTER FIX ---

        // Now, subtract the stable gravity estimate to get the user's linear acceleration
        val linearAccel = FloatArray(3)
        for (i in 0..2) {
            linearAccel[i] = accelWorld[i] - gravityEstimate[i]
        }


        if (isStill) {
            // If the device is still, force velocity to zero to stop drift.
            velocity = FloatArray(3) { 0f }
        } else {
            // If moving, integrate the clean linear acceleration to get velocity
            for (i in 0..2) {
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
        val variance = (worldAccelWindow.map { it[0] }.let { variance(it) } +
                worldAccelWindow.map { it[1] }.let { variance(it) } +
                worldAccelWindow.map { it[2] }.let { variance(it) })
        return variance < stillnessVarianceThreshold
    }

    private fun variance(list: List<Float>): Float {
        if (list.size < 2) return 0f
        val mean = list.average().toFloat()
        return list.fold(0f) { acc, v -> acc + (v - mean) * (v - mean) } / (list.size - 1)
    }

    private fun rotateToWorld(vectorBody: FloatArray, rotationMatrix: FloatArray): FloatArray {
        val vectorWorld = FloatArray(3)
        vectorWorld[0] = rotationMatrix[0] * vectorBody[0] + rotationMatrix[1] * vectorBody[1] + rotationMatrix[2] * vectorBody[2]
        vectorWorld[1] = rotationMatrix[3] * vectorBody[0] + rotationMatrix[4] * vectorBody[1] + rotationMatrix[5] * vectorBody[2]
        vectorWorld[2] = rotationMatrix[6] * vectorBody[0] + rotationMatrix[7] * vectorBody[1] + rotationMatrix[8] * vectorBody[2]
        return vectorWorld
    }
}