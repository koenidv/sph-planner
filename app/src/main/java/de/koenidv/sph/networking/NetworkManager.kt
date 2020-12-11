package de.koenidv.sph.networking

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
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

//  Created by koenidv on 11.12.2020.
class NetworkManager {

    fun loadSiteWithToken(url : String, listener : StringRequestListener) {

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
}