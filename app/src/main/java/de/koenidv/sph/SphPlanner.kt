package de.koenidv.sph

import android.app.Application
import android.content.Context
import com.androidnetworking.AndroidNetworking
import com.facebook.stetho.Stetho
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.koenidv.sph.database.CoursesDb
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
        fun applicationContext(): Context {
            return instance!!.applicationContext
        }

        // Set application tag for Log
        const val TAG = "SPH-Planner"
    }

    override fun onCreate() {
        super.onCreate()

        // Apply default remote configs
        Firebase.remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

        // Initialize stetho for network debugging
        Stetho.initializeWithDefaults(this)

        // Initialize Android Networking
        // Adding an Network Interceptor for Debugging purpose
        val okHttpClient = OkHttpClient.Builder()
                .addNetworkInterceptor(StethoInterceptor())
                .cookieJar(CookieStore)
                /*.connectTimeout(60, TimeUnit.SECONDS)*/ // sph timeout is 30 seconds
                .build()
        AndroidNetworking.initialize(applicationContext(), okHttpClient)


        // Upgrade from previous version
        val prefs = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        // Upgrade to 120 if user is signed in
        if (prefs.getBoolean("introComplete", false) &&
                prefs.getInt("appVersion", 0) < 121) {

            // Set new analytics properties
            val analytics = FirebaseAnalytics.getInstance(this)
            // Log school id GA as user property
            analytics.setUserProperty(
                    "school",
                    prefs.getString("schoolid", "0")!!)
            // Log an school course id example to GA
            analytics.setUserProperty(
                    "courseIdExample",
                    CoursesDb.getInstance().gmbIdExample)

            // Attachments were moved to files/attachments/
            // Delete all attachments in the old directory, but keep Firebase files
            val files = filesDir.listFiles()
            if (files != null) {
                for (f in files) {
                    if (!f.name.startsWith("generate") &&
                            !f.name.startsWith("Persisted") &&
                            !f.name.startsWith("frc")) {
                        f.delete()
                    }
                }
            }

            prefs.edit().putInt("appVersion", 120).apply()
        }
    }


}