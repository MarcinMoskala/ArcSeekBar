package com.marcinmoskala.arcseekbar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
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

    var onProgressChangedListener: ((seekArc: ArcSeekBar, progress: Int) -> Unit)? = null
    var onStartTrackingTouch: ((seekArc: ArcSeekBar) -> Unit)? = null
    var onStopTrackingTouch: ((seekArc: ArcSeekBar) -> Unit)? = null

    private val a = attrs?.let { context.obtainStyledAttributes(attrs, R.styleable.ArcSeekBar, defStyle, 0) }

    private var mThumb: Drawable = a?.getDrawable(R.styleable.ArcSeekBar_thumb) ?: resources.getDrawable(R.drawable.thumb)

    var maxProgress = a?.getInteger(R.styleable.ArcSeekBar_maxProgress, 100) ?: 100

    var progress: Int = a?.getInteger(R.styleable.ArcSeekBar_progress, 0) ?: 0
        set(progress) {
            field = bound(0, progress, maxProgress)
            onProgressChangedListener?.invoke(this, field)
            drawData = drawData?.copy(progress = field)
            invalidate()
        }

    private val defaultProgressWidth = (4 * context.resources.displayMetrics.density).toInt()
    private var mProgressWidth = a?.getDimension(R.styleable.ArcSeekBar_progressWidth, defaultProgressWidth.toFloat())?.toInt() ?: defaultProgressWidth

    private var mArcWidth = a?.getDimension(R.styleable.ArcSeekBar_arcWidth, 2F)?.toInt() ?: 2

    private var roundedEdges = a?.getBoolean(R.styleable.ArcSeekBar_roundEdges, false) ?: false
        set(value) {
            if (value) {
                mArcPaint.strokeCap = Paint.Cap.ROUND
                mProgressPaint.strokeCap = Paint.Cap.ROUND
            } else {
                mArcPaint.strokeCap = Paint.Cap.SQUARE
                mProgressPaint.strokeCap = Paint.Cap.SQUARE
            }
            field = value
        }

    private var mEnabled = a?.getBoolean(R.styleable.ArcSeekBar_enabled, true) ?: true

    private var mArcPaint: Paint = Paint().apply {
        color = a?.getColor(R.styleable.ArcSeekBar_arcColor, resources.getColor(android.R.color.darker_gray))
                ?: resources.getColor(android.R.color.darker_gray)
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = mArcWidth.toFloat()
        if (roundedEdges) strokeCap = Paint.Cap.ROUND
    }

    private var mProgressPaint: Paint = Paint().apply {
        color = a?.getColor(R.styleable.ArcSeekBar_progressColor, resources.getColor(android.R.color.holo_blue_light))
                ?: resources.getColor(android.R.color.holo_blue_light)
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = mProgressWidth.toFloat()
        if (roundedEdges) strokeCap = Paint.Cap.ROUND
    }

    init {
        a?.recycle()
    }

    private var drawData: ArcSeekBarData? = null

    override fun onDraw(canvas: Canvas) {
        drawData?.run {
            canvas.drawArc(arcRect, startAngle, sweepAngle, false, mArcPaint)
            canvas.drawArc(arcRect, startAngle, progressSweepAngle, false, mProgressPaint)
            if (mEnabled) {
                val thumbHalfheight = mThumb.intrinsicHeight / 2
                val thumbHalfWidth = mThumb.intrinsicWidth / 2
                mThumb.setBounds(thumbX - thumbHalfWidth, thumbY - thumbHalfheight, thumbX + thumbHalfWidth, thumbY + thumbHalfheight)
                mThumb.draw(canvas)
            }
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = View.getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        val width = View.getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val d = progressWidth + 2F
        drawData = ArcSeekBarData(d + paddingLeft, d + paddingTop, width.toFloat() - 2 * d - paddingLeft - paddingRight, height.toFloat() - 2 * d - paddingTop - paddingBottom, progress, maxProgress)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mEnabled) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    onStartTrackingTouch?.invoke(this)
                    updateOnTouch(event)
                }
                MotionEvent.ACTION_MOVE -> updateOnTouch(event)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    onStopTrackingTouch?.invoke(this)
                    isPressed = false
                }
            }
        }
        return mEnabled
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        if (mThumb.isStateful) {
            mThumb.state = drawableState
        }
        invalidate()
    }

    private fun updateOnTouch(event: MotionEvent) {
        val state = drawData ?: return
        val x = event.x
        val y = event.y
        if(y > state.height + state.dy * 2) return
        val distToCircleCenter = sqrt(pow(state.circleCenterX - x.toDouble(), 2.0) + pow(state.circleCenterY - y.toDouble(), 2.0))
        if (abs(distToCircleCenter - state.r) > mThumb.intrinsicHeight) return
        isPressed = true
        val innerWidthHalf = state.width / 2
        val xFromCenter = bound(-innerWidthHalf, x - state.circleCenterX, innerWidthHalf).toDouble()
        val touchAngle = acos(xFromCenter / state.r) + state.alphaRad - PI / 2
        val angleToMax = 1.0 - touchAngle / (2 * state.alphaRad)
        progress = (maxProgress * angleToMax).toInt()
    }

    var progressWidth: Int
        get() = mProgressWidth
        set(mProgressWidth) {
            this.mProgressWidth = mProgressWidth
            mProgressPaint.strokeWidth = mProgressWidth.toFloat()
        }

    var arcWidth: Int
        get() = mArcWidth
        set(mArcWidth) {
            this.mArcWidth = mArcWidth
            mArcPaint.strokeWidth = mArcWidth.toFloat()
        }

    var progressColor: Int
        get() = mProgressPaint.color
        set(color) {
            mProgressPaint.color = color
            invalidate()
        }

    var arcColor: Int
        get() = mArcPaint.color
        set(color) {
            mArcPaint.color = color
            invalidate()
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
}