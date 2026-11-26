package app.grillprobe

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

typealias Sample = Pair<Long, Double>

enum class Trend { RISING, FLAT, FALLING, STALLED }

data class HeatingCurve(
    val current: Double,
    val rate: Double,
    val k: Double,
) {
    val asymptote: Double get() = current + rate / k

    fun at(minutes: Double): Double = asymptote - (asymptote - current) * exp(-k * minutes)
}

data class Etas(
    val linear: Double? = null,
    val curve: Double? = null,
    val best: Double? = null,
    val plateau: Double? = null,
)

data class Prediction(
    val nowMs: Long,
    val current: Double,
    val rate: Double,
    val trend: Trend,
    val curve: HeatingCurve? = null,
    val etas: Etas = Etas(),
) {
    val heating: Boolean get() = trend == Trend.RISING

    fun projectAt(minutes: Double): Double = curve?.at(minutes) ?: (current + rate * minutes)
}

private class WeightedSums {
    private var w = 0.0
    private var x = 0.0
    private var y = 0.0
    private var xx = 0.0
    private var xy = 0.0

    fun add(
        weight: Double,
        xValue: Double,
        yValue: Double,
    ) {
        w += weight
        x += weight * xValue
        y += weight * yValue
        xx += weight * xValue * xValue
        xy += weight * xValue * yValue
    }

    fun slope(): Double? {
        val d = w * xx - x * x
        return if (abs(d) < EPSILON) null else (w * xy - x * y) / d
    }

    fun intercept(slope: Double): Double = (y - slope * x) / w

    private companion object {
        const val EPSILON = 1e-12
    }
}

private class Window(
    private val xs: DoubleArray,
    private val ys: DoubleArray,
    private val ws: DoubleArray,
) {
    val size: Int get() = xs.size
    val spanMinutes: Double get() = if (xs.isEmpty()) 0.0 else -xs.first()

    fun sums(transform: (Double) -> Double = { it }): WeightedSums {
        val sums = WeightedSums()
        for (i in xs.indices) sums.add(ws[i], transform(xs[i]), ys[i])
        return sums
    }

    fun sse(model: (Double) -> Double): Double {
        var total = 0.0
        for (i in xs.indices) {
            val r = ys[i] - model(xs[i])
            total += ws[i] * r * r
        }
        return total
    }
}

object Predictor {
    private const val RISING_RATE = 0.05
    private const val STALL_MIN_TEMP = 55.0
    private const val STALL_TARGET_MARGIN = 1.0
    private const val MAX_ETA_MIN = 8 * 60.0
    private const val TREND_WINDOW_MIN = 3.0
    private const val TREND_HALF_LIFE_MIN = 1.5
    private const val TURN_MARGIN = 1.0
    private const val CURVE_WINDOW_MIN = 30.0
    private const val CURVE_HALF_LIFE_MIN = 10.0
    private const val CURVE_MIN_SAMPLES = 15
    private const val CURVE_MIN_SPAN_MIN = 3.0
    private const val LINEAR_MIN_SAMPLES = 4
    private const val K_MIN = 1.0 / 600
    private const val K_MAX = 1.0 / 3
    private const val K_STEPS = 120
    private const val K_EDGE = 1.05
    private const val CURVE_MIN_GAIN = 0.85
    private const val PLATEAU_MARGIN = 0.2
    private const val MS_PER_MIN = 60_000.0
    private const val HALF = 0.5

    fun predict(
        samples: List<Sample>,
        target: Double?,
    ): Prediction? {
        if (samples.size < 2) return null
        val nowMs = samples.last().first
        val sinceHeatingStarted = samples.subList(heatingStart(samples), samples.size)
        val rising = sinceHeatingStarted.takeIf { it.size >= LINEAR_MIN_SAMPLES } ?: samples
        val segment = trendSegment(samples, nowMs, rising)
        return linearFit(window(segment, nowMs, TREND_WINDOW_MIN, TREND_HALF_LIFE_MIN))?.let { (rate, current) ->
            val trend = trendOf(rate, current, target)
            if (trend == Trend.RISING) {
                val curve = fitK(
                    window(rising, nowMs, CURVE_WINDOW_MIN, CURVE_HALF_LIFE_MIN)
                )?.let { HeatingCurve(current, rate, it) }
                Prediction(nowMs, current, rate, trend, curve, etas(current, rate, curve, target))
            } else {
                Prediction(nowMs, current, rate, trend)
            }
        }
    }

    private fun trendSegment(
        samples: List<Sample>,
        nowMs: Long,
        sinceHeating: List<Sample>,
    ): List<Sample> {
        val recent = samples.filter { (t, _) -> (t - nowMs) / MS_PER_MIN >= -TREND_WINDOW_MIN }
        if (recent.size < LINEAR_MIN_SAMPLES) return sinceHeating
        val current = recent.last().second
        val high = recent.maxOf { it.second }
        val low = recent.minOf { it.second }
        val peak = recent.indexOfLast { it.second == high }
        val valley = recent.indexOfLast { it.second == low }
        val start =
            when {
                peak > valley && current <= high - TURN_MARGIN -> peak
                valley > peak && current >= low + TURN_MARGIN -> valley
                else -> null
            }
        return start?.let { recent.subList(it, recent.size) }?.takeIf { it.size >= LINEAR_MIN_SAMPLES } ?: sinceHeating
    }

    private fun heatingStart(samples: List<Sample>): Int {
        var minValue = Double.MAX_VALUE
        var index = 0
        samples.forEachIndexed { i, (_, value) ->
            if (value <= minValue) {
                minValue = value
                index = i
            }
        }
        return index
    }

    private fun window(
        samples: List<Sample>,
        nowMs: Long,
        windowMin: Double,
        halfLifeMin: Double,
    ): Window {
        val recent = samples.filter { (t, _) -> (t - nowMs) / MS_PER_MIN >= -windowMin }
        val xs = DoubleArray(recent.size) { (recent[it].first - nowMs) / MS_PER_MIN }
        val ys = DoubleArray(recent.size) { recent[it].second }
        val ws = DoubleArray(recent.size) { HALF.pow(-xs[it] / halfLifeMin) }
        return Window(xs, ys, ws)
    }

    private fun linearFit(window: Window): Pair<Double, Double>? {
        if (window.size < LINEAR_MIN_SAMPLES) return null
        val sums = window.sums()
        val slope = sums.slope() ?: return null
        return slope to sums.intercept(slope)
    }

    private fun curveSse(
        window: Window,
        k: Double,
    ): Double? {
        val sums = window.sums { x -> -exp(-k * x) }
        val amplitude = sums.slope()?.takeIf { it > 0 } ?: return null
        val asymptote = sums.intercept(amplitude)
        return window.sse { x -> asymptote - amplitude * exp(-k * x) }
    }

    private fun fitK(window: Window): Double? {
        if (window.size < CURVE_MIN_SAMPLES || window.spanMinutes < CURVE_MIN_SPAN_MIN) return null
        val lineSse = linearFit(window)?.let { (slope, intercept) -> window.sse { x -> intercept + slope * x } }
        val best =
            (0..K_STEPS)
                .map { K_MIN * (K_MAX / K_MIN).pow(it.toDouble() / K_STEPS) }
                .mapNotNull { k -> curveSse(window, k)?.let { k to it } }
                .minByOrNull { it.second }
        val significant = best != null && lineSse != null && best.second < CURVE_MIN_GAIN * lineSse
        return best?.first?.takeIf { significant && it > K_MIN * K_EDGE }
    }

    private fun isStall(
        rate: Double,
        current: Double,
        target: Double?,
    ): Boolean {
        val belowTarget = target != null && current < target - STALL_TARGET_MARGIN
        return belowTarget && current > STALL_MIN_TEMP && abs(rate) <= RISING_RATE
    }

    private fun trendOf(
        rate: Double,
        current: Double,
        target: Double?,
    ): Trend =
        when {
            rate > RISING_RATE -> Trend.RISING
            isStall(rate, current, target) -> Trend.STALLED
            rate < -RISING_RATE -> Trend.FALLING
            else -> Trend.FLAT
        }

    private fun linearEta(
        current: Double,
        rate: Double,
        target: Double,
    ): Double? = ((target - current) / rate).takeIf { it < MAX_ETA_MIN }

    private fun curveEta(
        curve: HeatingCurve,
        target: Double,
    ): Double? {
        val minutes = ln((curve.asymptote - curve.current) / (curve.asymptote - target)) / curve.k
        return minutes.takeIf { it > 0 && it < MAX_ETA_MIN }
    }

    private fun etas(
        current: Double,
        rate: Double,
        curve: HeatingCurve?,
        target: Double?,
    ): Etas =
        when {
            target == null || target <= current -> Etas()
            curve == null -> linearEta(current, rate, target).let { Etas(linear = it, best = it) }
            curve.asymptote <= target + PLATEAU_MARGIN ->
                Etas(linear = linearEta(current, rate, target), plateau = curve.asymptote)
            else ->
                curveEta(curve, target).let { Etas(linear = linearEta(current, rate, target), curve = it, best = it) }
        }
}
