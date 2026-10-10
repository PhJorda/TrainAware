package com.example.trainaware

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt

class HeartbeatView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val darkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#373737".toColorInt()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }

    private val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#B4B4B4".toColorInt()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }

    private var activeIndex = 0
    private var isRunning = false

    private val directions = arrayOf(
        floatArrayOf(-0.7071f, -0.7071f), // Top-Left
        floatArrayOf(0.7071f, -0.7071f),  // Top-Right
        floatArrayOf(0.7071f, 0.7071f),   // Bottom-Right
        floatArrayOf(-0.7071f, 0.7071f)   // Bottom-Left
    )

    private val updateRunnable = object : Runnable {
        override fun run() {
            activeIndex = (activeIndex + 1) % 4
            invalidate()
            if (isRunning) {
                postDelayed(this, 300)
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    fun startAnimation() {
        if (!isRunning) {
            isRunning = true
            post(updateRunnable)
        }
    }

    fun stopAnimation() {
        isRunning = false
        removeCallbacks(updateRunnable)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val cx = w / 2f
        val cy = h / 2f
        val size = minOf(w, h)

        val strokeWidth = size * 0.14f
        darkPaint.strokeWidth = strokeWidth
        lightPaint.strokeWidth = strokeWidth

        val rInner = size * 0.16f
        val rOuter = size * 0.38f

        for (i in 0 until 4) {
            val dir = directions[i]
            val dx = dir[0]
            val dy = dir[1]
            val startX = cx + rInner * dx
            val startY = cy + rInner * dy
            val endX = cx + rOuter * dx
            val endY = cy + rOuter * dy

            val paint = if (i == activeIndex) lightPaint else darkPaint
            canvas.drawLine(startX, startY, endX, endY, paint)
        }
    }
}
