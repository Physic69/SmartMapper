package com.example.try1

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class PathView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private lateinit var bitmap: Bitmap
    private lateinit var bitmapCanvas: Canvas

    private val paint = Paint().apply {
        color = Color.BLUE
        strokeWidth = 10f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var hasFirstPoint = false

    // A fixed scale factor. Increase this to "zoom in".
    private val scaleFactor = 50f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            // Create the bitmap and canvas once the view has a size.
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bitmapCanvas = Canvas(bitmap)
        }
    }

    fun addPoint(x: Float, y: Float) {
        if (!::bitmapCanvas.isInitialized) return

        // Translate the coordinate to the center of the screen
        val screenX = (x * scaleFactor) + (width / 2f)
        val screenY = (y * -1 * scaleFactor) + (height / 2f) // Invert Y-axis

        if (!hasFirstPoint) {
            lastX = screenX
            lastY = screenY
            hasFirstPoint = true
            return
        }

        // Draw the new line segment onto our off-screen canvas
        bitmapCanvas.drawLine(lastX, lastY, screenX, screenY, paint)

        // Update the last point
        lastX = screenX
        lastY = screenY

        // Request a redraw to show the updated bitmap
        invalidate()
    }

    fun clearPath() {
        if (::bitmap.isInitialized) {
            // Erase the bitmap
            bitmap.eraseColor(Color.TRANSPARENT)
        }
        hasFirstPoint = false
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (::bitmap.isInitialized) {
            // The only job here is to draw our pre-made bitmap to the screen.
            canvas.drawBitmap(bitmap, 0f, 0f, null)
        }
    }
}