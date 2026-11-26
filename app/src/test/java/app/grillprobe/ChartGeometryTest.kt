package app.grillprobe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartGeometryTest {
    private val bounds = Bounds(40f, 30f, 340f, 230f)

    @Test
    fun needsTwoSamples() {
        assertNull(ChartGeometry.build(listOf(Cooks.START_MS to 20.0), null, null, bounds))
    }

    @Test
    fun horizonWithoutEstimateShowsHalfTheHistory() {
        assertEquals(10.0, ChartGeometry.horizonMinutes(4.0, null), 1e-9)
        assertEquals(40.0, ChartGeometry.horizonMinutes(80.0, null), 1e-9)
    }

    @Test
    fun horizonWithEstimateCoversTheEta() {
        assertEquals(21.0, ChartGeometry.horizonMinutes(10.0, 20.0), 1e-9)
        assertEquals(60.0, ChartGeometry.horizonMinutes(10.0, 300.0), 1e-9)
        assertEquals(2.0, ChartGeometry.horizonMinutes(10.0, 0.5), 1e-9)
    }

    @Test
    fun valueRangeIncludesTargetWithPadding() {
        val geometry = requireNotNull(ChartGeometry.build(Cooks.series(5.0) { 30.0 }, 70.0, null, bounds))
        assertTrue(geometry.low <= 27.0)
        assertTrue(geometry.high >= 73.0)
        assertEquals(0.0, geometry.low % 5.0, 1e-9)
    }

    @Test
    fun mapsValuesAndTimesIntoBounds() {
        val samples = Cooks.series(10.0, Cooks.linear(20.0, 2.0))
        val geometry = requireNotNull(ChartGeometry.build(samples, null, null, bounds))
        assertEquals(bounds.bottom, geometry.yOf(geometry.low), 1e-3f)
        assertEquals(bounds.top, geometry.yOf(geometry.high), 1e-3f)
        assertEquals(bounds.left, geometry.xOf(samples.first().first.toDouble()), 1e-3f)
        assertTrue(geometry.xOf(samples.last().first.toDouble()) < bounds.right)
    }

    @Test
    fun ticksStayInsideTheRange() {
        val samples = Cooks.series(90.0, Cooks.newton(5.0, 110.0, 0.02))
        val geometry = requireNotNull(ChartGeometry.build(samples, 95.0, null, bounds))
        val values = geometry.valueTicks()
        assertTrue(values.isNotEmpty())
        assertTrue(values.all { it >= geometry.low && it <= geometry.high })
        assertEquals(20.0, values[1] - values[0], 1e-9)
        assertTrue(geometry.timeTicks().size in 2..6)
    }
}
