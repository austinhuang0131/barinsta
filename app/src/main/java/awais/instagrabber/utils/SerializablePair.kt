package awais.instagrabber.utils

import android.util.Pair
import java.io.Serializable

/**
 * Constructor for a Pair.
 *
 * @param first  the first object in the Pair
 * @param second the second object in the pair
 */
data class SerializablePair<F, S>(@JvmField val first: F, @JvmField val second: S) : Pair<F, S>(first, second), Serializable