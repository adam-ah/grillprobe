package app.grillprobe

import org.junit.Assert.assertEquals
import org.junit.Test

class BackoffTest {
    @Test
    fun doublesFromTwoSecondsAndCapsAtOneMinute() {
        assertEquals(
            listOf(2_000L, 4_000L, 8_000L, 16_000L, 32_000L, 60_000L, 60_000L),
            (1..7).map { Backoff.delayMs(it) }
        )
    }

    @Test
    fun treatsZeroOrNegativeFailuresAsTheFirst() {
        assertEquals(2_000L, Backoff.delayMs(0))
        assertEquals(2_000L, Backoff.delayMs(-3))
    }
}
