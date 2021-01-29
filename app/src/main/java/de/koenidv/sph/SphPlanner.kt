package de.koenidv.sph

import android.app.Application
import android.content.Context
import com.androidnetworking.AndroidNetworking
import com.facebook.stetho.Stetho
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.koenidv.sph.networking.CookieStore
import okhttp3.OkHttpClient

//  Created by koenidv on 05.12.2020.
class SphPlanner : Application() {

    init {
        instance = this
    }

    companion object {
        var openInBrowserUrl: String? = null
        var randomGreeting: String? = null
        var randomGreetingTime: Long = 0
        var webViewFixed = false
        private var instance: SphPlanner? = null

        // Returns the applications context for usage everywhere within the app
        fun applicationContext() : Context {
            return instance!!.applicationContext
        }
        // Set application tag for Log
        const val TAG = "SPH-Planner"
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize stetho for network debugging
        Stetho.initializeWithDefaults(this)

        // Apply default remote configs
        Firebase.remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

        // Initialize Android Networking
        // Adding an Network Interceptor for Debugging purpose :
        val okHttpClient = OkHttpClient.Builder()
                .addNetworkInterceptor(StethoInterceptor())
                .cookieJar(CookieStore)
                /*.connectTimeout(60, TimeUnit.SECONDS)*/ // sph timeout is 30 seconds
                .build()
        AndroidNetworking.initialize(applicationContext(), okHttpClient)
    }


}