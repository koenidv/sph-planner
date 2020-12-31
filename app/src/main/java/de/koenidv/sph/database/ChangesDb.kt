package de.koenidv.sph.database

import android.content.ContentValues
import android.database.Cursor
import android.util.Log
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

        // Check if this course already exists
        val cursor = db.rawQuery("""SELECT * FROM changes WHERE id_course = "${change.id_course}"
            | AND date = ${change.date.time / 1000} AND lessons = "${change.lessons.joinToString(",")}"
            | AND type = ${change.type}""".trimMargin(), null)
        if (cursor.count == 0) {
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
        cursor.close()
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
                        // 0 is auto increment change id, we don't need that
                        cursor.getString(1), // id course
                        cursor.getString(2), // id course external
                        Date(cursor.getInt(3) * 1000L), // date
                        cursor.getString(4).split(",").map { it.toInt() }, // affected lessons
                        cursor.getInt(5), // type
                        cursor.getString(6), // id course external before
                        cursor.getString(7), // class name
                        cursor.getString(8), // class name before
                        cursor.getString(9), // id teacher
                        cursor.getString(10), // id substitute teacher
                        cursor.getString(11), // room
                        cursor.getString(12), // room before
                        cursor.getString(13), // description
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
    fun getFavorites(): List<Change> {
        Log.d("SPH-PLANNER", Date().time.toString())
        return getWithCursor(dbhelper.readableDatabase.rawQuery("SELECT * from changes " +
                "INNER JOIN courses ON changes.id_course = courses.course_id " +
                "WHERE changes.date >= ${(Date().time / 1000) - (24 * 60 * 60)} " +
                "AND (courses.isFavorite = 1 OR changes.id_course IS NULL) " +
                "ORDER BY changes.date ASC", null))
    }

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