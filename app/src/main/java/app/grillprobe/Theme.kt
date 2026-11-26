package app.grillprobe

import android.graphics.Typeface

object Palette {
    const val BACKGROUND = 0xFFF2F2F7.toInt()
    const val SURFACE = 0xFFFFFFFF.toInt()
    const val FILL = 0xFFE9E9EE.toInt()
    const val SEPARATOR = 0xFFE2E2E7.toInt()
    const val LABEL = 0xFF1C1C1E.toInt()
    const val SECONDARY = 0xFF6E6E73.toInt()
    const val TINT = 0xFFFF9500.toInt()
    const val TINT_DARK = 0xFFE07B00.toInt()
    const val RED = 0xFFFF3B30.toInt()
    const val BLUE = 0xFF007AFF.toInt()
    const val PREDICTION = 0x8C5AA9FF.toInt()
    const val GREEN = 0xFF34C759.toInt()
    const val GREEN_TEXT = 0xFF248A3D.toInt()
    const val NOTE_BACKGROUND = 0xFFFFF4D6.toInt()
    const val NOTE_TEXT = 0xFF8A5300.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()
    const val RIPPLE = 0x1F000000
    const val FILL_TOP = 0x40FF9500
    const val FILL_BOTTOM = 0x00FF9500
}

object TypeScale {
    const val ALARM = 96f
    const val HERO = 80f
    const val LARGE = 30f
    const val TITLE = 22f
    const val HEADLINE = 17f
    const val SUBHEAD = 15f
    const val FOOTNOTE = 13f
    const val CAPTION = 12f
    const val CAPTION2 = 11f
}

object Space {
    const val XXS = 2
    const val XS = 4
    const val S = 8
    const val M = 12
    const val L = 16
    const val XL = 24
    const val XXL = 32
}

object Radius {
    const val CARD = 16
    const val CONTROL = 12
    const val PILL = 999
}

enum class Weight(
    private val value: Int,
) {
    LIGHT(value = 300),
    REGULAR(value = 400),
    MEDIUM(value = 500),
    SEMIBOLD(value = 600),
    ;

    val typeface: Typeface by lazy { Typeface.create(Typeface.DEFAULT, value, false) }
}
