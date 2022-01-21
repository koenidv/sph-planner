package de.koenidv.sph.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import de.koenidv.sph.debugging.DebugLog
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

//  Created by StKL on 06.12.2021.
class TimebarDb private constructor() {
    private val dbhelper = DatabaseHelper.getInstance()

    /**
     * Saves timebar information to the db
     *
     * @param tmbr timebar information to save
     * @param lssnvld Valid date of timetable to save
     */
    fun save(tmbr: Array<Array<LocalTime>>, lssnvld: LocalDate) {
        val db: SQLiteDatabase = dbhelper.writableDatabase
        val cv = ContentValues()

        //Clear already done?
        //Equal - In case I'm here, I have new data - so cler again
        //Delete later if clear in timetable.kt under fetch is the only valid call path
        clear()

        //first valid date
        cv.put("someinfo", lssnvld.toString())
        db.insert("timebar", null, cv)
        //second the times
        for (array in tmbr) {
            for (value in array) {
                cv.put("someinfo", value.toString())
                db.insert("timebar", null, cv)
            }
        }
        DebugLog("Timebar", "Timebar Save OK - Array ${tmbr.size}")
        //Create db - Should not throw an exception as there are no unique constraints
    }

    /**
     * Get timebar information acc. class and save function info
     * @param
     * @return valid date of timetable and Array of timebar information
     */
    fun get(): Pair<   LocalDate, Array<Array<LocalTime>>   > {
        val db: SQLiteDatabase = dbhelper.readableDatabase
        val queryString = "SELECT * FROM timebar"
        val cursor = db.rawQuery(queryString, null)

        var returnValidDate = LocalDate.now()
        var returnTmbr = arrayOf(
            arrayOf(LocalTime.of(0, 0), LocalTime.of(0, 0)),
            arrayOf(LocalTime.of(0, 0), LocalTime.of(0, 0)),
            arrayOf(LocalTime.of(0, 0), LocalTime.of(0, 0)),
            arrayOf(LocalTime.of(0, 0), LocalTime.of(0, 0)),
            arrayOf(LocalTime.of(0, 0), LocalTime.of(0, 0)),
            arrayOf(LocalTime.of(0, 0), LocalTime.of(0, 0)),
            arrayOf(LocalTime.of(0, 0), LocalTime.of(0, 0)),
            arrayOf(LocalTime.of(0, 0), LocalTime.of(0, 0)),
            arrayOf(LocalTime.of(0, 0), LocalTime.of(0, 0)),
            arrayOf(LocalTime.of(0, 0), LocalTime.of(0, 0)),
            arrayOf(LocalTime.of(0, 0), LocalTime.of(0, 0)),
            arrayOf(LocalTime.of(0, 0), LocalTime.of(0, 0))
        )

        //first valid date
        if (cursor.moveToFirst()) {
            returnValidDate = LocalDate.parse(cursor.getString(0), DateTimeFormatter.ISO_LOCAL_DATE)
            //Text '2021-12-07' could not be parsed at index 2
            //second the times
            var i = 0
            var j = 0
            for (array in returnTmbr) {
                for (value in array) {
                    if(cursor.moveToNext()) {
                        returnTmbr[i][j] = LocalTime.parse(cursor.getString(0), DateTimeFormatter.ISO_LOCAL_TIME)
                    }
                    j++
                }
                i++
                j=0
            }
            //*/
        }
        cursor.close()

        DebugLog("Timebar", "Timebar Get OK - Array ${returnTmbr.size}")

        return Pair(returnValidDate, returnTmbr)
    }

    /**
     * Clears timebar information from timebar db
     */
    @Suppress("unused")
    fun clear() {
        val db = dbhelper.writableDatabase
        db.delete("timebar", null, null)
    }

    //companion objects are singleton objects (einelementige Menge, blanke Karte)
    //whose properties and functions are tied (gebunden)
    //to a class but not to the instance of that class
    // => Die Eigenschaft soll bei allen Instanzen der Klasse gleich bleiben
    // => Wird irgendwo daran etwas veraendert, veraendert sich die Eigenschaft ueberall
    companion object {
        var instance: TimebarDb? = null
            get() {
                if (field == null) {
                    field = TimebarDb()
                }
                return field
            }
            private set
    }
}