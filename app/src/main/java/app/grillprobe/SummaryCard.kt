package app.grillprobe

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView

class SummaryCard(
    private val context: Context,
    private val format: Formatter,
) {
    private val remaining = value()
    private val remainingDetail = detail()
    private val finish = value()
    private val finishDetail = detail()
    private val columns =
        context.horizontal().apply {
            gravity = Gravity.TOP
            addView(column(R.string.ready_in, remaining, remainingDetail), linear(0, WRAP_CONTENT, 1f))
            addView(context.separator(), linear(1, MATCH_PARENT))
            addView(
                column(R.string.done_at, finish, finishDetail).apply { setPadding(context.dp(Space.L), 0, 0, 0) },
                linear(0, WRAP_CONTENT, 1f),
            )
        }
    private val title = context.label(TypeScale.TITLE, Palette.LABEL, Weight.SEMIBOLD)
    private val message =
        context.label(TypeScale.SUBHEAD, Palette.SECONDARY).apply { setLineSpacing(0f, LINE_SPACING) }
    private val messageBlock =
        context.vertical().apply {
            addView(title, linear(MATCH_PARENT, WRAP_CONTENT))
            addView(message, linear(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(Space.XS) })
        }

    val view: LinearLayout =
        context.card().apply {
            addView(columns, linear(MATCH_PARENT, WRAP_CONTENT))
            addView(messageBlock, linear(MATCH_PARENT, WRAP_CONTENT))
        }

    fun bind(state: ScreenState) {
        val summary = Summary.of(state)
        view.visibility = if (summary == Summary.Hidden) View.GONE else View.VISIBLE
        val isEstimate = summary is Summary.Estimate
        columns.visibility = if (isEstimate) View.VISIBLE else View.GONE
        messageBlock.visibility = if (isEstimate) View.GONE else View.VISIBLE
        val warning = summary is Summary.LevellingOff
        view.background = context.rounded(if (warning) Palette.NOTE_BACKGROUND else Palette.SURFACE, Radius.CARD)
        when (summary) {
            is Summary.Estimate -> showEstimate(summary, state)
            is Summary.Reached -> showReached(summary, state)
            is Summary.LevellingOff -> showLevellingOff(summary, state)
            else -> SIMPLE_MESSAGES[summary]?.let { (heading, detail) -> show(heading, context.getString(detail)) }
        }
    }

    private fun showLevellingOff(
        levelling: Summary.LevellingOff,
        state: ScreenState,
    ) {
        val plateau = format.wholeTemperature(levelling.plateau, state.unit)
        val target = state.target?.let { format.wholeTemperature(it, state.unit) }.orEmpty()
        show(
            R.string.summary_levelling,
            context.getString(R.string.summary_levelling_detail, plateau, target),
            Palette.NOTE_TEXT
        )
    }

    private fun showEstimate(
        estimate: Summary.Estimate,
        state: ScreenState,
    ) {
        remaining.text = format.duration(estimate.minutes)
        val range = estimate.range
        remainingDetail.text =
            when {
                estimate.isEarly -> context.getString(R.string.early_estimate)
                range != null -> rangeText(range)
                else -> ""
            }
        finish.text = format.clock(estimate.finishMs)
        finishDetail.text = cookingTime(state.samples)
    }

    private fun rangeText(range: Pair<Double, Double>): String =
        context.getString(R.string.range, format.duration(range.first), format.duration(range.second))

    private fun showReached(
        reached: Summary.Reached,
        state: ScreenState,
    ) {
        val at = reached.atMs?.let { format.clock(it) } ?: context.getString(R.string.no_value)
        show(
            R.string.target_reached,
            context.getString(R.string.summary_reached_detail, at, cookingTime(state.samples)),
            Palette.GREEN_TEXT
        )
    }

    private fun show(
        heading: Int,
        detail: String,
        color: Int = Palette.LABEL,
    ) {
        title.setText(heading)
        title.setTextColor(color)
        message.text = detail
        message.setTextColor(if (color == Palette.NOTE_TEXT) Palette.NOTE_TEXT else Palette.SECONDARY)
    }

    private fun cookingTime(samples: List<Sample>): String =
        if (samples.size < 2) {
            ""
        } else {
            context.getString(
                R.string.cooking_for,
                format.duration((samples.last().first - samples.first().first) / MS_PER_MIN)
            )
        }

    private fun column(
        heading: Int,
        value: TextView,
        detail: TextView,
    ): LinearLayout =
        context.vertical().apply {
            addView(context.label(TypeScale.FOOTNOTE, Palette.SECONDARY, Weight.MEDIUM).apply { setText(heading) })
            addView(value, linear(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = context.dp(Space.XS) })
            addView(detail, linear(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = context.dp(Space.XS) })
        }

    private fun value(): TextView = context.label(
        TypeScale.LARGE,
        Palette.LABEL,
        Weight.SEMIBOLD
    ).apply { maxLines = 1 }

    private fun detail(): TextView = context.label(TypeScale.FOOTNOTE, Palette.SECONDARY).apply { maxLines = 1 }

    private companion object {
        const val MS_PER_MIN = 60_000.0
        const val LINE_SPACING = 1.15f
        val SIMPLE_MESSAGES: Map<Summary, Pair<Int, Int>> =
            mapOf(
                Summary.Stalled to (R.string.summary_stalled to R.string.summary_stalled_detail),
                Summary.Falling to (R.string.summary_falling to R.string.summary_falling_detail),
                Summary.NotRising to (R.string.summary_not_rising to R.string.summary_not_rising_detail),
                Summary.NeedsTarget to (R.string.summary_needs_target to R.string.summary_needs_target_detail),
                Summary.Estimating to (R.string.summary_estimating to R.string.summary_estimating_detail),
            )
    }
}
