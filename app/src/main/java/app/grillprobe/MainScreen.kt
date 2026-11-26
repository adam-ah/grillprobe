package app.grillprobe

import android.app.Activity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.LinearLayout

class MainScreen(
    private val activity: Activity,
    actions: Actions,
) {
    interface Actions {
        fun connect()

        fun disconnect()

        fun newCook()

        fun testAlarm()

        fun stopAlarm()

        fun toggleUnit()

        fun selectPreset(preset: Preset)

        fun editTarget()

        fun stepTarget(delta: Int)
    }

    private val format = Formatter(activity.resources)
    private val header = HeaderBar(activity)
    private val hero = HeroSection(activity, format, actions)
    private val presets = PresetPicker(activity, format, actions)
    private val summary = SummaryCard(activity, format)
    private val chart = ChartView(activity).apply { background = activity.rounded(Palette.SURFACE, Radius.CARD) }
    private val actionBar = ActionBar(activity, actions)
    private val alarm = AlarmOverlay(activity, format, actions)
    private val connect = activity.button(activity.getString(R.string.connect), Palette.TINT, Palette.WHITE) {
        actions.connect()
    }

    private val content: LinearLayout =
        activity.vertical().apply {
            addView(header.view)
            addView(connect, spaced(Space.M, activity.dp(CONNECT_HEIGHT)))
            addView(hero.view, spaced(Space.L))
            addView(presets.view, spaced(Space.M))
            addView(summary.view, spaced(Space.M))
            addView(chart, linear(MATCH_PARENT, 0, 1f).apply { topMargin = activity.dp(Space.M) })
            addView(actionBar.view, spaced(Space.M))
            setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                val side = activity.dp(Space.L)
                view.setPadding(
                    side + bars.left,
                    activity.dp(Space.S) + bars.top,
                    side + bars.right,
                    activity.dp(Space.M) + bars.bottom
                )
                insets
            }
        }

    val root: FrameLayout =
        FrameLayout(activity).apply {
            addView(content, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(alarm.view, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }

    fun render(state: ScreenState) {
        header.bind(state)
        connect.visibility = if (state.isServiceRunning) View.GONE else View.VISIBLE
        hero.bind(state)
        presets.bind(state)
        summary.bind(state)
        val empty = activity.getString(
            if (state.isServiceRunning) R.string.waiting_for_readings else R.string.connect_hint
        )
        chart.update(state.samples, state.target, state.prediction, state.unit, empty)
        actionBar.bind(state)
        alarm.bind(state)
    }

    private fun spaced(
        top: Int,
        height: Int = WRAP_CONTENT,
    ) = linear(MATCH_PARENT, height).apply { topMargin = activity.dp(top) }

    private companion object {
        const val CONNECT_HEIGHT = 52
    }
}
