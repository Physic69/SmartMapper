package com.example.try1

import kotlin.math.abs
import kotlin.math.sqrt

// THE FIX: The thresholds are now tunable parameters in the constructor
class EnhancedVelocityPositionEstimator(
    private val accelNoiseThreshold: Float = 0.05f,
    private val stillnessVarianceThreshold: Float = 0.015f, // Slightly increased for robustness
    private val stillnessMagnitudeThreshold: Float = 0.2f // New threshold for gravity check
) {
    private var velocity = FloatArray(3) { 0f }
    private var position = FloatArray(3) { 0f }
    private var lastTimestamp: Long = 0L

    private val worldAccelWindow = mutableListOf<FloatArray>()
    private var gravityEstimate = FloatArray(3) { 0f }
    private val gravityFilterAlpha = 0.8f
    private val EARTH_GRAVITY = 9.8f

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
        if (dt > 0.5f) {
            lastTimestamp = timestamp
            return Pair(velocity.copyOf(), position.copyOf())
        }
        lastTimestamp = timestamp

        val accelWorld = rotateToWorld(accelBody, rotationMatrix)

        worldAccelWindow.add(accelWorld.copyOf())
        if (worldAccelWindow.size > 20) { // Use a slightly larger window
            worldAccelWindow.removeAt(0)
        }

        val isStill = isDeviceStationary()

        if (isStill) {
            for (i in 0..2) {
                gravityEstimate[i] = gravityFilterAlpha * gravityEstimate[i] + (1 - gravityFilterAlpha) * accelWorld[i]
            }
        }

        val linearAccel = FloatArray(3)
        for (i in 0..2) {
            linearAccel[i] = accelWorld[i] - gravityEstimate[i]
        }

        if (isStill) {
            velocity = FloatArray(3) { 0f }
        } else {
            for (i in 0..2) {
                val a = if (abs(linearAccel[i]) < accelNoiseThreshold) 0f else linearAccel[i]
                velocity[i] += a * dt
            }
        }

        for (i in 0..2) {
            position[i] += velocity[i] * dt
        }

        return Pair(velocity.copyOf(), position.copyOf())
    }

    // --- START OF THE FIX ---
    private fun isDeviceStationary(): Boolean {
        if (worldAccelWindow.size < 20) return false

        // 1. Check if the acceleration signal is stable (low variance)
        val xVar = variance(worldAccelWindow.map { it[0] })
        val yVar = variance(worldAccelWindow.map { it[1] })
        val zVar = variance(worldAccelWindow.map { it[2] })
        val totalVariance = xVar + yVar + zVar
        if (totalVariance > stillnessVarianceThreshold) {
            return false
        }

        // 2. Check if the average acceleration magnitude is close to Earth's gravity
        val avgX = worldAccelWindow.map { it[0] }.average().toFloat()
        val avgY = worldAccelWindow.map { it[1] }.average().toFloat()
        val avgZ = worldAccelWindow.map { it[2] }.average().toFloat()
        val magnitude = sqrt(avgX * avgX + avgY * avgY + avgZ * avgZ)

        // It's only truly "still" if the signal is stable AND the magnitude is just gravity.
        return abs(magnitude - EARTH_GRAVITY) < stillnessMagnitudeThreshold
    }
    // --- END OF THE FIX ---

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
