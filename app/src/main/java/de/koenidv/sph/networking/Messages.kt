package de.koenidv.sph.networking

import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.StringRequestListener
import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import java.util.*

//  Created by koenidv on 31.01.2021.
class Messages {

    /**
     * Check if the number of visible messages has increased
     */
    fun fetch(callback: (success: Int) -> Unit, markAsRead: Boolean = false) {

        /*
         * We currently cannot get messages, decryption isn't implemented yet.
         * What we can do is get the number of visible messages and check
         * if it is larger than last time we checked.
         * The user will then be directed to sph in their browser,
         * where decryption works.
         * This will require them to sign in again, should they opt
         * not to use the AutoSPH signin service.
         */

        // Firstly, get an access token
        TokenManager().generateAccessToken { success, token ->
            // If getting a token failed, call onComplete
            // with the error and return
            if (success != NetworkManager.SUCCESS) {
                callback(success)
                return@generateAccessToken
            }

            // Make sure session id cookie is set
            CookieStore.setToken(token!!)

            // Now post messages.php with a few parameters
            // a=headers - Titles only (read for entire message)
            // getType=visibleOnly - Only get visible messages (could also be unvisibleOnly)
            // last=0 - Not yet sure what that does, but it is needed to not get an error
            AndroidNetworking.post(SphPlanner.applicationContext().getString(R.string.url_messages))
                    .addBodyParameter("a", "headers")
                    .addBodyParameter("getType", "visibleOnly")
                    .addBodyParameter("last", "0")
                    .setUserAgent("koenidv/sph-planner")
                    .build()
                    .getAsString(object : StringRequestListener {
                        override fun onResponse(response: String) {
                            // The response should be a json object with two values:
                            // total - The number of messages matching our request
                            // rows - The encrypted headers for each message
                            // We only need total's value and therefore don't have
                            // to parse the object
                            val messagesCount: Int
                            try {
                                messagesCount = response.substring(
                                        response.indexOf("\"total\":") + 8,
                                        response.indexOf(",")
                                ).toInt()
                            } catch (e: Exception) {
                                // If getting the message count failed,
                                // log and return
                                Log.e(SphPlanner.TAG, "Getting messages count failed")
                                Log.e(SphPlanner.TAG, e.stackTraceToString())
                                FirebaseCrashlytics.getInstance().recordException(e)
                                // Still continue with a success,
                                // sph's messages page is just too unreliable
                                // and this data not critical
                                callback(NetworkManager.SUCCESS)
                                return
                            }

                            // Get SharedPreferences to load and save messages count
                            val prefs = SphPlanner.applicationContext().getSharedPreferences(
                                    "sharedPrefs", AppCompatActivity.MODE_PRIVATE)

                            // Compare current messages count with the old one
                            // If current is higher and messages should not be marked as read,
                            // notify ui to update
                            if (prefs.getInt("messages_count", 0) < messagesCount
                                    && !markAsRead) {
                                // Update preferences to reflect that new messages might be available
                                prefs.edit().putBoolean("messages_unread", true).apply()

                                // Send a local broadcast to let the current fragment know
                                // it might want to show that new messages may be available
                                val uiBroadcast = Intent("uichange")
                                uiBroadcast.putExtra("content", "messages_maybe")
                                LocalBroadcastManager.getInstance(SphPlanner.applicationContext()).sendBroadcast(uiBroadcast)
                            }

                            // Update prefs even if current count is lower for use next time
                            prefs.edit()
                                    .putInt("messages_count", messagesCount)
                                    .putLong("updated_messages", Date().time)
                                    .apply()

                            // Finally, call back with a success
                            callback(NetworkManager.SUCCESS)
                        }

                        override fun onError(error: ANError) {
                            // Basic error handling should be enough,
                            // as this method failing will not cause
                            // any big issues
                            when (error.errorDetail) {
                                "connectionError" -> {
                                    callback(NetworkManager.FAILED_NO_NETWORK)
                                }
                                "requestCancelledError" -> {
                                    callback(NetworkManager.FAILED_CANCELLED)
                                }
                                else -> {
                                    callback(NetworkManager.FAILED_UNKNOWN)
                                }
                            }
                        }

                    })
        }
    }
}