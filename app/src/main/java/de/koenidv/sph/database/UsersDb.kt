package de.koenidv.sph.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.objects.User

//  Created by koenidv on 09.01.2021.
object UsersDb {

    var writable: SQLiteDatabase = DatabaseHelper.getInstance().writableDatabase

    /**
     * Get all users from the db,
     * ordered by pinned, then lastname
     */
    fun all(): List<User> = getWithCursor(
            writable.rawQuery(
                    "SELECT * FROM users ORDER BY pinned DESC, lastname", null
            ))

    /**
     * Get a users name
     */
    fun getName(userid: String, lastnamefirst: Boolean = false): String {
        val nameCursor = writable.rawQuery(
                "SELECT firstname, lastname FROM users WHERE user_id=\"$userid\" OR user_id=\"l-$userid\"",
                null
        )
        // If result is empty, return user id
        if (!nameCursor.moveToFirst()) {
            nameCursor.close()
            return userid
        }
        // Get template
        var name = SphPlanner.applicationContext().getString(
                if (lastnamefirst) R.string.users_name_template_last
                else R.string.users_name_template_first)
        // Replace placeholders
        name = name.replace("%firstname", nameCursor.getString(0))
                .replace("%lastname", nameCursor.getString(1))

        nameCursor.close()
        return name
    }

    /**
     * Save a list of users to the db
     */
    fun save(users: List<User>) {
        for (user in users) save(user)
    }

    /**
     * Save a single user to the db
     */
    private fun save(user: User) {
        // Put values into ContentValues
        val cv = ContentValues()
        cv.put("user_id", user.userId)
        cv.put("teacher_id", user.teacherId)
        cv.put("firstname", user.firstname)
        cv.put("lastname", user.lastname)
        cv.put("type", user.type)
        cv.put("pinned", if (user.isPinned) 1 else 0)

        // Add or update user in db
        val cursor: Cursor = writable.rawQuery(
                "SELECT * FROM users WHERE user_id = '" + user.userId + "'", null)
        // If the user does not already exist, insert
        // Else update
        if (cursor.count == 0) {
            writable.insert("users", null, cv)
        } else {
            // Don't update pinned
            cv.remove("pinned")
            writable.update("users", cv,
                    "user_id = '" + user.userId + "'", null)
        }
        cursor.close()
    }

    /**
     * Get a list of users from a cursor pointing at such a table
     */
    private fun getWithCursor(cursor: Cursor): List<User> {
        val returnList = mutableListOf<User>()
        if (cursor.moveToFirst()) {
            do {
                returnList.add(User(
                        userId = cursor.getString(0),
                        teacherId = cursor.getString(1),
                        firstname = cursor.getString(2),
                        lastname = cursor.getString(3),
                        type = cursor.getString(4),
                        isPinned = cursor.getInt(5) == 1
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return returnList
    }
}