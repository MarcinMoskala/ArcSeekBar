package com.marcinmoskala.arcseekbar

import android.graphics.RectF

internal data class ArcSeekBarData(
        val dx: Float,
        val dy: Float,
        val width: Float,
        val height: Float,
        val progress: Int,
        val maxProgress: Int
) {
    private val pi = Math.PI.toFloat()
    private val zero = 0.0001F
    val r: Float = height / 2 + width * width / 8 / height
    private val circleCenterX: Float = width / 2 + dy
    private val circleCenterY: Float = r + dx
    private val alphaRad: Float = bound(zero, Math.acos((r - height).toDouble() / r).toFloat(), 2 * pi)
    val arcRect: RectF = RectF(circleCenterX - r, circleCenterY - r, circleCenterX + r, circleCenterY + r)
    val startAngle: Float = bound(180F, 270 - alphaRad / 2 / pi * 360F, 360F)
    val sweepAngle: Float = bound(zero, (2F * alphaRad) / 2 / pi * 360F, 180F)
    val progressSweepRad = if(maxProgress == 0) zero else bound(zero, progress.toFloat() / maxProgress * 2 * alphaRad, 2 * pi)
    val progressSweepAngle: Float = progressSweepRad / 2 / pi * 360F
    val thumbX: Int = (r * Math.cos(alphaRad + Math.PI / 2 - progressSweepRad).toFloat() + circleCenterX).toInt()
    val thumbY: Int = (-r * Math.sin(alphaRad + Math.PI / 2 - progressSweepRad).toFloat() + circleCenterY).toInt()

    fun progressFromClick(x: Float, y: Float, thumbHeight: Int): Int? {
        if (y > height + dy * 2) return null
        val distToCircleCenter = Math.sqrt(Math.pow(circleCenterX - x.toDouble(), 2.0) + Math.pow(circleCenterY - y.toDouble(), 2.0))
        if (Math.abs(distToCircleCenter - r) > thumbHeight) return null
        val innerWidthHalf = width / 2
        val xFromCenter = bound(-innerWidthHalf, x - circleCenterX, innerWidthHalf).toDouble()
        val touchAngle = Math.acos(xFromCenter / r) + alphaRad - Math.PI / 2
        val angleToMax = 1.0 - touchAngle / (2 * alphaRad)
        return bound(0, ((maxProgress + 1) * angleToMax).toInt(), maxProgress)
    }
}