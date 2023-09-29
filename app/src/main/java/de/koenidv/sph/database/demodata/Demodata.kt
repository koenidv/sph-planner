package de.koenidv.sph.database.demodata

import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.database.DatabaseHelper
import de.koenidv.sph.database.UsersDb
import de.koenidv.sph.objects.Course
import de.koenidv.sph.objects.User

class Demodata {
    private var writable: SQLiteDatabase = DatabaseHelper.getInstance().writableDatabase

    fun populateDemoData() {

        // Users
        UsersDb.save(
            listOf(
                User("u-1", null, "Max", "Mustermann", "sus", false),
                User("u-2", null, "Erika", "Musterfrau", "sus", false),
                User("t-1", "teacher1", "Hans", "Hansmann", "lul", true),
                User("t-2", "teacher2", "Günther", "Heinz", "lul", false),
                User("t-3", "teacher3", "Karl", "Karlsson", "lul", true),
                User("t-4", "teacher4", "Frank", "Trollmann", "lul", false),
            )
        )

        // Courses
        CoursesDb.save(listOf(
            Course(courseId = "se21", fullname = "Mobile App Development", id_teacher = "t-4", color = Color.CYAN),
            Course(courseId = "d01", fullname = "Sachen machen", id_teacher = "t-1", color = Color.MAGENTA),
            Course(courseId = "d02", fullname = "Dingsen & Döngsen", id_teacher = "t-2", color = Color.BLUE),
        ))

        // todo other stuff

    }

}