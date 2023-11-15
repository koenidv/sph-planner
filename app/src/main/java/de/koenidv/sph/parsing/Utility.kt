package de.koenidv.sph.parsing

import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import java.util.*


//  Created by koenidv on 11.12.2020.
object Utility {
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
        val stringArray: Array<String> = SphPlanner.appContext().resources.getStringArray(stringArrayResourceId)
        val outputMap = mutableMapOf<String, String>()
        for (entry in stringArray) {
            val splitResult = entry.split("|")
            outputMap[splitResult[0]] = splitResult[1]
        }
        return outputMap.toMap()
    }

    /**
     * Convert px to dp
     */
    fun dpToPx(dp: Float): Float =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, SphPlanner.appContext().resources.displayMetrics)


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

    /**
     * Returns a contextual, sometimes random greeting for the user
     */
    fun getGreeting(): String {
        val prefs = SphPlanner.appContext().getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val random = Math.random()
        val greeting = when {
            random < 0.05 ->
                SphPlanner.appContext().getString(R.string.greeting_random_general_1)
            random < 0.1 ->
                SphPlanner.appContext().getString(R.string.greeting_random_general_2)
            random < 0.15 ->
                SphPlanner.appContext().getString(R.string.greeting_random_general_3)
            dayOfWeek == Calendar.FRIDAY && random < 0.4 ->
                SphPlanner.appContext().getString(R.string.greeting_friday_weekend)
            hourOfDay > 21 && random < 0.8 ->
                SphPlanner.appContext().getString(R.string.greeting_night)
            hourOfDay > 17 && random < 0.1 ->
                SphPlanner.appContext().getString(R.string.greeting_random_evening_1)
            hourOfDay > 17 && random < 0.2 ->
                SphPlanner.appContext().getString(R.string.greeting_random_evening_2)
            hourOfDay > 17 && random < 0.9 ->
                SphPlanner.appContext().getString(R.string.greeting_evening)
            hourOfDay < 8 && random < 0.1 ->
                SphPlanner.appContext().getString(R.string.greeting_random_morning_1)
            hourOfDay < 8 && random < 0.2 ->
                SphPlanner.appContext().getString(R.string.greeting_random_morning_2)
            hourOfDay < 8 ->
                SphPlanner.appContext().getString(R.string.greeting_morning)
            else -> SphPlanner.appContext().getString(R.string.greeting_general)
        }
        return greeting.replace("%name", prefs.getString("real_name", "").toString())
    }

    /**
     * Tints a view's background
     * @param view View to tint
     * @param color Color to use as color int
     * @param opacity opacity as hex int, i.e. 0xb4000000 for about 70%
     */
    fun tintBackground(view: View, color: Int, opacity: Int) {
        if (view.background is StateListDrawable || view.background is LayerDrawable)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                view.background.colorFilter = BlendModeColorFilter(
                        color and 0x00FFFFFF or opacity, BlendMode.SRC_ATOP)
            } else {
                @Suppress("DEPRECATION") // not in < Q
                view.background.setColorFilter(
                        color and 0x00FFFFFF or opacity, PorterDuff.Mode.SRC_ATOP)
            }
        else Log.e(SphPlanner.TAG, view.background.javaClass.toString() + " is not marked as tintable")
    }

}