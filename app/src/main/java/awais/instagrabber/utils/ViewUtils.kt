@file:JvmName("ViewUtils")

package awais.instagrabber.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat
import androidx.core.util.Pair
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import kotlin.jvm.internal.Intrinsics

fun createRoundRectDrawableWithIcon(context: Context, rad: Int, iconRes: Int): Drawable? {
    val defaultDrawable = ShapeDrawable(RoundRectShape(FloatArray(8) { rad.toFloat() }, null, null))
    defaultDrawable.paint.color = -0x1
    val d = ResourcesCompat.getDrawable(context.resources, iconRes, null) ?: return null
    val drawable = d.mutate()
    return CombinedDrawable(defaultDrawable, drawable)
}

fun createRoundRectDrawable(rad: Int, defaultColor: Int): Drawable {
    val defaultDrawable = ShapeDrawable(RoundRectShape(FloatArray(8) { rad.toFloat() }, null, null))
    defaultDrawable.paint.color = defaultColor
    return defaultDrawable
}

fun createFrame(
    width: Int,
    height: Float,
    gravity: Int,
    leftMargin: Float,
    topMargin: Float,
    rightMargin: Float,
    bottomMargin: Float
): FrameLayout.LayoutParams {
    val layoutParams = FrameLayout.LayoutParams(getSize(width.toFloat()), getSize(height), gravity)
    layoutParams.setMargins(
        Utils.convertDpToPx(leftMargin), Utils.convertDpToPx(topMargin), Utils.convertDpToPx(rightMargin),
        Utils.convertDpToPx(bottomMargin)
    )
    return layoutParams
}

fun createGradientDrawable(
    orientation: GradientDrawable.Orientation?,
    @ColorInt colors: IntArray?
): GradientDrawable {
    val drawable = GradientDrawable(orientation, colors)
    drawable.shape = GradientDrawable.RECTANGLE
    return drawable
}

private fun getSize(size: Float): Int {
    return if (size < 0) size.toInt() else Utils.convertDpToPx(size)
}

fun measure(view: View, parent: View): Pair<Int, Int> {
    view.measure(
        View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.UNSPECIFIED)
    )
    return Pair(view.measuredHeight, view.measuredWidth)
}

fun getTextViewValueWidth(textView: TextView, text: String?): Float {
    return textView.paint.measureText(text)
}

/**
 * Creates [SpringAnimation] for object.
 * If finalPosition is not [Float.NaN] then create [SpringAnimation] with
 * [SpringForce.mFinalPosition].
 *
 * @param object        Object
 * @param property      object's property to be animated.
 * @param finalPosition [SpringForce.mFinalPosition] Final position of spring.
 * @return [SpringAnimation]
 */
fun springAnimationOf(
    `object`: Any?,
    property: FloatPropertyCompat<Any?>?,
    finalPosition: Float?
): SpringAnimation {
    return finalPosition?.let { SpringAnimation(`object`, property, it) } ?: SpringAnimation(`object`, property)
}

fun suppressLayoutCompat(`$this$suppressLayoutCompat`: ViewGroup, suppress: Boolean) {
    Intrinsics.checkNotNullParameter(`$this$suppressLayoutCompat`, "\$this\$suppressLayoutCompat")
    if (Build.VERSION.SDK_INT >= 29) {
        `$this$suppressLayoutCompat`.suppressLayout(suppress)
    } else {
        hiddenSuppressLayout(`$this$suppressLayoutCompat`, suppress)
    }
}

private var tryHiddenSuppressLayout = true

@SuppressLint("NewApi")
private fun hiddenSuppressLayout(group: ViewGroup, suppress: Boolean) {
    if (tryHiddenSuppressLayout) {
        try {
            group.suppressLayout(suppress)
        } catch (var3: NoSuchMethodError) {
            tryHiddenSuppressLayout = false
        }
    }
}