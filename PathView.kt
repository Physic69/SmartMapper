package com.example.try1

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View

class PathView(context: Context) : View(context) {

    private val paint = Paint().apply {
        color = Color.BLUE
        strokeWidth = 10f // The thickness of the path line
        style = Paint.Style.STROKE // Draw the outline of the path
        strokeJoin = Paint.Join.ROUND // Makes the path's corners smooth
    }

    private val path = Path()
    private var isFirstPoint = true
    private val scaleFactor = 50f // Pixels per meter. Adjust this to zoom in/out.

    /**
     * Adds a new point to the path and triggers a redraw.
     */
    fun addPoint(x: Float, y: Float) {
        // We invert 'y' because on a phone screen, the Y-axis goes down,
        // but in standard coordinates, it goes up.
        val screenX = x * scaleFactor
        val screenY = y * -1 * scaleFactor

        if (isFirstPoint) {
            path.moveTo(screenX, screenY)
            isFirstPoint = false
        } else {
            path.lineTo(screenX, screenY)
        }
        // Invalidate tells the view that it needs to be redrawn.
        invalidate()
    }

    /**
     * Clears the path to start a new drawing.
     */
    fun clearPath() {
        path.reset()
        isFirstPoint = true
        invalidate()
    }

    /**
     * This is where the actual drawing happens.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // We move the drawing origin (0,0) to the center of the view.
        // This makes the path start drawing from the middle of the screen.
        canvas.translate(width / 2f, height / 2f)

        // Draw the path you've built.
        canvas.drawPath(path, paint)
    }
}