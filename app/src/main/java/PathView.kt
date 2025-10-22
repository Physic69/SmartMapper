package com.example.try1

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet // Import AttributeSet
import android.view.View

// THE FIX: Add @JvmOverloads and update the constructor to include AttributeSet
class PathView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.BLUE
        strokeWidth = 10f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }

    private val path = Path()
    private var isFirstPoint = true
    private val scaleFactor = 50f

    fun addPoint(x: Float, y: Float) {
        val screenX = x * scaleFactor
        val screenY = y * -1 * scaleFactor

        if (isFirstPoint) {
            path.moveTo(screenX, screenY)
            isFirstPoint = false
        } else {
            path.lineTo(screenX, screenY)
        }
        invalidate()
    }

    fun clearPath() {
        path.reset()
        isFirstPoint = true
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.translate(width / 2f, height / 2f)
        canvas.drawPath(path, paint)
    }
}