package app.grillprobe

enum class TemperatureUnit(
    val code: String,
    val format: Int,
) {
    CELSIUS(code = "C", format = R.string.temperature_celsius),
    FAHRENHEIT(code = "F", format = R.string.temperature_fahrenheit),
    ;

    fun fromCelsius(celsius: Double): Double = if (this == FAHRENHEIT) celsius * F_PER_C + FREEZING_F else celsius

    fun toCelsius(value: Double): Double = if (this == FAHRENHEIT) (value - FREEZING_F) / F_PER_C else value

    fun stepInCelsius(delta: Int): Double = if (this == FAHRENHEIT) delta / F_PER_C else delta.toDouble()

    fun deltaFromCelsius(delta: Double): Double = if (this == FAHRENHEIT) delta * F_PER_C else delta

    fun other(): TemperatureUnit = if (this == CELSIUS) FAHRENHEIT else CELSIUS

    companion object {
        private const val FREEZING_F = 32.0
        private const val F_PER_C = 9.0 / 5.0

        fun fromCode(code: String?): TemperatureUnit = entries.firstOrNull { it.code == code } ?: CELSIUS
    }
}
