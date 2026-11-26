package app.grillprobe

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

object Cooks {
    const val START_MS = 1_795_000_000_000L
    private const val STEP_MS = 3_000L
    private const val MS_PER_MIN = 60_000.0

    fun series(
        minutes: Double,
        temperature: (Double) -> Double,
    ): List<Sample> =
        (0..(minutes * MS_PER_MIN / STEP_MS).toInt()).map { step ->
            val elapsed = step * STEP_MS
            START_MS + elapsed to temperature(elapsed / MS_PER_MIN).roundToInt().toDouble()
        }

    fun newton(
        start: Double,
        asymptote: Double,
        k: Double,
    ): (Double) -> Double = { minutes -> asymptote - (asymptote - start) * exp(-k * minutes) }

    fun minutesToReach(
        start: Double,
        asymptote: Double,
        k: Double,
        target: Double,
    ): Double = ln((asymptote - start) / (asymptote - target)) / k

    fun linear(
        start: Double,
        perMinute: Double,
    ): (Double) -> Double = { minutes -> start + perMinute * minutes }
}
