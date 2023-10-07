package de.koenidv.sph.database.demodata

import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.database.DatabaseHelper
import de.koenidv.sph.database.FunctionTilesDb
import de.koenidv.sph.database.TasksDb
import de.koenidv.sph.database.UsersDb
import de.koenidv.sph.objects.Course
import de.koenidv.sph.objects.FunctionTile
import de.koenidv.sph.objects.Task
import de.koenidv.sph.objects.User
import java.util.Date

class Demodata {
    private var writable: SQLiteDatabase = DatabaseHelper.getInstance().writableDatabase

    fun populateDemoData() {

        // Users
        UsersDb.save(
            listOf(
                User("max.mustermann", null, "Max", "Mustermann", "sus", false),
                User("erika.musterfrau", null, "Erika", "Musterfrau", "sus", false),
                User("koenidv", null, "Florian", "König", "sus", false),
                User("hans.hansmann", "teacher1", "Hans", "Hansmann", "lul", false),
                User("guenther.heinz", "teacher2", "Günther", "Heinz", "lul", false),
                User("karl.karlsson", "teacher3", "Karl", "Karlsson", "lul", true),
                User("frank.trollmann", "teacher4", "Frank", "Trollmann", "lul", true),
                // todo generate more users when back on ground
            )
        )

        // Courses
        CoursesDb.save(listOf(
            Course(courseId = "se21", fullname = "Mobile App Development", id_teacher = "t-4", color = Color.CYAN, isFavorite = true),
            Course(courseId = "d01", fullname = "Sachen machen", id_teacher = "t-1", color = Color.MAGENTA, isFavorite = true),
            Course(courseId = "d02", fullname = "Dingsen & Döngsen", id_teacher = "t-2", color = Color.BLUE, isFavorite = true),
        ))

        // todo other stuff

        FunctionTilesDb.getInstance().save(listOf(
            FunctionTile(name = "Mein Unterricht", location = "", type = "mycourses", icon = "test", color = Color.GRAY),
            FunctionTile(name = "Nachrichten", location = "", type = "messages", icon = "test", color = Color.GRAY),
            FunctionTile(name = "Stundenplan", location = "", type = "timetable", icon = "test", color = Color.GRAY),
            FunctionTile(name = "Vertretungsplan", location = "", type = "changes", icon = "test", color = Color.GRAY),
            FunctionTile(name = "Lerngruppen", location = "", type = "studygroups", icon = "test", color = Color.GRAY),
            FunctionTile(name = "Moodle", location = "https://koeni.dev", type = "other", icon = "sitemap", color = Color.GRAY),
            FunctionTile(name = "Edupool", location = "https://koeni.dev", type = "other", icon = "play", color = Color.GRAY),
            FunctionTile(name = "Blog", location = "https://koeni.dev", type = "other", icon = "comment", color = Color.GRAY),
            FunctionTile(name = "Fileserver", location = "https://koeni.dev", type = "other", icon = "file-alt", color = Color.GRAY),
            FunctionTile(name = "Calculator", location = "https://koeni.dev", type = "other", icon = "calculator", color = Color.GRAY),
        ))

        TasksDb.getInstance().save(listOf(
            Task(taskId = "se21_task_1", id_course = "se21", id_post = "se21_post_1", description = "Create Demo Data", date = Date.parse("2023-10-06"), isDone = false)
        ))

    }

}