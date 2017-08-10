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
    val r: Float = height / 2 + width * width / 8 / height
    val circleCenterX: Float = width / 2 + dy
    val circleCenterY: Float = r + dx
    val alphaRad: Float = Math.acos((r - height).toDouble() / r).toFloat()
    val arcRect: RectF = RectF(circleCenterX - r, circleCenterY - r, circleCenterX + r, circleCenterY + r)
    val startAngle: Float = 270 - alphaRad / 2 / pi * 360F
    val sweepAngle: Float = (2F * alphaRad) / 2 / pi * 360F
    val progressSweepRad = progress.toFloat() / maxProgress * 2 * alphaRad
    val progressSweepAngle: Float = progressSweepRad / 2 / pi * 360F
    val thumbX: Int = (r * Math.cos(alphaRad + Math.PI / 2 - progressSweepRad).toFloat() + circleCenterX).toInt()
    val thumbY: Int = (-r * Math.sin(alphaRad + Math.PI / 2 - progressSweepRad).toFloat() + circleCenterY).toInt()
}