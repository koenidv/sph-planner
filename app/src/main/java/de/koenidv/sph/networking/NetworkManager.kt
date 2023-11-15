package de.koenidv.sph.networking

//import de.koenidv.sph.BuildConfig
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.afollestad.date.month
import com.afollestad.date.year
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.BuildConfig
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.androidnetworking.interfaces.StringRequestListener
import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.TAG
import de.koenidv.sph.SphPlanner.Companion.appContext
import de.koenidv.sph.SphPlanner.Companion.prefs
import de.koenidv.sph.database.ChangesDb
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.database.FunctionTilesDb
import de.koenidv.sph.database.SchedulesDb
import de.koenidv.sph.debugging.DebugLog
import de.koenidv.sph.debugging.Debugger
import de.koenidv.sph.debugging.Debugger.LOG_TYPE_WARNING
import de.koenidv.sph.objects.FunctionTile.Companion.FEATURE_CALENDAR
import de.koenidv.sph.objects.FunctionTile.Companion.FEATURE_CHANGES
import de.koenidv.sph.objects.FunctionTile.Companion.FEATURE_COURSES
import de.koenidv.sph.objects.FunctionTile.Companion.FEATURE_MESSAGES
import de.koenidv.sph.objects.FunctionTile.Companion.FEATURE_TIMETABLE
import de.koenidv.sph.objects.Schedule
import kotlinx.coroutines.*
import org.json.JSONObject
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
//import okhttp3.RequestBody.Companion.asRequestBody //to use file.asReqestBody
//import okhttp3.RequestBody.Companion.toRequestBody //to use content.toRequestBody




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
        const val FAILED_TOKEN = 7
        const val FAILED_DEMO = 8
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

        if (appContext().getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE).getBoolean("demoMode", false)) {
            DebugLog("NetMgr", "PTR: Using demo mode",
                type = LOG_TYPE_WARNING)
            callback(FAILED_DEMO)
            return
        }

        val updateList = mutableListOf<String>()
        val updateData = mutableMapOf<String, String>()
        var disableList = false

        val chngs = ChangesDb.instance!!.existAny()//Crash456

        // Log pull to refresh
        DebugLog("NetMgr", "Handling PTR",
                bundleOf("destination" to destinationId,
                        "args" to arguments))

        when (destinationId) {
            R.id.nav_home, R.id.nav_explore -> {
                //Crash456: Running through from nav_home swipe refresh
                // Update changes after 5min
                if (   (chngs) && (shouldUpdate("updated_changes", 5))   ) {
                    updateList.add("changes")
                }
                // posts after 15min
                if (shouldUpdate("updated_posts", 15)) updateList.add("posts")
                // messages after 15min
                if (shouldUpdate("updated_messages", 15)) updateList.add("messages")
                // holidays after 1 month
                if (shouldUpdate("updated_holidays", 31, TimeUnit.DAYS))
                    updateList.add("holidays")
                // calendar after 12h (720min)
                if (shouldUpdate("updated_calendar", 720)) updateList.add("calendar")
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
                // calendar after 12h (720min)
                if (shouldUpdate("updated_calendar", 720)) updateList.add("calendar")
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
        TokenManager.authenticate(forceNewToken) { success: Int ->

            // Log loading the page
            DebugLog("NetMgr", "Loading url authenticated",
                    bundleOf("url" to url,
                            "forceNewToken" to forceNewToken,
                            "tokenCb" to success))

            if (success == SUCCESS) {

                if(url.contains("kalender")) {
                    /*
                     * Idea is to use okhttp to receive a stream
                     * and to save the data in schedule db
                     *
                     * E/AndroidRuntime: FATAL EXCEPTION: main => at android.os.StrictMode$AndroidBlockGuardPolicy.onNetwork(...
                     * Rootcause is a timeintensive calculation has to be done in an async thread
                     */
                     //Token done and OK, now url has to be handled
                     val scope = CoroutineScope(Dispatchers.Unconfined)//CoroutineScope(Job() + Dispatchers.Main)
                     val job/* Unique instance ID*/ = scope.launch { clndrFetch(url) }
                     // launch = fire and forget - Start an asynchronous task execution from a normal function
                     // therein we can handle feedbacks with async
                     callback(SUCCESS, "")
                }
                else {
                    // Getting webpage
                    AndroidNetworking.get(url)
                        .setUserAgent("sph-planner")
                        .setPriority(Priority.LOW)
                        .build()
                        .getAsString(object : StringRequestListener {
                            override fun onResponse(response: String) {
                                val responseLine = response.replace("\n", "")

                                DebugLog("NetMgr", "responseLine: $responseLine")

                                if (responseLine.contains("- Schulportal Hessen")
                                    && !responseLine.contains("Login - Schulportal Hessen")
                                    && !responseLine.contains("Fehler - Schulportal Hessen")
                                    && !responseLine.contains("Schulauswahl - Schulportal Hessen")
                                ) {
                                    // Getting site was successful
                                    // Log site loaded
                                    DebugLog(
                                        "NetMgr", "Loaded url: Success",
                                        bundleOf(
                                            "url" to url,
                                            "title" to Debugger.responseTitle(response)
                                        ),
                                        Debugger.LOG_TYPE_SUCCESS
                                    )

                                    callback(SUCCESS, response)
                                    prefs.edit().putLong("token_last_success", Date().time).apply()
                                } else if (responseLine.contains("Login - Schulportal Hessen")
                                    || responseLine.contains("Fehler - Schulportal Hessen")
                                    || responseLine.contains("Schulauswahl - Schulportal Hessen")
                                ) {
                                    // Signin was not successful
                                    // Log error
                                    DebugLog(
                                        "NetMgr",
                                        "Error loading url, invalid credentials",
                                        bundleOf(
                                            "url" to url,
                                            "title" to Debugger.responseTitle(response)
                                        ),
                                        Debugger.LOG_TYPE_ERROR
                                    )

                                    Log.e(TAG, "Invalid credentials for $url")
                                    prefs.edit().putLong("token_last_success", 0).apply()
                                    callback(
                                        FAILED_INVALID_CREDENTIALS,
                                        response
                                    )//Crash456 - final statement and root cause - Because chnages are not supported from our school
                                } else if (response.contains("Wartungsarbeiten")) {
                                    // Maintenance work
                                    callback(FAILED_MAINTENANCE, response)

                                    // Log maintenance
                                    DebugLog(
                                        "NetMgr",
                                        "Error loading url, maintenance",
                                        bundleOf(
                                            "url" to url,
                                            "title" to Debugger.responseTitle(response)
                                        ),
                                        Debugger.LOG_TYPE_ERROR
                                    )
                                }
                            }

                            override fun onError(error: ANError) {
                                // Log network error
                                DebugLog(
                                    "TokenMgr",
                                    "NetError loading url authenticated",
                                    error, bundleOf(
                                        "url" to url
                                    )
                                )

                                when (error.errorDetail) {
                                    "connectionError" -> {
                                        // This will also be called if request timed out
                                        callback(
                                            FAILED_NO_NETWORK,
                                            "No connection to server or timeout"
                                        )
                                    }
                                    "requestCancelledError" -> {
                                        callback(FAILED_CANCELLED, "Network request was cancelled")
                                    }
                                    else -> {
                                        Toast.makeText(
                                            appContext(), "Error for $url",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        Toast.makeText(
                                            appContext(),
                                            error.errorCode.toString()
                                                    + ": " + error.errorDetail,
                                            Toast.LENGTH_LONG
                                        ).show()
                                        // Provide some details for user debugging
                                        callback(
                                            FAILED_UNKNOWN,
                                            "--- Network error code: ${error.errorCode}\n"
                                                    + "--- Error description: ${error.errorDetail}\n"
                                                    + "--- Error body: ${error.errorCode}\n"
                                                    + "--- No server response available ---"
                                        )
                                        // Log the error in Crashlytics
                                        FirebaseCrashlytics.getInstance().recordException(error)
                                    }
                                }
                            }

                        })
                }
            } else callback(success, null)
        } //w/o any cnd - TokenManager.authenticate(forceNewToken)
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
        DebugLog("NetMgr", "Updating invoked",
                bundleOf("updateList" to entries))

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
                            else {
                                callback(lastError!!)
                                DebugLog("NetMgr", "Issue in update")
                            }
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
                            "calendar" -> Clndr().fetch(callback = checkDone)
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
        val features = FunctionTilesDb.getInstance().supportedFeatures.map { it.type } +
                FunctionTilesDb.getInstance().getSupportedFeatureName("Kalender").map {it.name }

        val loadList = mutableListOf("userid", "courses")

        // Mark each supported feature for loading
                                                  loadList.add("holidays")
                                                  //has to be done BEFORE calendar because in schedule db we sync with holidays db!
                                                  //furthermore we need some space in between to have a filled holidays db
        if (features.contains(FEATURE_TIMETABLE)) loadList.add("timetable")
        if (features.contains(FEATURE_COURSES))   loadList.add("posts")
        if (features.contains(FEATURE_CHANGES))   loadList.add("changes")
        if (features.contains(FEATURE_CALENDAR))  loadList.add("calendar")
        if (features.contains(FEATURE_MESSAGES)) {
                                                  loadList.add("users")
                                                  loadList.add("messages")
        }

        // Log indexing status
        DebugLog("NetMgr", "Indexing list assembled",
                bundleOf("loadList" to loadList))

        // Load every item in loadList
        var index = 0
        var loadNextFeature = {}
        val onDone: (Int) -> Unit = {
            DebugLog("NetMgr", "onDone - it: " + it.toString())
            if (it == SUCCESS) {
                index++
                DebugLog("NetMgr", "onDone - it index: " + index.toString() + "/ " + loadList.size.toString())
                // If this was the last feature, call back success
                if (index >= loadList.size) {
                    DebugLog("NetMgr", "All features done - Final success callback for indexAll")
                    callback(SUCCESS)
                }
                // Else load the next feature
                else loadNextFeature()
            } else {
                /*
                StKl Test - In case index >= loadList.size means success is ok...

                Since initial download from github of SPHplanner v1.3.3 there was a crash at the end of onboarding.
                Crash location was => "Rootcause Crash #A"
                After investigation this location was creating -1 feedback for unknown error
                Debug leads to index 10 but list size is only 7?...
                    callback(10+index) => 20 => index 10 crash rootcause
                    callback(10+loadList.size) => 17 => listsize is 7! only (8 minus not supported changes = 7 - this fits)
                So following i tested an if cnd. We are in err case here but in case of index is gt list size we feedback succes
                Maybe solution is not final but crash is solved at the moment.
                Original code is in else part.
                 */
                if(index >= loadList.size) {
                    callback(SUCCESS)
                }
                else {
                    DebugLog("NetMgr", "onDone callback err - Stop loading")
                    // Callback an error and stop loading
                    callback(it)
                    // callback(10+index) => 20 => index 10 crash rootcause
                    // callback(10+loadList.size) => 17 => listsize is 7! only (8 minus not supported changes = 7 - this fits)
                    Log.e(TAG, "Error loading ${loadList[index]}: $it")
                }
            }
        }
        // Load the feature at the current index
        loadNextFeature = {
            // Log indexing status
            DebugLog("NetMgr", "Fetching ${loadList[index]} ..." + index.toString() + "...")
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
                "calendar" -> {
                    status(appContext().getString(R.string.index_status_calendar))
                    Clndr().fetch(onDone)
                }
                else -> {
                    // Log unsupported feature
                    DebugLog("NetMgr",
                            "Unsupported feature ${loadList[index]}",
                            type = Debugger.LOG_TYPE_ERROR)
                }
            }
        }
        // Begin loading
        loadNextFeature()
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

    //https://developer.android.com/kotlin/coroutines/coroutines-adv
    //unterbrechbare fun <NAME> als Coroutine
    private suspend fun clndrFetch(url: String) = coroutineScope {
        //aufgeschobenes Ergebnis 1
        val deferredOne = async { doClndrFetch(url) }
        deferredOne.await()
    }

    /*
     * To specify where the coroutines should run, Kotlin provides three dispatchers that you can use:
     *
     * Dispatchers.Main - Use this dispatcher to run a coroutine on the main Android thread. This should be used only for interacting with the UI and performing quick work. Examples include calling suspend functions, running Android UI framework operations, and updating LiveData objects.
     * Dispatchers.IO - This dispatcher is optimized to perform disk or network I/O outside of the main thread. Examples include using the Room component, reading from or writing to files, and running any network operations.
     * Dispatchers.Default - This dispatcher is optimized to perform CPU-intensive work outside of the main thread. Example use cases include sorting a list and parsing JSON.
    */
    private suspend fun doClndrFetch(url: String): String = withContext(Dispatchers.IO) {

        //Schoolyear starts ~AUG-n and ends ~JUN-(n+1)
        //So in case current year <= JUN we ask for (n-1) and n
        //In case we are > JUN we ask for n and (n+1)

        //n
        var sUrl = url//appContext().getString(R.string.url_calendar)
        //replace %yr with current year
        val c = Calendar.getInstance()
        c.time = Date()
        sUrl = sUrl.replace("%yr", c.year.toString())

        AndroidNetworking.get(sUrl)
            .setUserAgent("sph-planner")
            .setPriority(Priority.LOW)
            .build()
            .getAsString(object : StringRequestListener {
                override fun onResponse(response: String) {

                    if(response.contains("Von_Datum")) {//Checking for a unique string in csv export expected
                        var rspns = response
                        val replacementArray: List<String> = listOf("&quot;", "&amp;", "&lt;", "&gt;", "&nbsp;")//ToDo - find better way to remove special html chars only
                        for(replacement in replacementArray) rspns = rspns.replace(replacement, "")
                        val regex = "([^;]*[;]){8}([^;]*)([\\r\\n]+)".toRegex() //(any char except ; followed by ; {8 times} followed by any char followed by \r\n
                        var responseLine = ""

                        while(rspns.isNotEmpty()) {
                            try {
                                val match = regex.find(rspns)!!
                                if (!match.value.isNullOrEmpty()) {
                                    responseLine += match.value + "#split#"
                                    rspns = rspns.substring(match.range.last()+1)
                                } else {
                                    rspns = ""
                                }
                            }
                            catch (err:Error) {
                                //do nothing
                            }
                        }

                        if(responseLine.isNotEmpty()) {
                            val schedule: List<Schedule> = parseResponse(responseLine)
                            val schedulesDb = SchedulesDb
                            schedulesDb.save(schedule)
                        }
                    }
                }

                override fun onError(error: ANError) {
                    //do nothing
                }
            })

        //(n-1) or (n+1)
        sUrl = url//appContext().getString(R.string.url_calendar)
        c.time = Date()
        if(c.month <= 5) c.add(Calendar.YEAR, -1) else c.add(Calendar.YEAR, 1)
        sUrl = sUrl.replace("%yr", c.year.toString())

        AndroidNetworking.get(sUrl)
            .setUserAgent("sph-planner")
            .setPriority(Priority.LOW)
            .build()
            .getAsString(object : StringRequestListener {
                override fun onResponse(response: String) {

                    if(response.contains("Von_Datum")) {//Checking for a unique string in csv export expected
                        var rspns = response
                        val replacementArray: List<String> = listOf("&quot;", "&amp;", "&lt;", "&gt;", "&nbsp;")//ToDo - find better way to remove special html chars only
                        for(replacement in replacementArray) rspns = rspns.replace(replacement, "")
                        val regex = "([^;]*[;]){8}([^;]*)([\\r\\n]+)".toRegex() //(any char except ; followed by ; {8 times} followed by any char followed by \r\n
                        var responseLine = ""

                        while(rspns.isNotEmpty()) {
                            try {
                                val match = regex.find(rspns)!!
                                if (!match.value.isNullOrEmpty()) {
                                    responseLine += match.value + "#split#"
                                    rspns = rspns.substring(match.range.last()+1)
                                } else {
                                    rspns = ""
                                }
                            }
                            catch (err:Error) {
                                //do nothing
                            }
                        }

                        if(responseLine.isNotEmpty()) {
                            val schedule: List<Schedule> = parseResponse(responseLine)
                            val schedulesDb = SchedulesDb
                            schedulesDb.save(schedule)
                        }
                    }
                }

                override fun onError(error: ANError) {
                    //do nothing
                }
            })

        return@withContext ""

         /* okhttp approach => Other approach is functional
         val client = OkHttpClient.Builder()
            .addNetworkInterceptor { chain ->
                chain.proceed(
                    chain.request()
                        .newBuilder()
                        .header("User-Agent", "sph-planner")
                        .header("Authorization", tkn)
                        .build()
                )
            }
            .build()

        try {
            var sUrl = url//appContext().getString(R.string.url_calendar)
            //replace %yr with current year
            val c = Calendar.getInstance()
            c.time = Date()
            sUrl = sUrl.replace("%yr", c.year.toString())
            // Create URL
            //val myUrl = URL("https://publicobject.com/helloworld.txt")//test ok with 1759 byte length
            val myUrl = URL(sUrl)//Fehler - Du bist nicht angemeldet oder diese Funktion ist für dich nicht freigeschaltet!
            // Build request
            val tkn = prefs.getString("token", "")!!//CookieStore.getToken()!!

            val request = Request.Builder()
                .url(myUrl)
                .header("Authorization", tkn)
                .build()

            // Execute request
            val response = client.newCall(request).execute()

            val responseBody = response.body()
                ?: throw IllegalStateException("Response doesn't contain a file")

            var str = ""
            str += response.body()!!.string()
            str += ""

            var lngth: Long = 0
            lngth += response.body()!!.contentLength()
            lngth += 1

            return@withContext responseBody.toString()
        }
        catch(err:Error) {
            print("Error when executing get request: "+err.localizedMessage)
            return@withContext ""
        }
        */
    }

    private fun parseResponse(rspns: String): List<Schedule> {
        //Input example
        /*
        Titel;Art;Von_Datum;Von_Uhrzeit;Bis_Datum;Bis_Uhrzeit;Beschreibung;Ort;Verantwortlich
        Herbstferien Hessen 2022;Ferien/ beweglicher Ferientag/ Feiertag;24.10.2022;00:00;29.10.2022;23:59;;;
        Weihnachtsferien Hessen 2022/2023;Ferien/ beweglicher Ferientag/ Feiertag;22.12.2022;00:00;07.01.2023;23:59;;;
        */
        //Assigned to a schedule list
        val returnSchedule: MutableList<Schedule> = mutableListOf()

        try {
            val rspnsLineArr = rspns.split("#split#").toMutableList()//Line per cell - Now: Go through
            rspnsLineArr.removeFirst()//delete line with table headers
            for (line in rspnsLineArr) {
                if (!line.isNullOrEmpty()) {
                    val lineArr = line.split(";").toMutableList()//parts of a line
                    val mySchedule = Schedule()

                    if (!lineArr[0].isNullOrEmpty()) {
                        mySchedule.nme = lineArr[0]
                        if (!lineArr[6].isNullOrEmpty()) mySchedule.txt = lineArr[6] else mySchedule.txt = lineArr[0]
                    }
                    if (!lineArr[1].isNullOrEmpty()) mySchedule.ctgr = lineArr[1]
                    if (!lineArr[2].isNullOrEmpty()) mySchedule.strt =
                        SimpleDateFormat("dd.MM.yyyy HH:mm").parse("${lineArr[2]} ${lineArr[3]}")
                    if (!lineArr[4].isNullOrEmpty()) mySchedule.nd =
                        SimpleDateFormat("dd.MM.yyyy HH:mm").parse("${lineArr[4]} ${lineArr[5]}")

                    if ((mySchedule.nd.time - mySchedule.strt.time)/*ms*/ > (16*60*60*1000)/*ms*/) {//Diff gt 16h
                        mySchedule.drtn = 0
                        mySchedule.hr = ""
                    }
                    else {
                        mySchedule.drtn = ((mySchedule.nd.time - mySchedule.strt.time)/(1000*60)).toInt()//Diff in [min]
                        mySchedule.hr = "" //ToDo - try an assignment/ algorithm later on with timebar information
                    }

                    mySchedule.crs = "none"
                    mySchedule.src = "portal"
                    mySchedule.shr = false

                    if (!lineArr[7].isNullOrEmpty()) mySchedule.plc = lineArr[7]

                    if (!lineArr[8].isNullOrEmpty()) {
                        //last element - delete of line end/ file end information before assignment
                        lineArr[8] = lineArr[8].replace("\r", "")
                        lineArr[8] = lineArr[8].replace("\n", "")
                        mySchedule.rsp = lineArr[8]
                    }

                    returnSchedule.add(mySchedule)
                }
            }

            return returnSchedule
        }
        catch (err:Error) {
            return returnSchedule
        }
    }



}