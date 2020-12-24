package de.koenidv.sph.database

import android.content.ContentValues
import android.database.Cursor
import de.koenidv.sph.objects.Lesson

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
     * Get lessons for a specific day
     * @param day 0: Monday,.. 4: Friday
     * @param favorites If only favorite courses should be returned
     * @return List of lessons matching the requirements
     */
    fun getDay(day: Int, favorites: Boolean): List<List<Lesson>> {
        val db = dbhelper.readableDatabase
        // Get lessons matching the request
        val query: String = if (favorites) {
            ("SELECT * from timetable "
                    + "INNER JOIN courses ON timetable.id_course = courses.course_id "
                    + "WHERE timetable.day = " + day + " AND courses.isFavorite = 1")
        } else {
            ("SELECT * from timetable "
                    + "WHERE day = " + day)
        }
        val unorderedList = getWithCursor(db.rawQuery(query, null)).toMutableList()
        // Map each lesson hour to a list
        val orderedList = mutableListOf<MutableList<Lesson>>()
        unorderedList.map { orderedList[it.hour].add(it) }


        // todo probably won't work
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