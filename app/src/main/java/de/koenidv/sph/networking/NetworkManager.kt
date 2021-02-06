package de.koenidv.sph.networking

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.androidnetworking.interfaces.OkHttpResponseListener
import com.androidnetworking.interfaces.StringRequestListener
import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.TAG
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.database.FunctionTilesDb
import de.koenidv.sph.objects.FunctionTile.Companion.FEATURE_CHANGES
import de.koenidv.sph.objects.FunctionTile.Companion.FEATURE_COURSES
import de.koenidv.sph.objects.FunctionTile.Companion.FEATURE_MESSAGES
import de.koenidv.sph.objects.FunctionTile.Companion.FEATURE_TIMETABLE
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Response
import org.json.JSONObject
import java.util.*

//  Created by koenidv on 11.12.2020.
class NetworkManager {

    companion object {
        const val FAILED_UNKNOWN = -1
        const val SUCCESS = 0
        const val FAILED_NO_NETWORK = 1
        const val FAILED_INVALID_CREDENTIALS = 2
        const val FAILED_MAINTENANCE = 3
        const val FAILED_SERVER_ERROR = 4
        const val FAILED_CANCELLED = 5
        const val FAILED_CRYPTION = 6
    }

    fun handlePullToRefresh(destinationId: Int,
                            arguments: Bundle?,
                            callback: (success: Int) -> Unit) {
        val prefs = applicationContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val time = Date().time
        val updateList = mutableListOf<String>()
        var disableList = false

        when (destinationId) {
            R.id.nav_home, R.id.nav_explore -> {
                // Update changes after 10min
                if (time - prefs.getLong("updated_changes", 0) > 10 * 60 * 1000)
                    updateList.add("changes")
                // posts after 30min
                if (time - prefs.getLong("updated_posts", 0) > 30 * 60 * 1000)
                    updateList.add("posts")
                // messages after 20min
                if (time - prefs.getLong("updated_messages", 0) > 20 * 60 * 1000)
                    updateList.add("messages")
                // holidays after 1 month
                if (time - prefs.getLong("updated_messages", 0) > 31 * 86400000L)
                    updateList.add("holidays")
            }
            R.id.nav_courses, R.id.frag_tasks, R.id.frag_allposts, R.id.frag_attachments -> {
                // 2 minutes cooldown, update all posts
                if (time - prefs.getLong("updated_posts", 0) > 2 * 60 * 100)
                    updateList.add("posts")
            }
            R.id.frag_timetable -> {
                updateList.add("timetable")
            }
            R.id.frag_changes -> {
                // Changes fragment
                // Update changes after 30sec cooldown
                if (time - prefs.getLong("updated_changes", 0) > 30 * 1000)
                    updateList.add("changes")
            }
            R.id.frag_course_overview -> {
                disableList = true
                // Course Overview fragment
                // Update posts, tasks, files, links
                if (arguments?.getString("courseId") != null) {
                    // Only after 2 minutes
                    if (time - prefs.getLong("updated_posts_${arguments.getString("courseId")}", 0) > 2 * 60 * 1000) {
                        // Update posts for this course
                        Posts(this).load(listOf(
                                CoursesDb.getInstance().getByInternalId(arguments.getString("courseId")))) {

                            if (it == SUCCESS) {
                                // Send broadcast to update posts, tasks and attachments in CourseOverviewFragment
                                val uiBroadcast = Intent("uichange")
                                uiBroadcast.putExtra("content", "posts")
                                LocalBroadcastManager.getInstance(applicationContext()).sendBroadcast(uiBroadcast)
                            }

                            callback(it)
                        }
                    } else
                    // Just return SUCCESS if posts for this course were updated within the last 2 minutes
                        callback(SUCCESS)
                }
            }
            R.id.frag_webview -> {
                disableList = true
                // WebViewFragment, send a broadcast to reload webview
                val uiBroadcast = Intent("uichange")
                uiBroadcast.putExtra("content", "webview")
                LocalBroadcastManager.getInstance(applicationContext()).sendBroadcast(uiBroadcast)
                // Let the activity know it can hide the refreshing icon
                // But wait a short while first so the user doesn't think nothing happened
                GlobalScope.launch {
                    delay(600)
                    callback(SUCCESS)
                }
            }
        }
        if (!disableList)
            update(updateList) { callback(it) }

    }

    /**
     * Loads a url within the sph and handles authentication
     * @param url URL to load
     */
    fun loadSiteWithToken(url: String,
                          forceNewToken: Boolean = false,
                          callback: (success: Int, result: String?) -> Unit) {

        // Getting an access token
        TokenManager().authenticate(forceNewToken) { success: Int, token: String? ->
            if (success == SUCCESS) {

                // Getting webpage
                AndroidNetworking.get(url)
                        .setUserAgent("sph-planner")
                        .setPriority(Priority.LOW)
                        .build()
                        .getAsString(object : StringRequestListener {
                            override fun onResponse(response: String) {
                                val prefs = applicationContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
                                val responseLine = response.replace("\n", "")
                                if (responseLine.contains("- Schulportal Hessen")
                                        && !responseLine.contains("Login - Schulportal Hessen")
                                        && !responseLine.contains("Fehler - Schulportal Hessen")
                                        && !responseLine.contains("Schulauswahl - Schulportal Hessen")) {
                                    // Getting site was successful
                                    callback(SUCCESS, response)
                                    prefs.edit().putLong("token_last_success", Date().time).apply()
                                } else if (responseLine.contains("Login - Schulportal Hessen")
                                        || responseLine.contains("Fehler - Schulportal Hessen")
                                        || responseLine.contains("Schulauswahl - Schulportal Hessen")) {
                                    // Signin was not successful
                                    callback(FAILED_INVALID_CREDENTIALS, response)
                                    Log.e(TAG, "Invalid credentials for $url")
                                    prefs.edit().putLong("token_last_success", 0).apply()
                                } else if (response.contains("Wartungsarbeiten")) {
                                    // Maintenance work
                                    callback(FAILED_MAINTENANCE, response)
                                }
                            }

                            override fun onError(error: ANError) {
                                when (error.errorDetail) {
                                    "connectionError" -> {
                                        // This will also be called if request timed out
                                        callback(FAILED_NO_NETWORK, "No connection to server or timeout")
                                    }
                                    "requestCancelledError" -> {
                                        callback(FAILED_CANCELLED, "Network request was cancelled")
                                    }
                                    else -> {
                                        Toast.makeText(applicationContext(), "Error for $url",
                                                Toast.LENGTH_LONG).show()
                                        Toast.makeText(applicationContext(),
                                                error.errorCode.toString()
                                                        + ": " + error.errorDetail,
                                                Toast.LENGTH_LONG).show()
                                        // Provide some details for user debugging
                                        callback(FAILED_UNKNOWN,
                                                "--- Network error code: ${error.errorCode}\n"
                                                        + "--- Error description: ${error.errorDetail}\n"
                                                        + "--- Error body: ${error.errorCode}\n"
                                                        + "--- No server response available ---")
                                        // Log the error in Crashlytics
                                        FirebaseCrashlytics.getInstance().recordException(error)
                                    }
                                }
                            }

                        })
            } else callback(success, null)
        }
    }

    /**
     * Load a webpage.
     * Sph pages might be loaded with a token, but they're not guaranteed to be
     */
    fun getJSON(url: String, callback: (success: Int, result: JSONObject?) -> Unit) {
        // Get the site using FAN
        AndroidNetworking.get(url).build()
                .getAsJSONObject(object : JSONObjectRequestListener {
                    override fun onResponse(response: JSONObject) {
                        // Call back with the JSONObject
                        callback(SUCCESS, response)
                    }

                    override fun onError(error: ANError) {
                        // Log error
                        Log.e(TAG, "Loading $url failed")
                        FirebaseCrashlytics.getInstance().recordException(error)
                        // Callback
                        callback(when (error.errorDetail) {
                            "connectionError" -> FAILED_NO_NETWORK
                            "requestCancelledError" -> FAILED_CANCELLED
                            else -> FAILED_UNKNOWN
                        }, null)
                    }

                })
    }


    /**
     * Update multiple scopes
     * @param entries List of strings with scopes to update
     * (See in when below for what's supported)
     */
    fun update(entries: List<String>, callback: (success: Int) -> Unit) {
        if (entries.isNotEmpty()) {
            // Prepare token
            TokenManager().authenticate { success, _ ->
                if (success == SUCCESS) {
                    var number = 0 // Completed calls
                    var lastError: Int? = null // Last error occurred

                    // Callback for each function
                    val checkDone = { thissuccess: Int ->
                        // If the function did not call back successfully,
                        // save the last error to return later
                        if (thissuccess != SUCCESS)
                            lastError = thissuccess
                        // If this was the last function executed,
                        // call back with a success if all went good
                        // or the last error occurred
                        number++
                        if (number == entries.size) {
                            if (lastError == null) callback(SUCCESS)
                            else callback(lastError!!)
                        }

                    }

                    // Run the respective funtion for each update order
                    for (entry in entries) {
                        when (entry) {
                            "posts" -> Posts(this).fetch(checkDone)
                            "changes" -> Changes(this).fetch(checkDone)
                            "timetable" -> Timetable().fetch(checkDone)
                            "messages" -> Messages().fetch(checkDone)
                            "holidays" -> Holidays().fetch(checkDone)
                        }
                    }
                } else callback(success)
            }
        } else callback(SUCCESS)
    }

    /**
     * Called on first sign in, load everything we need
     * This currently includes courses, the timetable, posts, changes, message count, users, holidays
     */
    fun indexAll(status: (status: String) -> Unit, callback: (success: Int) -> Unit) {
        // todo include tiles

        // Get all supported features as String list
        val features = FunctionTilesDb.getInstance().supportedFeatures.map { it.type }
        val loadList = mutableListOf("courses")

        // Mark each supported feature for loading
        if (features.contains(FEATURE_TIMETABLE)) loadList.add("timetable")
        if (features.contains(FEATURE_COURSES)) loadList.add("posts")
        if (features.contains(FEATURE_CHANGES)) loadList.add("changes")
        if (features.contains(FEATURE_MESSAGES)) {
            loadList.add("messages")
            loadList.add("users")
        }
        loadList.add("holidays")

        // Load every item in loadList
        var index = 0
        var loadNextFeature = {}
        val onDone: (Int) -> Unit = {
            if (it == SUCCESS) {
                index++
                // If this was the last feature, call back success
                if (index == loadList.size) callback(SUCCESS)
                // Else load the next feature
                else loadNextFeature()
            } else {
                // Callback an error and stop loading
                callback(it)
                Log.e(TAG, "Error loading ${loadList[index]}: $it")
            }
        }
        // Load the feature at the current index
        loadNextFeature = {
            when (loadList[index]) {
                "courses" -> {
                    Courses(this).createIndex(onDone, features)
                    status(applicationContext().getString(R.string.index_status_courses))
                }
                "timetable" -> {
                    Timetable().fetch(onDone)
                    status(applicationContext().getString(R.string.index_status_timetable))
                }
                "posts" -> {
                    Posts(this).fetch(onDone, true)
                    status(applicationContext().getString(R.string.index_status_posts))
                }
                "changes" -> {
                    Changes(this).fetch(onDone)
                    status(applicationContext().getString(R.string.index_status_changes))
                }
                "messages" -> {
                    Messages().fetch(onDone, true)
                    status(applicationContext().getString(R.string.index_status_messages))
                }
                "users" -> {
                    Users().fetch(onDone)
                    status(applicationContext().getString(R.string.index_status_users))
                }
                "holidays" -> {
                    Holidays().fetch(onDone)
                    status(applicationContext().getString(R.string.index_status_holidays))
                }
                else -> Log.e(TAG, "Unsupported feature " + loadList[index])
            }
        }
        // Begin loading
        loadNextFeature()
    }

    /**
     * Loads an url to resolve it
     * @return resolved url or given url if there was an error
     */
    fun resolveUrl(url: String, callback: (success: Int, resolvedUrl: String) -> Unit) {
        // Getting an access token
        TokenManager().authenticate { success: Int, token: String? ->
            if (success == SUCCESS) {

                // Getting webpage as OkHttp
                AndroidNetworking.get(url)
                        .setUserAgent("koenidv/sph-planner")
                        .setPriority(Priority.LOW)
                        .build()
                        .getAsOkHttpResponse(object : OkHttpResponseListener {
                            override fun onResponse(response: Response) {
                                // In some cases, sph redirects back to the home page (why?)
                                // In this case, ignore the new response
                                if (response.request().url().toString() != "https://start.schulportal.hessen.de/index.php") {
                                    callback(SUCCESS, response.request().url().toString())
                                } else {
                                    callback(FAILED_UNKNOWN, url)
                                }
                            }

                            override fun onError(anError: ANError?) {
                                // Running in emulator will cause an ssl error
                                // Network error will also happen if there is a timeout or no connection
                                // Not a huge deal though, the unresolved url will work as well. For now.
                                Log.e(TAG, anError!!.errorDetail + ": " + url)
                                callback(FAILED_UNKNOWN, url)
                            }

                        })
            } else callback(success, url)
        }
    }

}