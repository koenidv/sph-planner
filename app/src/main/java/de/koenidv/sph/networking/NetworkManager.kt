package de.koenidv.sph.networking

import com.androidnetworking.AndroidNetworking
import de.koenidv.sph.SphPlanner
import okhttp3.OkHttpClient
import com.facebook.stetho.Stetho
import com.facebook.stetho.okhttp3.StethoInterceptor


//  Created by koenidv on 05.12.2020.
class NetworkManager {

    /** Signs in with an externally generated access token and returns it
     *
     */
    fun getAccessToken(): String {

        // Adding an Network Interceptor for Debugging purpose :
        val okHttpClient = OkHttpClient.Builder()
                .addNetworkInterceptor(StethoInterceptor())
                .build()
        AndroidNetworking.initialize(SphPlanner.applicationContext(), okHttpClient)

        return "test"
    }

}