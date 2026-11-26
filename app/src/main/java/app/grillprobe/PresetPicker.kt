package app.grillprobe

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout

class PresetPicker(
    private val context: Context,
    private val format: Formatter,
    private val actions: MainScreen.Actions,
) {
    private var openAnimal: Animal? = null
    private var shownUnit: TemperatureUnit? = null
    private var lastState: ScreenState? = null
    private var chips: List<Pair<Preset, Button>> = emptyList()
    private val animalButtons: Map<Animal, LinearLayout> = Animal.entries.associateWith { animalButton(it) }
    private val chipRow = context.horizontal()
    private val chipScroll =
        HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            isHorizontalFadingEdgeEnabled = true
            setFadingEdgeLength(context.dp(Space.XL))
        }
    private val animals =
        context.horizontal().apply {
            animalButtons.values.forEach { addView(it, linear(0, MATCH_PARENT, 1f)) }
        }
    private val detail =
        context.horizontal().apply {
            visibility = View.GONE
            val back = context.button(context.getString(R.string.back), Palette.FILL, Palette.LABEL) { close() }
            back.textSize = TypeScale.TITLE
            back.background = ripple(context.rounded(Palette.FILL, Radius.PILL))
            back.contentDescription = context.getString(R.string.back_to_animals)
            val size = context.dp(BACK_SIZE)
            addView(back, linear(size, size).apply { marginEnd = context.dp(Space.S) })
            chipScroll.addView(chipRow)
            addView(chipScroll, linear(0, WRAP_CONTENT, 1f))
        }

    val view: FrameLayout =
        FrameLayout(context).apply {
            minimumHeight = context.dp(PICKER_HEIGHT)
            addView(animals, FrameLayout.LayoutParams(MATCH_PARENT, context.dp(PICKER_HEIGHT)))
            addView(detail, FrameLayout.LayoutParams(MATCH_PARENT, context.dp(PICKER_HEIGHT)))
        }

    fun bind(state: ScreenState) {
        lastState = state
        animalButtons.forEach { (animal, button) ->
            val fill = if (state.preset?.animal == animal) Palette.SURFACE else Palette.BACKGROUND
            button.background = ripple(context.rounded(fill, Radius.CARD))
        }
        val animal = openAnimal
        if (animal != null && state.unit != shownUnit) fillChips(animal, state.unit)
        chips.forEach { (preset, chip) ->
            val selected = preset == state.preset
            chip.background = ripple(context.rounded(if (selected) Palette.TINT else Palette.FILL, Radius.PILL))
            chip.setTextColor(if (selected) Palette.WHITE else Palette.LABEL)
        }
    }

    private fun open(animal: Animal) {
        openAnimal = animal
        shownUnit = null
        swap(animals, detail)
        lastState?.let { bind(it) }
        val selected = chips.firstOrNull { (preset, _) -> preset == lastState?.preset }?.second
        chipScroll.scrollTo(0, 0)
        if (selected != null) chipScroll.post { chipScroll.smoothScrollTo(selected.left - context.dp(Space.L), 0) }
    }

    private fun close() {
        openAnimal = null
        swap(detail, animals)
    }

    private fun swap(
        from: View,
        to: View,
    ) {
        from.visibility = View.GONE
        to.alpha = 0f
        to.visibility = View.VISIBLE
        to.animate().alpha(1f).setDuration(FADE_MS).start()
    }

    private fun fillChips(
        animal: Animal,
        unit: TemperatureUnit,
    ) {
        shownUnit = unit
        chipRow.removeAllViews()
        chips = Preset.of(animal).map { it to chip(it, unit) }
        chips.forEach { (_, chip) ->
            chipRow.addView(
                chip,
                linear(WRAP_CONTENT, context.dp(CHIP_HEIGHT)).apply { marginEnd = context.dp(Space.S) }
            )
        }
    }

    private fun chip(
        preset: Preset,
        unit: TemperatureUnit,
    ): Button {
        val text =
            context.getString(
                R.string.preset_chip,
                context.getString(preset.label),
                format.wholeTemperature(preset.celsius.toDouble(), unit),
            )
        return context.button(text, Palette.FILL, Palette.LABEL) { actions.selectPreset(preset) }.apply {
            textSize = TypeScale.SUBHEAD
            typeface = Weight.MEDIUM.typeface
            val size = context.dp(CHIP_ICON)
            val icon = context.getDrawable(preset.animal.icon)?.apply { setBounds(0, 0, size, size) }
            setCompoundDrawablesRelative(icon, null, null, null)
            compoundDrawablePadding = context.dp(Space.XS)
            setPadding(context.dp(Space.XS), 0, context.dp(Space.L), 0)
        }
    }

    private fun animalButton(animal: Animal): LinearLayout =
        context.vertical().apply {
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            contentDescription = context.getString(animal.label)
            setOnClickListener { open(animal) }
            val icon = ImageView(context).apply { setImageResource(animal.icon) }
            val size = context.dp(ICON_SIZE)
            addView(icon, linear(size, size))
            addView(
                context.label(TypeScale.CAPTION, Palette.LABEL, Weight.MEDIUM).apply { setText(animal.label) },
                linear(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = context.dp(Space.XXS) },
            )
        }

    private companion object {
        const val PICKER_HEIGHT = 72
        const val ICON_SIZE = 44
        const val BACK_SIZE = 40
        const val CHIP_HEIGHT = 40
        const val CHIP_ICON = 30
        const val FADE_MS = 160L
    }
}
