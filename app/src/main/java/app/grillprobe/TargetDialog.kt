package app.grillprobe

import android.app.Activity
import android.app.AlertDialog
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.FrameLayout
import kotlin.math.roundToInt

object TargetDialog {
    fun show(
        activity: Activity,
        celsius: Double?,
        unit: TemperatureUnit,
        onSet: (Double) -> Unit,
    ) {
        val input =
            EditText(activity).apply {
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                gravity = Gravity.CENTER
                textSize = TypeScale.LARGE
                setText(celsius?.let { unit.fromCelsius(it).roundToInt().toString() }.orEmpty())
                setSelection(text.length)
            }
        val frame =
            FrameLayout(activity).apply {
                val side = activity.dp(Space.XL)
                setPadding(side, activity.dp(Space.S), side, 0)
                addView(input)
            }
        val celsiusTitle = unit == TemperatureUnit.CELSIUS
        val title = if (celsiusTitle) R.string.target_dialog_celsius else R.string.target_dialog_fahrenheit
        AlertDialog
            .Builder(activity)
            .setTitle(title)
            .setView(frame)
            .setPositiveButton(R.string.set) { _, _ ->
                input.text
                    .toString()
                    .replace(',', '.')
                    .toDoubleOrNull()
                    ?.let { onSet(unit.toCelsius(it)) }
            }.setNegativeButton(R.string.cancel, null)
            .show()
        input.requestFocus()
    }
}
