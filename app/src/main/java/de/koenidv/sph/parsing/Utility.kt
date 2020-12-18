package de.koenidv.sph.parsing

import de.koenidv.sph.SphPlanner


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

    /**
     * Helper to determine if either one element is null or both are equal
     * @returns true if any element is null or both are equal
     */
    fun nullOrEquals(first: Any?, second: Any?): Boolean {
        return (
                first == null
                        || second == null
                        || first == second
                )
    }

    /**
     * Helper to parse a map in string array res
     * Defined in arrays as <string-array>...<item>key|value</item>
     * @param stringArrayResourceId Resource id of the string array to be parsed
     * @return Parsed string map
     */
    fun parseStringArray(stringArrayResourceId: Int): Map<String, String> {
        val stringArray: Array<String> = SphPlanner.applicationContext().resources.getStringArray(stringArrayResourceId)
        val outputMap = mutableMapOf<String, String>()
        for (entry in stringArray) {
            val splitResult = entry.split("|")
            outputMap[splitResult[0]] = splitResult[1]
        }
        return outputMap.toMap()
    }

}