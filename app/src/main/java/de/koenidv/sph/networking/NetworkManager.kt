package de.koenidv.sph.networking

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.OkHttpResponseListener
import com.androidnetworking.interfaces.StringRequestListener
import com.facebook.stetho.okhttp3.StethoInterceptor
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.database.PostAttachmentsDb
import de.koenidv.sph.database.PostsDb
import de.koenidv.sph.objects.*
import de.koenidv.sph.parsing.RawParser
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.*
import java.util.concurrent.TimeUnit

//  Created by koenidv on 11.12.2020.
@Suppress("PropertyName")
class NetworkManager {

    val FAILED_UNKNOWN = -1
    val SUCCESS = 0
    val FAILED_NO_NETWORK = 1
    val FAILED_INVALID_CREDENTIALS = 2
    val FAILED_MAINTENANCE = 3
    val FAILED_CANCELLED = 4

    // todo save last refresh for checks
    fun createCourseIndex(onComplete: (success: Int) -> Unit) {
        val prefs = applicationContext().getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)
        // Remove old courses, it'll just lead to isses
        val coursesDb = CoursesDb.getInstance()
        coursesDb.clear()
        // Set courses last updated to 0 in case this gets cancelled
        prefs.edit().putLong("courses_last_updated", 0).apply()


        // Firstly, load courses from timetable so we have an overview
        loadSiteWithToken("https://start.schulportal.hessen.de/stundenplan.php", onComplete = { successTimetable: Int, responseTimetable: String? ->
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
     * Load and save Posts, PostAtachments and PostTasks for a list of courses
     * @param coursesToLoad Courses that should be loaded, all courses with NumberIds when null
     */
    fun loadAndSavePosts(coursesToLoad: List<Course>? = null, onComplete: (success: Int) -> Unit) {
        // Use all courses with number_id if nothing was specified
        val courses = coursesToLoad ?: CoursesDb.getInstance().withNumberId
        // Save all errors in a list, only return one later
        val errors = mutableListOf<Int>()
        // Get all current posts for comparison
        val allPosts = PostsDb.getInstance().all

        // Load every course
        for (course in courses) {
            loadSiteWithToken("https://start.schulportal.hessen.de/meinunterricht.php?a=sus_view&id=" + course.number_id,
                    onComplete = { success: Int, result: String? ->
                        if (success == SUCCESS) {
                            // Parse data
                            RawParser().parsePosts(course.courseId, result!!, allPosts,
                                    onParsed = { posts: List<Post>,
                                                 attachments: List<PostAttachment>,
                                                 tasks: List<PostTask>,
                                                 links: List<PostLink> ->
                                        // Write all that parsed stuff to the database
                                        PostsDb.getInstance().save(posts)
                                        PostAttachmentsDb.getInstance().save(attachments)
                                        // todo last post from spo_kmb_4 missing
                                        // todo Save tasks
                                        // todo Save links
                                        Toast.makeText(applicationContext(), "Heute schon, Kartoffel", Toast.LENGTH_SHORT).show()
                                    })
                        } else {
                            errors.add(success)
                        }
                    })
        }
    }

    /**
     * Loads a url within the sph and handles authentication
     * @param url URL to load
     */
    fun loadSiteWithToken(url: String, onComplete: (success: Int, result: String?) -> Unit) {

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
                                if (!response.contains("Login - Schulportal Hessen")
                                        && !response.contains("Fehler - Schulportal Hessen")) {
                                    // Getting site was successful
                                    onComplete(SUCCESS, response)
                                    prefs.edit().putLong("token_last_success", Date().time).apply()
                                } else if (response.contains("Login - Schulportal Hessen")
                                        || response.contains("Fehler - Schulportal Hessen")) {
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
                                        // This will also be called if reqest timed out
                                        onComplete(NetworkManager().FAILED_NO_NETWORK, null)
                                    }
                                    "requestCancelledError" -> {
                                        onComplete(NetworkManager().FAILED_CANCELLED, null)
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
}