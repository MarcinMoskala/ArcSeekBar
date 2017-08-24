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
        defStyle: Int = 0
) : View(context, attrs, defStyle) {

    var onProgressChangedListener: (ProgressListener)? = null
    var onStartTrackingTouch: (ProgressListener)? = null
    var onStopTrackingTouch: (ProgressListener)? = null

    private val a = attrs?.let { context.obtainStyledAttributes(attrs, R.styleable.ArcSeekBar, defStyle, 0) }

    var maxProgress = a.useOrDefault(100) { getInteger(R.styleable.ArcSeekBar_maxProgress, it) }
        set(progress) {
            field = bound(0, progress, Int.MAX_VALUE)
            drawData?.let { drawData = it.copy(maxProgress = progress) }
            invalidate()
        }

    var progress: Int = a.useOrDefault(0) { getInteger(R.styleable.ArcSeekBar_progress, it) }
        set(progress) {
            field = bound(0, progress, maxProgress)
            onProgressChangedListener?.invoke(progress)
            drawData?.let { drawData = it.copy(progress = progress) }
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
            progressBackgroundPaint.strokeWidth = mArcWidth
        }

    var progressColor: Int
        get() = progressPaint.color
        set(color) {
            progressPaint.color = color
            invalidate()
        }

    var progressBackgroundColor: Int
        get() = progressBackgroundPaint.color
        set(color) {
            progressBackgroundPaint.color = color
            invalidate()
        }

    private val thumb: Drawable = a?.getDrawable(R.styleable.ArcSeekBar_thumb) ?: resources.getDrawable(R.drawable.thumb)

    private var roundedEdges = a.useOrDefault(true) { getBoolean(R.styleable.ArcSeekBar_roundEdges, it) }
        set(value) {
            if (value) {
                progressBackgroundPaint.strokeCap = Paint.Cap.ROUND
                progressPaint.strokeCap = Paint.Cap.ROUND
            } else {
                progressBackgroundPaint.strokeCap = Paint.Cap.SQUARE
                progressPaint.strokeCap = Paint.Cap.SQUARE
            }
            field = value
        }

    private var progressBackgroundPaint: Paint = makeProgressPaint(
            color = a.useOrDefault(resources.getColor(android.R.color.darker_gray)) { getColor(R.styleable.ArcSeekBar_progressBackgroundColor, it) },
            width = progressBackgroundWidth
    )

    private var progressPaint: Paint = makeProgressPaint(
            color = a.useOrDefault(resources.getColor(android.R.color.holo_blue_light)) { getColor(R.styleable.ArcSeekBar_progressColor, it) },
            width = progressWidth
    )

    private var mEnabled = a?.getBoolean(R.styleable.ArcSeekBar_enabled, true) ?: true

    init {
        a?.recycle()
    }

    private var drawerDataObservers: List<(ArcSeekBarData) -> Unit> = emptyList()

    private fun doWhenDrawerDataAreReady(f: (ArcSeekBarData) -> Unit) {
        if (drawData != null) f(drawData!!) else drawerDataObservers += f
    }

    private var drawData: ArcSeekBarData? = null
        set(value) {
            field = value ?: return
            val temp = drawerDataObservers.toList()
            temp.forEach { it(value) }
            drawerDataObservers -= temp
        }

    override fun onDraw(canvas: Canvas) {
        drawData?.run {
            canvas.drawArc(arcRect, startAngle, sweepAngle, false, progressBackgroundPaint)
            canvas.drawArc(arcRect, startAngle, progressSweepAngle, false, progressPaint)
            if (mEnabled) drawThumb(canvas)
        }
    }

    private fun ArcSeekBarData.drawThumb(canvas: Canvas) {
        val thumbHalfHeight = thumb.intrinsicHeight / 2
        val thumbHalfWidth = thumb.intrinsicWidth / 2
        thumb.setBounds(thumbX - thumbHalfWidth, thumbY - thumbHalfHeight, thumbX + thumbHalfWidth, thumbY + thumbHalfHeight)
        thumb.draw(canvas)
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

    fun setProgressBackgroundGradient(vararg colors: Int) {
        setGradient(progressBackgroundPaint, *colors)
    }

    fun setProgressGradient(vararg colors: Int) {
        setGradient(progressPaint, *colors)
    }

    private fun setGradient(paint: Paint, vararg colors: Int) {
        doWhenDrawerDataAreReady {
            paint.shader = LinearGradient(it.dx, 0F, it.width, 0F, colors, null, Shader.TileMode.CLAMP)
        }
        invalidate()
    }

    private fun makeProgressPaint(color: Int, width: Float) = Paint().apply {
        this.color = color
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = width
        if (roundedEdges) strokeCap = Paint.Cap.ROUND
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
        progress = bound(0, ((maxProgress + 1) * angleToMax).toInt(), maxProgress)
    }

    override fun isEnabled(): Boolean = mEnabled

    override fun setEnabled(enabled: Boolean) {
        this.mEnabled = enabled
    }

    fun <T, R> T?.useOrDefault(default: R, usage: T.(R) -> R) = if (this == null) default else usage(default)
}