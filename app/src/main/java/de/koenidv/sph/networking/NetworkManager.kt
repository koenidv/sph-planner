package de.koenidv.sph.networking

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.OkHttpResponseListener
import com.androidnetworking.interfaces.StringRequestListener
import com.facebook.stetho.okhttp3.StethoInterceptor
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import de.koenidv.sph.database.*
import de.koenidv.sph.objects.*
import de.koenidv.sph.parsing.RawParser
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Response
import org.jsoup.Jsoup
import java.util.*
import java.util.concurrent.TimeUnit

//  Created by koenidv on 11.12.2020.
@Suppress("PropertyName")
class NetworkManager {

    companion object {
        const val FAILED_UNKNOWN = -1
        const val SUCCESS = 0
        const val FAILED_NO_NETWORK = 1
        const val FAILED_INVALID_CREDENTIALS = 2
        const val FAILED_MAINTENANCE = 3
        const val FAILED_SERVER_ERROR = 4
        const val FAILED_CANCELLED = 5
    }

    fun handlePullToRefresh(destinationId: Int, arguments: Bundle?, onComplete: (success: Int) -> Unit) {
        val prefs = applicationContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val time = Date().time
        val updateList = mutableListOf<String>()
        var disableList = false

        when (destinationId) {
            R.id.nav_home, R.id.nav_explore -> {
                // Update changes after 10min, posts after 30min
                if (time - prefs.getLong("updated_changes", 0) > 10 * 60 * 1000) updateList.add("changes")
                if (time - prefs.getLong("updated_posts", 0) > 30 * 60 * 1000) updateList.add("posts")
            }
            R.id.nav_courses -> {
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
            R.id.frag_course_overview, R.id.frag_tasks, R.id.frag_attachments -> {
                disableList = true
                // Course Overview fragment
                // Update posts, tasks, files, links
                if (arguments?.getString("courseId") != null) {
                    // Only after 2 minutes
                    if (time - prefs.getLong("updated_posts_${arguments.getString("courseId")}", 0) > 2 * 60 * 1000) {
                        // Update posts for this course
                        loadAndSavePosts(listOf(
                                CoursesDb.getInstance().getByInternalId(arguments.getString("courseId")))) {

                            if (it == SUCCESS) {
                                // Send broadcast to update posts, tasks and attachments in CourseOverviewFragment
                                val uiBroadcast = Intent("uichange")
                                uiBroadcast.putExtra("content", "posts")
                                LocalBroadcastManager.getInstance(applicationContext()).sendBroadcast(uiBroadcast)
                            }

                            onComplete(it)
                        }
                    } else
                    // Just return SUCCESS if posts for this course were updated within the last 2 minutes
                        onComplete(SUCCESS)
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
                    onComplete(SUCCESS)
                }
            }
        }
        if (!disableList)
            update(updateList) { onComplete(it) }

    }


    fun indexAll(onComplete: (success: Int) -> Unit) {
        // todo include tiles
        // Firstly, create a index of all courses
        createCourseIndex { courses ->
            if (courses == SUCCESS)
            // Load and parse timetable
                loadAndSaveTimetable { lessons ->
                    if (lessons == SUCCESS)
                    // Load all posts, tasks, attachments and links from all courses
                        NetworkManager().loadAndSavePosts(markAsRead = true) { posts ->
                            if (posts == SUCCESS)
                            // Load changes, if there are any
                                NetworkManager().loadAndSaveChanges { changes ->
                                    if (changes == SUCCESS)
                                        onComplete(SUCCESS)
                                    else onComplete(changes)
                                }
                            else onComplete(posts)
                        }
                    else onComplete(lessons)
                }
            else onComplete(courses)
        }
    }


    // todo save last refresh for checks
    private fun createCourseIndex(onComplete: (success: Int) -> Unit) {
        val prefs = applicationContext().getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)
        // Remove old courses, it'll just lead to isses
        val coursesDb = CoursesDb.getInstance()
        coursesDb.clear()
        // Set courses last updated to 0 in case this gets cancelled
        prefs.edit().putLong("courses_last_updated", 0).apply()

        // Firstly, load courses from timetable so we have an overview
        loadSiteWithToken(applicationContext().getString(R.string.url_timetable), onComplete = { successTimetable: Int, responseTimetable: String? ->
            if (successTimetable == SUCCESS) {
                // Save parsed courses from timetable
                coursesDb.save(RawParser().parseCoursesFromTimetable(responseTimetable!!))
                // Secondly, load those courses from study groups to find out where the user belongs
                loadSiteWithToken("https://start.schulportal.hessen.de/lerngruppen.php", onComplete = { successStudygroups: Int, responseStudygroups: String? ->
                    if (successStudygroups == SUCCESS) {
                        // Save parsed courses from study groups
                        coursesDb.setNulledNotFavorite()
                        coursesDb.save(RawParser().parseCoursesFromStudygroups(responseStudygroups!!))
                        // Lastly, load courses again from posts overview to get number ids
                        loadSiteWithToken("https://start.schulportal.hessen.de/meinunterricht.php", onComplete = { successOverview: Int, responseOverview: String? ->
                            if (successOverview == SUCCESS) {
                                // Save parsed courses from posts overview
                                coursesDb.save(RawParser().parseCoursesFromPostsoverview(responseOverview!!))
                                // Remember when we last updated the courses
                                prefs.edit().putLong("courses_last_updated", Date().time).apply()
                            }
                            onComplete(successOverview)
                        })
                    } else onComplete(successStudygroups)
                })
            } else onComplete(successTimetable)
        })
    }

    /**
     * Load and save timetable from sph
     * This will replace any current lessons in the timetable
     */
    private fun loadAndSaveTimetable(onComplete: (success: Int) -> Unit) {
        NetworkManager().loadSiteWithToken(applicationContext().getString(R.string.url_timetable),
                onComplete = { success: Int, result: String? ->
                    if (success == SUCCESS) {
                        TimetableDb.instance!!.clear()
                        TimetableDb.instance!!.save(RawParser().parseTimetable(result!!))
                        applicationContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
                                .edit().putLong("updated_timetable", Date().time).apply()
                    }
                    onComplete(success)
                })
    }

    /**
     * Load and save Posts, PostAtachments and PostTasks for a list of courses
     * @param coursesToLoad Courses that should be loaded, all courses with NumberIds when null
     */
    private fun loadAndSavePosts(coursesToLoad: List<Course>? = null,
                                 markAsRead: Boolean = false,
                                 onComplete: (success: Int) -> Unit) {
        // Use all courses with number_id if nothing was specified
        val courses = coursesToLoad ?: CoursesDb.getInstance().withNumberId
        // Save all errors in a list, only return one later
        val errors = mutableListOf<Int>()
        // Get all current posts for comparison
        val allPosts = PostsDb.getInstance().all
        // Counter for done courses
        var counter = 0

        val prefs = applicationContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val time = Date().time

        // Load every course
        for (course in courses) {
            loadSiteWithToken(applicationContext().getString(R.string.url_course_overview).replace("%numberid", course.number_id.toString()),
                    onComplete = { success: Int, result: String? ->
                        if (success == SUCCESS) {
                            // Parse data
                            RawParser().parsePosts(course.courseId,
                                    result!!,
                                    allPosts,
                                    markAsRead,
                                    onParsed = { posts: List<Post>,
                                                 files: List<FileAttachment>,
                                                 tasks: List<PostTask>,
                                                 links: List<LinkAttachment> ->
                                        // Write all that parsed stuff to the database
                                        PostsDb.getInstance().save(posts)
                                        PostTasksDb.getInstance().save(tasks)
                                        FileAttachmentsDb.getInstance().save(files)
                                        LinkAttachmentsDb.getInstance().save(links)
                                    })
                            // Remember when we last refreshed this course
                            prefs.edit().putLong("updated_posts_${course.courseId}", time).apply()
                        } else {
                            errors.add(success)
                        }
                        counter++
                        if (counter == courses.size) {
                            // Return success or the highest error code
                            if (errors.isEmpty()) {
                                onComplete(SUCCESS)
                                prefs.edit().putLong("updated_posts", time).apply()
                            } else onComplete(errors.maxOf { it })
                        }
                    })
        }
    }

    /**
     * Load changes from sph and save them to changes db
     */
    private fun loadAndSaveChanges(onComplete: (success: Int) -> Unit) {
        NetworkManager().loadSiteWithToken(applicationContext().getString(R.string.url_changes),
                onComplete = { success: Int, result: String? ->
                    if (success == SUCCESS) {
                        ChangesDb.instance!!.removeOld()
                        ChangesDb.instance!!.save(RawParser().parseChanges(result!!))
                        applicationContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
                                .edit().putLong("updated_changes", Date().time).apply()

                        // Send broadcast to update changes
                        val uiBroadcast = Intent("uichange")
                        uiBroadcast.putExtra("content", "changes")
                        LocalBroadcastManager.getInstance(applicationContext()).sendBroadcast(uiBroadcast)
                    }
                    onComplete(success)
                })
    }

    /**
     * Loads a url within the sph and handles authentication
     * @param url URL to load
     */
    fun loadSiteWithToken(url: String, forceNewToken: Boolean = false, onComplete: (success: Int, result: String?) -> Unit) {

        // Getting an access token
        TokenManager().generateAccessToken(forceNewToken) { success: Int, token: String? ->
            if (success == SUCCESS) {
                // Setting sid cookie
                CookieStore.saveFromResponse(
                        HttpUrl.parse("https://schulportal.hessen.de")!!,
                        listOf(Cookie.Builder().domain("schulportal.hessen.de").name("sid").value(token!!).build()))

                // Adding an Network Interceptor for Debugging purpose :
                val okHttpClient = OkHttpClient.Builder()
                        .addNetworkInterceptor(StethoInterceptor())
                        .cookieJar(CookieStore)
                        .connectTimeout(60, TimeUnit.SECONDS) // sph timeout is 30 seconds
                        .build()
                AndroidNetworking.initialize(applicationContext(), okHttpClient)

                // Getting webpage
                AndroidNetworking.get(url)
                        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.27 Safari/537.36")
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
                                    onComplete(SUCCESS, response)
                                    prefs.edit().putLong("token_last_success", Date().time).apply()
                                } else if (responseLine.contains("Login - Schulportal Hessen")
                                        || responseLine.contains("Fehler - Schulportal Hessen")
                                        || responseLine.contains("Schulauswahl - Schulportal Hessen")) {
                                    // Signin was not successful
                                    onComplete(FAILED_INVALID_CREDENTIALS, null)
                                    prefs.edit().putLong("token_last_success", 0).apply()
                                } else if (response.contains("Wartungsarbeiten")) {
                                    // Maintenance work
                                    onComplete(FAILED_MAINTENANCE, null)
                                }
                            }

                            override fun onError(error: ANError) {
                                when (error.errorDetail) {
                                    "connectionError" -> {
                                        // This will also be called if request timed out
                                        onComplete(FAILED_NO_NETWORK, null)
                                    }
                                    "requestCancelledError" -> {
                                        onComplete(FAILED_CANCELLED, null)
                                    }
                                    else -> {
                                        Toast.makeText(applicationContext(), error.toString(), Toast.LENGTH_LONG).show()
                                    }
                                }
                            }

                        })
            } else onComplete(success, null)
        }
    }

    /**
     * Loads an url to resolve it
     * @return resolved url or given url if there was an error
     */
    fun resolveUrl(url: String, onComplete: (success: Int, resolvedUrl: String) -> Unit) {
        // Getting an access token
        TokenManager().generateAccessToken { success: Int, token: String? ->
            if (success == SUCCESS) {
                // Setting sid cookie
                CookieStore.saveFromResponse(
                        HttpUrl.parse("https://schulportal.hessen.de")!!,
                        listOf(Cookie.Builder().domain("schulportal.hessen.de").name("sid").value(token!!).build()))

                // Adding an Network Interceptor for Debugging purpose :
                val okHttpClient = OkHttpClient.Builder()
                        .addNetworkInterceptor(StethoInterceptor())
                        .cookieJar(CookieStore)
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .build()
                AndroidNetworking.initialize(applicationContext(), okHttpClient)

                // Getting webpage as OkHttp
                AndroidNetworking.get(url)
                        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.27 Safari/537.36")
                        .setPriority(Priority.LOW)
                        .build()
                        .getAsOkHttpResponse(object : OkHttpResponseListener {
                            override fun onResponse(response: Response) {
                                // In some cases, sph redirects back to the home page (why?)
                                // In this case, ignore the new response
                                if (response.request().url().toString() != "https://start.schulportal.hessen.de/index.php") {
                                    onComplete(SUCCESS, response.request().url().toString())
                                } else {
                                    onComplete(FAILED_UNKNOWN, url)
                                }
                            }

                            override fun onError(anError: ANError?) {
                                // Running in emulator will cause an ssl errora
                                // Network error will also happen if there is a timeout or no connection
                                // Not a huge deal though, the unresolved url will work as well. For now.
                                Log.e(SphPlanner.TAG, anError!!.errorDetail + ": " + url)
                                onComplete(FAILED_UNKNOWN, url)
                            }

                        })
            } else onComplete(success, url)
        }
    }

    /**
     * Mark a task as done in the db and send a post to sph to mark it as read there, too
     * @param numberId NumberId of the course the task belongs to
     * @param postId Id of the post the task is attached to
     * @param isDone Whether the task is now done or not
     */
    // todo retry later on error
    fun markTaskAsDone(numberId: String, postId: String, isDone: Boolean, onComplete: (success: Int) -> Unit) {
        // Mark as (un)done in the db
        PostTasksDb.getInstance().setDone(postId, isDone)
        // Mark as done on sph
        // Cancel potential pending requests for this same task, just to be sure
        AndroidNetworking.cancel("task-$postId")
        // We need an access token first
        TokenManager().generateAccessToken { success: Int, token: String? ->
            if (success == SUCCESS) {
                // Set sid cookie
                CookieStore.saveFromResponse(
                        HttpUrl.parse("https://schulportal.hessen.de")!!,
                        listOf(Cookie.Builder().domain("schulportal.hessen.de")
                                .name("sid").value(token!!).build()))

                // todo initialize ANet only once
                AndroidNetworking.initialize(applicationContext(),
                        OkHttpClient.Builder().cookieJar(CookieStore).build())

                // Send a post request to let sph know the task is done
                AndroidNetworking.post("https://start.schulportal.hessen.de/meinunterricht.php")
                        .addBodyParameter("a", "sus_homeworkDone")
                        .addBodyParameter("id", numberId)
                        .addBodyParameter("entry", postId.substring(postId.lastIndexOf("_") + 1))
                        .addBodyParameter("b", if (isDone) "done" else "undone")
                        .setTag("task-$postId")
                        .build()
                        .getAsString(object : StringRequestListener {
                            override fun onResponse(response: String) {
                                if (response == "1")
                                    onComplete(SUCCESS)
                                else
                                    onComplete(FAILED_UNKNOWN)
                            }

                            override fun onError(error: ANError) {
                                when (error.errorDetail) {
                                    "connectionError" -> onComplete(FAILED_NO_NETWORK)
                                    "requestCancelledError" -> onComplete(FAILED_CANCELLED)
                                    else -> onComplete(FAILED_UNKNOWN)
                                }
                            }
                        })

            } else onComplete(success)
        }
    }

    fun update(entries: List<String>, onComplete: (success: Int) -> Unit) {
        if (entries.isNotEmpty()) {
            // Prepare token
            TokenManager().generateAccessToken { success, _ ->
                if (success == SUCCESS) {
                    var number = 0
                    var lastError: Int? = null
                    val checkDone = { thissuccess: Int ->
                        if (thissuccess != SUCCESS)
                            lastError = thissuccess
                        number++
                        if (number == entries.size) {
                            if (lastError == null) onComplete(SUCCESS)
                            else onComplete(lastError!!)
                        }

                    }
                    for (entry in entries) {
                        when (entry) {
                            "posts" -> updatePosts(checkDone)
                            "changes" -> loadAndSaveChanges(checkDone)
                            "timetable" -> loadAndSaveTimetable(checkDone)
                        }
                    }
                } else onComplete(success)
            }
        } else onComplete(SUCCESS)
    }

    /**
     * Checks for new posts on the overview page
     * and only loads them for courses with a new entry
     * This will not update anything if a post was added with an older date
     * Therefore we also load courses that haven't been loaded within the last 48 hours
     */
    private fun updatePosts(onComplete: (success: Int) -> Unit) {
        // Get my courses page
        loadSiteWithToken(applicationContext().getString(R.string.url_allposts)) { success, response ->
            if (success == SUCCESS) {
                // Check which courses should be updated
                // Any course that is not in this list does not have any posts.
                val prefs = applicationContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
                val courses = mutableListOf<Course>()
                val courseids = mutableListOf<String>()
                val rows = Jsoup.parse(response!!).select("#aktuell tbody tr")
                val postsdb = PostsDb.getInstance()
                val coursesdb = CoursesDb.getInstance()
                val timenow = Date().time

                var numberId: String
                var postIndex: String
                var courseId: String?
                var latestPost: Post?

                for (row in rows) {
                    numberId = row.attr("data-book")
                    postIndex = row.attr("data-entry")
                    // Todo add number id if there isn't any yet
                    courseId = coursesdb.getCourseIdByNumberId(numberId)
                    latestPost = postsdb.getByCourseId(courseId, 1).getOrNull(0)

                    if (latestPost?.postId?.substring(latestPost.postId.lastIndexOf("_") + 1)
                            != postIndex
                            || timenow - prefs.getLong("updated_posts_${courseId}", 0) > 48 * 60 * 60 * 1000
                    ) {
                        courses.add(coursesdb.getByInternalId(courseId))
                        courseids.add(courseId)
                    }
                }

                if (courses.isNotEmpty()) {
                    // Now that we got all the courses that should be updated, update them
                    loadAndSavePosts(courses) {
                        prefs.edit().putLong("updated_posts", timenow).apply()
                        // Send broadcast to update posts
                        val uiBroadcast = Intent("uichange")
                        uiBroadcast.putExtra("content", "posts")
                        uiBroadcast.putExtra("courses", courseids.toTypedArray())
                        LocalBroadcastManager.getInstance(applicationContext()).sendBroadcast(uiBroadcast)
                        onComplete(it)
                    }
                } else onComplete(SUCCESS)
            } else onComplete(success)
        }

    }
}