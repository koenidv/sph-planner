package de.koenidv.sph.database

import android.content.ContentValues
import android.database.Cursor
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.objects.Holiday
import de.koenidv.sph.objects.Schedule
import java.util.*

//  Created by StKl JAN-2022
object SchedulesDb {

    private val dbhelper = DatabaseHelper.getInstance()

    /**
     * Clears all schedules from schedule db
     */
    fun clear() {
        val db = dbhelper.writableDatabase
        //db.rawQuery("DELETE FROM schedules", null).close();
        db.rawQuery("DROP TABLE IF EXISTS schedules", null).close()//performs also a delete
        //db.delete("schedules", null, null)
    }

    /**
     * Tries to clear all schedules from schedule db comming from studygroups
     */
    fun clearStudyGroupEntries(schedules: List<Schedule>) {
        //val db = dbhelper.writableDatabase
        // What happens if already stored schedules are changed?
        // Deleting complete schedule db is NOT an option
        // Solution: Deleting all entries with fitting cnd:
        //    + schedule.txt same as schedules.txt AND
        //    + schedule.crs same as schedules.crs AND
        //    + schedule.src == portal

        //ToDo

    }

    /**
     * Adds or updates schedules in the database
     * Will override everything for the same schedule.crs if it's not null
     *
     * @param schedules List of schedules to be added or updated
     */
    fun save(schedules: List<Schedule>) {
        //Sync with holidayDb for entries with "beweglicher Ferientag"
        val hlds = HolidaysDb().future

        for (schedule in schedules) {
            save(schedule)

            //beweglicher Ferientag?
            if(   schedule.ctgr == SphPlanner.appContext().getString(R.string.category_for_db_sync_holiday)   ) {
                //start and ende exists in hlds (Check year, month, day only)?
                val c = Calendar.getInstance()
                c.time = schedule.strt
                val d = Calendar.getInstance()
                d.time = schedule.nd
                //Sat or Sun? Set to Mon in case of start and end is not the same day (e.g. Sun, 3.10.year would than be a Monday...
                if (c.get(Calendar.DAY_OF_YEAR) != d.get(Calendar.DAY_OF_YEAR)) {
                    if(c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) c.add(Calendar.DATE, 2)
                    if(c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) c.add(Calendar.DATE, 1)
                }
                val str2 = "${c.get(Calendar.DAY_OF_MONTH)}.${c.get(Calendar.MONTH)}.${c.get(Calendar.YEAR)}"
                val yr = "${c.get(Calendar.YEAR)}"
                //Sat or Sun? Set to Fri
                if (c.get(Calendar.DAY_OF_YEAR) != d.get(Calendar.DAY_OF_YEAR)) {
                    if (d.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) d.add(Calendar.DATE, -1)
                    if (d.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) d.add(Calendar.DATE, -2)
                }
                val str4 = "${d.get(Calendar.DAY_OF_MONTH)}.${d.get(Calendar.MONTH)}.${d.get(Calendar.YEAR)}"

                var trggr = true
                for (entry in hlds) {

                    c.time = entry.start
                    d.time = entry.end
                    //Sat or Sun? Set to Mon
                    if (c.get(Calendar.DAY_OF_YEAR) != d.get(Calendar.DAY_OF_YEAR)) {
                        if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) c.add(
                            Calendar.DATE,
                            2
                        )
                        if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) c.add(Calendar.DATE, 1)
                    }
                    val str1 = "${c.get(Calendar.DAY_OF_MONTH)}.${c.get(Calendar.MONTH)}.${c.get(Calendar.YEAR)}"
                    //Sat or Sun? Set to Fri
                    if (c.get(Calendar.DAY_OF_YEAR) != d.get(Calendar.DAY_OF_YEAR)) {
                        if (d.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) d.add(
                            Calendar.DATE,
                            -1
                        )
                        if (d.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) d.add(Calendar.DATE, -2)
                    }
                    val str3 = "${d.get(Calendar.DAY_OF_MONTH)}.${d.get(Calendar.MONTH)}.${d.get(Calendar.YEAR)}"

                    if((str1==str2) && (str3==str4)) {
                        trggr = false
                    }//if
                }//for
                if(trggr) {//no holiday entry matched - sync required

                    //If we have a vacation entry but start and end was not in holidays db => Sync/ save with holiday db
                    val holi = Holiday(
                        schedule.nme,//id - e.g. herbstferien-2021-HE
                        schedule.strt,//start
                        schedule.nd,//end
                        schedule.nme,//name
                        yr//year
                    )
                    HolidaysDb().save(holi)
                }
            }//if
        }//for
    }

    /**
     * Adds or updates a schedule in the database
     * Will override everything for the same schedule.crs if it's not null
     *
     * @param schedule schedule to be added or updated
     */
    fun save(schedule: Schedule) {
        val db = dbhelper.writableDatabase
        val cv = ContentValues()

        // Only use values that are non-null => All values will never be null
        cv.put("nameS", schedule.nme)
        /*if (schedule.strt != null)*/ cv.put("startS", schedule.strt.time)//long
        /*if (schedule.nd != null)*/ cv.put("endeS", schedule.nd.time)//long
        /*if (schedule.hr != null)*/ cv.put("hourS", schedule.hr)
        /*if (schedule.drtn != null)*/ cv.put("durationS", schedule.drtn)
        /*if (schedule.txt != null)*/ cv.put("textS", schedule.txt)
        /*if (schedule.crs != null)*/ cv.put("courseS", schedule.crs)
        /*if (schedule.ctgr != null)*/ cv.put("categoryS", schedule.ctgr)
        /*if (schedule.src != null)*/ cv.put("sourceS", schedule.src)
        /*if (schedule.shr != null)*/ cv.put("shareS", schedule.shr)
        /*if (schedule.shr != null)*/ cv.put("locationS", schedule.plc)
        /*if (schedule.shr != null)*/ cv.put("respS", schedule.rsp)

        val cursor = db.rawQuery("CREATE TABLE IF NOT EXISTS schedules (nameS TEXT UNIQUE PRIMARY KEY, startS INTEGER, endeS INTEGER, hourS TEXT, durationS INTEGER, textS TEXT, courseS TEXT, categoryS TEXT, sourceS TEXT, shareS INTEGER, locationS TEXT, respS TEXT)", null)

        // Check if row exists and insert or update accordingly
            //val cursor = db.rawQuery("SELECT * FROM schedules WHERE nameS = '" + schedule.nme + "'", null)
        if (cursor.count == 0) {
            db.insert("schedules", null, cv)
        } else {
            db.update("schedules", cv, "nameS = '" + schedule.nme + "'", null)
        }
        cursor.close()
    }

    /**
    * get all schedules in database
    *
    * @return all Schedules
    */
    fun getAll(): List<Schedule> {
        val returnList: MutableList<Schedule> = ArrayList()
        val queryString = "SELECT * FROM schedules ORDER BY startS ASC"
        val db = dbhelper.readableDatabase
        val cursor = db.rawQuery(queryString, null)
        if (cursor.moveToFirst()) {
            do {
                returnList.add(cursorToSchedule(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return returnList
    }

    /**
     * get all schedules from start date
     *
     * @return all Schedules
     */
    fun getSchedWithStartDate(date: Date): List<Schedule> {
        //calculate the start date 0am
        val c = Calendar.getInstance()
        c.time = date
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        val stDate: Date = c.time
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE,59)
        c.set(Calendar.SECOND, 59)
        c.set(Calendar.MILLISECOND, 999)
        val ndDate: Date = c.time

        val returnList: MutableList<Schedule> = ArrayList()
        val queryString = "SELECT * FROM schedules WHERE startS >= ${stDate.time} AND startS <= ${ndDate.time}"
        val db = dbhelper.readableDatabase
        val cursor = db.rawQuery(queryString, null)
        if (cursor.moveToFirst()) {
            do {
                returnList.add(cursorToSchedule(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return returnList
    }

    /**
     * get all schedules for a specific course
     *
     * @return all Schedules for a specific course
     */
    fun getSchedForCourse(course: String?): List<Schedule> {
        //CoursesDb - named_id - e.g. Sport 05f1
        //SchedulesDb - courseS - e.g. Sport
        //First 2 chars, independant of small/ big
        //SQL lie example: WHERE column LIKE '%cats%'  --case-insensitive
        val returnList: MutableList<Schedule> = ArrayList()

        if(  !course.isNullOrEmpty()  && (course.length >= 2)   ) {
            val ky = course.substring(0, 2)
            val queryString = "SELECT * FROM schedules WHERE courseS LIKE '%$ky%'"
            val db = dbhelper.readableDatabase
            val cursor = db.rawQuery(queryString, null)
            if (cursor.moveToFirst()) {
                do {
                    returnList.add(cursorToSchedule(cursor))
                } while (cursor.moveToNext())
            }
            cursor.close()
        }

        return returnList
    }

    /**
    * Get a course by its internal id
    *
    * @param courseId Internal id of the course to find
    * @return Course with internal id or null
    */
    /* Can be updated for needed purpose later on
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
    */

    /**
    * search for courses
    *
    * @param condition will sort for courses which match condition
    * @return List of all matching courses
    */
    /*
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
    */

    /**
    * Get all courses that have a number id
    *
    * @return List of all courses where number_id is not null
    */
    /*
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
    */

    /**
    * @param schedule schedule which should be deleted
    * @return True, if schedule was deleted
    */
    fun delete(schedule: Schedule): Boolean {
        val queryString = "DELETE FROM schedules WHERE nameS =" + schedule.nme
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
    * Get a schedule from a coursor pointing at schedules.*
    */
    private fun cursorToSchedule(cursor: Cursor): Schedule {
        return Schedule(
                cursor.getString(0),                //name
                Date(cursor.getLong(1)),            //start
                Date(cursor.getLong(2)),            //end
                cursor.getString(3),                //hr: List<Int>
                cursor.getInt(4),                   //duration
                cursor.getString(5),                //text
                cursor.getString(6),                //course
                cursor.getString(7),                //category
                cursor.getString(8),                //source
                cursor.getInt(9) == 1,          //share
                cursor.getString(10),               //location
                cursor.getString(11)                //responsible
               )
        }
    }


