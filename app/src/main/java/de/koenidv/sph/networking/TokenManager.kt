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
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.TAG
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import okhttp3.OkHttpClient
import java.util.*
import java.util.concurrent.TimeUnit


//  Created by koenidv on 05.12.2020.
class TokenManager {

    val prefs: SharedPreferences = applicationContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)

    /**
     * Creates an signed-in access token
     * @param forceNewToken if a new token should be generated, even if an old one should still be valid
     * @param onComplete Called when a token is ready
     */
    fun generateAccessToken(forceNewToken: Boolean = false, onComplete: (success: Int, token: String?) -> Unit) {


        Log.d(TAG, "Time since last success: " + (Date().time - prefs.getLong("token_last_success", 0)).toString())
        // todo still 15min?

        // Return existing, signed-in token if it was used within 15 Minutes
        // Else get a new token
        if (Date().time - prefs.getLong("token_last_success", 0) <= 15 * 60 * 1000
                && Date().time - prefs.getLong("token_last_success", 0) > 0
                && !forceNewToken) {
            onComplete(NetworkManager.SUCCESS, prefs.getString("token", "")!!)
        } else {
            // Get a new token
            if (prefs.getString("user", "") != null && prefs.getString("password", "") != null) {

                // Adding an Network Interceptor for Debugging purpose :
                val okHttpClient = OkHttpClient.Builder()
                        .addNetworkInterceptor(StethoInterceptor())
                        .cookieJar(CookieStore)
                        .cache(null)
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .build()
                AndroidNetworking.initialize(applicationContext(), okHttpClient)

                CookieStore.clearCookies()

                AndroidNetworking.post(applicationContext().getString(R.string.url_login))
                        .addBodyParameter("user", prefs.getString("schoolid", "") + "." + prefs.getString("user", ""))
                        .addBodyParameter("password", prefs.getString("password", ""))
                        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.27 Safari/537.36")
                        .setTag("token")
                        .setPriority(Priority.HIGH)
                        .build()
                        .getAsString(object : StringRequestListener {
                            override fun onResponse(response: String) {
                                if (Firebase.remoteConfig.getBoolean("token_fix_0106")) {
                                    // As of 2021/01/06 we need to load the site again
                                    // to actually get a session id
                                    AndroidNetworking.get("https://start.schulportal.hessen.de/index.php?i="
                                            + prefs.getString("schoolid", "5146"))
                                            .setTag("token-2")
                                            .setPriority(Priority.HIGH)
                                            .build()
                                            .getAsString(object : StringRequestListener {
                                                override fun onResponse(response: String) {
                                                    validateTokenResponse(response, onComplete)
                                                }

                                                override fun onError(error: ANError) {
                                                    handleError(error, onComplete)
                                                }

                                            })
                                } else {
                                    validateTokenResponse(response, onComplete)
                                }
                            }

                            override fun onError(error: ANError) {
                                handleError(error, onComplete)
                            }
                        })
            }
        }
    }

    private fun validateTokenResponse(response: String, onComplete: (success: Int, token: String?) -> Unit) {
        if (CookieStore.getCookie("schulportal.hessen.de", "sid") != null
                && response.contains("- Schulportal Hessen")
                && !response.contains("Login - Schulportal Hessen")
                && !response.contains("Schulauswahl - Schulportal Hessen")
                && !response.contains("Login failed!")) {
            // Login success todo not always
            onComplete(NetworkManager.SUCCESS, CookieStore.getCookie("schulportal.hessen.de", "sid")!!)
            prefs.edit().putString("token", CookieStore.getCookie("schulportal.hessen.de", "sid"))
                    .putLong("token_last_success", Date().time)
                    .apply()
        } else if (response.contains("Login - Schulportal Hessen")
                || response.contains("Schulauswahl - Schulportal Hessen")) {
            // Login not successful
            onComplete(NetworkManager.FAILED_INVALID_CREDENTIALS, null)
            prefs.edit().putLong("token_last_success", 0).apply()
        } else if (response.contains("Wartungsarbeiten")) {
            // Cannot login at the moment
            onComplete(NetworkManager.FAILED_MAINTENANCE, null)
        } else if (response.contains("Login failed!")
                || response.contains("nonce is empty")) {
            onComplete(NetworkManager.FAILED_SERVER_ERROR, null)
            Log.d(TAG, "Login failed :/")
        } else {
            onComplete(NetworkManager.FAILED_UNKNOWN, null)
            Log.d(TAG, "Login failed; Reason unknown!")
            Log.d(TAG, response)
        }
    }

    private fun handleError(error: ANError, onComplete: (success: Int, token: String?) -> Unit) {
        if (error.errorCode == 0) {
            when (error.errorDetail) {
                "connectionError" -> {
                    // This will also be called if reqest timed out
                    onComplete(NetworkManager.FAILED_NO_NETWORK, null)
                }
                "requestCancelledError" -> {
                    onComplete(NetworkManager.FAILED_CANCELLED, null)
                }
                else -> {
                    Toast.makeText(applicationContext(), error.toString(), Toast.LENGTH_LONG).show()
                }
            }
        } else {
            if (error.errorCode == 500)
                onComplete(NetworkManager.FAILED_SERVER_ERROR, null)
            else {
                Toast.makeText(applicationContext(), error.toString(), Toast.LENGTH_LONG).show()
                onComplete(NetworkManager.FAILED_UNKNOWN, null)
            }
        }
    }

    /**
     * Creates an AES key for encryption / decryption
     */
    /*fun generateAesKey(forceNewKey: Boolean = false, onComplete: (success: Int, key: String?) -> Unit) {
        if (forceNewKey || prefs.getString("token", "token") != prefs.getString("aes_for_token", "aes")) {

            // Creates a (sudo-)random password that will be used
            var password = "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx-xxxxxx3xx".replace("[xy]".toRegex(), Integer.toHexString((floor(Math.random() * 17)).toInt()))

        }
    }*/
}