package app.grillprobe

import android.content.Context
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout

class HeaderBar(
    private val context: Context,
) {
    private val dot = View(context)
    private val status =
        context.label(TypeScale.SUBHEAD, Palette.LABEL, Weight.MEDIUM).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
    private val percent = context.label(TypeScale.FOOTNOTE, Palette.SECONDARY, Weight.MEDIUM)
    private val battery = BatteryView(context)

    val view: LinearLayout =
        context.horizontal().apply {
            val size = context.dp(Space.S)
            addView(dot, linear(size, size).apply { marginEnd = context.dp(Space.S) })
            addView(status, linear(0, WRAP_CONTENT, 1f))
            addView(percent, linear(WRAP_CONTENT, WRAP_CONTENT).apply { marginStart = context.dp(Space.M) })
            addView(
                battery,
                linear(
                    context.dp(BATTERY_WIDTH),
                    context.dp(BATTERY_HEIGHT)
                ).apply { marginStart = context.dp(Space.XS) },
            )
        }

    fun bind(state: ScreenState) {
        dot.background = context.rounded(if (state.isConnected) Palette.GREEN else Palette.SECONDARY, Radius.PILL)
        status.text = state.status
        status.setTextColor(if (state.isConnected) Palette.LABEL else Palette.SECONDARY)
        val level = state.battery
        val visibility = if (level == null) View.GONE else View.VISIBLE
        percent.visibility = visibility
        battery.visibility = visibility
        if (level != null) {
            percent.text = context.getString(R.string.battery, level)
            battery.setLevel(level)
        }
    }

    private companion object {
        const val BATTERY_WIDTH = 24
        const val BATTERY_HEIGHT = 12
    }
}
