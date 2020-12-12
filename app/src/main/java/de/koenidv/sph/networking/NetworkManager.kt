package de.koenidv.sph.networking

import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.StringRequestListener
import com.facebook.stetho.okhttp3.StethoInterceptor
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import de.koenidv.sph.database.DatabaseHelper
import de.koenidv.sph.parsing.RawParser
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

//  Created by koenidv on 11.12.2020.
class NetworkManager {

    // todo save last refresh for checks
    fun createIndex(listener: DoneListener) {
        // Remove old courses, it'll just lead to isses
        val dbHelper = DatabaseHelper(applicationContext())
        dbHelper.clear()

        // Firstly, load courses from timetable so we have an overview
        loadSiteWithToken("https://start.schulportal.hessen.de/stundenplan.php", object : StringRequestListener {
            override fun onResponse(response: String?) {
                dbHelper.save(RawParser().parseCoursesFromTimetable(response!!))

                // Secondly, load those courses from study groups to find out where the user belongs
                loadSiteWithToken("https://start.schulportal.hessen.de/lerngruppen.php", object : StringRequestListener {
                    override fun onResponse(response: String?) {
                        dbHelper.setNulledNotFavorite()
                        dbHelper.save(RawParser().parseCoursesFromStudygroups(response!!))
                    }

                    override fun onError(anError: ANError?) {
                        listener.onComplete(false)
                    }
                })

            }

            override fun onError(anError: ANError?) {
                listener.onComplete(false)
            }
        })
    }


    /**
     * Loads a url within the sph and handles authentication
     * @param url URL to load
     * @param listener Listen for results
     */
    private fun loadSiteWithToken(url: String, listener: StringRequestListener) {

        // Getting an access token
        TokenManager().generateAccessToken(object : TokenManager.TokenGeneratedListener {
            override fun onTokenGenerated(token: String) {
                // Setting sid cookie
                CookieStore.saveFromResponse(
                        HttpUrl.parse("https://schulportal.hessen.de")!!,
                        listOf(Cookie.Builder().domain("schulportal.hessen.de").name("sid").value(token).build()))

                // Adding an Network Interceptor for Debugging purpose :
                val okHttpClient = OkHttpClient.Builder()
                        .addNetworkInterceptor(StethoInterceptor())
                        .cookieJar(CookieStore)
                        .build()
                AndroidNetworking.initialize(applicationContext(), okHttpClient)

                // Getting webpage
                AndroidNetworking.get(url)
                        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.27 Safari/537.36")
                        .setPriority(Priority.LOW)
                        .build()
                        .getAsString(listener)
            }
        })

    }

    interface DoneListener {
        fun onComplete(success: Boolean)
    }
}