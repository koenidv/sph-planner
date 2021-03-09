package de.koenidv.sph.networking

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.database.ChangesDb
import de.koenidv.sph.debugging.DebugLog
import de.koenidv.sph.debugging.Debugger
import de.koenidv.sph.parsing.RawParser
import java.util.*

//  Created by koenidv on 31.01.2021.
class Changes(private val networkManager: NetworkManager = NetworkManager()) {

    /**
     * Load changes from sph and save them to changes db
     */
    fun fetch(callback: (success: Int) -> Unit) {
      
        // Log fetching changes
        if (Debugger.DEBUGGING_ENABLED)
            DebugLog("Changes", "Fetching changes").log()
      
        networkManager.getSiteAuthed(SphPlanner.appContext().getString(R.string.url_changes),
                callback = { success: Int, result: String? ->

                    if (success == NetworkManager.SUCCESS) {
                        ChangesDb.instance!!.removeOld()
                        ChangesDb.instance!!.save(RawParser().parseChanges(result!!))
                        SphPlanner.appContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
                                .edit().putLong("updated_changes", Date().time).apply()

                        // Send broadcast to update changes
                        val uiBroadcast = Intent("uichange")
                        uiBroadcast.putExtra("content", "changes")
                        LocalBroadcastManager.getInstance(SphPlanner.appContext()).sendBroadcast(uiBroadcast)
                    }
                    callback(success)
                })
    }

}