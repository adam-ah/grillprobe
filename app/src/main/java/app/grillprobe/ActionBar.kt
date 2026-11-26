package app.grillprobe

import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class ActionBar(
    private val context: Context,
    private val actions: MainScreen.Actions,
) {
    private class Action(
        val view: LinearLayout,
        val label: TextView,
    )

    private val unit = action(R.drawable.ic_probe, R.string.show_fahrenheit, Palette.TINT) { actions.toggleUnit() }
    private val disconnect = action(R.drawable.ic_power, R.string.disconnect, Palette.RED) { actions.disconnect() }

    val view: LinearLayout =
        context.horizontal().apply {
            background = context.rounded(Palette.SURFACE, Radius.CARD)
            val items =
                listOf(
                    unit,
                    action(R.drawable.ic_add, R.string.new_cook, Palette.TINT) { actions.newCook() },
                    action(R.drawable.ic_bell, R.string.test_alarm, Palette.TINT) { actions.testAlarm() },
                    disconnect,
                )
            items.forEach { addView(it.view, linear(0, context.dp(BAR_HEIGHT), 1f)) }
        }

    fun bind(state: ScreenState) {
        unit.label.setText(
            if (state.unit == TemperatureUnit.CELSIUS) R.string.show_fahrenheit else R.string.show_celsius
        )
        disconnect.view.isEnabled = state.isServiceRunning
        disconnect.view.alpha = if (state.isServiceRunning) 1f else DISABLED_ALPHA
    }

    private fun action(
        icon: Int,
        text: Int,
        color: Int,
        onClick: () -> Unit,
    ): Action {
        val label = context.label(TypeScale.CAPTION2, color, Weight.MEDIUM).apply { setText(text) }
        val layout =
            context.vertical().apply {
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true
                background = ripple(context.rounded(Palette.SURFACE, Radius.CARD))
                setOnClickListener { onClick() }
                val image =
                    ImageView(context).apply {
                        setImageResource(icon)
                        imageTintList = ColorStateList.valueOf(color)
                    }
                val size = context.dp(ICON_SIZE)
                addView(image, linear(size, size))
                addView(label, linear(WRAP, WRAP).apply { topMargin = context.dp(Space.XXS) })
            }
        return Action(layout, label)
    }

    private companion object {
        const val BAR_HEIGHT = 58
        const val ICON_SIZE = 22
        const val DISABLED_ALPHA = 0.35f
        const val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT
    }
}
