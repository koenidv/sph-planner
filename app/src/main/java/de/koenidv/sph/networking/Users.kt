package de.koenidv.sph.networking

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.database.UsersDb
import de.koenidv.sph.objects.User
import org.json.JSONObject
import java.util.*
import java.util.regex.Pattern

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
                                    UsersDb().save(users)
                                    callback(NetworkManager.SUCCESS)
                                }

                            }

                            override fun onError(error: ANError) {
                                // Handle request errors
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
     * Send en email to an adress based upon the template in SharedPrefs
     */
    fun sendEmail(user: User) {
        // Get the template
        var template = SphPlanner.applicationContext()
                .getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
                .getString("users_mail_template", "")!!

        Toast.makeText(SphPlanner.applicationContext(), template, Toast.LENGTH_LONG).show()

        // Replace placholders with data for firstname: fname(n) for first n chars
        template = Pattern.compile("""#fname\((\d)\)""")
                .matcher(template)
                .replaceAll(user.firstname.toString().take("$1".toInt()).toLowerCase(Locale.ROOT))
                .replace("#firstname", user.firstname.toString().toLowerCase(Locale.ROOT))
        // lastname: fname(n) for first n chars
        template = Pattern.compile("""#lname\((\d)\)""")
                .matcher(template)
                .replaceAll(user.lastname.toString().take("$1".toInt()).toLowerCase(Locale.ROOT))
                .replace("#lastname", user.lastname.toString().toLowerCase(Locale.ROOT))
        // Abbreviation and id
        template = template.replace("#abbr", user.teacherId.toString().toLowerCase(Locale.ROOT))
                .replace("#id", user.userId.replace("l-", ""))

        // Now start an intent to send an email to that address
        val emailIntent = Intent.createChooser(Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$template")

        }, "Send feedback").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        SphPlanner.applicationContext().startActivity(emailIntent)
    }

}