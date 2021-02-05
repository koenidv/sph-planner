package de.koenidv.sph.debugging

import android.os.Bundle
import androidx.core.os.bundleOf
import com.androidnetworking.error.ANError
import de.koenidv.sph.debugging.Debugger.LOG_TYPE_ERROR
import de.koenidv.sph.debugging.Debugger.LOG_TYPE_INFO
import java.util.*

//  Created by koenidv on 05.02.2021.
data class DebugLog(
        val source: String,
        val description: String,
        val data: Bundle = bundleOf(),
        val type: Int = LOG_TYPE_INFO,
        val timestamp: Long = Date().time,
) {

    constructor(source: String, description: String, ANerror: ANError) : this(
            source, description, bundleOf(
            "errorCode" to ANerror.errorCode,
            "errorBody" to ANerror.errorBody,
            "errorDetail" to ANerror.errorBody),
            LOG_TYPE_ERROR
    )

    fun log() {
        Debugger.log(this)
    }
}
