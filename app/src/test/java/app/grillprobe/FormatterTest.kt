package app.grillprobe

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
class FormatterTest {
    private val format by lazy { Formatter(ApplicationProvider.getApplicationContext<Application>().resources) }

    @Before
    fun setUp() {
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @Test
    fun formatsDurations() {
        assertEquals("--", format.duration(null))
        assertEquals("--", format.duration(Double.NaN))
        assertEquals("<1 min", format.duration(0.4))
        assertEquals("12 min", format.duration(12.4))
        assertEquals("2h 05m", format.duration(125.0))
    }

    @Test
    fun formatsNumbers() {
        assertEquals("62.4", format.decimal(62.43))
        assertEquals("65", format.whole(65.0))
        assertEquals("65.5", format.whole(65.5))
        assertEquals("+0.84", format.signed(0.8412))
        assertEquals("-0.50", format.signed(-0.5))
    }

    @Test
    fun formatsTemperaturesWithUnits() {
        assertEquals("62 °C", format.temperature(62.43, TemperatureUnit.CELSIUS))
        assertEquals("144 °F", format.temperature(62.43, TemperatureUnit.FAHRENHEIT))
        assertEquals("93 °C", format.wholeTemperature(93.0, TemperatureUnit.CELSIUS))
        assertEquals("199 °F", format.wholeTemperature(93.0, TemperatureUnit.FAHRENHEIT))
        assertEquals("Rising 1.7 °F/min", format.rate(0.94, TemperatureUnit.FAHRENHEIT))
        assertEquals("Cooling 0.7 °C/min", format.rate(-0.68, TemperatureUnit.CELSIUS))
    }

    @Test
    fun formatsClockTimes() {
        assertEquals("18:42", format.clock(Cooks.START_MS - Cooks.START_MS % 86_400_000L + (18 * 60 + 42) * 60_000L))
    }
}
