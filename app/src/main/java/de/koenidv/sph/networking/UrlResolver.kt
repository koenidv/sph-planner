package de.koenidv.sph.networking

import android.util.Log
import androidx.core.os.bundleOf
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.OkHttpResponseListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.debugging.DebugLog
import de.koenidv.sph.debugging.Debugger
import de.koenidv.sph.objects.FunctionTile
import okhttp3.Response

//  Created by koenidv on 22.03.2021.
class UrlResolver {

    /**
     * Resolves feature urls if necessary
     */
    //callback explanation: If i call callback, the function will be executed
    fun resolveFeatureUrls(features: List<FunctionTile>, callback: () -> Unit) {
        if (FirebaseRemoteConfig.getInstance().getBoolean("urlresolver_enable_resolving")) {
            // Count resolved tile urls
            var tilesResolved = 0
            // Only resolve sites that have this weird /index.php?a=x&e=y redirect
            val filteredFeatures = features.filter { it.location.contains("index.php?") }

            // If there are no redirects, just call back
            if (filteredFeatures.isEmpty()) {
                callback()
                return
            }

            DebugLog("FeaturesFrag", "Starting feature resolving")

            // Resolve each feature
            for (feature in filteredFeatures) {
                DebugLog("FeaturesFrag", "Callback w/ some resolving")
                resolveUrl(feature.location, callback = { success: Int, resolved: String ->
                    // Save new url to object
                    if (success == NetworkManager.SUCCESS || success == NetworkManager.FAILED_UNKNOWN) {
                        // If success or sph redirected back to home
                        feature.location = resolved
                    }
                    // Save number of tiles resolved
                    tilesResolved++

                    DebugLog("FeaturesFrag", tilesResolved.toString() + "/ " + filteredFeatures.size)

                    // If this was the last tile
                    if (tilesResolved >= filteredFeatures.size) {
                        // As of 22.3.21, sph gets really confused with their session ids when using
                        // their redirects. Therefore reset it after we resolved the urls.
                        if (FirebaseRemoteConfig.getInstance().getBoolean("urlresolver_reset_token")) {
                            TokenManager.reset() //tknrst
                        }

                        DebugLog("FeaturesFrag", "Featuring resolving callback")
                        callback()
                    }
                })
            }
        }
        else {
            DebugLog("FeaturesFrag", "Callback w/o some resolving")
            callback()
        }
    }

    /**
     * Loads an url to resolve it
     * @return resolved url or given url if there was an error
     */
    private fun resolveUrl(url: String, callback: (success: Int, resolvedUrl: String) -> Unit) {
        // Log resolving url
        DebugLog("NetMgr", "Resolving url", bundleOf(
                "url" to url
        ))

        // Getting an access token
        TokenManager.authenticate { success: Int ->

            if (success != NetworkManager.SUCCESS) callback(success, url)

            // Getting webpage as OkHttpResponse
            AndroidNetworking.get(url)
                    .setUserAgent("koenidv/sph-planner")
                    .setPriority(Priority.LOW)
                    .build()
                    .getAsOkHttpResponse(object : OkHttpResponseListener {
                        override fun onResponse(response: Response) {
                            // In some cases, sph redirects back to the home page (why?)
                            // In this case, ignore the new response
                            if (response.request().url().toString() != "https://start.schulportal.hessen.de/index.php") {
                                callback(NetworkManager.SUCCESS, response.request().url().toString())

                                // Log success
                                DebugLog("NetMgr", "Url resolving: success",
                                        bundleOf(
                                                "url" to url,
                                                "resolvedUrl" to response.request().url().toString()
                                        ), Debugger.LOG_TYPE_SUCCESS)
                            } else {
                                callback(NetworkManager.FAILED_UNKNOWN, url)

                                // Log warning
                                DebugLog("NetMgr", "Url resolving failed",
                                        bundleOf(
                                                "url" to url,
                                                "resolvedUrl" to response.request().url().toString()
                                        ), Debugger.LOG_TYPE_WARNING)
                            }
                        }

                        override fun onError(anError: ANError?) {
                            // Running in emulator will cause an ssl error
                            // Network error will also happen if there is a timeout or no connection
                            // Not a huge deal though, the unresolved url will work as well. For now.
                            Log.e(SphPlanner.TAG, anError!!.errorDetail + ": " + url)
                            callback(NetworkManager.FAILED_UNKNOWN, url)

                            // Log warning
                            DebugLog("NetMgr", "Url resolving failed",
                                    anError, bundleOf("url" to url),
                                    Debugger.LOG_TYPE_WARNING)
                        }

                    })
        }
    }
}