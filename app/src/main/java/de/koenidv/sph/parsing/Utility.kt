package de.koenidv.sph.parsing

import android.content.pm.PackageManager
import android.util.TypedValue
import de.koenidv.sph.SphPlanner
import java.util.*


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

    /**
     * Check if an app is installed on device
     * @param packageName Package Id of the app
     * @return true, if app is installed, false, if not
     * Might always return false on Api 30+
     */
    fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Convert px to dp
     */
    fun dpToPx(dp: Float): Float =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, SphPlanner.applicationContext().resources.displayMetrics)


    /**
     * Get today, after 5pm tomorrow and monday on weekends
     * @return 0: monday,.. 4: friday
     */
    fun getCurrentDayAdjusted(): Int {
        // Get today or tomorrow after 5pm
        var weekDay = Calendar.getInstance()[Calendar.DAY_OF_WEEK] - 2
        if (Calendar.getInstance()[Calendar.HOUR_OF_DAY] > 16) weekDay++
        if (weekDay < 0 || weekDay > 4) weekDay = 0
        return weekDay
    }

}