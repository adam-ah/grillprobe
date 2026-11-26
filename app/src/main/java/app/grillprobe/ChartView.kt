package app.grillprobe

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class ChartView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private class Label(
        val x: Float,
        val y: Float,
        val text: String,
    )

    private class LegendItem(
        val text: String,
        val color: Int,
        var x: Float = 0f,
        var isVisible: Boolean = true,
    )

    private var samples: List<Sample> = emptyList()
    private var target: Double? = null
    private var forecast: Forecast? = null
    private var unit = TemperatureUnit.CELSIUS
    private var geometry: ChartGeometry? = null
    private var valueLabels: List<Label> = emptyList()
    private var timeLabels: List<Label> = emptyList()
    private var targetLabel: Label? = null
    private val targetPill = RectF()
    private var current: PointF? = null
    private var etaPoint: PointF? = null
    private var etaLabel: Label? = null
    private var emptyText = ""
    private val measured = Path()
    private val area = Path()
    private val projection = Path()
    private val clock = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val legend =
        listOf(
            LegendItem(context.getString(R.string.legend_measured), Palette.TINT),
            LegendItem(context.getString(R.string.legend_predicted), Palette.PREDICTION),
            LegendItem(context.getString(R.string.target), Palette.RED),
        )

    private val gridPaint = context.strokePaint(Palette.SEPARATOR, 1f)
    private val axisPaint = context.textPaint(Palette.SECONDARY, Paint.Align.RIGHT)
    private val timePaint = context.textPaint(Palette.SECONDARY, Paint.Align.CENTER)
    private val legendPaint = context.textPaint(Palette.SECONDARY, Paint.Align.LEFT)
    private val emptyPaint = context.textPaint(Palette.SECONDARY, Paint.Align.CENTER).apply {
        textSize = context.dpf(TypeScale.SUBHEAD)
    }
    private val pillTextPaint = context.textPaint(Palette.WHITE, Paint.Align.CENTER).apply {
        typeface = Weight.SEMIBOLD.typeface
    }
    private val measuredPaint = context.strokePaint(Palette.TINT, MEASURED_WIDTH)
    private val areaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val projectionPaint =
        context.strokePaint(Palette.PREDICTION, PROJECTION_WIDTH).apply {
            pathEffect = DashPathEffect(floatArrayOf(context.dpf(DASH), context.dpf(DASH)), 0f)
        }
    private val targetPaint =
        context.strokePaint(Palette.RED, TARGET_WIDTH).apply {
            pathEffect = DashPathEffect(floatArrayOf(context.dpf(DASH_LONG), context.dpf(DASH)), 0f)
        }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ringPaint = context.strokePaint(Palette.BLUE, RING_WIDTH)
    private val etaTextPaint =
        context.textPaint(Palette.BLUE, Paint.Align.CENTER).apply { typeface = Weight.SEMIBOLD.typeface }

    fun update(
        samples: List<Sample>,
        target: Double?,
        prediction: Prediction?,
        unit: TemperatureUnit,
        emptyText: String,
    ) {
        this.emptyText = emptyText
        this.samples = samples.map { (time, celsius) -> time to unit.fromCelsius(celsius) }
        this.target = target?.let { unit.fromCelsius(it) }
        this.unit = unit
        forecast =
            prediction?.takeIf { it.heating }?.let { heating ->
                Forecast(heating.etas.best) { minutes -> unit.fromCelsius(heating.projectAt(minutes)) }
            }
        rebuild()
        invalidate()
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int,
    ) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuild()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val g = geometry
        if (g == null) {
            canvas.drawText(emptyText, width / 2f, height / 2f, emptyPaint)
            return
        }
        valueLabels.forEach { label ->
            canvas.drawLine(g.bounds.left, label.y, g.bounds.right, label.y, gridPaint)
            canvas.drawText(label.text, label.x, label.y + context.dpf(LABEL_BASELINE), axisPaint)
        }
        timeLabels.forEach { canvas.drawText(it.text, it.x, it.y, timePaint) }
        canvas.drawPath(area, areaPaint)
        canvas.drawPath(measured, measuredPaint)
        canvas.drawPath(projection, projectionPaint)
        drawTarget(canvas, g)
        drawPoints(canvas)
        drawLegend(canvas)
    }

    private fun drawTarget(
        canvas: Canvas,
        g: ChartGeometry,
    ) {
        val label = targetLabel ?: return
        canvas.drawLine(targetPill.right + context.dpf(SMALL_GAP), label.y, g.bounds.right, label.y, targetPaint)
        fillPaint.color = Palette.RED
        canvas.drawRoundRect(targetPill, targetPill.height() / 2, targetPill.height() / 2, fillPaint)
        canvas.drawText(label.text, targetPill.centerX(), label.y + context.dpf(LABEL_BASELINE), pillTextPaint)
    }

    private fun drawPoints(canvas: Canvas) {
        etaPoint?.let { point ->
            fillPaint.color = Palette.WHITE
            canvas.drawCircle(point.x, point.y, context.dpf(DOT), fillPaint)
            canvas.drawCircle(point.x, point.y, context.dpf(DOT), ringPaint)
        }
        etaLabel?.let { canvas.drawText(it.text, it.x, it.y, etaTextPaint) }
        current?.let { dot(canvas, it, Palette.TINT) }
    }

    private fun dot(
        canvas: Canvas,
        point: PointF,
        color: Int,
    ) {
        fillPaint.color = Palette.WHITE
        canvas.drawCircle(point.x, point.y, context.dpf(DOT_RING), fillPaint)
        fillPaint.color = color
        canvas.drawCircle(point.x, point.y, context.dpf(DOT), fillPaint)
    }

    private fun drawLegend(canvas: Canvas) {
        val y = context.dpf(LEGEND_BASELINE)
        legend.filter { it.isVisible }.forEach { item ->
            fillPaint.color = item.color
            canvas.drawCircle(item.x, y - context.dpf(LEGEND_DOT_OFFSET), context.dpf(LEGEND_DOT), fillPaint)
            canvas.drawText(item.text, item.x + context.dpf(LEGEND_TEXT_GAP), y, legendPaint)
        }
    }

    private fun rebuild() {
        val bounds =
            Bounds(
                context.dpf(PAD_LEFT),
                context.dpf(PAD_TOP),
                width - context.dpf(PAD_RIGHT),
                height - context.dpf(PAD_BOTTOM),
            )
        val g = if (width > 0) ChartGeometry.build(samples, target, forecast, bounds) else null
        geometry = g
        if (g == null) return
        val labelX = bounds.left - context.dpf(SMALL_GAP)
        valueLabels = g.valueTicks().map { Label(labelX, g.yOf(it), degrees(it)) }
        timeLabels = g.timeTicks().map {
            Label(g.xOf(it), height - context.dpf(TIME_BASELINE), clock.format(Date(it.toLong())))
        }
        buildMeasured(g)
        buildProjection(g)
        buildTarget(g)
        layoutLegend()
        areaPaint.shader = areaGradient(bounds.top, bounds.bottom)
    }

    private fun degrees(value: Double): String = context.getString(R.string.degrees, value.roundToInt().toString())

    private fun buildMeasured(g: ChartGeometry) {
        measured.reset()
        area.reset()
        val smooth = centeredMovingAverage(DoubleArray(samples.size) { samples[it].second }, SMOOTHING_RADIUS)
        val minStep = context.dpf(MIN_POINT_SPACING)
        var lastX = Float.NEGATIVE_INFINITY
        samples.forEachIndexed { i, (time, _) ->
            val x = g.xOf(time.toDouble())
            val y = g.yOf(smooth[i])
            when {
                i == 0 -> {
                    measured.moveTo(x, y)
                    area.moveTo(x, g.bounds.bottom)
                    area.lineTo(x, y)
                }
                x - lastX >= minStep || i == samples.lastIndex -> {
                    measured.lineTo(x, y)
                    area.lineTo(x, y)
                }
                else -> return@forEachIndexed
            }
            lastX = x
        }
        val end = PointF(g.xOf(samples.last().first.toDouble()), g.yOf(smooth.last()))
        current = end
        area.lineTo(end.x, g.bounds.bottom)
        area.close()
    }

    private fun buildProjection(g: ChartGeometry) {
        projection.reset()
        etaPoint = null
        etaLabel = null
        val ahead = forecast ?: return
        val now = samples.last().first.toDouble()
        for (i in 0..SEGMENTS) {
            val minutes = g.projectionMinutes * i / SEGMENTS
            val x = g.xOf(now + minutes * MS_PER_MIN)
            val y = g.yOf(ahead.at(minutes).coerceIn(g.low, g.high))
            if (i == 0) projection.moveTo(x, y) else projection.lineTo(x, y)
        }
        val eta = ahead.etaMinutes?.takeIf { it <= g.projectionMinutes } ?: return
        val finishMs = now + eta * MS_PER_MIN
        val point = PointF(g.xOf(finishMs), g.yOf(ahead.at(eta)))
        etaPoint = point
        val halfText = etaTextPaint.measureText(clock.format(Date(finishMs.toLong()))) / 2
        val labelX = point.x.coerceAtMost(g.bounds.right - halfText)
        etaLabel = Label(labelX, point.y + context.dpf(ETA_LABEL_OFFSET), clock.format(Date(finishMs.toLong())))
    }

    private fun buildTarget(g: ChartGeometry) {
        val value = target
        if (value == null) {
            targetLabel = null
            return
        }
        val y = g.yOf(value)
        val text = context.getString(unit.format, value.roundToInt().toString())
        val halfWidth = pillTextPaint.measureText(text) / 2 + context.dpf(PILL_PADDING)
        val halfHeight = context.dpf(PILL_HALF_HEIGHT)
        val centerX = g.bounds.left + halfWidth
        targetPill.set(centerX - halfWidth, y - halfHeight, centerX + halfWidth, y + halfHeight)
        targetLabel = Label(centerX, y, text)
    }

    private fun layoutLegend() {
        var x = context.dpf(PAD_LEFT)
        legend[1].isVisible = forecast != null
        legend.filter { it.isVisible }.forEach { item ->
            item.x = x
            x += context.dpf(LEGEND_TEXT_GAP) + legendPaint.measureText(item.text) + context.dpf(LEGEND_ITEM_GAP)
        }
    }

    private companion object {
        const val MS_PER_MIN = 60_000.0
        const val SEGMENTS = 60
        const val SMOOTHING_RADIUS = 20
        const val MIN_POINT_SPACING = 2f
        const val PAD_LEFT = 40f
        const val PAD_TOP = 36f
        const val PAD_RIGHT = 14f
        const val PAD_BOTTOM = 28f
        const val MEASURED_WIDTH = 3f
        const val PROJECTION_WIDTH = 2f
        const val TARGET_WIDTH = 1.5f
        const val DASH = 5f
        const val DASH_LONG = 7f
        const val DOT = 4.5f
        const val DOT_RING = 7f
        const val RING_WIDTH = 2f
        const val ETA_LABEL_OFFSET = 20f
        const val SMALL_GAP = 6f
        const val LABEL_BASELINE = 4f
        const val TIME_BASELINE = 9f
        const val PILL_PADDING = 7f
        const val PILL_HALF_HEIGHT = 9f
        const val LEGEND_BASELINE = 22f
        const val LEGEND_DOT = 3.5f
        const val LEGEND_DOT_OFFSET = 3.5f
        const val LEGEND_TEXT_GAP = 9f
        const val LEGEND_ITEM_GAP = 14f
    }
}
