package de.koenidv.sph.networking

import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.StringRequestListener
import com.google.firebase.crashlytics.FirebaseCrashlytics  //https://firebase.google.com/products/crashlytics =>
// Track, prioritize and fix crashes faster
import com.google.firebase.ktx.Firebase                     //https://firebase.google.com/docs/android/setup =>
// Firebase support for kotlin and setup
import com.google.firebase.remoteconfig.ktx.remoteConfig    //https://firebase.google.com/docs/remote-config =>
/*
Firebase Remote Config ist ein Cloud-Dienst, mit dem Sie das Verhalten und das Erscheinungsbild Ihrer App ändern können,
ohne dass Benutzer ein App-Update herunterladen müssen.
Wenn Sie Remote Config verwenden, erstellen Sie In-App-Standardwerte, die das Verhalten und die Darstellung Ihrer App steuern.
Anschließend können Sie später die Firebase-Konsole oder die Back-End-APIs von Remote Config verwenden,
um In-App-Standardwerte für alle App-Nutzer oder für Segmente Ihrer Nutzerbasis zu überschreiben.
Ihre App steuert, wann Updates angewendet werden
und sie kann häufig nach Updates suchen und diese mit vernachlässigbaren Auswirkungen auf die Leistung anwenden.
 */
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.TAG
import de.koenidv.sph.SphPlanner.Companion.appContext
import de.koenidv.sph.SphPlanner.Companion.prefs
import de.koenidv.sph.debugging.DebugLog
import de.koenidv.sph.debugging.Debugger
import de.koenidv.sph.debugging.Debugger.LOG_TYPE_ERROR
import de.koenidv.sph.debugging.Debugger.LOG_TYPE_WARNING
import java.util.*


//  Created by koenidv on 05.12.2020.
object TokenManager {

    private var lastTokenCheck = 0L
    var userid = prefs.getString("userid", "0")!!

    /**
     * Creates an signed-in access token and saves it to CookieStore
     * @param onComplete Called when a token is ready
     */
    fun authenticate(forceNewToken: Boolean = false, onComplete: (success: Int) -> Unit) {
        getToken(forceNewToken) { success, _ ->
            onComplete(success)
        }
    }

    /**
     * Creates an signed-in access token and saves it to CookieStore
     * @param forceNewToken if a new token should be generated, even if an old one should still be valid
     * @param onComplete Called when a token is ready
     */
    fun getToken(forceNewToken: Boolean = false, onComplete: (success: Int, token: String?) -> Unit) {
        if (appContext().getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE).getBoolean("demoMode", false)) {
            DebugLog("TokenMgr", "Using demo mode",
                    type = LOG_TYPE_WARNING)

            onComplete(NetworkManager.SUCCESS, "demo")
            return
        }

        // Use existing, signed-in token if it was used within 15 Minutes
        // Else get a new token
        if (Date().time - prefs.getLong("token_last_success", 0) <= 15 * 60 * 1000
                && Date().time - prefs.getLong("token_last_success", 0) > 0
                && !forceNewToken) {

            // Log using old access token
            DebugLog("TokenMgr", "Using known sid token")

            // If stored session id does not match the last known token,
            // overwrite it
            // Only check once per minute
            if (Date().time - lastTokenCheck > 60 * 1000) {
                if (CookieStore.getToken() !== prefs.getString("token", "")) {  //Referential equality - if token reference in the cookie != reference in pref (App/ User Vorlieben)
                    CookieStore.setToken(prefs.getString("token", "")!!)        //make it equal

                    // Log writing token to cookiestore
                    DebugLog("TokenMgr", "Rewriting sid cookie")
                }
                // Save this time so we don't have to check again within a short time span
                lastTokenCheck = Date().time
            }

            // Call back with success
            onComplete(NetworkManager.SUCCESS, prefs.getString("token", "")!!)
        } else {
            // Get a new token
            if (prefs.getString("user", "") != null && prefs.getString("password", "") != null) {   //User and pw exist
                // Log creating new access token
                DebugLog("TokenMgr", "Authorizing new sid token")

                // Clear old cookies to make sure we get a fresh start
                CookieStore.clearCookies()

                // Try loading sph's sign in page and save the sid cookie
                getTokenWithUrlencoded(onComplete)
            } else { //NO user or pw found
                // Log token failure
                DebugLog("TokenMgr",
                        "Cannot authorize token, no credentials provided",
                        type = LOG_TYPE_ERROR)
                // StKl:26.12.2021 Call back with NO success
                onComplete(NetworkManager.FAILED_TOKEN, prefs.getString("token", "")!!)
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
            DebugLog("TokenMgr", "Sid token authenticated: Success",
                    type = Debugger.LOG_TYPE_SUCCESS)

            onComplete(NetworkManager.SUCCESS, CookieStore.getCookie("schulportal.hessen.de", "sid")!!)

        } else if (response.contains("Login - Schulportal Hessen")
                || response.contains("Schulauswahl - Schulportal Hessen")) {
            // Login not successful

            prefs.edit().putLong("token_last_success", 0).apply()

            // Log failure
            DebugLog("TokenMgr", "Sid auth failed: Invalid credentials",
                    bundleOf("response" to response), LOG_TYPE_ERROR)
            Log.e(TAG, "Login failed; Invalid credentials")

            onComplete(NetworkManager.FAILED_INVALID_CREDENTIALS, null)
        } else if (response.contains("Wartungsarbeiten")) {
            // Cannot login at the moment

            // Log failure
            DebugLog("TokenMgr", "Sid auth failed: Maintenance",
                    bundleOf("response" to response), LOG_TYPE_ERROR)

            onComplete(NetworkManager.FAILED_MAINTENANCE, null)
        } else if (response.contains("Login failed!")
                || response.contains("nonce is empty")
                || response.contains("Aktuell ist leider kein Zugriff möglich. " +
                        "Bitte versuchen Sie es später erneut.")) {
            // Server error

            // Log failure
            DebugLog("TokenMgr", "Sid auth failed: Server error",
                    bundleOf("response" to response), LOG_TYPE_ERROR)

            onComplete(NetworkManager.FAILED_SERVER_ERROR, null)
        } else {
            // Some other error

            // Log failure
            DebugLog("TokenMgr", "Sid auth failed: Unknown error",
                    bundleOf("response" to response), LOG_TYPE_ERROR)
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
        DebugLog("TokenMgr", "NetError getting a sid token", error)

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
                    Toast.makeText(appContext(), error.toString(), Toast.LENGTH_LONG).show()
                    onComplete(NetworkManager.FAILED_UNKNOWN, null)
                }
            }
        } else {
            if (error.errorCode == 500)
                onComplete(NetworkManager.FAILED_SERVER_ERROR, null)
            else {
                Toast.makeText(appContext(), error.toString(), Toast.LENGTH_LONG).show()
                FirebaseCrashlytics.getInstance().recordException(error)
                onComplete(NetworkManager.FAILED_UNKNOWN, null)
            }
        }
    }

    /**
     * Send credentials as application/x-www-form-urlencoded,
     * validate response and save sid cookie if possible
     * 06.03.2021: Removed tempary fix using multipart form data
     */
    private fun getTokenWithUrlencoded(onComplete: (success: Int, token: String?) -> Unit) {
        AndroidNetworking.post(appContext().getString(R.string.url_login))
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
                            DebugLog("TokenMgr", "Using 0106 fix",
                                    type = LOG_TYPE_WARNING)
                            AndroidNetworking.get("https://start.schulportal.hessen.de/index.php?i="
                                    //+ prefs.getString("schoolid", "5146")) <= Used Gymnasium am Mosbacher Berg, Wiesbaden (5146) from FlKö as default value
                                    + prefs.getString("schoolid", "6119"))
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

    // Resets the authentication token
    fun reset() {
        // Log resetting
        DebugLog("TokenMgr",
                "TokenManager reset",
                type = LOG_TYPE_WARNING)

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