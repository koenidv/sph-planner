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

        fun applicationContext() : Context {
            return instance!!.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        Stetho.initializeWithDefaults(this);
    }
}