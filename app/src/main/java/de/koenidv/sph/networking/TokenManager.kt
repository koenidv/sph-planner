package de.koenidv.sph.networking

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.Log
import android.widget.Toast
import androidx.core.os.bundleOf
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.StringRequestListener
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.TAG
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import de.koenidv.sph.debugging.DebugLog
import de.koenidv.sph.debugging.Debugger
import de.koenidv.sph.debugging.Debugger.LOG_TYPE_ERROR
import de.koenidv.sph.debugging.Debugger.LOG_TYPE_WARNING
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.util.*


//  Created by koenidv on 05.12.2020.
class TokenManager {

    companion object {
        var lastTokenCheck = 0L
    }

    val prefs: SharedPreferences = applicationContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)

    /**
     * Creates an signed-in access token and saves it to CookieStore
     * @param forceNewToken if a new token should be generated, even if an old one should still be valid
     * @param onComplete Called when a token is ready
     */
    fun authenticate(forceNewToken: Boolean = false, onComplete: (success: Int, token: String?) -> Unit) {

        // Use existing, signed-in token if it was used within 15 Minutes
        // Else get a new token
        if (Date().time - prefs.getLong("token_last_success", 0) <= 15 * 60 * 1000
                && Date().time - prefs.getLong("token_last_success", 0) > 0
                && !forceNewToken) {

            // Log using old access token
            if (Debugger.DEBUGGING_ENABLED)
                DebugLog("TokenMgr", "Using known sid token").log()

            // If stored session id does not match the last known token,
            // overwrite it
            // Only check once per minute
            if (Date().time - lastTokenCheck > 60 * 1000) {
                if (CookieStore.getToken() !== prefs.getString("token", "")) {
                    CookieStore.setToken(prefs.getString("token", "")!!)

                    // Log writing token to cookiestore
                    if (Debugger.DEBUGGING_ENABLED)
                        DebugLog("TokenMgr", "Rewriting sid cookie").log()
                }
                // Save this time so we don't have to check again within a short time span
                lastTokenCheck = Date().time
            }

            // Call back with success
            onComplete(NetworkManager.SUCCESS, prefs.getString("token", "")!!)
        } else {
            // Get a new token
            if (prefs.getString("user", "") != null && prefs.getString("password", "") != null) {
                // Log creating new access token
                if (Debugger.DEBUGGING_ENABLED)
                    DebugLog("TokenMgr", "Authorizing new sid token").log()

                // Clear old cookies to make sure we get a fresh start
                CookieStore.clearCookies()

                // Try loading sph's sign in page and save the sid cookie
                if (Firebase.remoteConfig.getBoolean("token_fix_0130")) {

                    // As of 2021/01/30, we now need to send the credentials data using
                    // multipart/form-data instead of application/x-www-form-urlencoded
                    // Why though? Don't ask me...
                    getTokenWithMultipart(onComplete)

                } else {

                    // If sph changes this back, just use the old logic
                    getTokenWithUrlencoded(onComplete)

                }
            } else {
                // Log token failure
                if (Debugger.DEBUGGING_ENABLED) {
                    DebugLog("TokenMgr",
                            "Cannot authorize token, no credentials provided",
                            type = LOG_TYPE_ERROR).log()
                }
            }
        }
    }

    /**
     * Check if the response indicates that the user is signed in
     * and if so, save the current session id cookie
     * else return the applicable error code
     */
    private fun validateTokenResponse(response: String, onComplete: (success: Int, token: String?) -> Unit) {
        if (CookieStore.getCookie("schulportal.hessen.de", "sid") != null
                && response.contains("- Schulportal Hessen")
                && !response.contains("Login - Schulportal Hessen")
                && !response.contains("Schulauswahl - Schulportal Hessen")
                && !response.contains("Login failed!")) {

            // Login was successful!
            prefs.edit().putString("token", CookieStore.getCookie("schulportal.hessen.de", "sid"))
                    .putLong("token_last_success", Date().time)
                    .apply()

            // Log success
            if (Debugger.DEBUGGING_ENABLED)
                DebugLog("TokenMgr", "Sid token authenticated: Success",
                        type = Debugger.LOG_TYPE_SUCCESS).log()

            onComplete(NetworkManager.SUCCESS, CookieStore.getCookie("schulportal.hessen.de", "sid")!!)
          
        } else if (response.contains("Login - Schulportal Hessen")
                || response.contains("Schulauswahl - Schulportal Hessen")) {
            // Login not successful

            prefs.edit().putLong("token_last_success", 0).apply()

            // Log failure
            if (Debugger.DEBUGGING_ENABLED)
                DebugLog("TokenMgr", "Sid auth failed: Invalid credentials",
                        bundleOf("response" to response), LOG_TYPE_ERROR).log()
            Log.e(TAG, "Login failed; Invalid credentials")

            onComplete(NetworkManager.FAILED_INVALID_CREDENTIALS, null)
        } else if (response.contains("Wartungsarbeiten")) {
            // Cannot login at the moment

            // Log failure
            if (Debugger.DEBUGGING_ENABLED)
                DebugLog("TokenMgr", "Sid auth failed: Maintenance",
                        bundleOf("response" to response), LOG_TYPE_ERROR).log()
            Log.d(TAG, "Login failed; Maintenance")

            onComplete(NetworkManager.FAILED_MAINTENANCE, null)
        } else if (response.contains("Login failed!")
                || response.contains("nonce is empty")
                || response.contains("Aktuell ist leider kein Zugriff möglich. " +
                        "Bitte versuchen Sie es später erneut.")) {
            // Server error

            // Log failure
            if (Debugger.DEBUGGING_ENABLED)
                DebugLog("TokenMgr", "Sid auth failed: Server error",
                        bundleOf("response" to response), LOG_TYPE_ERROR).log()
            Log.d(TAG, "Login failed; Server error")

            onComplete(NetworkManager.FAILED_SERVER_ERROR, null)
        } else {
            // Some other error

            // Log failure
            if (Debugger.DEBUGGING_ENABLED)
                DebugLog("TokenMgr", "Sid auth failed: Unknown error",
                        bundleOf("response" to response), LOG_TYPE_ERROR).log()
            Log.e(TAG, "Login failed; Reason unknown!")
            Log.d(TAG, response)

            onComplete(NetworkManager.FAILED_UNKNOWN, null)
        }
    }

    /**
     * Return an internal error code for an networking error
     */
    private fun handleError(error: ANError, onComplete: (success: Int, token: String?) -> Unit) {
        // Log network error
        if (Debugger.DEBUGGING_ENABLED)
            DebugLog("TokenMgr",
                    "NetError getting a sid token",
                    error
            ).log()

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
                    onComplete(NetworkManager.FAILED_UNKNOWN, null)
                }
            }
        } else {
            if (error.errorCode == 500)
                onComplete(NetworkManager.FAILED_SERVER_ERROR, null)
            else {
                Toast.makeText(applicationContext(), error.toString(), Toast.LENGTH_LONG).show()
                FirebaseCrashlytics.getInstance().recordException(error)
                onComplete(NetworkManager.FAILED_UNKNOWN, null)
            }
        }
    }

    /**
     * Send credentials as application/x-www-form-urlencoded,
     * validate response and save sid cookie if possible
     */
    private fun getTokenWithUrlencoded(onComplete: (success: Int, token: String?) -> Unit) {
        AndroidNetworking.post(applicationContext().getString(R.string.url_login))
                .addBodyParameter("user", prefs.getString("schoolid", "") +
                        "." + prefs.getString("user", ""))
                .addBodyParameter("password", prefs.getString("password", ""))
                .setTag("token")
                .setPriority(Priority.HIGH)
                .build()
                .getAsString(object : StringRequestListener {
                    override fun onResponse(response: String) {
                        if (Firebase.remoteConfig.getBoolean("token_fix_0106")) {
                            // As of 2021/01/06 we need to load the site again
                            // to actually get a session id
                            // Log 0106 fix
                            if (Debugger.DEBUGGING_ENABLED)
                                DebugLog("TokenMgr", "Using 0106 fix").log()
                            Log.d(TAG, "Using second token request (0106)")
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

    /**
     * Send credentials as multipart/form-data,
     * validate response and save sid cookie if possible
     */
    private fun getTokenWithMultipart(onComplete: (success: Int, token: String?) -> Unit) {
        // Log 0130 fix
        if (Debugger.DEBUGGING_ENABLED) DebugLog("TokenMgr", "Using 0130 fix").log()

        if (isOnline()) {
            GlobalScope.launch {
                Log.d(TAG, "Using fallback token request (0130)")
                val client = OkHttpClient().newBuilder()
                        .cookieJar(CookieStore)
                        .build()
                val body: RequestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                        .build()
                val request: Request = Request.Builder()
                        .url("https://login.schulportal.hessen.de/")
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.22 Safari/537.36")
                        .method("POST", body)
                        .build()

                @Suppress("BlockingMethodInNonBlockingContext")
                val response = client.newCall(request).execute()

                @Suppress("BlockingMethodInNonBlockingContext")
                val responsebody = response.body()?.string()

                if (response.isSuccessful && responsebody != null) {
                    validateTokenResponse(responsebody, onComplete)
                } else {
                    onComplete(NetworkManager.FAILED_SERVER_ERROR, null)
                }
            }
        } else onComplete(NetworkManager.FAILED_NO_NETWORK, null)
    }

    /** Check if device is online,
     * copied from https://developer.android.com/training/basics/network-ops/managing */
    @Suppress("DEPRECATION")
    private fun isOnline(): Boolean {
        val connMgr = applicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = connMgr.activeNetworkInfo
        return networkInfo?.isConnected == true
    }

    // Resets the authentication token
    fun reset() {
        // Log resetting
        if (Debugger.DEBUGGING_ENABLED)
            DebugLog("TokenMgr",
                    "TokenManager reset",
                    type = LOG_TYPE_WARNING).log()

        // Clear cookies
        CookieStore.clearCookies()
        // Remove from SharedPrefs
        prefs.edit()
                .remove("token")
                .remove("token_last_success")
                .apply()
        // Reset last check time
        lastTokenCheck = 0L

    }

}