package de.koenidv.sph.database

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.koenidv.sph.SphPlanner.Companion.cacheprefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

//  Created by koenidv on 22.02.2021.
class CacheManager {

    companion object {
        /**
         * Called on app startup
         * Retrieve cache maps from SharedPrefs
         */
        fun init() {
            CoroutineScope(Dispatchers.IO).launch {
                val cache = CacheManager()
                // Restore some processed values from sharedprefs
                // Only restore if not older than a week and same locale
                if (Date().time - cacheprefs.getLong("time", 0) < 7 * 24 * 360 * 1000 &&
                        cacheprefs.getString("locale", "") == Locale.getDefault().language) {
                    // Restore users
                    cache.restoreUsers()
                    // Restore course colors
                    cache.restoreCourseColors()
                } else {
                    // Reset time if invalid
                    cacheprefs.edit().putLong("time", 0).apply()
                }
            }
        }

        /**
         * Called on app exit
         * Save chache maps to SharedPrefs
         */
        fun save() {
            // Set time and locale if invalid
            if (cacheprefs.getLong("time", 0) == 0L) {
                cacheprefs.edit().putLong("time", Date().time).apply()
            }
            if (cacheprefs.getString("locale", "") != Locale.getDefault().language) {
                cacheprefs.edit().putString("locale", Locale.getDefault().language).apply()
            }
            // Save users cache
            cacheprefs.edit().putString("users_cache", Gson().toJson(UsersDb.cache)).apply()
            // Save course colors cache
            cacheprefs.edit().putString("course_colors_cache", Gson().toJson(CoursesDb.colorCache)).apply()
        }
    }


    /**
     *
     *  User names
     *
     */

    /**
     * Restore users cache
     */
    fun restoreUsers() {
        val userstype = object : TypeToken<Map<String, Map<String, String>>>() {}.type
        UsersDb.cache = Gson().fromJson(
                cacheprefs.getString("users_cache", ""), userstype)
    }


    /**
     *
     *  Courses
     *
     */

    /**
     * Restore course colors
     */
    fun restoreCourseColors() {
        val maptype = object : TypeToken<Map<String, Int>>() {}.type
        CoursesDb.colorCache = Gson().fromJson(
                cacheprefs.getString("course_colors_cache", ""), maptype)
    }

    /**
     * Invalidate course color cache
     */
    fun invalidateCourseColors() {
        // Clear course color cache in SharedPrefs as well as in CoursesDb
        cacheprefs.edit().putString("course_colors_cache", "").apply()
        CoursesDb.colorCache.clear()
    }


}