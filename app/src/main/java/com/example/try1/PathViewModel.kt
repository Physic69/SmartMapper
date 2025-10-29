package com.example.try1

import android.graphics.PointF
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PathViewModel : ViewModel() {

    // --- START OF FIX ---
    // Define the maximum number of points to keep in memory and draw.
    // 500 is a good balance between path length and performance.
    private val windowSize = 500
    // --- END OF FIX ---

    private val _pathPoints = MutableLiveData<List<PointF>>(emptyList())
    val pathPoints: LiveData<List<PointF>> = _pathPoints

    fun addPoint(x: Float, y: Float) {
        val currentPoints = _pathPoints.value?.toMutableList() ?: mutableListOf()
        currentPoints.add(PointF(x, y))

        // --- START OF FIX ---
        // If the list is now larger than our window size,
        // remove the oldest point from the beginning of the list.
        if (currentPoints.size > windowSize) {
            currentPoints.removeAt(0)
        }
        // --- END OF FIX ---

        _pathPoints.value = currentPoints
    }

    fun clearPath() {
        _pathPoints.value = emptyList()
    }
}