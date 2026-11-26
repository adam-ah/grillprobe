package app.grillprobe

import org.junit.Assert.assertEquals
import org.junit.Test

class TemperatureUnitTest {
    @Test
    fun convertsBetweenCelsiusAndFahrenheit() {
        assertEquals(212.0, TemperatureUnit.FAHRENHEIT.fromCelsius(100.0), 1e-9)
        assertEquals(-40.0, TemperatureUnit.FAHRENHEIT.fromCelsius(-40.0), 1e-9)
        assertEquals(74.0, TemperatureUnit.FAHRENHEIT.toCelsius(165.2), 1e-9)
        assertEquals(63.0, TemperatureUnit.CELSIUS.fromCelsius(63.0), 1e-9)
    }

    @Test
    fun stepsAreOneDisplayDegree() {
        assertEquals(1.0, TemperatureUnit.CELSIUS.stepInCelsius(1), 1e-9)
        assertEquals(-5.0 / 9.0, TemperatureUnit.FAHRENHEIT.stepInCelsius(-1), 1e-9)
    }

    @Test
    fun ratesConvertWithoutOffset() {
        assertEquals(1.8, TemperatureUnit.FAHRENHEIT.deltaFromCelsius(1.0), 1e-9)
    }

    @Test
    fun unknownCodeFallsBackToCelsius() {
        assertEquals(TemperatureUnit.FAHRENHEIT, TemperatureUnit.fromCode("F"))
        assertEquals(TemperatureUnit.CELSIUS, TemperatureUnit.fromCode(null))
        assertEquals(TemperatureUnit.CELSIUS, TemperatureUnit.fromCode("K"))
    }

    @Test
    fun everyAnimalHasPresets() {
        Animal.entries.forEach { assertEquals(true, Preset.of(it).isNotEmpty()) }
    }
}
