package com.example.try1

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class PathView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.BLUE
        strokeWidth = 5f // Thinner line for better scaling
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }

    private val path = Path()
    // --- START OF FIX ---
    private val points = mutableListOf<PointF>()
    private val pathBounds = RectF()
    // --- END OF FIX ---

    fun addPoint(x: Float, y: Float) {
        // We store the raw coordinates. Scaling is handled during the draw phase.
        points.add(PointF(x, y))
        rebuildPath()
        invalidate() // Request a redraw
    }

    fun clearPath() {
        points.clear()
        path.reset()
        pathBounds.setEmpty()
        invalidate()
    }

    private fun rebuildPath() {
        path.reset()
        if (points.isNotEmpty()) {
            path.moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }
            // Calculate the bounding box of the path
            path.computeBounds(pathBounds, true)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (path.isEmpty) return

        // --- START OF FIX ---
        // 1. Calculate the required scale to fit the path
        val viewWidth = width.toFloat() - paddingLeft - paddingRight
        val viewHeight = height.toFloat() - paddingTop - paddingBottom
        val pathWidth = if (pathBounds.width() == 0f) 1f else pathBounds.width()
        val pathHeight = if (pathBounds.height() == 0f) 1f else pathBounds.height()

        val scaleX = viewWidth / pathWidth
        val scaleY = viewHeight / pathHeight
        val scaleFactor = min(scaleX, scaleY) * 0.9f // Use 90% of the space

        // 2. Center the drawing
        // Move canvas origin to the center of the view
        canvas.translate(viewWidth / 2f, viewHeight / 2f)
        // Apply the dynamic scale
        canvas.scale(scaleFactor, -scaleFactor) // -scaleFactor to flip Y-axis
        // Move the canvas so the center of the path is at the origin
        canvas.translate(-pathBounds.centerX(), -pathBounds.centerY())
        // --- END OF FIX ---

        canvas.drawPath(path, paint)
    }
}
