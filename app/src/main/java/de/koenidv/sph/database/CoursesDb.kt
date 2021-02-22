package de.koenidv.sph.database

import android.content.ContentValues
import android.database.Cursor
import de.koenidv.sph.objects.Course
import java.util.*

// Kotlin migration of CoursesDb.java created by R-Theis on 04.12.2020.
//  Created by koenidv on 22.02.2021.
object CoursesDb {

    private val dbhelper = DatabaseHelper.getInstance()

    /**
     * Clears all courses from course db
     */
    fun clear() {
        val db = dbhelper.writableDatabase
        //db.rawQuery("DELETE FROM courses", null).close();
        db.delete("courses", null, null)
    }

    /**
     * Adds or updates courses in the database
     * Will override everything for the same course_id if it's not null
     *
     * @param courses List of courses to be added or updated
     */
    fun save(courses: List<Course>) {
        for (course in courses) {
            save(course)
        }
    }

    /**
     * Adds or updates a course in the database
     * Will override everything for the same course_id if it's not null
     *
     * @param course course to be added or updated
     */
    fun save(course: Course) {
        val db = dbhelper.writableDatabase
        val cv = ContentValues()

        // Only use values that are non-null
        cv.put("course_id", course.courseId)
        if (course.gmb_id != null) cv.put("gmb_id", course.gmb_id)
        if (course.sph_id != null) cv.put("sph_id", course.sph_id)
        if (course.named_id != null) cv.put("named_id", course.named_id)
        if (course.number_id != null) cv.put("number_id", course.number_id)
        if (course.fullname != null) cv.put("fullname", course.fullname)
        cv.put("id_teacher", course.id_teacher) // Will never be null
        if (course.isFavorite != null) cv.put("isFavorite", course.isFavorite)
        if (course.isLK != null) cv.put("isLK", course.isLK)
        if (course.color != null) cv.put("color", course.color)


        // Check if row exists and insert or update accordingly
        val cursor = db.rawQuery("SELECT * FROM courses WHERE course_id = '" + course.courseId + "'", null)
        if (cursor.count == 0) db.insert("courses", null, cv) else db.update("courses", cv, "course_id = '" + course.courseId + "'", null)
        cursor.close()
    }

    /**
     * This will set all courses with isFavorite = null in the db to isFavorite = false
     * Used in course indexing where we know which courses are favorites, but not which are not
     */
    fun setNulledNotFavorite() {
        val db = dbhelper.writableDatabase
        db.execSQL("UPDATE courses SET isFavorite = 0 WHERE isFavorite IS NULL")
    }

    /**
     * get all courses in database
     *
     * @return all Courses
     */
    fun getAll(): List<Course> {
        val returnList: MutableList<Course> = ArrayList()
        val queryString = "SELECT * FROM courses"
        val db = dbhelper.readableDatabase
        val cursor = db.rawQuery(queryString, null)
        if (cursor.moveToFirst()) {
            do {
                val newCourse = Course(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.getString(6),
                        cursor.getInt(7) == 1,
                        cursor.getInt(8) == 1,
                        cursor.getInt(9))
                returnList.add(newCourse)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return returnList
    }

    /**
     * Get a course by its internal id
     *
     * @param courseId Internal id of the course to find
     * @return Course with internal id or null
     */
    fun getByInternalId(courseId: String?): Course? {
        if (courseId == null) return null
        // Query course from Database
        val db = dbhelper.readableDatabase
        val queryString = "SELECT * FROM courses WHERE course_id = \"$courseId\""
        val cursor = db.rawQuery(queryString, null)
        // Return result or null if there's none
        return if (cursor.moveToFirst()) {
            val toReturn = cursorToCourse(cursor)
            cursor.close()
            toReturn
        } else {
            cursor.close()
            null
        }
    }

    /**
     * search for courses
     *
     * @param condition will sort for courses which match condition
     * @return List of all matching courses
     */
    fun getByInternalPrefix(condition: String): List<Course> {
        val returnList: MutableList<Course> = ArrayList()
        //Filter Course from Database
        val queryString = "SELECT * FROM courses WHERE course_id LIKE \"$condition%\" ORDER BY course_id ASC"
        val db = dbhelper.readableDatabase
        val cursor = db.rawQuery(queryString, null)
        if (cursor.moveToFirst()) {
            do {
                returnList.add(cursorToCourse(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return returnList
    }

    /**
     * Get a course by named_id
     *
     * @param namedId External named id to look for
     * @return Course with specified named id or null if none was found
     */
    fun getByNamedId(namedId: String): List<Course> {
        val cursor = dbhelper.readableDatabase.rawQuery("SELECT * FROM courses WHERE named_id LIKE '$namedId%'", null)
        val returnList: MutableList<Course> = ArrayList()
        // Return first row
        if (cursor.moveToFirst()) {
            do {
                returnList.add(cursorToCourse(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return returnList
    }

    /**
     * Get favorite Courses
     *
     * @return List of favorite courses in db
     */
    fun getFavorites(): List<Course> {
        val returnList: MutableList<Course> = ArrayList()
        val cursor = dbhelper.readableDatabase.rawQuery("SELECT * FROM courses WHERE isFavorite=1", null)

        // Add each row to returnList
        if (cursor.moveToFirst()) {
            do {
                returnList.add(cursorToCourse(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return returnList
    }

    /**
     * Get all courses that have a number id
     *
     * @return List of all courses where number_id is not null
     */
    fun getWithNumberId(): List<Course> {
        val returnList: MutableList<Course> = ArrayList()
        val cursor = dbhelper.readableDatabase.rawQuery("SELECT * FROM courses WHERE number_id IS NOT NULL", null)

        // Add each row to returnList
        if (cursor.moveToFirst()) {
            do {
                returnList.add(cursorToCourse(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return returnList
    }

    /**
     * Get a list of teacher ids from all favorite courses
     */
    fun getFavoriteTeacherIds(): List<String> {
        val cursor = dbhelper.readableDatabase.rawQuery("SELECT id_teacher FROM courses WHERE isFavorite=1", null)
        val teacherIds = ArrayList<String>()

        // Add each row to teacherIds
        if (cursor.moveToFirst()) {
            do {
                teacherIds.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return teacherIds
    }

    /**
     * @param course course which should be deleted
     * @return True, if course was deleted
     */
    fun delete(course: Course): Boolean {
        val queryString = "DELETE FROM courses WHERE course_id =" + course.courseId
        val db = dbhelper.readableDatabase
        val cursor = db.rawQuery(queryString, null)
        return if (cursor.moveToFirst()) {
            cursor.close()
            true
        } else {
            cursor.close()
            false
        }
    }

    /**
     * Get a course's internal id by its external gmb id
     */
    fun getCourseIdByGmbId(gmbId: String): String? {
        val cursor = dbhelper.readableDatabase.rawQuery("SELECT course_id FROM courses "
                + "WHERE gmb_id = \"" + gmbId + "\"", null)
        return if (cursor.moveToFirst()) {
            val id = cursor.getString(0)
            cursor.close()
            id
        } else {
            cursor.close()
            null
        }
    }

    /**
     * Returns a course id for a number id
     *
     * @param numberId SPH Number ID
     * @return Internal course id
     */
    fun getCourseIdByNumberId(numberId: String): String? {
        val cursor = dbhelper.readableDatabase.rawQuery("SELECT course_id FROM courses "
                + "WHERE number_id = \"" + numberId + "\"", null)
        return if (cursor.moveToFirst()) {
            val id = cursor.getString(0)
            cursor.close()
            id
        } else {
            cursor.close()
            null
        }
    }

    /**
     * Get a course by its external sph id
     */
    fun getBySphId(Sph_id: String): Course? {
        val queryString = "SELECT * FROM courses WHERE sph_id = \"$Sph_id\""
        val db = dbhelper.readableDatabase
        var returnCourse: Course? = null
        val cursor = db.rawQuery(queryString, null)
        if (cursor.count != 0) {
            cursor.moveToFirst()
            returnCourse = cursorToCourse(cursor)
            cursor.close()
        }
        return returnCourse
    }

    /**
     * Get a course's color by its internal id
     *
     * @param internalId Internal id of the course to look for
     * @return Its color als color int
     */
    fun getColor(internalId: String): Int {
        val cursor = dbhelper.readableDatabase.rawQuery("SELECT color FROM courses WHERE course_id=\"" + internalId.toLowerCase(Locale.ROOT) + "\"", null)
        if (!cursor.moveToFirst()) return 0
        val color = cursor.getInt(0)
        cursor.close()
        return color
    }

    /**
     * Set a course's color
     *
     * @param internalId The course's internal id
     * @param color      New color to set as color int
     */
    fun setColor(internalId: String, color: Int) {
        dbhelper.writableDatabase.execSQL("UPDATE courses SET color=\""
                + color + "\" WHERE course_id = \"" + internalId + "\"")
        CacheManager().invalidateCourseColors()
    }

    /**
     * Get a course's full name by its internal id
     *
     * @param internalId Internal id of the course to look for
     * @return Its full name or its internal id if none is saved
     */
    fun getFullname(internalId: String): String? {
        val cursor = dbhelper.readableDatabase.rawQuery("SELECT fullname FROM courses WHERE course_id=\"" + internalId.toLowerCase(Locale.ROOT) + "\"", null)
        if (!cursor.moveToFirst()) return null
        val name = cursor.getString(0)
        cursor.close()
        return name ?: internalId
    }

    /**
     * Check if a course is favorite
     *
     * @param internalId The course's internal id
     * @return null if the course is not in the db, true/false if it's favorite or not
     */
    fun isFavorite(internalId: String?): Boolean? {
        if (internalId == null) return null
        val cursor = dbhelper.readableDatabase.rawQuery("SELECT isFavorite FROM courses WHERE course_id=\"" + internalId.toLowerCase(Locale.ROOT) + "\"", null)
        if (!cursor.moveToFirst()) return null
        val favorite = cursor.getInt(0) == 1
        cursor.close()
        return favorite
    }

    /**
     * Get a gmb id of any course, for analytics
     */
    fun getGmbIdExample(): String {
        val cursor = dbhelper.readableDatabase.rawQuery(
                "SELECT gmb_id FROM courses LIMIT 1", null)
        if (!cursor.moveToFirst()) return "nocourses"
        val example = cursor.getString(0)
        cursor.close()
        return example
    }

    /**
     * Get a course from a coursor pointing at courses.*
     */
    fun cursorToCourse(cursor: Cursor): Course {
        val courseId = cursor.getString(0)
        val gmbId = cursor.getString(1)
        val sphId = cursor.getString(2)
        val namedId = cursor.getString(3)
        val numberId = cursor.getString(4)
        val fullname = cursor.getString(5)
        val idTeacher = cursor.getString(6)
        val isFavorite = cursor.getInt(7) == 1
        val isLK = cursor.getInt(8) == 1
        val color = cursor.getInt(9)
        return Course(courseId, gmbId, sphId, namedId, numberId, fullname, idTeacher, isFavorite, isLK, color)
    }
}

