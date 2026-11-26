package app.grillprobe

import android.content.Context
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader

fun Context.strokePaint(
    color: Int,
    widthDp: Float,
): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = dpf(widthDp)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

fun Context.textPaint(
    color: Int,
    align: Paint.Align,
): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textAlign = align
        textSize = dpf(TypeScale.CAPTION2)
        typeface = Weight.MEDIUM.typeface
    }

fun areaGradient(
    top: Float,
    bottom: Float,
): LinearGradient = LinearGradient(0f, top, 0f, bottom, Palette.FILL_TOP, Palette.FILL_BOTTOM, Shader.TileMode.CLAMP)
