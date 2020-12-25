package de.koenidv.sph.database

import android.content.ContentValues
import android.database.Cursor
import de.koenidv.sph.objects.Course
import de.koenidv.sph.objects.Lesson
import de.koenidv.sph.objects.TimetableEntry

//  Created by koenidv on 24.12.2020.
// Sorry, had to do this in Kotlin. Sometimes Java is horrible.
class TimetableDb private constructor() {
    private val dbhelper = DatabaseHelper.getInstance()

    /**
     * Saves a list of lessons to the db
     *
     * @param lessons Lessons to save
     */
    fun save(lessons: List<Lesson>) {
        for (lesson in lessons) {
            save(lesson)
        }
    }

    fun save(lesson: Lesson) {
        val db = dbhelper.readableDatabase
        val cv = ContentValues()

        // Put values into ContentValues
        cv.put("id_course", lesson.idCourse)
        cv.put("day", lesson.day)
        cv.put("hour", lesson.hour)
        cv.put("room", lesson.room)

        // Add lesson to the db
        // Should not throw an exception as there are no unique constraints
        db.insert("timetable", null, cv)
    }

    /**
     * Get lessons for every day as a List of days (1) with hours (2) with simultaneous lessons (3)
     * @param favorites If only favorite courses should be returned (default: true)
     * @return List of lessons matching the requirements
     */
    fun get(favorites: Boolean = true): List<List<List<TimetableEntry>>> {
        val db = dbhelper.readableDatabase
        val courses = if (favorites)
            CoursesDb.getInstance().favorites
        else
            CoursesDb.getInstance().all

        // Get lessons matching the request
        val query: String = if (favorites) {
            "SELECT * from timetable " +
                    "INNER JOIN courses ON timetable.id_course = courses.course_id " +
                    "WHERE courses.isFavorite = 1"
        } else {
            "SELECT * from timetable"
        }
        val unorderedList = getWithCursor(db.rawQuery(query, null)).toMutableList()
        // Create a list for each day containing as many lesson lists as the timetable has maximum hours per day
        val orderedList: List<List<MutableList<TimetableEntry>>> = List(5) { day: Int ->
            List(unorderedList.filter { it.day == day }.maxOf { it.hour }) { mutableListOf() }
        }
        // Map each lesson to the corresponding day / hour list
        unorderedList.map {
            orderedList[it.day][it.hour - 1].add(TimetableEntry(
                    it,
                    courses.find { course: Course? -> course?.courseId == it.idCourse },
                    null
            ))
        }

        return orderedList
    }

    /**
     * Get a list of lessons with a cursor pointing at the table
     * @param cursor Initialized cursor. Will close.
     * @return List of lessons
     */
    private fun getWithCursor(cursor: Cursor): List<Lesson> {
        val returnList: MutableList<Lesson> = mutableListOf()
        if (cursor.moveToFirst()) {
            do {
                val idCourse = cursor.getString(0)
                val day = cursor.getInt(1)
                val hur = cursor.getInt(2)
                val room = cursor.getString(3)
                returnList.add(Lesson(idCourse, day, hur, room))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return returnList
    }

    /**
     * Clears all lessons from timetable db
     */
    @Suppress("unused")
    fun clear() {
        val db = dbhelper.writableDatabase
        db.delete("timetable", null, null)
    }

    companion object {
        var instance: TimetableDb? = null
            get() {
                if (field == null) {
                    field = TimetableDb()
                }
                return field
            }
            private set
    }
}