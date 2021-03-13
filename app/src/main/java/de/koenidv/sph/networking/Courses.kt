package de.koenidv.sph.networking

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.database.FunctionTilesDb
import de.koenidv.sph.debugging.DebugLog
import de.koenidv.sph.debugging.Debugger
import de.koenidv.sph.objects.FunctionTile
import de.koenidv.sph.parsing.RawParser
import java.util.*

//  Created by koenidv on 31.01.2021.
class Courses(private val networkManager: NetworkManager = NetworkManager()) {

    /**
     * Load and parse courses from timetable, studygroups and mycourses, if supported
     * @param features: List of supported features. Defaults to values from the database
     * @param callback: Callback on success or error
     */
    fun createIndex(
            callback: (success: Int) -> Unit,
            features: List<String?> = FunctionTilesDb.getInstance().supportedFeatures.map { it.type }) {
        // Log creating course index
        DebugLog("Courses", "Starting course indexing")

        val prefs = SphPlanner.appContext().getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)
        // Remove old courses, it'll just lead to isses
        val coursesDb = CoursesDb
        coursesDb.clear()
        // Set courses last updated to 0 in case this gets cancelled
        prefs.edit().putLong("courses_last_updated", 0).apply()

        // Mark each supported feature that we might need for courses loading
        val loadList = mutableListOf<String>()
        // Firstly, load courses from timetable so we have an overview
        if (features.contains(FunctionTile.FEATURE_TIMETABLE)) loadList.add("timetable")
        // Secondly, load those courses from study groups to find out where the user belongs
        if (features.contains(FunctionTile.FEATURE_STUDYGROUPS)) loadList.add("studygroups")
        // Lastly, load courses again from posts overview to get number ids
        if (features.contains(FunctionTile.FEATURE_COURSES)) loadList.add("mycourses")

        // Load each item from loadList
        var index = 0
        var loadNextCourses = {}
        val onDone: (Int) -> Unit = {
            if (it == NetworkManager.SUCCESS) {
                index++
                // If this was the last feature, save the current time and call back success
                if (index == loadList.size) {
                    // Remember when we last updated the courses
                    prefs.edit().putLong("courses_last_updated", Date().time).apply()
                    callback(NetworkManager.SUCCESS)
                }
                // Else load the next feature
                else loadNextCourses()
            } else {
                // Callback an error and stop loading
                callback(it)
                Log.d(SphPlanner.TAG, "Error loading " + loadList[index])
            }
        }
        // Load the feature at the current index
        loadNextCourses = {
            // Log loading courses
            DebugLog("Changes", "Loading courses from ${loadList[index]}")

            when (loadList[index]) {
                "timetable" -> {
                    networkManager.getSiteAuthed(
                            SphPlanner.appContext().getString(R.string.url_timetable),
                            callback = { success: Int, response: String? ->
                                if (success == NetworkManager.SUCCESS) {
                                    // Save parsed courses from timetable
                                    coursesDb.save(RawParser().parseCoursesFromTimetable(response!!))
                                    // Remember when we last updated the courses from the timetable
                                    prefs.edit().putLong("courses_last_updated_timetable", Date().time).apply()
                                }
                                onDone(success)
                                // Log success
                                DebugLog("Courses",
                                        "Loaded courses from timetable: $success",
                                        type = Debugger.LOG_TYPE_VAR)
                            })
                }
                "studygroups" -> {
                    networkManager.getSiteAuthed(
                            SphPlanner.appContext().getString(R.string.url_studygroups),
                            callback = { success: Int, response: String? ->
                                if (success == NetworkManager.SUCCESS) {
                                    // We now know which courses are favorites,
                                    // so mark all unknown as not favorite
                                    coursesDb.setNulledNotFavorite()
                                    // Save parsed courses from study groups
                                    coursesDb.save(RawParser().parseCoursesFromStudygroups(response!!))
                                    // Remember when we last updated the courses from studygroups
                                    prefs.edit().putLong("courses_last_updated_studygroups", Date().time).apply()
                                }
                                onDone(success)
                                // Log success
                                DebugLog("Courses",
                                        "Loaded courses from studygroups: $success",
                                        type = Debugger.LOG_TYPE_VAR)
                            })
                }
                "mycourses" -> {
                    networkManager.getSiteAuthed(
                            SphPlanner.appContext().getString(R.string.url_mycourses),
                            callback = { success: Int, response: String? ->
                                if (success == NetworkManager.SUCCESS) {
                                    // Save parsed courses from posts overview
                                    coursesDb.save(RawParser().parseCoursesFromPostsoverview(response!!))
                                    // Remember when we last updated the courses from my courses
                                    prefs.edit().putLong("courses_last_updated_mycourses", Date().time).apply()
                                }
                                onDone(success)
                                // Log success
                                DebugLog("Courses",
                                        "Loaded courses from mycourses: $success",
                                        type = Debugger.LOG_TYPE_VAR)
                            })
                }
            }
        }
        // Begin loading
        loadNextCourses()
    }
}