package app.grillprobe

import android.content.res.Resources
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class Formatter(
    private val res: Resources,
) {
    private val clockFormat = SimpleDateFormat(CLOCK_PATTERN, Locale.getDefault())

    fun duration(minutes: Double?): String {
        val rounded = minutes?.takeIf { it.isFinite() }?.roundToInt()
        return when {
            minutes == null || rounded == null -> res.getString(R.string.no_value)
            minutes < 1 -> res.getString(R.string.less_than_minute)
            rounded < MINUTES_PER_HOUR -> res.getQuantityString(R.plurals.minutes, rounded, rounded)
            else -> res.getString(R.string.hours_minutes, rounded / MINUTES_PER_HOUR, rounded % MINUTES_PER_HOUR)
        }
    }

    fun clock(ms: Long): String = clockFormat.format(Date(ms))

    fun decimal(value: Double): String = String.format(Locale.getDefault(), "%.1f", value)

    fun whole(value: Double): String = if (value % 1.0 == 0.0) value.roundToInt().toString() else decimal(value)

    fun signed(value: Double): String = String.format(Locale.getDefault(), "%+.2f", value)

    fun temperature(
        celsius: Double,
        unit: TemperatureUnit,
    ): String = wholeTemperature(celsius, unit)

    fun wholeTemperature(
        celsius: Double,
        unit: TemperatureUnit,
    ): String = res.getString(unit.format, unit.fromCelsius(celsius).roundToInt().toString())

    fun rate(
        celsiusPerMinute: Double,
        unit: TemperatureUnit,
    ): String {
        val magnitude = decimal(abs(unit.deltaFromCelsius(celsiusPerMinute)))
        val label = if (celsiusPerMinute >= 0) R.string.rate_rising else R.string.rate_falling
        return res.getString(label, res.getString(unit.format, magnitude))
    }

    fun styled(
        text: String,
        unitColor: Int,
    ): CharSequence {
        val split = text.lastIndexOf(' ').takeIf { it > 0 } ?: return text
        return SpannableString(text).apply {
            setSpan(RelativeSizeSpan(UNIT_SCALE), split, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(ForegroundColorSpan(unitColor), split, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private companion object {
        const val CLOCK_PATTERN = "HH:mm"
        const val MINUTES_PER_HOUR = 60
        const val UNIT_SCALE = 0.42f
    }
}
