package app.grillprobe

sealed interface Summary {
    data class Estimate(
        val minutes: Double,
        val finishMs: Long,
        val isEarly: Boolean,
        val range: Pair<Double, Double>?,
    ) : Summary

    data class Reached(
        val atMs: Long?,
    ) : Summary

    data class LevellingOff(
        val plateau: Double,
    ) : Summary

    data object Stalled : Summary

    data object Falling : Summary

    data object NotRising : Summary

    data object NeedsTarget : Summary

    data object Estimating : Summary

    data object Hidden : Summary

    companion object {
        private const val MS_PER_MIN = 60_000.0

        fun of(state: ScreenState): Summary {
            val prediction = state.prediction
            val best = prediction?.etas?.best
            return when {
                state.samples.size < 2 -> Hidden
                state.isReached -> Reached(state.reachedAt)
                state.target == null -> NeedsTarget
                prediction == null -> Estimating
                else -> fromPrediction(prediction, best)
            }
        }

        private fun fromPrediction(
            prediction: Prediction,
            best: Double?,
        ): Summary {
            val plateau = prediction.etas.plateau
            return when {
                plateau != null -> LevellingOff(plateau)
                prediction.trend == Trend.STALLED -> Stalled
                prediction.trend == Trend.FALLING -> Falling
                prediction.trend == Trend.FLAT -> NotRising
                best == null -> Estimating
                else -> estimate(prediction, best)
            }
        }

        private fun estimate(
            prediction: Prediction,
            best: Double,
        ): Estimate {
            val linear = prediction.etas.linear
            val curve = prediction.etas.curve
            val range = if (linear != null && curve != null) minOf(linear, curve) to maxOf(linear, curve) else null
            return Estimate(best, prediction.nowMs + (best * MS_PER_MIN).toLong(), prediction.curve == null, range)
        }
    }
}
