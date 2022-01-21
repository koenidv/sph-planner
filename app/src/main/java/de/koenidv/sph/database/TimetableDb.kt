package de.koenidv.sph.database

import android.content.ContentValues
import android.database.Cursor
import de.koenidv.sph.debugging.DebugLog
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
        //DebugLog("TimetblDb - Save lesson", "Parser war OK - Now save it. Check if list is still ok?")
        for (lesson in lessons) {
            save(lesson)
        }
    }

    fun save(lesson: Lesson) {
        val db = dbhelper.readableDatabase
        val cv = ContentValues()

        DebugLog("TimetblDb - Save lesson", "... $lesson")

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
        // todo get courses with lessons query

        val courses = if (favorites)
            CoursesDb.getFavorites()
        else
            CoursesDb.getAll()

        // Get lessons matching the request
        val query: String = if (favorites) {
            "SELECT * from timetable " +
            "INNER JOIN courses ON timetable.id_course = courses.course_id " /*+
            "WHERE courses.isFavorite = 1"*/ //StKl:04.12.2021 - Deleted because timetable was NOT complete - NOT favourite ones were missing
            //todo - check where Favourites are set and why
        } else {
            "SELECT * from timetable"
        }

        val unorderedList = getWithCursor(db.rawQuery(query, null)).toMutableList()
        // If there are no courses, return an empty list
        if (unorderedList.isNullOrEmpty()) return listOf()

        DebugLog("TimetblDb", "Unordered list: $unorderedList")
        /*
        Example of
        Unordered list: [
            Lesson(idCourse=m_be_1, day=1, hour=1, room=025, isDisplayed=null),
            Lesson(idCourse=bio_tur_1, day=3, hour=1, room=215, isDisplayed=null),
            Lesson(idCourse=mu_hff_1, day=4, hour=1, room=018, isDisplayed=null),
            Lesson(idCourse=mu_hff_1, day=4, hour=2, room=018, isDisplayed=null),
            Lesson(idCourse=e_wz_1, day=0, hour=2, room=025, isDisplayed=null),
            Lesson(idCourse=e_wz_1, day=1, hour=2, room=025, isDisplayed=null),
            Lesson(idCourse=e_wz_1, day=1, hour=3, room=025, isDisplayed=null),
            Lesson(idCourse=d_wz_1, day=3, hour=2, room=025, isDisplayed=null),
            Lesson(idCourse=d_wz_1, day=3, hour=3, room=025, isDisplayed=null),
            Lesson(idCourse=m_be_1, day=0, hour=3, room=025, isDisplayed=null),
            Lesson(idCourse=m_be_1, day=0, hour=4, room=025, isDisplayed=null),
            ...
            Bundle[{}]
         */

        // Create a list for each day containing as many lesson lists as the timetable has maximum hours per day
        val orderedList: List<List<MutableList<TimetableEntry>>> = try {
            List(5) { day: Int ->
                List(try {
                    // Get the maximum number of lessons for this day
                    unorderedList.filter { it.day == day }.maxOf { it.hour }
                } catch (nse: NoSuchElementException) {
                    // If there are no lessons for this day
                    0
                }) {
                    // Mutable list for each hour per day
                    mutableListOf()
                }
            }
        } catch (nse: NoSuchElementException) {
            List(5) { listOf(mutableListOf()) }
        }
        //val lssn = Lesson(idCourse="", day=-1, hour=-1, room="", isDisplayed=null)
        // Map each lesson to the corresponding day / hour list
        unorderedList.map {
            orderedList[it.day][it.hour - 1].add(TimetableEntry(
                    it,
                    courses.find { course: Course? -> course?.courseId == it.idCourse },
                    null
            ))
        }

        //DebugLog("TimetblDb", "Ordered list: $orderedList")
        /*
        Example of
        Ordered list: [
        [
            [],
            [TimetableEntry(lesson=Lesson(idCourse=e_wz_1, day=0, hour=2, room=025, isDisplayed=null), course=Course(courseId=e_wz_1, gmb_id=e, sph_id=051E08 - F, named_id=Englisch 05f1, number_id=null, fullname=English, id_teacher=wz, isFavorite=true, isLK=false, color=-38349), changes=null)],
            [TimetableEntry(lesson=Lesson(idCourse=m_be_1, day=0, hour=3, room=025, isDisplayed=null), course=Course(courseId=m_be_1, gmb_id=m, sph_id=051M03 - F, named_id=Mathe 05f1, number_id=null, fullname=Maths, id_teacher=be, isFavorite=true, isLK=false, color=-16756544), changes=null)],
            [TimetableEntry(lesson=Lesson(idCourse=m_be_1, day=0, hour=4, room=025, isDisplayed=null), course=Course(courseId=m_be_1, gmb_id=m, sph_id=051M03 - F, named_id=Mathe 05f1, number_id=null, fullname=Maths, id_teacher=be, isFavorite=true, isLK=false, color=-16756544), changes=null)]
        ],

        [
            [TimetableEntry(lesson=Lesson(idCourse=m_be_1, day=1, hour=1, room=025, isDisplayed=null), course=Course(courseId=m_be_1, gmb_id=m, sph_id=051M03 - F, named_id=Mathe 05f1, number_id=null, fullname=Maths, id_teacher=be, isFavorite=true, isLK=false, color=-16756544), changes=null)],
            [TimetableEntry(lesson=Lesson(idCourse=e_wz_1, day=1, hour=2, room=025, isDisplayed=null), course=Course(courseId=e_wz_1, gmb_id=e, sph_id=051E08 - F, named_id=Englisch 05f1, number_id=null, fullname=English, id_teacher=wz, isFavorite=true, isLK=false, color=-38349), changes=null)],
            [TimetableEntry(lesson=Lesson(idCourse=e_wz_1, day=1, hour=3, room=025, isDisplayed=null), course=Course(courseId=e_wz_1, gmb_id=e, sph_id=051E08 - F, named_id=Englisch 05f1, number_id=null, fullname=English, id_teacher=wz, isFavorite=true, isLK=false, color=-38349), changes=null)],
            [TimetableEntry(lesson=Lesson(idCourse=d_wz_1, day=1, hour=4, room=025, isDisplayed=null), course=Course(courseId=d_wz_1, gmb_id=d, sph_id=051D03 - F, named_id=Deutsch 05f1, number_id=null, fullname=German, id_teacher=wz, isFavorite=true, isLK=false, color=-2097152), changes=null)],
            [TimetableEntry(lesson=Lesson(idCourse=d_wz_1, day=1, hour=5, room=025, isDisplayed=null), course=Course(courseId=d_wz_1, gmb_id=d, sph_id=051D03 - F, named_id=Deutsch 05f1, number_id=null, fullname=German, id_teacher=wz, isFavorite=true, isLK=false, color=-2097152), changes=null)],
            [TimetableEntry(lesson=Lesson(idCourse=bio_tur_1, day=1, hour=6, room=215, isDisplayed=null), course=Course(courseId=bio_tur_1, gmb_id=bio, sph_id=051BIO01 - F, named_id=Bio 05f1, number_id=null, fullname=Biology, id_teacher=tur, isFavorite=true, isLK=false, color=-10608585), changes=null)]
        ],

        ...
        */

        /*
        Because of padding/ margin setup, the timetable could be not in shape in case of free lessons
        So, detect [] in ordered list above and fill with empty lesson
        Set color to background somewhere else...
        */
        val defaultTimeTableEntry1 =
            //mutableListOf(
                TimetableEntry(
                    Lesson(idCourse="", day=-1, hour=-1, room="", isDisplayed=null),
                    Course(courseId="free", gmb_id="", sph_id="", named_id="", "", fullname="", id_teacher="", isFavorite=true, isLK=false, color=-16756544),
                   null
                )
            //)
        //val optimizedOrderedList: MutableList<List<MutableList<TimetableEntry>>> = mutableListOf()
        val optimizedOrderedList: MutableList<List<MutableList<TimetableEntry>>> = orderedList.toMutableList()
        for (i in optimizedOrderedList) {
            for(j in i) {
                if(j.isEmpty()) {
                    j.add(defaultTimeTableEntry1)               }
                // else do nothing
            }
        }

        DebugLog("TimetblDb", "Optimized Ordered list: $optimizedOrderedList")

        return orderedList
    }

    /**
     * Get a list of lessons with a cursor pointing at the table
     * @param cursor Initialized cursor. Will close.
     * @return List of lessons
     */
    private fun getWithCursor(cursor: Cursor): List<Lesson> {
        val returnList: MutableList<Lesson> = mutableListOf()
        //DebugLog("TimetblDb", "Cursor list: $cursor")
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

    //companion objects are singleton objects (einelementige Menge, blanke Karte)
    //whose properties and functions are tied (gebunden)
    //to a class but not to the instance of that class
    // => Die Eigenschaft soll bei allen Instanzen der Klasse gleich bleiben
    // => Wird irgendwo daran etwas veraendert, veraendert sich die Eigenschaft ueberall
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