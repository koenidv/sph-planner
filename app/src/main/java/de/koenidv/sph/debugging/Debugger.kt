package de.koenidv.sph.debugging

//  Created by koenidv on 05.02.2021.
object Debugger {
    const val DEBUGGING_ENABLED = true

    const val LOG_TYPE_SUCCESS = -1
    const val LOG_TYPE_INFO = 0
    const val LOG_TYPE_VAR = 1
    const val LOG_TYPE_WARNING = 2
    const val LOG_TYPE_ERROR = 3

    private val logs = mutableListOf<DebugLog>()

    fun log(log: DebugLog) {
        logs.add(log)
    }

    /**
     * Get an html document's title from its source
     */
    fun responseTitle(response: String): String {
        return try {
            response.substring(
                    response.indexOf("<title>" + 7),
                    response.indexOf("</title>"))
        } catch (e: Exception) {
            "Reponse String does not contain a title"
        }
    }


}