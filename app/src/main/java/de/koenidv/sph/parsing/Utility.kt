package de.koenidv.sph.parsing

//  Created by koenidv on 11.12.2020.
class Utility {
    /**
     * Helper to get the nth index of a String in a String
     * @param str String to check
     * @param substr String to look for
     * @param n Ordinal of occurance (0 -> First)
     * @return Index of the nth occurence of a String or -1 if none is found
     */
    fun ordinalIndexOf(str: String, substr: String?, n: Int): Int {
        var order = n
        var pos = -1
        do {
            pos = str.indexOf(substr!!, pos + 1)
        } while (order-- > 0 && pos != -1)
        return pos
    }

    fun nullOrEquals(first: Any?, second: Any?): Boolean {
        return (
                first == null
                        || second == null
                        || first == second
                )
    }

}