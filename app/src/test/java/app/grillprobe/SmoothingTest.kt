package app.grillprobe

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmoothingTest {
    @Test
    fun keepsEndpointsExact() {
        val values = doubleArrayOf(20.0, 21.0, 21.0, 22.0, 24.0, 23.0, 25.0)
        val smooth = centeredMovingAverage(values, 3)
        assertEquals(values.first(), smooth.first(), 1e-9)
        assertEquals(values.last(), smooth.last(), 1e-9)
    }

    @Test
    fun preservesStraightLines() {
        val values = DoubleArray(50) { 10.0 + it * 0.5 }
        assertArrayEquals(values, centeredMovingAverage(values, 10), 1e-9)
    }

    @Test
    fun flattensQuantisationSteps() {
        val stairs = DoubleArray(60) { (it / 10).toDouble() }
        val smooth = centeredMovingAverage(stairs, 5)
        val maxJump = (1 until smooth.size).maxOf { smooth[it] - smooth[it - 1] }
        assertTrue(maxJump < 0.2)
    }

    @Test
    fun handlesEmptyAndSingleInputs() {
        assertEquals(0, centeredMovingAverage(DoubleArray(0), 4).size)
        assertArrayEquals(doubleArrayOf(7.0), centeredMovingAverage(doubleArrayOf(7.0), 4), 1e-9)
    }
}
