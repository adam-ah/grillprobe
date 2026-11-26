package app.grillprobe

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

fun Context.dpf(value: Float): Float = value * resources.displayMetrics.density

fun Context.rounded(
    color: Int,
    radius: Int,
): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = dpf(radius.toFloat())
    }

fun ripple(content: Drawable): RippleDrawable = RippleDrawable(ColorStateList.valueOf(Palette.RIPPLE), content, null)

fun Context.label(
    size: Float,
    color: Int = Palette.LABEL,
    weight: Weight = Weight.REGULAR,
): TextView =
    TextView(this).apply {
        textSize = size
        setTextColor(color)
        typeface = weight.typeface
        includeFontPadding = false
        fontFeatureSettings = "tnum"
    }

fun Context.button(
    text: String,
    fill: Int,
    textColor: Int,
    onClick: () -> Unit,
): Button =
    Button(this, null, 0).apply {
        this.text = text
        gravity = Gravity.CENTER
        isAllCaps = false
        textSize = TypeScale.HEADLINE
        typeface = Weight.SEMIBOLD.typeface
        setTextColor(textColor)
        background = ripple(rounded(fill, Radius.CONTROL))
        minHeight = 0
        minimumHeight = 0
        minWidth = 0
        minimumWidth = 0
        stateListAnimator = null
        setOnClickListener { onClick() }
    }

fun Context.vertical(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

fun Context.horizontal(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

fun Context.card(): LinearLayout =
    vertical().apply {
        background = rounded(Palette.SURFACE, Radius.CARD)
        val padding = dp(Space.L)
        setPadding(padding, padding, padding, padding)
    }

fun Context.separator(): View = View(this).apply { setBackgroundColor(Palette.SEPARATOR) }

fun linear(
    width: Int,
    height: Int,
    weight: Float = 0f,
): LinearLayout.LayoutParams = LinearLayout.LayoutParams(width, height, weight)
