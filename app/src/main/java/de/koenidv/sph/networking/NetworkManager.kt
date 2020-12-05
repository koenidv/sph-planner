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
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import de.koenidv.sph.ui.home.HomeFragment
import de.koenidv.sph.ui.home.HomeViewModel
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient


//  Created by koenidv on 05.12.2020.
class NetworkManager {

    val cookies = Cookies()

    /** Signs in with an externally generated access token and returns it
     *
     */
    fun getAccessToken(): String {

        val prefs: SharedPreferences = applicationContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)

        // Adding an Network Interceptor for Debugging purpose :
        val okHttpClient = OkHttpClient.Builder()
                .addNetworkInterceptor(StethoInterceptor())
                .cookieJar(cookies)
                .build()
        AndroidNetworking.initialize(applicationContext(), okHttpClient)

        AndroidNetworking.post("https://login.schulportal.hessen.de/")
                .addBodyParameter("user", prefs.getString("user", ""))
                .addBodyParameter("password", prefs.getString("password", ""))
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.27 Safari/537.36")
                .setTag("test")
                .setPriority(Priority.LOW)
                .build()
                .getAsString(object : StringRequestListener {
                    override fun onResponse(response: String) {
                        Toast.makeText(applicationContext(), response, Toast.LENGTH_LONG).show()
                        Log.d("sid", cookies.getCookie("schulportal.hessen.de", "sid")
                                ?: "SID Error")
                        prefs.edit().putString("token", cookies.getCookie("schulportal.hessen.de", "sid")).apply()
                    }

                    override fun onError(error: ANError) {
                        Toast.makeText(applicationContext(), error.toString(), Toast.LENGTH_LONG).show()
                    }
                })

        return "test"
    }

    class Cookies : CookieJar {
        private val cookies: HashMap<String, List<Cookie>> = HashMap()

        // Save cookies for a domain and account for sph's weird cookie behavior
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            if (url.host().contains("schulportal.hessen.de"))
                this.cookies[url.host().substring(url.host().indexOf(".") + 1)] = cookies
            else
                this.cookies[url.host()] = cookies

        }

        // Load cookies for a domain and account for sph's weird cookie behavior
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            if (url.host().contains("schulportal.hessen.de"))
                return cookies[url.host().substring(url.host().indexOf(".") + 1)] ?: ArrayList()
            else
                return cookies[url.host()] ?: ArrayList()

        }

        // Get a specific cookie for a domain
        fun getCookie(host: String, name: String): String? {
            return cookies[host]?.firstOrNull { it.name() == name }?.value()
        }
    }

}