package de.koenidv.sph

import android.app.Application
import android.content.Context
import com.facebook.stetho.Stetho

//  Created by koenidv on 05.12.2020.
class SphPlanner : Application() {

    init {
        instance = this
    }

    companion object {
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
    }
}