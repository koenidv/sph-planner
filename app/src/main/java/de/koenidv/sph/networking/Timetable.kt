package de.koenidv.sph.networking

import android.content.Context
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.appContext
import de.koenidv.sph.database.TimebarDb
import de.koenidv.sph.database.TimetableDb
import de.koenidv.sph.debugging.DebugLog
import de.koenidv.sph.debugging.Debugger
import de.koenidv.sph.objects.Lesson
import de.koenidv.sph.parsing.RawParser
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

//  Created by koenidv on 31.01.2021.
class Timetable {

    /**
     * Load and save timetable from sph
     * This will replace any current lessons in the timetable
     */
    fun fetch(callback: (success: Int) -> Unit) {

        // Log fetching timetable
        DebugLog("Timetable", "Fetching timetable")

        NetworkManager().getSiteAuthed(appContext().getString(R.string.url_timetable),
                callback = { success: Int, result: String? ->

                    if (success == NetworkManager.SUCCESS) {
                        TimetableDb.instance!!.clear()
                        TimebarDb.instance!!.clear()
                        DebugLog("Timetable", "Clear")

                        //Info
                        //Rueckgabewert result kann null enthalten => Zuweisung ist nur erfolgreich wenn der Rückgabewert nicht null ist
                        //Bei Rueckgabewert null gibt es eine null pointer exception
                        //Somit ist sichergestellt, dass save null sicher laeuft (Kotlin Praemisse)
                        //Damit zusammenhaengend: var? weist darauf hin, dass null enthalten sein kann

                        //An dieser Stelle (Ich fetche mir den Stundenplan) muss ich eine neue DB erstellen
                        //Diese speichert die Stundenzeiten und das Datum wann der Stundenplan definiert wurde
                        //Diese Aktion wird an dieser Stelle (Neues Einloggen) mitgezogen
                        //Infos liegen durch das parsen bereits vor (Triple Rueckgabewert)

                        DebugLog("Timetable", "Parser")
                        val (aLssnLst:List<Lesson>, anArryTms:Array<Array<LocalTime>>, aPlnDt:LocalDate) = RawParser().parseTimetable(result!!)
                        DebugLog("Timetable", "Timebar Save")
                        TimebarDb.instance!!.save(anArryTms, aPlnDt)
                        TimetableDb.instance!!.save(aLssnLst)
                        DebugLog("Timetable", "All OK")

                        appContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
                                .edit().putLong("updated_timetable", Date().time).apply()
                    }
                    callback(success)
                    // Log success
                    DebugLog("Timetable", "Fetched timetable: $success",
                            type = Debugger.LOG_TYPE_VAR)
                })
    }

}