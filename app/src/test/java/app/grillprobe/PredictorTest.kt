package app.grillprobe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PredictorTest {
    @Test
    fun needsAtLeastTwoSamples() {
        assertNull(Predictor.predict(emptyList(), 60.0))
        assertNull(Predictor.predict(listOf(Cooks.START_MS to 20.0), 60.0))
    }

    @Test
    fun flatTemperatureIsNotHeating() {
        val prediction = predict(Cooks.series(10.0) { 24.0 }, null)
        assertEquals(Trend.FLAT, prediction.trend)
        assertNull(prediction.etas.best)
    }

    @Test
    fun fallingTemperatureHasNoEstimate() {
        val prediction = predict(Cooks.series(10.0, Cooks.linear(70.0, -0.5)), 80.0)
        assertEquals(Trend.FALLING, prediction.trend)
        assertNull(prediction.etas.best)
        assertNull(prediction.curve)
    }

    @Test
    fun steadyRiseUsesLinearEstimate() {
        val prediction = predict(Cooks.series(6.0, Cooks.linear(20.0, 1.0)), 60.0)
        assertEquals(Trend.RISING, prediction.trend)
        assertNull(prediction.curve)
        assertEquals(34.0, prediction.etas.best ?: Double.NaN, 3.0)
    }

    @Test
    fun ignoresTheFlatStretchBeforeHeatingStarted() {
        val samples = Cooks.series(12.0) { minutes -> if (minutes < 10) 25.0 else 25.0 + 2.0 * (minutes - 10) }
        val prediction = predict(samples, 60.0)
        assertEquals(2.0, prediction.rate, 0.3)
    }

    @Test
    fun curveFitPredictsASlowingCook() {
        val start = 5.0
        val asymptote = 110.0
        val k = 0.02
        val target = 90.0
        val now = 45.0
        val prediction = predict(Cooks.series(now, Cooks.newton(start, asymptote, k)), target)
        val actual = Cooks.minutesToReach(start, asymptote, k, target) - now
        val best = requireNotNull(prediction.etas.best)
        assertNotNull(prediction.curve)
        assertEquals(actual, best, actual * 0.15)
        assertTrue("linear estimate is optimistic", (prediction.etas.linear ?: 0.0) < best)
    }

    @Test
    fun warnsWhenTheCurveLevelsOffBelowTarget() {
        val prediction = predict(Cooks.series(25.0, Cooks.newton(20.0, 70.0, 0.06)), 80.0)
        assertNull(prediction.etas.best)
        assertEquals(70.0, prediction.etas.plateau ?: Double.NaN, 6.0)
    }

    @Test
    fun flatAboveFiftyFiveBelowTargetIsAStall() {
        val samples = Cooks.series(20.0) { minutes -> if (minutes < 10) 30.0 + 3.5 * minutes else 65.0 }
        assertEquals(Trend.STALLED, predict(samples, 90.0).trend)
    }

    @Test
    fun reachedTargetHasNoEstimate() {
        val prediction = predict(Cooks.series(10.0, Cooks.linear(50.0, 1.0)), 55.0)
        assertNull(prediction.etas.best)
        assertNull(prediction.etas.linear)
    }

    @Test
    fun coolingRateSettlesOnceTheTemperatureStopsMoving() {
        val samples =
            Cooks.series(12.0) { minutes ->
                when {
                    minutes < 1 -> 26.0
                    minutes < 5 -> 32.0 - (minutes - 1)
                    else -> 28.0
                }
            }
        val rates =
            listOf(6.0, 7.0, 8.0, 10.0, 12.0).map { at ->
                val upTo = samples.filter { it.first <= Cooks.START_MS + (at * 60_000).toLong() }
                kotlin.math.abs(predict(upTo, 74.0).rate)
            }
        rates.zipWithNext().forEach { (earlier, later) -> assertTrue("rate grew: $rates", later <= earlier + 1e-9) }
        assertTrue("still reporting cooling: $rates", rates.last() < 0.05)
    }

    @Test
    fun peakInsideTheWindowDoesNotHideTheDrop() {
        val samples = Cooks.series(
            4.0
        ) { minutes -> if (minutes < 1.5) 26.0 + minutes * 4 else 32.0 - (minutes - 1.5) * 2 }
        val prediction = predict(samples, 74.0)
        assertEquals(Trend.FALLING, prediction.trend)
        assertEquals(-2.0, prediction.rate, 0.6)
    }

    @Test
    fun projectionStartsAtTheCurrentTemperature() {
        val prediction = predict(Cooks.series(45.0, Cooks.newton(5.0, 110.0, 0.02)), 90.0)
        assertEquals(prediction.current, prediction.projectAt(0.0), 1e-9)
        assertTrue(prediction.projectAt(10.0) > prediction.current)
    }

    private fun predict(
        samples: List<Sample>,
        target: Double?,
    ): Prediction = requireNotNull(Predictor.predict(samples, target))
}
