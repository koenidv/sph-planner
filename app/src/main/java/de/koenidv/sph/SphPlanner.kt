package de.koenidv.sph

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import com.androidnetworking.AndroidNetworking
import com.facebook.stetho.Stetho
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.database.FunctionTilesDb
import de.koenidv.sph.database.UsersDb
import de.koenidv.sph.networking.CookieStore
import de.koenidv.sph.networking.Holidays
import de.koenidv.sph.networking.Messages
import de.koenidv.sph.networking.NetworkManager
import de.koenidv.sph.objects.FunctionTile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.*


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

        lateinit var prefs: SharedPreferences
        lateinit var cacheprefs: SharedPreferences

        /**
         * Store some processed values from the db in sharedprefs
         */
        fun saveCache() {
            // Users
            if (cacheprefs.getLong("users_time", 0) == 0L) {
                cacheprefs.edit().putLong("users_time", Date().time).apply()
            }
            if (cacheprefs.getString("users_locale", "") != Locale.getDefault().language) {
                cacheprefs.edit().putString("users_locale", Locale.getDefault().language).apply()
            }
            cacheprefs.edit().putString("users_cache", Gson().toJson(UsersDb.cache)).apply()
        }
    }

    override fun onCreate() {
        super.onCreate()

        prefs = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        cacheprefs = getSharedPreferences("cache", Context.MODE_PRIVATE)

        // Apply default remote configs
        Firebase.remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

        // Enable Crashlytics and Analytics only in non-debug configuration
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(!BuildConfig.DEBUG)

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

        upgrade()

        CoroutineScope(Dispatchers.IO).launch {
            // Store some processed values from the db in sharedprefs
            // Only restore if not older than a week and same locale
            if (Date().time - cacheprefs.getLong("users_time", 0) <
                    7 * 24 * 360 * 1000 &&
                    cacheprefs.getString("users_locale", "") ==
                    Locale.getDefault().language) {

                val userstype = object : TypeToken<Map<String, Map<String, String>>>() {}.type
                UsersDb.cache = Gson().fromJson(
                        cacheprefs.getString("users_cache", ""), userstype)
            }
        }
    }

    // Upgrade from previous version
    private fun upgrade() {
        val prefs = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)

        if (prefs.getBoolean("introComplete", false)) {

            // 120: GA props
            if (prefs.getInt("appVersion", 0) < 120) {
                // Set new analytics properties
                val analytics = FirebaseAnalytics.getInstance(this)
                // Log school id GA as user property
                analytics.setUserProperty(
                        "school",
                        prefs.getString("schoolid", "0")!!)
                // Log an school course id example to GA
                analytics.setUserProperty(
                        "courseIdExample",
                        CoursesDb.getGmbIdExample())

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

            // 130: Messages, holidays
            if (prefs.getInt("appVersion", 0) < 130) {
                if (FunctionTilesDb.getInstance().supports(FunctionTile.FEATURE_MESSAGES)) {
                    Toast.makeText(this, R.string.upgrading_messages, Toast.LENGTH_LONG).show()
                    NetworkManager().getOwnUserId {
                        Messages().fetch(archived = true) {
                            Holidays().fetch {
                                Toast.makeText(this, R.string.done, Toast.LENGTH_LONG).show()
                                prefs.edit().putInt("appVersion", 130).apply()
                            }
                        }
                    }
                } else {
                    NetworkManager().getOwnUserId {
                        Messages().fetch {
                            prefs.edit().putInt("appVersion", 130).apply()
                        }
                    }
                }
            }
        }
    }


}