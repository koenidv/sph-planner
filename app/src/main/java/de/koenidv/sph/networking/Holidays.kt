package de.koenidv.sph.networking

import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONArrayRequestListener
import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.appContext
import de.koenidv.sph.SphPlanner.Companion.prefs
import de.koenidv.sph.database.HolidaysDb
import de.koenidv.sph.debugging.DebugLog
import de.koenidv.sph.debugging.Debugger
import de.koenidv.sph.objects.Holiday
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

//  Created by koenidv on 29.01.2021.
class Holidays {

    /**
     * Fetch and save all future holidays for Hesse
     * @param callback Called on completion with status code
     */
    fun fetch(callback: (success: Int) -> Unit) {
        // Log fetching holidays
        if (Debugger.DEBUGGING_ENABLED)
            DebugLog("Holidays", "Fetching holidays").log()

        // Get all holidays for HE from ferien-api.de
        AndroidNetworking.get(appContext().getString(R.string.url_holidays))
                .build()
                .getAsJSONArray(object : JSONArrayRequestListener {
                    override fun onResponse(response: JSONArray?) {
                        // Return if response is somehow null
                        if (response == null) {
                            callback(NetworkManager.FAILED_UNKNOWN)
                            // Log error
                            if (Debugger.DEBUGGING_ENABLED)
                                DebugLog("Holidays",
                                        "Error fetching holidays: Response is null",
                                        type = Debugger.LOG_TYPE_ERROR).log()
                            return
                        }

                        // Get current date
                        val now = Date()
                        var start: Date
                        var obj: JSONObject
                        val dateformat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.ROOT)
                        val holidaysDb = HolidaysDb()
                        // Parse JSON Array to list of Holiday object
                        // Check each object in the array
                        for (i in 0 until response.length()) {
                            obj = response[i] as JSONObject
                            start = dateformat.parse(obj.getString("start"))!!
                            // Only add this holiday if it is in the future,
                            // also, the api started duplicating events, once with spaces,
                            // once with dashes. Only use those with dashes
                            if (start.after(now)
                                    && !obj.getString("slug").contains(" ")) {
                                // Parse object and save to Db
                                holidaysDb.save(Holiday(
                                        obj.getString("slug"),
                                        start,
                                        dateformat.parse(obj.getString("end"))!!,
                                        obj.getString("name"),
                                        obj.getString("year")
                                ))
                            }
                        }
                        callback(NetworkManager.SUCCESS)
                        prefs.edit().putLong("updated_holidays", Date().time).apply()
                        // Log success
                        if (Debugger.DEBUGGING_ENABLED)
                            DebugLog("Holidays", "Holidays fetched: Success",
                                    type = Debugger.LOG_TYPE_SUCCESS).log()
                    }

                    override fun onError(error: ANError) {
                        // Log error
                        if (Debugger.DEBUGGING_ENABLED)
                            DebugLog("Holidays", "Error loading holidays",
                                    error).log()

                        when (error.errorDetail) {
                            "connectionError" -> {
                                // This will also be called if request timed out
                                callback(NetworkManager.FAILED_NO_NETWORK)
                            }
                            "requestCancelledError" -> {
                                callback(NetworkManager.FAILED_CANCELLED)
                            }
                            else -> {
                                callback(NetworkManager.FAILED_UNKNOWN)
                                // Some other error, log to Crashlytics
                                FirebaseCrashlytics.getInstance().recordException(error)
                            }
                        }
                    }

                })
    }

}