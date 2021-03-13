package de.koenidv.sph.debugging

import android.content.*
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.google.gson.GsonBuilder
import de.koenidv.sph.BuildConfig
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.appContext
import de.koenidv.sph.database.CoursesDb
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader


//  Created by koenidv on 05.02.2021.
object Debugger {
    var DEBUGGING_ENABLED = BuildConfig.DEBUG

    const val LOG_TYPE_SUCCESS = -1
    const val LOG_TYPE_INFO = 0
    const val LOG_TYPE_VAR = 1
    const val LOG_TYPE_WARNING = 2
    const val LOG_TYPE_ERROR = 3

    private val logs = mutableListOf<DebugLog>()
    private var logcatAdded = false

    val prefs: SharedPreferences = appContext()
            .getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)

    init {
        DEBUGGING_ENABLED = prefs.getBoolean("debugging_enabled", false)
        if (DEBUGGING_ENABLED) addStartLog()
    }

    /**
     * Adds the DebugLog to the list of logs
     */
    fun log(log: DebugLog) {
        logs.add(log)
    }

    /**
     * Check if the log contains any entries
     */
    fun isEmpty() = logs.isEmpty()

    /**
     * Enable or Disable logging
     */
    fun setEnabled(enabled: Boolean) {
        DEBUGGING_ENABLED = enabled
        prefs.edit().putBoolean("debugging_enabled", enabled).apply()
        if (enabled) addStartLog()
        else logcatAdded = false
    }

    /**
     * Get an html document's title from its source
     */
    fun responseTitle(response: String): String {
        if (!DEBUGGING_ENABLED) return "(Debugging disabled)"
        return try {
            response
                    .substringAfter("<title>")
                    .substringBefore("<")
        } catch (e: Exception) {
            "Getting a title from response failed: " + response.take(200)
        }
    }

    /**
     * Uploads the log to dogbin, then shows a share sheet with the link
     */
    fun share() {
        addLogcat()
        upload {
            // Share link to to dogbin
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, it)
                this.type = "text/plain"
            }
            val chooser = Intent.createChooser(sendIntent,
                    appContext().getString(R.string.debugger_share_long))
                    .apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        // todo Debugger: Custom share target
                        // Provide a share target to open a user's email app with debug data
                    }
            appContext().startActivity(chooser)
        }
    }

    /**
     * Copies the log to the clipboard, then opens the online log viewer
     */
    fun view() {
        addLogcat()
        // Copy the log
        val clipboard: ClipboardManager = appContext()
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("sphplanner-debug",
                GsonBuilder().setPrettyPrinting().create().toJson(logs))
        clipboard.setPrimaryClip(clip)
        // Open the website
        appContext().startActivity(
                Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://koenidv.github.io/sph-planner/debugger"))
                        .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
    }

    /**
     * Uploads all logged entries and calls back with a debuggerlink to it
     */
    private fun upload(callback: (url: String) -> Unit) {
        // Upload text to https://del.dog
        AndroidNetworking.post("https://del.dog/documents")
                // Add pretty-printed json object string as body
                .addStringBody(GsonBuilder().setPrettyPrinting().create().toJson(logs))
                .build()
                .getAsJSONObject(object : JSONObjectRequestListener {
                    override fun onResponse(response: JSONObject) {
                        try {
                            callback(appContext().getString(
                                    R.string.url_web_debugger, response.get("key")))
                        } catch (e: Exception) {
                            Toast.makeText(appContext(),
                                    "Uploading to dogbin failed: " + e.message,
                                    Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onError(anError: ANError?) {
                        Toast.makeText(appContext(),
                                "Uploading to dogbin failed", Toast.LENGTH_LONG).show()
                    }

                })
    }

    /**
     * Add a log with school id and an gmbId example on startup
     */
    private fun addStartLog() {
        logs.add(DebugLog("Debugger",
                "Started logging",
                bundleOf("school" to prefs.getString("schoolid", "0"),
                        "gmbIdExample" to CoursesDb.getGmbIdExample())))
    }

    /**
     * Append this session's logcat to the log
     */
    private fun addLogcat() {
        if (!logcatAdded) {
            // Try appending this session's logcat
            try {
                val process = Runtime.getRuntime().exec("logcat -d")
                val bufferedReader = BufferedReader(
                        InputStreamReader(process.inputStream))
                val log = mutableMapOf<String, String>()
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    log[(log.size + 1).toString()] = line.toString()
                }
                DebugLog("logcat", "Logcat dump",
                        bundleOf(*log.toList().toTypedArray()))
                logcatAdded = true
            } catch (e: IOException) {
                DebugLog("logcat", "Dumping logcat failed",
                        type = LOG_TYPE_ERROR)
            }
        }
    }

}