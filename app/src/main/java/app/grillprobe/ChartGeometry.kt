package app.grillprobe

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class Forecast(
    val etaMinutes: Double?,
    val at: (Double) -> Double,
)

data class Bounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

class ChartGeometry private constructor(
    val bounds: Bounds,
    val startMs: Double,
    val endMs: Double,
    val low: Double,
    val high: Double,
    val projectionMinutes: Double,
) {
    fun xOf(timeMs: Double): Float = (bounds.left + (timeMs - startMs) / (endMs - startMs) * bounds.width).toFloat()

    fun yOf(value: Double): Float = (bounds.bottom - (value - low) / (high - low) * bounds.height).toFloat()

    fun valueTicks(): List<Double> {
        val range = high - low
        val step =
            when {
                range > WIDE_RANGE -> WIDE_STEP
                range > MEDIUM_RANGE -> MEDIUM_STEP
                else -> VALUE_STEP
            }
        return generateSequence(ceil(low / step) * step) { it + step }.takeWhile { it <= high }.toList()
    }

    fun timeTicks(): List<Double> {
        val minutes = (endMs - startMs) / MS_PER_MIN
        val stepMs = (TIME_STEPS.firstOrNull { minutes / it <= MAX_TIME_LABELS } ?: LONGEST_TIME_STEP) * MS_PER_MIN
        return generateSequence(ceil(startMs / stepMs) * stepMs) { it + stepMs }.takeWhile { it <= endMs }.toList()
    }

    companion object {
        private const val MS_PER_MIN = 60_000.0
        private const val VALUE_STEP = 5.0
        private const val MEDIUM_STEP = 10.0
        private const val WIDE_STEP = 20.0
        private const val MEDIUM_RANGE = 25.0
        private const val WIDE_RANGE = 60.0
        private const val VALUE_PADDING = 3.0
        private const val MIN_PROJECTION = -20.0
        private const val MAX_PROJECTION = 300.0
        private const val MIN_HORIZON = 10.0
        private const val MIN_ETA_HORIZON = 2.0
        private const val ETA_HORIZON_FLOOR = 60.0
        private const val ETA_MARGIN = 1.05
        private const val HISTORY_SHARE = 0.5
        private const val HISTORY_MULTIPLE = 2.0
        private const val MAX_TIME_LABELS = 5
        private const val LONGEST_TIME_STEP = 240
        private val TIME_STEPS = listOf(5, 10, 15, 30, 60, 120)

        fun build(
            samples: List<Sample>,
            target: Double?,
            forecast: Forecast?,
            bounds: Bounds,
        ): ChartGeometry? {
            if (samples.size < 2) return null
            val start = samples.first().first.toDouble()
            val now = samples.last().first.toDouble()
            val eta = forecast?.etaMinutes
            val horizon = horizonMinutes((now - start) / MS_PER_MIN, eta)
            val projection = eta?.let { min(it, horizon) } ?: horizon
            val projected =
                forecast
                    ?.at
                    ?.invoke(projection)
                    ?.takeIf { it.isFinite() }
                    ?.coerceIn(MIN_PROJECTION, MAX_PROJECTION)
            val values = samples.map { it.second } + listOfNotNull(target, projected)
            val low = floor((values.min() - VALUE_PADDING) / VALUE_STEP) * VALUE_STEP
            val high = ceil((values.max() + VALUE_PADDING) / VALUE_STEP) * VALUE_STEP
            return ChartGeometry(bounds, start, now + horizon * MS_PER_MIN, low, high, projection)
        }

        fun horizonMinutes(
            historyMinutes: Double,
            eta: Double?,
        ): Double =
            if (eta == null) {
                max(historyMinutes * HISTORY_SHARE, MIN_HORIZON)
            } else {
                max(min(eta * ETA_MARGIN, max(historyMinutes * HISTORY_MULTIPLE, ETA_HORIZON_FLOOR)), MIN_ETA_HORIZON)
            }
    }
}
