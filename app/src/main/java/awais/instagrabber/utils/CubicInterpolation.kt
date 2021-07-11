package awais.instagrabber.utils

import java.util.*

class CubicInterpolation @JvmOverloads constructor(array: FloatArray, cubicTension: Int = 0) {
    private val array: FloatArray
    private val tangentFactor: Int
    private val length: Int
    private fun getTangent(k: Int): Float {
        return tangentFactor * (getClippedInput(k + 1) - getClippedInput(k - 1)) / 2
    }

    fun interpolate(t: Float): Float {
        val k = Math.floor(t.toDouble()).toInt()
        val m = floatArrayOf(getTangent(k), getTangent(k + 1))
        val p = floatArrayOf(getClippedInput(k), getClippedInput(k + 1))
        val t1 = t - k
        val t2 = t1 * t1
        val t3 = t1 * t2
        return (2 * t3 - 3 * t2 + 1) * p[0] + (t3 - 2 * t2 + t1) * m[0] + (-2 * t3 + 3 * t2) * p[1] + (t3 - t2) * m[1]
    }

    private fun getClippedInput(i: Int): Float {
        return if (i >= 0 && i < length) {
            array[i]
        } else array[clipClamp(i, length)]
    }

    private fun clipClamp(i: Int, n: Int): Int {
        return Math.max(0, Math.min(i, n - 1))
    }

    init {
        this.array = Arrays.copyOf(array, array.size)
        length = array.size
        tangentFactor = 1 - Math.max(0, Math.min(1, cubicTension))
    }
}