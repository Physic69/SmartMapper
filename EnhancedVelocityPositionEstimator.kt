package com.example.try1

import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.sqrt

class EnhancedVelocityPositionEstimator(
    private val accelNoiseThreshold: Float = 0.05f,
    private val stillnessVarianceThreshold: Float = 0.01f
) {
    private var velocity = FloatArray(3) { 0f }
    private var position = FloatArray(3) { 0f }
    private var lastTimestamp: Long = 0L

    private val worldAccelWindow = mutableListOf<FloatArray>()
    private var gravityEstimate = FloatArray(3) { 0f }
    private val gravityFilterAlpha = 0.98f

    // NEW: Update function that accepts a rotation matrix
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

        for (i in 0..2) {
            gravityEstimate[i] = gravityFilterAlpha * gravityEstimate[i] + (1 - gravityFilterAlpha) * accelWorld[i]
        }

        val linearAccel = FloatArray(3)
        for (i in 0..2) {
            linearAccel[i] = accelWorld[i] - gravityEstimate[i]
        }

        worldAccelWindow.add(linearAccel.copyOf())
        if (worldAccelWindow.size > 15) {
            worldAccelWindow.removeAt(0)
        }

        if (isDeviceStationary()) {
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

    // NEW: rotateToWorld function using the rotation matrix
    private fun rotateToWorld(vectorBody: FloatArray, rotationMatrix: FloatArray): FloatArray {
        val vectorWorld = FloatArray(3)
        // The rotation matrix is 3x3, stored in a 9-element array (row-major).
        // [ m0 m1 m2 ]   [ v0 ]   [ m0*v0 + m1*v1 + m2*v2 ]
        // [ m3 m4 m5 ] * [ v1 ] = [ m3*v0 + m4*v1 + m5*v2 ]
        // [ m6 m7 m8 ]   [ v2 ]   [ m6*v0 + m7*v1 + m8*v2 ]
        vectorWorld[0] = rotationMatrix[0] * vectorBody[0] + rotationMatrix[1] * vectorBody[1] + rotationMatrix[2] * vectorBody[2]
        vectorWorld[1] = rotationMatrix[3] * vectorBody[0] + rotationMatrix[4] * vectorBody[1] + rotationMatrix[5] * vectorBody[2]
        vectorWorld[2] = rotationMatrix[6] * vectorBody[0] + rotationMatrix[7] * vectorBody[1] + rotationMatrix[8] * vectorBody[2]
        return vectorWorld
    }
}