package de.koenidv.sph

import android.app.Application
import android.content.Context
import android.util.Log
import com.androidnetworking.AndroidNetworking
import com.facebook.stetho.Stetho
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.networking.CookieStore
import de.koenidv.sph.networking.Cryption
import okhttp3.OkHttpClient
import kotlin.system.measureTimeMillis

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

        /*
         * Testing with JS
         */

        var value = "U2FsdGVkX18sFLn6sqWt1LnXbnvWmXx05zTjXNsVuF3FGgElowmgbZ1ycfwvHsOLTWYguVLJiwB0hSo5g5nGGgAHtJJK6AArlaxSFP3GNJ32Ly6nDmPcxEMXtowx0VPVvRBBG\\/o9RUvpeWxGNTW1vWjL0KhuZ+EGWeUCSrU3qLeWzx+jf0wjFqvIaQsPDtbmbvzvBf+zQixVBNKGtcpzLh+cGHaUv5xiSJkyenhMG0MmK9mYMTrwlnSaSm+zHboKFv+LIHmGQeW6zRNvMAVpX8dmKceQNuNpz+E30rh7d8GSeJ\\/sJeZu8Jug2Mk9pFCYg295D0fqAtxuYDXKgw0dSkIxRq54q1f1D7SwUnN95FWui0ZC7NbQWohXGu18K3QLIn9WOleD2wUFh3qbP3+stTXCInc+MFuwyz3KYznTP9szFtZoviSdyu5f8XxGMJmXs1lqG9ZTfFOwAqBtJnVNefDwwMDetuH7mSDBHqjqp0rhvhOoaTkXIqhSagWZqGhgMUlOUDc9hp41ttYU2hPS71HR66fhBSdVYLrD9jREp5M4t8AjO9MLDPuUFgWeFVMO2TAciI8obRRN1PkDBaeANCbRu7PBxSiDfbhL3QJhpgT4TiuhpV15v5j6Tg3oyKOCtc8X\\/WyrDfMcCcjTmTkiE3afd\\/GzAQRo6xYhSDjMnW3hGyce6OYQCeQWXSv0peXjjrm3pbrebANFflR\\/+2+0UoMqw3Xat48JZ4eJfc04JL31qfLr8UgsL0ZuD450TXgIPYshdnvSShy22Uussg2u\\/mxV0lgC2zP4NjJNcrcViXo2bsctXLA+6\\/3E\\/Fjh0174YVLfvXJcS2xt7+i2dr+ulz4qUyu\\/I+WWiQaABTvzsWevOUqJbezKQqHniMVnIo22Mfjn6WcghTIfnybl8ikQmmR6O0b85g+I8z7GMRwyUkvvtMZgVZW1DbbB2le8VTR2AqzKKfzBMwehDW2iQdCihyGfft1OKQ52odeoOA3TmwGAQyJ4xe2bnr1MWpVyRK\\/Ut1t8z+\\/\\/joHMAo03ISjrOlMX2VTRJgn\\/2h+ZRpZmZEGqnNayRFD+xev1NfsGyfwGBukgg\\/0AiJBYqofW7W3f1q6fIuLjVP2UlAuPrJysAdJlwZ1YsHLlCOF0O\\/thGR5Keq1ImUj\\/qgShSBMTsgdg2WQ9Kf0eFsFJ5\\/GPc8Rxz3Qg+6PiBH6yNFu0BKUqkPDQ+PjsQXFDCaOD0+K2X22WpTOPr9lqARP24oSDaEQgO9VT11XkN8MvgVhvAjyYzILiAIeFtbSb\\/icoGeVflsdHnbo93YwqVv22HK1UATdhZItMb+sXCqeHfAl\\/+0D6FB9hl\\/n7jiwzCMwdW6G0v0KLHXDjwBnwA3qKMI3vsDzLtemQxLJmjDcUhGWijE9AqTp4TSdxOFgzyjnpXGeNriFRNo0VkCUHI0a0M\\/XRRm1O+rWdu+hFUf2EMq8Bo7ZAOhzukDGBZaaC4tweoj\\/+eka7tDJtvJ6LE1FuFW85x9T1M8gHU04XaBEPh5+Z\\/dlBkcOdnzXrNFdu45ZUcdNxd9jrxOEd\\/CQlFzcF+vrRIgdpBNy6Ge6+U0+RMpkS8pkfP1zI58DEBlL04ZgoUkVN+qzuWbVRPbTagdnGqlqzfI60PeeLo8ILdJqt5SNKas98aLiVudVMIhRJKS1aCHwkPmdG9Aq3gSYkE2wzvp+gfbpDMRFr\\/NU91PZVAYgo9K8tXKy9oJ263GcTi5BOhK4XWevAkOY7\\/CFOBFpyJpWhG\\/3qCr5RAg0S4tHiO6caRJNZq+12zoQC2sF1gRogP5KT6Cd+TJyttBFPJLVFMOHfcYEfjvc="
        value = value.replace("\\", "")
        val key = "U2FsdGVkX19/LxM5Y8BDeoyBPUjPMGUCklswhYhtze2HRROBbmPd55C63I9m/xlUSHPaVxg6whQtzAy3aioYsg=="

        val time = measureTimeMillis {
            Log.d(TAG, Cryption().execute("decrypt", arrayOf(value, key)).toString())
        }
        Log.d(TAG, "Time to start and execute: $time ms")

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