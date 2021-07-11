package awais.instagrabber.utils

/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Container to ease passing around a tuple of two objects. This object provides a sensible
 * implementation of equals(), returning true if equals() is true on each of the contained
 * objects.
 */
/**
 * Constructor for a Pair.
 *
 * @param first  the first object in the Pair
 * @param second the second object in the pair
 */
data class NullSafePair<F, S>(@JvmField val first: F, @JvmField val second: S) {
    companion object {
        /**
         * Convenience method for creating an appropriately typed pair.
         *
         * @param a the first object in the Pair
         * @param b the second object in the pair
         * @return a Pair that is templatized with the types of a and b
         */
        fun <A, B> create(a: A, b: B): NullSafePair<A, B> {
            return NullSafePair(a, b)
        }
    }
}