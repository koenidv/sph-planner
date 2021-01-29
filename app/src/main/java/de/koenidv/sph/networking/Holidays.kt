package de.koenidv.sph.networking

import android.util.Log
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONArrayRequestListener
import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.TAG
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import de.koenidv.sph.database.HolidaysDb
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
        // Get all holidays for HE from ferien-api.de
        AndroidNetworking.get(applicationContext().getString(R.string.url_holidays))
                .build()
                .getAsJSONArray(object : JSONArrayRequestListener {
                    override fun onResponse(response: JSONArray?) {
                        // Return if response is somehow null
                        if (response == null) {
                            callback(NetworkManager.FAILED_UNKNOWN)
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
                    }

                    override fun onError(error: ANError) {
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