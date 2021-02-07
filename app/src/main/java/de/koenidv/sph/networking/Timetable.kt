package de.koenidv.sph.networking

import android.content.Context
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import de.koenidv.sph.database.TimetableDb
import de.koenidv.sph.parsing.RawParser
import java.util.*

//  Created by koenidv on 31.01.2021.
class Timetable {

    /**
     * Load and save timetable from sph
     * This will replace any current lessons in the timetable
     */
    fun fetch(callback: (success: Int) -> Unit) {
        NetworkManager().getSiteAuthed(applicationContext().getString(R.string.url_timetable),
                callback = { success: Int, result: String? ->
                    if (success == NetworkManager.SUCCESS) {
                        TimetableDb.instance!!.clear()
                        TimetableDb.instance!!.save(RawParser().parseTimetable(result!!))
                        applicationContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
                                .edit().putLong("updated_timetable", Date().time).apply()
                    }
                    callback(success)
                })
    }

}