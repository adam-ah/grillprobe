package app.grillprobe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SummaryTest {
    @Test
    fun hiddenWithoutReadings() {
        assertEquals(Summary.Hidden, Summary.of(state(emptyList(), 60.0)))
    }

    @Test
    fun asksForATargetFirst() {
        assertEquals(Summary.NeedsTarget, Summary.of(state(Cooks.series(6.0, Cooks.linear(20.0, 1.0)), null)))
    }

    @Test
    fun earlyHeatingGivesAnEarlyEstimate() {
        val summary = Summary.of(state(Cooks.series(6.0, Cooks.linear(20.0, 1.0)), 60.0))
        assertTrue(summary is Summary.Estimate)
        assertTrue((summary as Summary.Estimate).isEarly)
    }

    @Test
    fun curvedCookGivesARange() {
        val summary = Summary.of(state(Cooks.series(45.0, Cooks.newton(5.0, 110.0, 0.02)), 90.0)) as Summary.Estimate
        val range = requireNotNull(summary.range)
        assertTrue(range.first <= summary.minutes && summary.minutes <= range.second)
    }

    @Test
    fun reachedReportsWhenTheTargetWasCrossed() {
        val samples = Cooks.series(10.0, Cooks.linear(50.0, 1.0))
        val summary = Summary.of(state(samples, 55.0)) as Summary.Reached
        assertEquals(samples.first { it.second >= 55.0 }.first, summary.atMs)
    }

    @Test
    fun levellingOffWarnsInsteadOfEstimating() {
        val summary = Summary.of(state(Cooks.series(25.0, Cooks.newton(20.0, 70.0, 0.06)), 80.0))
        assertTrue(summary is Summary.LevellingOff)
    }

    @Test
    fun fallingBeforeTargetSaysSo() {
        assertEquals(Summary.Falling, Summary.of(state(Cooks.series(10.0, Cooks.linear(50.0, -0.5)), 80.0)))
    }

    private fun state(
        samples: List<Sample>,
        target: Double?,
    ) = ScreenState(
        status = "",
        isConnected = true,
        isServiceRunning = true,
        battery = null,
        unit = TemperatureUnit.CELSIUS,
        target = target,
        preset = null,
        samples = samples,
        prediction = Predictor.predict(samples, target),
        isAlarmActive = false,
    )
}
