package app.grillprobe

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout

class AlarmOverlay(
    private val context: Context,
    private val format: Formatter,
    private val actions: MainScreen.Actions,
) {
    private val temperature = context.label(TypeScale.ALARM, Palette.WHITE, Weight.LIGHT)
    private val goal = context.label(TypeScale.HEADLINE, Palette.WHITE, Weight.MEDIUM)
    private var pulse: ValueAnimator? = null

    val view: LinearLayout =
        context.vertical().apply {
            gravity = Gravity.CENTER
            setBackgroundColor(Palette.TINT)
            visibility = View.GONE
            isClickable = true
            val bell =
                ImageView(context).apply {
                    setImageResource(R.drawable.ic_bell)
                    imageTintList = ColorStateList.valueOf(Palette.WHITE)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }
            val bellSize = context.dp(BELL_SIZE)
            addView(bell, linear(bellSize, bellSize))
            addView(
                context.label(
                    TypeScale.LARGE,
                    Palette.WHITE,
                    Weight.SEMIBOLD
                ).apply { setText(R.string.target_reached) },
                linear(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = context.dp(Space.M) },
            )
            addView(temperature, linear(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = context.dp(Space.S) })
            addView(goal, linear(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = context.dp(Space.XS) })
            val silence =
                context.button(
                    context.getString(R.string.stop_alarm),
                    Palette.WHITE,
                    Palette.TINT_DARK
                ) { actions.stopAlarm() }
            silence.background = ripple(context.rounded(Palette.WHITE, Radius.PILL))
            addView(
                silence,
                linear(context.dp(BUTTON_WIDTH), context.dp(BUTTON_HEIGHT)).apply { topMargin = context.dp(Space.XXL) },
            )
        }

    fun bind(state: ScreenState) {
        val reading = state.temperature?.let { format.temperature(it, state.unit) }
        temperature.text = reading?.let { format.styled(it, Palette.WHITE) } ?: ""
        goal.text = goalText(state)
        if (state.isAlarmActive && view.visibility != View.VISIBLE) {
            view.visibility = View.VISIBLE
            pulse =
                ObjectAnimator.ofArgb(view, "backgroundColor", Palette.TINT, Palette.TINT_DARK).apply {
                    duration = PULSE_MS
                    repeatMode = ValueAnimator.REVERSE
                    repeatCount = ValueAnimator.INFINITE
                    start()
                }
        } else if (!state.isAlarmActive && view.visibility == View.VISIBLE) {
            pulse?.cancel()
            view.visibility = View.GONE
        }
    }

    private fun goalText(state: ScreenState): String {
        val target = state.target?.let { format.wholeTemperature(it, state.unit) } ?: return ""
        val preset = state.preset ?: return context.getString(R.string.alarm_target, target)
        return context.getString(R.string.alarm_preset_target, context.getString(preset.label), target)
    }

    private companion object {
        const val BELL_SIZE = 48
        const val BUTTON_WIDTH = 240
        const val BUTTON_HEIGHT = 56
        const val PULSE_MS = 900L
    }
}
