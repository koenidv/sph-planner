package de.koenidv.sph.networking

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.os.bundleOf
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.database.UsersDb
import de.koenidv.sph.debugging.DebugLog
import de.koenidv.sph.debugging.Debugger
import de.koenidv.sph.objects.User
import org.json.JSONObject
import java.util.*

//  Created by koenidv on 31.01.2021.
class Users {

    /**
     * Load users, filter for teachers and save to db
     * This will use sph's message recipient search function
     * to get every recipient for each character.
     * This might include other students, but fortunately we should
     * be able to just filter for type=lul
     */
    fun fetch(callback: (success: Int) -> Unit) {
        // Log fetching users
        if (Debugger.DEBUGGING_ENABLED)
            DebugLog("Users", "Fetching users").log()

        // We need to make sure that we have an access token
        TokenManager().authenticate { success, token ->
            // If getting a token failed, call onComplete
            // with the error and return
            if (success != NetworkManager.SUCCESS) {
                callback(success)
                return@authenticate
            }

            val users = mutableListOf<User>()
            val userIds = mutableListOf<String>()
            val favoriteTeachers = CoursesDb.getInstance().favoriteTeacherIds

            // Iterate through every single char
            var char = 'a'
            var completed = 0
            while (char <= 'z') {
                // Log fetching users
                if (Debugger.DEBUGGING_ENABLED)
                    DebugLog("Users", "Loading users for $char").log()
                // Not get the all recipients for the current character
                AndroidNetworking.post(SphPlanner.applicationContext().getString(R.string.url_messages))
                        .addBodyParameter("a", "searchRecipt")
                        .addBodyParameter("q", char.toString())
                        .build()
                        .getAsJSONObject(object : JSONObjectRequestListener {
                            override fun onResponse(response: JSONObject) {
                                var index = 0
                                val items = response.getJSONArray("items")
                                var currentItem: JSONObject
                                var text: String
                                var firstname: String?
                                var lastname: String?
                                var teacherId: String?

                                // Get every item from the array
                                while (index < response.getInt("total_count")) {
                                    currentItem = items.getJSONObject(index)

                                    // If list doesn't contain this user yet
                                    // Also, only get teachers at this time (type=lul)
                                    if (!userIds.contains(currentItem.getString("id"))
                                            && currentItem.getString("type") == "lul") {
                                        // We'll only use names with "," and "(..)"
                                        // This way we'll ignore admin entries
                                        // However, this will not add anything if a school does not
                                        // show a teacher's first name or shorthand
                                        text = currentItem.getString("text")

                                        if (text.contains(",") &&
                                                text.contains("""\(.*\)""".toRegex())) {

                                            // Get the name and shorthand
                                            lastname = text.substring(0, text.indexOf(","))
                                            firstname = text.substring(
                                                    text.indexOf(", ") + 2,
                                                    text.indexOf(" (")
                                            )
                                            teacherId = text.substring(
                                                    text.indexOf(" (") + 2,
                                                    text.indexOf(")")
                                            ).toLowerCase(Locale.ROOT)

                                            // Add it to the list
                                            users.add(User(
                                                    userId = currentItem.getString("id"),
                                                    teacherId = teacherId,
                                                    firstname = firstname,
                                                    lastname = lastname,
                                                    type = currentItem.getString("type"),
                                                    favoriteTeachers.contains(teacherId)
                                            ))
                                            // Add id to the list for faster checks of existing users
                                            userIds.add(currentItem.getString("id"))
                                        }
                                    }

                                    index++ // Next user for this result
                                }

                                // We could check for char = z here, but that'd break if one
                                // request takes longer than the previous
                                completed++
                                if (completed == 26) {
                                    // If this was the last request, save the user list
                                    // and call back success
                                    UsersDb.save(users)
                                    callback(NetworkManager.SUCCESS)

                                    // Log success
                                    if (Debugger.DEBUGGING_ENABLED)
                                        DebugLog("Users", "Loaded users",
                                                bundleOf("usersCount" to users.size),
                                                type = Debugger.LOG_TYPE_SUCCESS).log()
                                }

                            }

                            override fun onError(error: ANError) {
                                // Handle request errors

                                // Log error
                                if (Debugger.DEBUGGING_ENABLED)
                                    DebugLog("Users", "Error loading users",
                                            error).log()
                                Log.d(SphPlanner.TAG, error.errorDetail)

                                when (error.errorDetail) {
                                    "connectionError" -> {
                                        callback(NetworkManager.FAILED_NO_NETWORK)
                                    }
                                    "requestCancelledError" -> {
                                        callback(NetworkManager.FAILED_CANCELLED)
                                    }
                                    else -> {
                                        callback(NetworkManager.FAILED_UNKNOWN)
                                    }
                                }
                            }

                        })
                // Use the next char
                char++
            }

        }
    }

    /**
     * Add a teacher using their id and username (lastname, firstname (abbr))
     */
    fun addTeacherFromMessage(id: String, username: String) {
        val matches = Regex("""(.*),\s(.*)\s\((.*)\)""").find(username)?.groupValues

        if (matches != null && matches.size > 1) {
            UsersDb.save(listOf(User(
                    "l-$id",
                    matches[3],
                    matches[2],
                    matches[1],
                    "lul",
                    false // todo check if teacher should be pinned
            )))
        }
    }

    /**
     * Find a user's id by their username (lastname, firstname (abbr))
     */
    fun getTeacherUserId(username: String): String? {
        val matches = Regex("""(.*),\s(.*)\s\((.*)\)""").find(username)?.groupValues

        return if (matches != null && matches.size > 1) {
            UsersDb.getTeacherUserId(matches[2], matches[1], matches[3])
        } else {
            null
        }
    }


    /**
     * Send en email to an adress based upon the template in SharedPrefs
     */
    fun sendEmail(user: User) {
        // Get the template
        var template = SphPlanner.applicationContext()
                .getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
                .getString("users_mail_template", "")!!

        // Replace placholders with data for firstname: fname(n) for first n chars
        template = Regex("""#fname\((\d)\)""").replace(template, transform = { match ->
            user.firstname.toString()
                    .take(match.groupValues[1].toInt())
                    .toLowerCase(Locale.ROOT)
        })
        // lastname: lname(n) for first n chars
        template = Regex("""#lname\((\d)\)""").replace(template, transform = { match ->
            user.lastname.toString()
                    .take(match.groupValues[1].toInt())
                    .toLowerCase(Locale.ROOT)
        })
        // Full names, abbreviation and id
        template = template.replace("#firstname", user.firstname.toString().toLowerCase(Locale.ROOT))
                .replace("#lastname", user.lastname.toString().toLowerCase(Locale.ROOT))
                .replace("#abbr", user.teacherId.toString().toLowerCase(Locale.ROOT))
                .replace("#id", user.userId.replace("l-", ""))

        // Now start an intent to send an email to that address
        val emailIntent = Intent.createChooser(Intent(Intent.ACTION_SENDTO)
                .apply {
                    data = Uri.parse("mailto:$template")
                },
                SphPlanner.applicationContext().getString(R.string.users_email_title)
                        .replace("%firstname", user.firstname.toString())
                        .replace("%lastname", user.lastname.toString()))
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

        SphPlanner.applicationContext().startActivity(emailIntent)
    }

}