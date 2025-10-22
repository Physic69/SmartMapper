package com.example.try1

import android.graphics.PointF
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PathViewModel : ViewModel() {

    private val _pathPoints = MutableLiveData<List<PointF>>(emptyList())
    val pathPoints: LiveData<List<PointF>> = _pathPoints

    fun addPoint(x: Float, y: Float) {
        val currentPoints = _pathPoints.value?.toMutableList() ?: mutableListOf()
        currentPoints.add(PointF(x, y))
        _pathPoints.value = currentPoints
    }

    fun clearPath() {
        _pathPoints.value = emptyList()
    }
}