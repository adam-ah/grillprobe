package app.grillprobe

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout

class HeroSection(
    private val context: Context,
    private val format: Formatter,
    private val actions: MainScreen.Actions,
) {
    private val current =
        context.label(TypeScale.HERO, Palette.LABEL, Weight.LIGHT).apply {
            maxLines = 1
            setAutoSizeTextTypeUniformWithConfiguration(
                HERO_MIN_SP,
                TypeScale.HERO.toInt(),
                2,
                TypedValue.COMPLEX_UNIT_SP
            )
        }
    private val rate = context.label(TypeScale.SUBHEAD, Palette.SECONDARY, Weight.MEDIUM)
    private val presetIcon = ImageView(
        context
    ).apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
    private val targetCaption =
        context.label(TypeScale.FOOTNOTE, Palette.SECONDARY, Weight.MEDIUM).apply {
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
    private val targetValue =
        context.label(TypeScale.LARGE, Palette.RED, Weight.SEMIBOLD).apply {
            maxLines = 1
            isClickable = true
            contentDescription = context.getString(R.string.edit_target)
            setOnClickListener { actions.editTarget() }
        }
    private var shownPreset: Preset? = null

    val view: LinearLayout =
        context.horizontal().apply {
            gravity = Gravity.BOTTOM
            addView(readingColumn(), linear(0, WRAP_CONTENT, 1f).apply { marginEnd = context.dp(Space.M) })
            addView(targetCard(), linear(context.dp(TARGET_CARD_WIDTH), WRAP_CONTENT))
        }

    fun bind(state: ScreenState) {
        current.text =
            state.temperature
                ?.let { format.styled(format.temperature(it, state.unit), Palette.SECONDARY) }
                ?: context.getString(R.string.no_value)
        val prediction = state.prediction
        val moving = prediction?.takeIf { it.trend == Trend.RISING || it.trend == Trend.FALLING }
        rate.text = moving?.let { format.rate(it.rate, state.unit) }.orEmpty()
        rate.setTextColor(if (moving?.trend == Trend.FALLING) Palette.BLUE else Palette.TINT)
        rate.visibility = if (moving == null) View.INVISIBLE else View.VISIBLE
        val target = state.target
        targetValue.text = target?.let { format.styled(format.wholeTemperature(it, state.unit), Palette.RED) }
            ?: context.getString(R.string.set_target)
        targetValue.setTextColor(if (target == null) Palette.SECONDARY else Palette.RED)
        targetValue.textSize = if (target == null) TypeScale.TITLE else TypeScale.LARGE
        val name = context.getString(state.preset?.label ?: R.string.custom_target)
        val caption = if (target == null) {
            context.getString(
                R.string.target
            )
        } else {
            context.getString(R.string.target_with_name, name)
        }
        targetCaption.text = caption
        showPreset(state.preset)
    }

    private fun showPreset(preset: Preset?) {
        presetIcon.visibility = if (preset == null) View.GONE else View.VISIBLE
        if (preset == null || preset == shownPreset) {
            shownPreset = preset
            return
        }
        val popIn = shownPreset?.animal != preset.animal
        shownPreset = preset
        presetIcon.setImageResource(preset.animal.icon)
        if (popIn) {
            presetIcon.scaleX = POP_START
            presetIcon.scaleY = POP_START
            presetIcon
                .animate()
                .scaleX(1f)
                .scaleY(1f)
                .setInterpolator(OvershootInterpolator(POP_TENSION))
                .setDuration(POP_MS)
                .start()
        }
    }

    private fun readingColumn(): View =
        context.vertical().apply {
            gravity = Gravity.CENTER_HORIZONTAL
            addView(caption(R.string.current), linear(WRAP_CONTENT, WRAP_CONTENT))
            current.gravity = Gravity.CENTER
            addView(current, linear(MATCH_PARENT, context.dp(HERO_HEIGHT)).apply { topMargin = context.dp(Space.XXS) })
            addView(rate, linear(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = context.dp(Space.XS) })
        }

    private fun targetCard(): View =
        context.card().apply {
            val padding = context.dp(Space.M)
            setPadding(padding, padding, padding, padding)
            targetCaption.gravity = Gravity.CENTER
            addView(targetCaption, linear(MATCH_PARENT, WRAP_CONTENT))
            val row = context.horizontal().apply { gravity = Gravity.CENTER }
            val icon = context.dp(PRESET_ICON)
            row.addView(presetIcon, linear(icon, icon).apply { marginEnd = context.dp(Space.S) })
            row.addView(targetValue, linear(WRAP_CONTENT, WRAP_CONTENT))
            addView(row, linear(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(Space.XS) })
            addView(stepper(), linear(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(Space.S) })
        }

    private fun stepper(): View =
        context.horizontal().apply {
            background = context.rounded(Palette.FILL, Radius.CONTROL)
            addView(stepButton(R.string.decrease, R.string.lower_target, -1), linear(0, context.dp(STEPPER_HEIGHT), 1f))
            addView(context.separator(), linear(1, context.dp(STEPPER_DIVIDER)))
            addView(stepButton(R.string.increase, R.string.raise_target, 1), linear(0, context.dp(STEPPER_HEIGHT), 1f))
        }

    private fun stepButton(
        symbol: Int,
        description: Int,
        delta: Int,
    ): Button =
        context.button(context.getString(symbol), Palette.FILL, Palette.LABEL) { actions.stepTarget(delta) }.apply {
            textSize = TypeScale.TITLE
            typeface = Weight.REGULAR.typeface
            contentDescription = context.getString(description)
        }

    private fun caption(text: Int) = context.label(TypeScale.FOOTNOTE, Palette.SECONDARY, Weight.MEDIUM).apply {
        setText(text)
    }

    private companion object {
        const val HERO_HEIGHT = 84
        const val TARGET_CARD_WIDTH = 168
        const val HERO_MIN_SP = 40
        const val PRESET_ICON = 40
        const val POP_START = 0.4f
        const val POP_TENSION = 3f
        const val POP_MS = 420L
        const val STEPPER_HEIGHT = 34
        const val STEPPER_DIVIDER = 18
    }
}
