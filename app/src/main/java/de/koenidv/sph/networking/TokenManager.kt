package de.koenidv.sph.networking

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.StringRequestListener
import com.facebook.stetho.okhttp3.StethoInterceptor
import de.koenidv.sph.SphPlanner.Companion.TAG
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import okhttp3.Cookie
import okhttp3.OkHttpClient
import java.util.*


//  Created by koenidv on 05.12.2020.
class TokenManager {

    val prefs: SharedPreferences = applicationContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)

    /*
     * Returns an signed-in access token
     * todo: Use callbacks
     * todo: Documentation
     */

    fun generateAccessToken(callback: TokenGeneratedListener) {

        // Return existing, signed-in token if it was used within 15 Minutes
        // Else get a new token
        if (Date().time - prefs.getLong("token_last_success", 0) <= 15 * 60 * 1000) {
            Log.d(TAG, prefs.getString("token", "")!! + " (reuse)")
            callback.onTokenGenerated(prefs.getString("token", "")!!)
        } else {

            if (prefs.getString("user", "") != null && prefs.getString("password", "") != null) {

                // Adding an Network Interceptor for Debugging purpose :
                val okHttpClient = OkHttpClient.Builder()
                        .addNetworkInterceptor(StethoInterceptor())
                        .cookieJar(CookieStore)
                        .build()
                AndroidNetworking.initialize(applicationContext(), okHttpClient)

                CookieStore.clearCookies()

                AndroidNetworking.post("https://login.schulportal.hessen.de/")
                        .addBodyParameter("user", prefs.getString("user", ""))
                        .addBodyParameter("password", prefs.getString("password", ""))
                        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.27 Safari/537.36")
                        .setTag("test")
                        .setPriority(Priority.LOW)
                        .build()
                        .getAsString(object : StringRequestListener {
                            override fun onResponse(response: String) {
                                // Todo check if sign-in was successfull
                                prefs.edit().putString("token", CookieStore.getCookie("schulportal.hessen.de", "sid"))
                                        .putLong("token_last_success", Date().time)
                                        .apply()

                                Log.d(TAG, prefs.getString("token", "")!!)
                                if (CookieStore.getCookie("schulportal.hessen.de", "sid") != null)
                                    callback.onTokenGenerated(CookieStore.getCookie("schulportal.hessen.de", "sid")!!)
                            }

                            override fun onError(error: ANError) {
                                Toast.makeText(applicationContext(), error.toString(), Toast.LENGTH_LONG).show()
                            }
                        })
            }
        }
    }


    /*
    // DEMO: Getting calendar ics link

    fun getCalendarLink() {
        // Adding an Network Interceptor for Debugging purpose :
        val okHttpClient = OkHttpClient.Builder()
                .addNetworkInterceptor(StethoInterceptor())
                .cookieJar(CookieStore)
                .build()
        AndroidNetworking.initialize(applicationContext(), okHttpClient)


        AndroidNetworking.post("https://start.schulportal.hessen.de/kalender.php")
                .addBodyParameter("f", "iCalAbo")
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.27 Safari/537.36")
                .setTag("test")
                .setPriority(Priority.LOW)
                .build()
                .getAsString(object : StringRequestListener {
                    override fun onResponse(response: String) {
                        prefs.edit().putString("iCalLink", response).apply()
                        Log.d(TAG, response)
                    }

                    override fun onError(error: ANError) {
                        Toast.makeText(applicationContext(), error.errorDetail, Toast.LENGTH_LONG).show()
                        Log.d(TAG, error.errorBody)
                    }
                })
    }
    */

    interface TokenGeneratedListener {
        fun onTokenGenerated(token: String)
    }


}