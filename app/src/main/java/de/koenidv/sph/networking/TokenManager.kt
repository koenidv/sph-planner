package de.koenidv.sph.networking

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.StringRequestListener
import com.facebook.stetho.okhttp3.StethoInterceptor
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import okhttp3.OkHttpClient
import java.util.*
import java.util.concurrent.TimeUnit


//  Created by koenidv on 05.12.2020.
class TokenManager {

    val prefs: SharedPreferences = applicationContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)

    /*
     * Returns an signed-in access token
     * todo: Documentation
     */
    fun generateAccessToken(callback: TokenGeneratedListener, forceNewToken : Boolean = false) {

        // Return existing, signed-in token if it was used within 15 Minutes
        // Else get a new token
        if (Date().time - prefs.getLong("token_last_success", 0) <= 15 * 60 * 1000 && !forceNewToken) {
            callback.onTokenGenerated(NetworkManager().SUCCESS, prefs.getString("token", "")!!)
        } else {
            // Get a new token
            if (prefs.getString("user", "") != null && prefs.getString("password", "") != null) {

                // Adding an Network Interceptor for Debugging purpose :
                val okHttpClient = OkHttpClient.Builder()
                        .addNetworkInterceptor(StethoInterceptor())
                        .cookieJar(CookieStore)
                        .cache(null)
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .build()
                AndroidNetworking.initialize(applicationContext(), okHttpClient)

                CookieStore.clearCookies()

                AndroidNetworking.post("https://login.schulportal.hessen.de/")
                        .addBodyParameter("user", prefs.getString("schoolid", "") + "." + prefs.getString("user", ""))
                        .addBodyParameter("password", prefs.getString("password", ""))
                        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.27 Safari/537.36")
                        .setTag("token")
                        .setPriority(Priority.HIGH)
                        .build()
                        .getAsString(object : StringRequestListener {
                            override fun onResponse(response: String) {
                                if (CookieStore.getCookie("schulportal.hessen.de", "sid") != null
                                        && !response.contains("Login - Schulportal Hessen")) {
                                    // Login success
                                    callback.onTokenGenerated(NetworkManager().SUCCESS, CookieStore.getCookie("schulportal.hessen.de", "sid")!!)
                                    prefs.edit().putString("token", CookieStore.getCookie("schulportal.hessen.de", "sid"))
                                            .putLong("token_last_success", Date().time)
                                            .apply()
                                } else if (response.contains("Login - Schulportal Hessen")) {
                                    // Login not successfull
                                    callback.onTokenGenerated(NetworkManager().FAILED_INVALID_CREDENTIALS, "")
                                    prefs.edit().putLong("token_last_success", 0).apply()
                                } else if (response.contains("Wartungsarbeiten")) {
                                    // Cannot login at the moment
                                    callback.onTokenGenerated(NetworkManager().FAILED_MAINTENANCE, "")
                                }
                            }

                            override fun onError(error: ANError) {
                                when (error.errorDetail) {
                                    "connectionError" -> {
                                        // This will also be called if reqest timed out
                                        callback.onTokenGenerated(NetworkManager().FAILED_NO_NETWORK, "")
                                    }
                                    "requestCancelledError" -> {
                                        callback.onTokenGenerated(NetworkManager().FAILED_CANCELLED, "")
                                    }
                                    else -> {
                                        Toast.makeText(applicationContext(), error.toString(), Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        })
            }
        }
    }

    interface TokenGeneratedListener {
        fun onTokenGenerated(success: Int, token: String)
    }


}