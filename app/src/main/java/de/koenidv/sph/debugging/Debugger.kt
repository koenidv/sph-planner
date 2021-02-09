package de.koenidv.sph.debugging

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.google.gson.GsonBuilder
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import org.json.JSONObject


//  Created by koenidv on 05.02.2021.
object Debugger {
    const val DEBUGGING_ENABLED = true

    const val LOG_TYPE_SUCCESS = -1
    const val LOG_TYPE_INFO = 0
    const val LOG_TYPE_VAR = 1
    const val LOG_TYPE_WARNING = 2
    const val LOG_TYPE_ERROR = 3

    private val logs = mutableListOf<DebugLog>()

    /**
     * Adds the DebugLog to the list of logs
     */
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

    /**
     * Uploads the log to dogbin, then shows a share sheet with the link
     */
    fun share() {
        upload {
            // Share link to to dogbin
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, it)
                this.type = "text/plain"
            }
            val chooser = Intent.createChooser(sendIntent,
                    SphPlanner.applicationContext().getString(R.string.debugger_share_long))
                    .apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
            SphPlanner.applicationContext().startActivity(chooser)
        }
    }

    /**
     * Copies the log to the clipboard, then opens the online log viewer
     */
    fun view() {
        // Copy the log
        val clipboard: ClipboardManager = SphPlanner.applicationContext()
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("sphplanner-debug",
                GsonBuilder().setPrettyPrinting().create().toJson(logs))
        clipboard.setPrimaryClip(clip)
        // Open the website
        SphPlanner.applicationContext().startActivity(
                Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://koenidv.github.io/sph-planner/debugger"))
                        .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
    }

    /**
     * Uploads all logged entries and calls back with a link to it
     */
    private fun upload(callback: (url: String) -> Unit) {
        // Upload text to https://del.dog, API key in BuildConfig
        // If you need the apikeys.properties file, contact koenidv
        AndroidNetworking.post("https://del.dog/documents")
                // Add pretty-printed json object string as body
                .addStringBody(GsonBuilder().setPrettyPrinting().create().toJson(logs))
                .build()
                .getAsJSONObject(object : JSONObjectRequestListener {
                    override fun onResponse(response: JSONObject) {
                        try {
                            callback("https://del.dog/" + response.get("key"))
                        } catch (e: Exception) {
                            Toast.makeText(SphPlanner.applicationContext(),
                                    "Uploading to dogbin failed: " + e.message,
                                    Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onError(anError: ANError?) {
                        Toast.makeText(SphPlanner.applicationContext(),
                                "Uploading to dogbin failed", Toast.LENGTH_LONG).show()
                    }

                })
    }

    /**
     * Check if the log contains any entries
     */
    fun isEmpty() = logs.isEmpty()


}