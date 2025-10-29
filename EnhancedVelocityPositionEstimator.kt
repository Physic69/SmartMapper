package com.example.try1

import kotlin.math.abs
import kotlin.math.sqrt

class EnhancedVelocityPositionEstimator(
    private val accelNoiseThreshold: Float = 0.05f,
    private val stillnessVarianceThreshold: Float = 0.01f,
    private val stillnessMagnitudeThreshold: Float = 0.4f
) {
    private enum class MotionState { STATIONARY, MOVING }
    private var motionState = MotionState.STATIONARY

    // The device must be still for this duration in seconds to be considered stopped.
    private val requiredStillnessDurationSec = 0.15f
    private var stillnessEntryTimestamp: Long = 0L

    private var velocity = FloatArray(3) { 0f }
    private var position = FloatArray(3) { 0f }
    private var lastTimestamp: Long = 0L
    private val worldAccelWindow = mutableListOf<FloatArray>()
    private var gravityEstimate = FloatArray(3) { 0f }
    private val gravityFilterAlpha = 0.8f
    private val EARTH_GRAVITY = 9.8f

    // DEBUGGING: Public property to hold the latest variance value
    var lastCalculatedVariance: Float = 0f
        private set

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
        if (dt <= 0) return Pair(velocity.copyOf(), position.copyOf())
        lastTimestamp = timestamp

        val accelWorld = rotateToWorld(accelBody, rotationMatrix)
        worldAccelWindow.add(accelWorld.copyOf())
        if (worldAccelWindow.size > 20) worldAccelWindow.removeAt(0)

        val isPotentiallyStill = isDevicePotentiallyStationary()
        updateMotionState(isPotentiallyStill, timestamp)

        if (motionState == MotionState.STATIONARY) {
            velocity = FloatArray(3) { 0f }
            for (i in 0..2) {
                gravityEstimate[i] = gravityFilterAlpha * gravityEstimate[i] + (1 - gravityFilterAlpha) * accelWorld[i]
            }
        } else { // MOVING
            val linearAccel = FloatArray(3)
            for (i in 0..2) {
                linearAccel[i] = accelWorld[i] - gravityEstimate[i]
            }
            for (i in 0..2) {
                // This is pure integration, no damping.
                val a = if (abs(linearAccel[i]) < accelNoiseThreshold) 0f else linearAccel[i]
                velocity[i] += a * dt
            }
        }

        for (i in 0..2) {
            position[i] += velocity[i] * dt
        }

        return Pair(velocity.copyOf(), position.copyOf())
    }

    private fun updateMotionState(isPotentiallyStill: Boolean, timestamp: Long) {
        if (isPotentiallyStill) {
            if (stillnessEntryTimestamp == 0L) {
                stillnessEntryTimestamp = timestamp
            }
            val stillnessDuration = (timestamp - stillnessEntryTimestamp) / 1_000_000_000.0f
            if (stillnessDuration >= requiredStillnessDurationSec) {
                motionState = MotionState.STATIONARY
            }
        } else {
            stillnessEntryTimestamp = 0L
            motionState = MotionState.MOVING
        }
    }

    private fun isDevicePotentiallyStationary(): Boolean {
        if (worldAccelWindow.size < 20) return false
        val totalVariance = worldAccelWindow.map { it[0] }.let { variance(it) } +
                worldAccelWindow.map { it[1] }.let { variance(it) } +
                worldAccelWindow.map { it[2] }.let { variance(it) }

        // DEBUGGING: Store the calculated variance so the service can access it.
        lastCalculatedVariance = totalVariance

        if (totalVariance > stillnessVarianceThreshold) return false

        val avgX = worldAccelWindow.map { it[0] }.average().toFloat()
        val avgY = worldAccelWindow.map { it[1] }.average().toFloat()
        val avgZ = worldAccelWindow.map { it[2] }.average().toFloat()
        val magnitude = sqrt(avgX * avgX + avgY * avgY + avgZ * avgZ)

        return abs(magnitude - EARTH_GRAVITY) < stillnessMagnitudeThreshold
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