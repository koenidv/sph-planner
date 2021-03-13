package de.koenidv.sph.debugging

import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import com.androidnetworking.error.ANError
import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.koenidv.sph.SphPlanner.Companion.TAG
import de.koenidv.sph.debugging.Debugger.LOG_TYPE_ERROR
import de.koenidv.sph.debugging.Debugger.LOG_TYPE_INFO
import de.koenidv.sph.debugging.Debugger.LOG_TYPE_WARNING
import java.util.*

//  Created by koenidv on 05.02.2021.
/**
 * Create a debug log and instantly log it to Debugger
 */
data class DebugLog(
        val source: String,
        val description: String,
        val data: Bundle = bundleOf(),
        val type: Int = LOG_TYPE_INFO,
        val timestamp: Long = Date().time,
) {

    // Log this entry as soon as it's created
    init {
        log()
    }

    // Construct a debug log from a networking error
    constructor(source: String, description: String, ANerror: ANError, data: Bundle = bundleOf(), type: Int = LOG_TYPE_ERROR) :
            this(
                    source, description,
                    bundleOf(
                            "errorCode" to ANerror.errorCode,
                            "errorBody" to ANerror.errorBody,
                            "errorDetail" to ANerror.errorBody,
                    ).apply { putAll(data) },
                    type
            )

    // Construct a debug log from an exception and record it to firebase
    constructor(source: String, description: String, error: Exception) : this(
            source, description,
            bundleOf("exception" to error.stackTraceToString()),
            LOG_TYPE_ERROR
    ) {
        FirebaseCrashlytics.getInstance().recordException(error)
    }

    /**
     * Save this entry to the debug log
     */
    private fun log() {
        if (Debugger.DEBUGGING_ENABLED)
            Debugger.log(this)
        when (type) {
            LOG_TYPE_WARNING -> Log.d("$TAG $source", description)
            LOG_TYPE_ERROR -> Log.e("$TAG $source", description)
        }
    }
}
