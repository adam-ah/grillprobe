package app.grillprobe

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class BatteryView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val outline = context.strokePaint(Palette.SECONDARY, OUTLINE_WIDTH)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val body = RectF()
    private val level = RectF()
    private val tip = RectF()
    private var percent = 0

    fun setLevel(value: Int) {
        percent = value.coerceIn(0, FULL)
        contentDescription = context.getString(R.string.battery, percent)
        layoutRects()
        invalidate()
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int,
    ) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutRects()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = context.dpf(CORNER)
        canvas.drawRoundRect(body, radius, radius, outline)
        fill.color = if (percent <= LOW) Palette.RED else Palette.LABEL
        canvas.drawRoundRect(level, radius / 2, radius / 2, fill)
        fill.color = Palette.SECONDARY
        canvas.drawRoundRect(tip, radius / 2, radius / 2, fill)
    }

    private fun layoutRects() {
        val stroke = context.dpf(OUTLINE_WIDTH)
        val tipWidth = context.dpf(TIP_WIDTH)
        val inset = context.dpf(INSET)
        body.set(stroke / 2, stroke / 2, width - tipWidth - stroke, height - stroke / 2)
        val inner = body.width() - inset * 2
        level.set(
            body.left + inset,
            body.top + inset,
            body.left + inset + inner * percent / FULL,
            body.bottom - inset
        )
        tip.set(width - tipWidth, height * TIP_TOP, width.toFloat(), height * (1 - TIP_TOP))
    }

    private companion object {
        const val FULL = 100
        const val LOW = 20
        const val OUTLINE_WIDTH = 1.2f
        const val TIP_WIDTH = 1.6f
        const val INSET = 1.8f
        const val CORNER = 3f
        const val TIP_TOP = 0.32f
    }
}
