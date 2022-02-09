package de.koenidv.sph.networking

import android.content.Context
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.appContext
import de.koenidv.sph.debugging.DebugLog
import de.koenidv.sph.debugging.Debugger
import java.util.*


//  Created by StKl JAN-2022
class Clndr {

    /**
     * Fetch and save all own calendar entries from Schulportal
     * @param callback Called on completion with status code
     */
    fun fetch(callback: (success: Int) -> Unit) {
        // Log fetching holidays
        DebugLog("Calendar", "Fetching calendar entries")



        NetworkManager().getSiteAuthed(appContext().getString(R.string.url_calendar),
            callback = { success: Int, result: String? ->

                if (success == NetworkManager.SUCCESS) {
                    appContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
                        .edit().putLong("updated_calendar", Date().time).apply()
                }
                callback(success)
                // Log success
                DebugLog("Calendar", "Fetched calendar: $success",
                    type = Debugger.LOG_TYPE_VAR)
            })

    } // end of fetch(...)


} // end of class