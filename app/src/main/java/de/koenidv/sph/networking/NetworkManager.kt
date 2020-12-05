package de.koenidv.sph.networking

import android.widget.Toast
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONArrayRequestListener
import com.androidnetworking.interfaces.StringRequestListener
import com.facebook.stetho.okhttp3.StethoInterceptor
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import okhttp3.OkHttpClient
import org.json.JSONArray


//  Created by koenidv on 05.12.2020.
class NetworkManager {

    /** Signs in with an externally generated access token and returns it
     *
     */
    public fun getAccessToken(): String {

        // Adding an Network Interceptor for Debugging purpose :
        val okHttpClient = OkHttpClient.Builder()
                .addNetworkInterceptor(StethoInterceptor())
                .build()
        AndroidNetworking.initialize(applicationContext(), okHttpClient)

        AndroidNetworking.get("https://koenidv.de/")
                .setTag("test")
                .setPriority(Priority.LOW)
                .build()
                .getAsString(object : StringRequestListener {
                    override fun onResponse(response: String) {
                        Toast.makeText(applicationContext(), response, Toast.LENGTH_LONG).show()
                    }

                    override fun onError(error: ANError) {
                        Toast.makeText(applicationContext(), error.toString(), Toast.LENGTH_LONG).show()
                    }
                })

        return "test";
    }

}