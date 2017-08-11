package com.marcinmoskala.arcseekbar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.lang.Math.*

class ArcSeekBar @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = -1
) : View(context, attrs, defStyle) {

    var onProgressChangedListener: (ProgressListener)? = null
    var onStartTrackingTouch: (ProgressListener)? = null
    var onStopTrackingTouch: (ProgressListener)? = null

    private val a = attrs?.let { context.obtainStyledAttributes(attrs, R.styleable.ArcSeekBar, defStyle, 0) }

    var maxProgress = a.useOrDefault(100) { getInteger(R.styleable.ArcSeekBar_maxProgress, it) }

    var progress: Int = a.useOrDefault(0) { getInteger(R.styleable.ArcSeekBar_progress, it) }
        set(progress) {
            field = bound(0, progress, maxProgress)
            onProgressChangedListener?.invoke(field)
            drawData = drawData?.copy(progress = field)
            invalidate()
        }

    var progressWidth: Float = a.useOrDefault(4 * context.resources.displayMetrics.density) { getDimension(R.styleable.ArcSeekBar_progressWidth, it) }
        set(value) {
            field = value
            progressPaint.strokeWidth = value
        }

    var progressBackgroundWidth: Float = a.useOrDefault(2F) { getDimension(R.styleable.ArcSeekBar_progressBackgroundWidth, it) }
        set(mArcWidth) {
            field = mArcWidth
            arcPaint.strokeWidth = mArcWidth
        }

    var progressColor: Int
        get() = progressPaint.color
        set(color) {
            progressPaint.color = color
            invalidate()
        }

    var arcColor: Int
        get() = arcPaint.color
        set(color) {
            arcPaint.color = color
            invalidate()
        }

    private val thumb: Drawable = a?.getDrawable(R.styleable.ArcSeekBar_progressDrawable) ?: resources.getDrawable(R.drawable.thumb)

    private var roundedEdges = a.useOrDefault(true) { getBoolean(R.styleable.ArcSeekBar_roundEdges, it) }
        set(value) {
            if (value) {
                arcPaint.strokeCap = Paint.Cap.ROUND
                progressPaint.strokeCap = Paint.Cap.ROUND
            } else {
                arcPaint.strokeCap = Paint.Cap.SQUARE
                progressPaint.strokeCap = Paint.Cap.SQUARE
            }
            field = value
        }

    private var mEnabled = a?.getBoolean(R.styleable.ArcSeekBar_enabled, true) ?: true

    private var arcPaint: Paint = Paint().apply {
        color = a.useOrDefault(resources.getColor(android.R.color.darker_gray)) { getColor(R.styleable.ArcSeekBar_progressBackgroundColor, it) }
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = progressBackgroundWidth
        if (roundedEdges) strokeCap = Paint.Cap.ROUND
    }

    fun setArcGradient(vararg colors: Int) {
        doWhenDrawerDataAreReady {
            arcPaint.shader = LinearGradient(0F, 0F, 2 * it.width, 0F, colors, null, Shader.TileMode.CLAMP)
        }
        invalidate()
    }

    private var progressPaint: Paint = Paint().apply {
        color = a.useOrDefault(resources.getColor(android.R.color.holo_blue_light)) { getColor(R.styleable.ArcSeekBar_progressColor, it) }
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = this@ArcSeekBar.progressWidth
        if (roundedEdges) strokeCap = Paint.Cap.ROUND
    }

    fun setProgressGradient(vararg colors: Int) {
        doWhenDrawerDataAreReady {
            progressPaint.shader = LinearGradient(it.dx, 0F, it.width, 0F, colors, null, Shader.TileMode.CLAMP)
        }
        invalidate()
    }

    init {
        a?.recycle()
    }

    private var waitingForDrawerData: List<(ArcSeekBarData) -> Unit> = emptyList()

    private fun doWhenDrawerDataAreReady(f: (ArcSeekBarData) -> Unit) {
        if (drawData != null) f(drawData!!) else waitingForDrawerData += f
    }

    private var drawData: ArcSeekBarData? = null
        set(value) {
            field = value!!
            val temp = waitingForDrawerData.toList()
            temp.forEach { it(value) }
            waitingForDrawerData -= temp
        }

    override fun onDraw(canvas: Canvas) {
        drawData?.run {
            canvas.drawArc(arcRect, startAngle, sweepAngle, false, arcPaint)
            canvas.drawArc(arcRect, startAngle, progressSweepAngle, false, progressPaint)
            if (mEnabled) {
                val thumbHalfheight = thumb.intrinsicHeight / 2
                val thumbHalfWidth = thumb.intrinsicWidth / 2
                thumb.setBounds(thumbX - thumbHalfWidth, thumbY - thumbHalfheight, thumbX + thumbHalfWidth, thumbY + thumbHalfheight)
                thumb.draw(canvas)
            }
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = View.getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        val width = View.getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val dx = maxOf(thumb.intrinsicWidth.toFloat() / 2, this.progressWidth) + 2
        val dy = maxOf(thumb.intrinsicHeight.toFloat() / 2, this.progressWidth) + 2
        val realWidth = width.toFloat() - 2 * dx - paddingLeft - paddingRight
        val realHeight = minOf(height.toFloat() - 2 * dy - paddingTop - paddingBottom, realWidth / 2)
        drawData = ArcSeekBarData(dx + paddingLeft, dy + paddingTop, realWidth, realHeight, progress, maxProgress)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mEnabled) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    onStartTrackingTouch?.invoke(progress)
                    updateOnTouch(event)
                }
                MotionEvent.ACTION_MOVE -> updateOnTouch(event)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    onStopTrackingTouch?.invoke(progress)
                    isPressed = false
                }
            }
        }
        return mEnabled
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        if (thumb.isStateful) {
            thumb.state = drawableState
        }
        invalidate()
    }

    private fun updateOnTouch(event: MotionEvent) {
        val state = drawData ?: return
        val x = event.x
        val y = event.y
        if (y > state.height + state.dy * 2) return
        val distToCircleCenter = sqrt(pow(state.circleCenterX - x.toDouble(), 2.0) + pow(state.circleCenterY - y.toDouble(), 2.0))
        if (abs(distToCircleCenter - state.r) > thumb.intrinsicHeight) return
        isPressed = true
        val innerWidthHalf = state.width / 2
        val xFromCenter = bound(-innerWidthHalf, x - state.circleCenterX, innerWidthHalf).toDouble()
        val touchAngle = acos(xFromCenter / state.r) + state.alphaRad - PI / 2
        val angleToMax = 1.0 - touchAngle / (2 * state.alphaRad)
        progress = (maxProgress * angleToMax).toInt()
    }

    override fun isEnabled(): Boolean = mEnabled

    override fun setEnabled(enabled: Boolean) {
        this.mEnabled = enabled
    }

    private fun <T : Number> bound(min: T, value: T, max: T) = when {
        value.toDouble() > max.toDouble() -> max
        value.toDouble() < min.toDouble() -> min
        else -> value
    }

    fun <T, R> T?.useOrDefault(default: R, usage: T.(R) -> R) = if (this == null) default else usage(default)
}