package de.koenidv.sph.networking

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.androidnetworking.interfaces.OkHttpResponseListener
import com.androidnetworking.interfaces.StringRequestListener
import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.koenidv.sph.BuildConfig
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.TAG
import de.koenidv.sph.SphPlanner.Companion.appContext
import de.koenidv.sph.SphPlanner.Companion.prefs
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.database.FunctionTilesDb
import de.koenidv.sph.debugging.DebugLog
import de.koenidv.sph.debugging.Debugger
import de.koenidv.sph.objects.FunctionTile.Companion.FEATURE_CHANGES
import de.koenidv.sph.objects.FunctionTile.Companion.FEATURE_COURSES
import de.koenidv.sph.objects.FunctionTile.Companion.FEATURE_MESSAGES
import de.koenidv.sph.objects.FunctionTile.Companion.FEATURE_TIMETABLE
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Response
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.*
import java.util.concurrent.TimeUnit

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

    /**
     * Determine if a module should be updated
     * Takes the module's update time preferences key, a max age and time unit
     * @return In production, true if updated age is older than maxAge, always true in debug
     */
    private fun shouldUpdate(preference: String,
                             maxAge: Long,
                             timeUnit: TimeUnit = TimeUnit.MINUTES): Boolean =
            BuildConfig.DEBUG || prefs.getLong(preference, 0) > timeUnit.toMillis(maxAge)

    /**
     * Handle updating when pull to refresh layout is activated
     */
    fun handlePullToRefresh(destinationId: Int,
                            arguments: Bundle?,
                            callback: (success: Int) -> Unit) {
        val updateList = mutableListOf<String>()
        val updateData = mutableMapOf<String, String>()
        var disableList = false

        // Log pull to refresh
        if (Debugger.DEBUGGING_ENABLED)
            DebugLog("NetMgr", "Handling PTR",
                    bundleOf("destination" to destinationId,
                            "args" to arguments)).log()

        when (destinationId) {
            R.id.nav_home, R.id.nav_explore -> {
                // Update changes after 5min
                if (shouldUpdate("updated_changes", 5)) updateList.add("changes")
                // posts after 15min
                if (shouldUpdate("updated_posts", 15)) updateList.add("posts")
                // messages after 15min
                if (shouldUpdate("updated_messages", 15)) updateList.add("messages")
                // holidays after 1 month
                if (shouldUpdate("updated_holidays", 31, TimeUnit.DAYS))
                    updateList.add("holidays")
            }
            R.id.nav_courses, R.id.frag_tasks, R.id.frag_allposts, R.id.frag_attachments -> {
                // 2 minutes cooldown, update all posts
                if (shouldUpdate("updated_posts", 2)) updateList.add("posts")
            }
            R.id.nav_messages -> {
                // Update all messages after 30sec
                if (shouldUpdate("updated_messages", 30, TimeUnit.SECONDS))
                    updateList.add("messages")
            }
            R.id.chatFragment -> {
                // Messages for this conversation after 30sec
                val conversationId = arguments?.getString("conversationId")
                if (conversationId != null) {
                    if (shouldUpdate("updated_messages_$conversationId",
                                    30, TimeUnit.SECONDS)) {
                        updateList.add("chat")
                        updateData["chatid"] = conversationId
                    }
                }
            }
            R.id.frag_timetable -> {
                updateList.add("timetable")
            }
            R.id.frag_changes -> {
                // Changes fragment
                // Update changes after 30sec cooldown
                if (shouldUpdate("updated_changes", 30, TimeUnit.SECONDS))
                    updateList.add("changes")
            }
            R.id.frag_course_overview -> {
                disableList = true
                // Course Overview fragment
                // Update posts, tasks, files, links
                if (arguments?.getString("courseId") != null) {
                    // Only after 2 minutes
                    if (shouldUpdate(
                                    "updated_posts_${arguments.getString("courseId")}",
                                    2)) {
                        // Update posts for this course
                        Posts(this).load(listOf(
                                CoursesDb.getByInternalId(arguments.getString("courseId"))!!)) {

                            if (it == SUCCESS) {
                                // Send broadcast to update posts, tasks and attachments in CourseOverviewFragment
                                val uiBroadcast = Intent("uichange")
                                uiBroadcast.putExtra("content", "posts")
                                LocalBroadcastManager.getInstance(appContext()).sendBroadcast(uiBroadcast)
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
                LocalBroadcastManager.getInstance(appContext()).sendBroadcast(uiBroadcast)
                // Let the activity know it can hide the refreshing icon
                // But wait a short while first so the user doesn't think nothing happened
                GlobalScope.launch {
                    delay(600)
                    callback(SUCCESS)
                }
            }
        }
        if (!disableList)
            update(updateList, updateData) { callback(it) }

    }

    /**
     * Loads a url within the sph and handles authentication
     * @param url URL to load
     */
    fun getSiteAuthed(url: String,
                      forceNewToken: Boolean = false,
                      callback: (success: Int, result: String?) -> Unit) {

        // Getting an access token
        TokenManager.getToken(forceNewToken) { success: Int, _ ->

            // Log loading the page
            if (Debugger.DEBUGGING_ENABLED)
                DebugLog("NetMgr", "Loading url authenticated",
                        bundleOf("url" to url,
                                "forceNewToken" to forceNewToken,
                                "tokenCb" to success)).log()

            if (success == SUCCESS) {

                // Getting webpage
                AndroidNetworking.get(url)
                        .setUserAgent("sph-planner")
                        .setPriority(Priority.LOW)
                        .build()
                        .getAsString(object : StringRequestListener {
                            override fun onResponse(response: String) {
                                val prefs = appContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
                                val responseLine = response.replace("\n", "")

                                if (responseLine.contains("- Schulportal Hessen")
                                        && !responseLine.contains("Login - Schulportal Hessen")
                                        && !responseLine.contains("Fehler - Schulportal Hessen")
                                        && !responseLine.contains("Schulauswahl - Schulportal Hessen")) {
                                    // Getting site was successful
                                    // Log site loaded
                                    if (Debugger.DEBUGGING_ENABLED)
                                        DebugLog("NetMgr", "Loaded url: Success",
                                                bundleOf("url" to url,
                                                        "title" to Debugger.responseTitle(response)),
                                                Debugger.LOG_TYPE_SUCCESS).log()

                                    callback(SUCCESS, response)
                                    prefs.edit().putLong("token_last_success", Date().time).apply()
                                } else if (responseLine.contains("Login - Schulportal Hessen")
                                        || responseLine.contains("Fehler - Schulportal Hessen")
                                        || responseLine.contains("Schulauswahl - Schulportal Hessen")) {
                                    // Signin was not successful
                                    // Log error
                                    if (Debugger.DEBUGGING_ENABLED)
                                        DebugLog("NetMgr",
                                                "Error loading url, invalid credentials",
                                                bundleOf("url" to url,
                                                        "title" to Debugger.responseTitle(response)),
                                                Debugger.LOG_TYPE_ERROR).log()

                                    Log.e(TAG, "Invalid credentials for $url")
                                    prefs.edit().putLong("token_last_success", 0).apply()
                                    callback(FAILED_INVALID_CREDENTIALS, response)
                                } else if (response.contains("Wartungsarbeiten")) {
                                    // Maintenance work
                                    callback(FAILED_MAINTENANCE, response)

                                    // Log maintenance
                                    if (Debugger.DEBUGGING_ENABLED)
                                        DebugLog("NetMgr",
                                                "Error loading url, maintenance",
                                                bundleOf("url" to url,
                                                        "title" to Debugger.responseTitle(response)),
                                                Debugger.LOG_TYPE_ERROR).log()
                                }
                            }

                            override fun onError(error: ANError) {
                                // Log network error
                                if (Debugger.DEBUGGING_ENABLED)
                                    DebugLog("TokenMgr",
                                            "NetError loading url authenticated",
                                            error, bundleOf(
                                            "url" to url
                                    )
                                    ).log()

                                when (error.errorDetail) {
                                    "connectionError" -> {
                                        // This will also be called if request timed out
                                        callback(FAILED_NO_NETWORK, "No connection to server or timeout")
                                    }
                                    "requestCancelledError" -> {
                                        callback(FAILED_CANCELLED, "Network request was cancelled")
                                    }
                                    else -> {
                                        Toast.makeText(appContext(), "Error for $url",
                                                Toast.LENGTH_LONG).show()
                                        Toast.makeText(appContext(),
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
    fun getJson(url: String, callback: (success: Int, result: JSONObject?) -> Unit) {
        // Get the site using FAN
        AndroidNetworking.get(url)
                .build()
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
     * Authenticates with sph, posts the specified body and returns json response
     */
    fun postJsonAuthed(url: String, body: Map<String, String> = mapOf(),
                       headers: Map<String, String> = mapOf(),
                       callback: (success: Int, result: JSONObject?) -> Unit) {
        // Authenticate
        TokenManager.getToken { success, _ ->
            // Cancel if authentication was not successful
            if (success != SUCCESS) {
                callback(success, null)
                return@getToken
            }

            // Add default headers
            val allHeaders = mutableMapOf(
                    "X-Requested-With" to "XMLHttpRequest",
                    "User-Agent" to "koenidv/sph-planner")
            allHeaders.putAll(headers)

            // Post using FAN
            AndroidNetworking.post(url)
                    .addBodyParameter(body)
                    .addHeaders(allHeaders)
                    .build()
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
    }


    /**
     * Update multiple scopes
     * @param entries List of strings with scopes to update
     * (See in when below for what's supported)
     */
    fun update(entries: List<String>, data: Map<String, String>, callback: (success: Int) -> Unit) {
        // Log updating
        if (Debugger.DEBUGGING_ENABLED)
            DebugLog("NetMgr", "Updating invoked",
                    bundleOf("updateList" to entries)).log()

        if (entries.isNotEmpty()) {
            // Prepare token
            TokenManager.getToken { success, _ ->
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
                            "posts" -> Posts(this).fetch(callback = checkDone)
                            "changes" -> Changes(this).fetch(callback = checkDone)
                            "timetable" -> Timetable().fetch(callback = checkDone)
                            "messages" -> Messages().fetch(callback = checkDone)
                            "holidays" -> Holidays().fetch(callback = checkDone)
                            "chat" -> Messages().updateConversation(data["chatid"], checkDone)
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
        val loadList = mutableListOf("userid", "courses")

        // Mark each supported feature for loading
        if (features.contains(FEATURE_TIMETABLE)) loadList.add("timetable")
        if (features.contains(FEATURE_COURSES)) loadList.add("posts")
        if (features.contains(FEATURE_CHANGES)) loadList.add("changes")
        if (features.contains(FEATURE_MESSAGES)) {
            loadList.add("users")
            loadList.add("messages")
        }
        loadList.add("holidays")

        // Log indexing status
        if (Debugger.DEBUGGING_ENABLED)
            DebugLog("NetMgr", "Indexing list assembled",
                    bundleOf("loadList" to loadList)).log()

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
            // Log feature loading
            // Log indexing status
            if (Debugger.DEBUGGING_ENABLED)
                DebugLog("NetMgr", "Fetching ${loadList[index]}").log()
            when (loadList[index]) {
                "userid" -> {
                    status(appContext().getString(R.string.index_status_userid))
                    getOwnUserId(onDone)
                }
                "courses" -> {
                    status(appContext().getString(R.string.index_status_courses))
                    Courses(this).createIndex(onDone, features)
                }
                "timetable" -> {
                    status(appContext().getString(R.string.index_status_timetable))
                    Timetable().fetch(onDone)
                }
                "posts" -> {
                    status(appContext().getString(R.string.index_status_posts))
                    Posts(this).fetch(onDone, true)
                }
                "changes" -> {
                    status(appContext().getString(R.string.index_status_changes))
                    Changes(this).fetch(onDone)
                }
                "messages" -> {
                    status(appContext().getString(R.string.index_status_messages))
                    Messages().fetch(archived = true, callback = onDone)
                }
                "users" -> {
                    status(appContext().getString(R.string.index_status_users))
                    Users().fetch(onDone)
                }
                "holidays" -> {
                    status(appContext().getString(R.string.index_status_holidays))
                    Holidays().fetch(onDone)
                }
                else -> {
                    // Log unsupported feature
                    if (Debugger.DEBUGGING_ENABLED)
                        DebugLog("NetMgr",
                                "Unsupported feature ${loadList[index]}",
                                type = Debugger.LOG_TYPE_ERROR).log()
                    Log.e(TAG, "Unsupported feature " + loadList[index])
                }
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
        // Log resolving url
        if (Debugger.DEBUGGING_ENABLED)
            DebugLog("NetMgr", "Resolving url", bundleOf(
                    "url" to url
            )).log()

        // Getting an access token
        TokenManager.getToken { success: Int, _ ->
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

                                    // Log success
                                    if (Debugger.DEBUGGING_ENABLED)
                                        DebugLog("NetMgr", "Url resolving: success",
                                                bundleOf(
                                                        "url" to url,
                                                        "resolvedUrl" to response.request().url().toString()
                                                ), Debugger.LOG_TYPE_SUCCESS).log()
                                } else {
                                    callback(FAILED_UNKNOWN, url)

                                    // Log warning
                                    if (Debugger.DEBUGGING_ENABLED)
                                        DebugLog("NetMgr", "Url resolving failed",
                                                bundleOf(
                                                        "url" to url,
                                                        "resolvedUrl" to response.request().url().toString()
                                                ), Debugger.LOG_TYPE_WARNING).log()
                                }
                            }

                            override fun onError(anError: ANError?) {
                                // Running in emulator will cause an ssl error
                                // Network error will also happen if there is a timeout or no connection
                                // Not a huge deal though, the unresolved url will work as well. For now.
                                Log.e(TAG, anError!!.errorDetail + ": " + url)
                                callback(FAILED_UNKNOWN, url)

                                // Log warning
                                if (Debugger.DEBUGGING_ENABLED)
                                    DebugLog("NetMgr", "Url resolving failed",
                                            anError, bundleOf("url" to url),
                                            Debugger.LOG_TYPE_WARNING).log()
                            }

                        })
            } else callback(success, url)
        }
    }

    /**
     * Loads the user's profile picture site and gets their user id from there
     */
    fun getOwnUserId(callback: (success: Int) -> Unit) {
        // Load the site where one can upload a profile picture
        getSiteAuthed("https://start.schulportal.hessen.de/benutzerverwaltung.php?a=userFoto")
        { success, result ->
            if (success == SUCCESS) {
                // Extract the user id
                val document = Jsoup.parse(result)
                val profilePicture = document.selectFirst("div#content img")
                        .attr("src")
                val userid = profilePicture.substringAfter("&p=")
                        .substringBefore("&")

                TokenManager.userid = userid
                appContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
                        .edit().putString("userid", userid).apply()

                callback(SUCCESS)
            } else callback(success)
        }
    }

}