package de.koenidv.sph.database

import android.content.ContentValues
import android.database.Cursor
import de.koenidv.sph.objects.Change
import java.util.*

//  Created by koenidv on 24.12.2020.
// Sorry, had to do this in Kotlin. Sometimes Java is horrible.
class ChangesDb private constructor() {
    private val dbhelper = DatabaseHelper.getInstance()

    /**
     * Saves a list of changes to the db
     * @param changes Changes to save
     */
    fun save(changes: List<Change>) {
        for (change in changes) {
            save(change)
        }
    }

    fun save(change: Change) {
        val db = dbhelper.writableDatabase
        val cv = ContentValues()

        // Put values into ContentValues
        cv.put("id_course", change.id_course)
        cv.put("id_course_external", change.id_course_external)
        cv.put("date", change.date.time / 1000)
        cv.put("lessons", change.lessons.joinToString(","))
        cv.put("type", change.type)
        cv.put("id_course_external_before", change.id_course_external_before)
        cv.put("className", change.className)
        cv.put("className_before", change.className_before)
        cv.put("id_teacher", change.id_teacher)
        cv.put("id_subsTeacher", change.id_subsTeacher)
        cv.put("room", change.room)
        cv.put("room_before", change.room_before)
        cv.put("description", change.description)

        // Add change to the db if it's not already there
        db.insert("changes", null, cv)
    }

    /**
     * Get a list of changes with a cursor pointing at the table
     * @param cursor Initialized cursor. Will close.
     * @return List of changes
     */
    private fun getWithCursor(cursor: Cursor): List<Change> {
        val returnList: MutableList<Change> = mutableListOf()
        if (cursor.moveToFirst()) {
            do {
                returnList.add(Change(
                        cursor.getString(0), // id course
                        cursor.getString(1), // id course external
                        Date(cursor.getInt(2) * 1000L), // date
                        cursor.getString(3).split(",").map { it.toInt() }, // affected lessons
                        cursor.getInt(4), // type
                        cursor.getString(5), // id course external before
                        cursor.getString(6), // class name
                        cursor.getString(7), // class name before
                        cursor.getString(8), // id teacher
                        cursor.getString(9), // id substitute teacher
                        cursor.getString(10), // room
                        cursor.getString(11), // room before
                        cursor.getString(12), // description
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return returnList
    }

    /**
     * Returns changes for favorite courses with a date of today or later
     * Ordered by date, ascending
     */
    fun getFavorites(): List<Change> =
            getWithCursor(dbhelper.readableDatabase.rawQuery("SELECT * from changes " +
                    "INNER JOIN courses ON changes.id_course = courses.course_id " +
                    "WHERE changes.date >= ${Date().time / 1000 - 24 * 60 * 60} " +
                    "AND (courses.isFavorite = 1 OR changes.id_course IS NULL) " +
                    "ORDER BY changes.date ASC", null))

    /**
     * Returns all changes, ordered by date
     */
    fun getAll(): List<Change> = getWithCursor(dbhelper.readableDatabase.rawQuery(
            "SELECT * from changes ORDER BY date ASC", null))

    /**
     * Remove changes with a date older than 24 hours ago
     */
    fun removeOld() {
        val limit = Date().time / 1000 - 24 * 60 * 60
        dbhelper.writableDatabase.execSQL("DELETE FROM changes WHERE date < $limit")
    }

    /**
     * Clears all changes from changes db
     */
    @Suppress("unused")
    fun clear() {
        val db = dbhelper.writableDatabase
        db.delete("changes", null, null)
    }

    companion object {
        var instance: ChangesDb? = null
            get() {
                if (field == null) {
                    field = ChangesDb()
                }
                return field
            }
            private set
    }
}