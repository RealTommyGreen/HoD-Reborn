package com.hod.reborn.touch

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.view.View

class TouchOverlayLockButtonView(context: Context) : View(context) {

    private var locked = true

    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.FILL
    }

    init {
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 12.dpToPx().toFloat()
            setColor(0xAA111820.toInt())
            setStroke(1.dpToPx(), 0x66FFFFFF)
        }
        alpha = 0.34f
    }

    fun setLocked(locked: Boolean) {
        this.locked = locked
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val iconName = if (locked) "lock_keyhole" else "lock_keyhole_open"
        val shapeBounds = RectF(0f, 0f, width.toFloat(), height.toFloat())
        SvgIconManager.renderIcon(canvas, context, iconName, shapeBounds, iconPaint)
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
