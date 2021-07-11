@file:JvmName("NumberUtils")

package awais.instagrabber.utils

import java.util.*
import kotlin.math.ln
import kotlin.math.pow

fun getResultingHeight(requiredWidth: Int, height: Int, width: Int): Int {
    return requiredWidth * height / width
}

fun getResultingWidth(requiredHeight: Int, height: Int, width: Int): Int {
    return requiredHeight * width / height
}

// TODO Replace all usages with kotlin Random.nextLong() once converted to kotlin
fun random(origin: Long, bound: Long): Long {
    val random = Random()
    var r = random.nextLong()
    val n = bound - origin
    val m = n - 1
    when {
        n and m == 0L -> r = (r and m) + origin // power of two
        n > 0L -> {
            // reject over-represented candidates
            var u = r ushr 1 // ensure non-negative
            while (u + m - u % n.also { r = it } < 0L) { // rejection check
                // retry
                u = random.nextLong() ushr 1
            }
            r += origin
        }
        else -> {
            // range not representable as long
            while (r < origin || r >= bound) r = random.nextLong()
        }
    }
    return r
}

fun calculateWidthHeight(height: Int, width: Int, maxHeight: Int, maxWidth: Int): NullSafePair<Int, Int> {
    if (width > maxWidth) {
        var tempHeight = getResultingHeight(maxWidth, height, width)
        var tempWidth = maxWidth
        if (tempHeight > maxHeight) {
            tempWidth = getResultingWidth(maxHeight, tempHeight, tempWidth)
            tempHeight = maxHeight
        }
        return NullSafePair(tempWidth, tempHeight)
    }
    if (height < maxHeight && width < maxWidth || height > maxHeight) {
        var tempWidth = getResultingWidth(maxHeight, height, width)
        var tempHeight = maxHeight
        if (tempWidth > maxWidth) {
            tempHeight = getResultingHeight(maxWidth, tempHeight, tempWidth)
            tempWidth = maxWidth
        }
        return NullSafePair(tempWidth, tempHeight)
    }
    return NullSafePair(width, height)
}

fun roundFloat2Decimals(value: Float): Float {
    return ((value + (if (value >= 0) 1 else -1) * 0.005f) * 100).toInt() / 100f
}

fun abbreviate(number: Long, options: AbbreviateOptions? = null): String {
    // adapted from https://stackoverflow.com/a/9769590/1436766
    var threshold = 1000
    var addSpace = false
    if (options != null) {
        threshold = options.threshold
        addSpace = options.addSpaceBeforePrefix
    }
    if (number < threshold) return "" + number
    val exp = (ln(number.toDouble()) / ln(threshold.toDouble())).toInt()
    return String.format(
        Locale.US,
        "%.1f%s%c",
        number / threshold.toDouble().pow(exp.toDouble()),
        if (addSpace) " " else "",
        "kMGTPE"[exp - 1]
    )
}

data class AbbreviateOptions(val threshold: Int = 1000, val addSpaceBeforePrefix: Boolean = false)