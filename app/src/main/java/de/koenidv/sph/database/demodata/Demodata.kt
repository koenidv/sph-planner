package de.koenidv.sph.database.demodata

import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import de.koenidv.sph.database.AttachmentsDb
import de.koenidv.sph.database.ChangesDb
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.database.DatabaseHelper
import de.koenidv.sph.database.FileAttachmentsDb
import de.koenidv.sph.database.FunctionTilesDb
import de.koenidv.sph.database.HolidaysDb
import de.koenidv.sph.database.LinkAttachmentsDb
import de.koenidv.sph.database.PostsDb
import de.koenidv.sph.database.TasksDb
import de.koenidv.sph.database.TimetableDb
import de.koenidv.sph.database.UsersDb
import de.koenidv.sph.networking.Holidays
import de.koenidv.sph.objects.Change
import de.koenidv.sph.objects.Course
import de.koenidv.sph.objects.FileAttachment
import de.koenidv.sph.objects.FunctionTile
import de.koenidv.sph.objects.Holiday
import de.koenidv.sph.objects.Lesson
import de.koenidv.sph.objects.LinkAttachment
import de.koenidv.sph.objects.Post
import de.koenidv.sph.objects.Task
import de.koenidv.sph.objects.TimetableEntry
import de.koenidv.sph.objects.User
import java.util.Date
import java.time.LocalDate
import java.time.ZoneId

class Demodata {
    private var writable: SQLiteDatabase = DatabaseHelper.getInstance().writableDatabase

    fun populateDemoData() {

        // Users
        UsersDb.save(
            listOf(
                User("l-max.mustermann", null, "Max", "Mustermann", "sus", false),
                User("l-erika.musterfrau", null, "Erika", "Musterfrau", "sus", false),
                User("l-koenidv", null, "Florian", "König", "sus", false),
                User("l-hans.hansmann", "teacher1", "Hans", "Hansmann", "lul", true),
                User("l-guenther.heinz", "teacher2", "Günther", "Heinz", "lul", true),
                User("l-karl.karlsson", "teacher3", "Karl", "Karlsson", "lul", true),
                User("l-frank.trollmann", "teacher4", "Frank", "Trollmann", "lul", true),
                User("l-julia.weber", "teacher5", "Julia", "Weber", "lul", false),
                User("l-peter.schmidt", "teacher6", "Peter", "Schmidt", "lul", false),
                User("l-sabine.mueller", "teacher7", "Sabine", "Müller", "lul", false),
                User("l-thomas.fischer", "teacher8", "Thomas", "Fischer", "lul", false),
                User("l-laura.schulz", "teacher9", "Laura", "Schulz", "lul", false),
                User("l-simon.hofmann", "teacher10", "Simon", "Hofmann", "lul", false),
                User("l-natalie.koch", "teacher11", "Natalie", "Koch", "lul", false),
                User("l-matthias.bauer", "teacher12", "Matthias", "Bauer", "lul", true),
                User("l-andrea.voigt", "teacher13", "Andrea", "Voigt", "lul", false),
                User("l-markus.wolf", "teacher14", "Markus", "Wolf", "lul", false),
                User("l-sandra.lehmann", "teacher15", "Sandra", "Lehmann", "lul", false),
                User("l-david.koch", null, "David", "Koch", "sus", false),
                User("l-anna.schwarz", null, "Anna", "Schwarz", "sus", false),
                User("l-leon.schulz", null, "Leon", "Schulz", "sus", false),
                User("l-lisa.meyer", null, "Lisa", "Meyer", "sus", false),
                User("l-tim.schmidt", null, "Tim", "Schmidt", "sus", false)
            )
        )

        // Courses
        CoursesDb.save(listOf(
            Course(courseId = "se21", fullname = "Mobile App Development", id_teacher = "teacher4", color = Color.parseColor("#03A9F4"), isFavorite = true),
            Course(courseId = "d01", fullname = "Sachen machen", id_teacher = "teacher1", color = Color.parseColor("#FFA726"), isFavorite = true),
            Course(courseId = "d02", fullname = "Dingsen & Döngsen", id_teacher = "teacher2", color = Color.parseColor("#4CAF50"), isFavorite = true),
            Course(courseId = "cs101", fullname =  "Introduction to Computer Science", id_teacher = "teacher1", color =  Color.parseColor("#9C27B0"), isFavorite = true),
            Course(courseId = "webdev01", fullname =  "Web Development", id_teacher = "teacher2", color =  Color.parseColor("#2196F3"), isFavorite =  true),
            Course(courseId = "dbms01", fullname =  "Database Management Systems", id_teacher = "teacher3", color =  Color.parseColor("#8BC34A"), isFavorite = true),
            Course(courseId = "ai2023", fullname =  "Artificial Intelligence", id_teacher = "teacher12", color =  Color.parseColor("#FF5722"), isFavorite =  true),
        ))

        TimetableDb.instance!!.save(listOf(
            Lesson(idCourse = "se21", day = 0, hour = 1, room = "Jungle"),
            Lesson(idCourse = "se21", day = 0, hour = 2, room = "Jungle"),
            Lesson(idCourse = "d01", day = 0, hour = 3, room = "Roomy"),
            Lesson(idCourse = "dbms01", day = 0, hour = 4, room = "R2"),
            Lesson(idCourse = "cs101", day = 0, hour = 5, room = "Matrix"),
            Lesson(idCourse = "webdev01", day = 0, hour = 6, room = "Matrix"),
            Lesson(idCourse = "ai2023", day = 1, hour = 3, room = "Matrix"),
            Lesson(idCourse = "ai2023", day = 1, hour = 4, room = "Matrix"),
            Lesson(idCourse = "ai2023", day = 1, hour = 5, room = "Matrix"),
            Lesson(idCourse = "d02", day = 1, hour = 6, room = "Matrix"),
            Lesson(idCourse = "se21", day = 2, hour = 2, room = "Jungle"),
            Lesson(idCourse = "webdev01", day = 2, hour = 4, room = "Jungle"),
            Lesson(idCourse = "dbms01", day = 2, hour = 5, room = "R2"),
            Lesson(idCourse = "dbms01", day = 2, hour = 6, room = "R2"),
            Lesson(idCourse = "d01", day = 2, hour = 8, room = "Team HQ"),
            Lesson(idCourse = "d01", day = 2, hour = 9, room = "Team HQ"),
        ))

        ChangesDb.instance!!.save(listOf(
            Change(id_course = "dbms01", date = LocalDate.of(2023, 11, 15).toDate(), lessons = listOf(5, 6), type = Change.TYPE_CANCELLED),
            Change(id_course = "d01", date = LocalDate.of(2023, 11, 15).toDate(), lessons = listOf(8, 9), type = Change.TYPE_EXAM)
        ))

        FunctionTilesDb.getInstance().save(listOf(
            FunctionTile(name = "Mein Unterricht", location = "", type = "mycourses", icon = "test", color = Color.WHITE),
            FunctionTile(name = "Nachrichten", location = "", type = "messages", icon = "test", color = Color.WHITE),
            FunctionTile(name = "Stundenplan", location = "", type = "timetable", icon = "test", color = Color.WHITE),
            FunctionTile(name = "Vertretungsplan", location = "", type = "changes", icon = "test", color = Color.WHITE),
            FunctionTile(name = "Lerngruppen", location = "", type = "studygroups", icon = "test", color = Color.WHITE), // this is a not user-facing in the app, but a required feature to map the user to their classes
            FunctionTile(name = "Moodle", location = "https://koeni.dev", type = "other", icon = "sitemap", color = Color.parseColor("#9c27b0")),
            FunctionTile(name = "Edupool", location = "https://koeni.dev", type = "other", icon = "play", color = Color.parseColor("#03a9f4")),
            FunctionTile(name = "Blog", location = "https://koeni.dev", type = "other", icon = "comment", color = Color.parseColor("#795548")),
            FunctionTile(name = "Fileserver", location = "https://koeni.dev", type = "other", icon = "file-alt", color = Color.parseColor("#4caf50")),
            FunctionTile(name = "Calculator", location = "https://koeni.dev", type = "other", icon = "calculator", color = Color.parseColor("#ffa726")),
        ))

        PostsDb.getInstance().save(listOf(
            Post(postId = "se21_post_1", id_course = "se21", date = LocalDate.of(2023, 10, 6).toDate(), title = "This is a post", description = "Here teachers can share additional material on a session, for example.", unread = true),
            Post(postId = "se21_post_2", id_course = "se21", date = LocalDate.of(2023, 10, 7).toDate(), title = "Getting Started with Android Development", description = "Learn the basics of Android app development in this post. We'll cover setting up your development environment and creating your first app.", unread = true),
            Post(postId = "se21_post_3", id_course = "se21", date = LocalDate.of(2023, 10, 8).toDate(), title = "UI Design Principles for Mobile Apps", description = "Explore the key principles of user interface (UI) design for mobile apps, including layout, navigation, and user experience.", unread = true),
            Post(postId = "se21_post_4", id_course = "se21", date = LocalDate.of(2023, 10, 9).toDate(), title = "Working with Kotlin in Mobile Development", description = "In this post, we'll dive into Kotlin, the programming language used in mobile app development, and how to use it effectively.", unread = true),
            Post(postId = "se21_post_5", id_course = "se21", date = LocalDate.of(2023, 10, 10).toDate(), title = "Handling Data in Mobile Apps", description = "Learn how to manage and store data in mobile apps, from local storage to connecting to remote databases.", unread = true),
            Post(postId = "se21_post_6", id_course = "se21", date = LocalDate.of(2023, 10, 11).toDate(), title = "Testing and Debugging Mobile Apps", description = "Discover best practices for testing and debugging your mobile apps to ensure they work flawlessly on different devices.", unread = true),
            Post(postId = "se21_post_7", id_course = "se21", date = LocalDate.of(2023, 10, 12).toDate(), title = "Publishing Your Mobile App", description = "Get insights into the app publishing process, including app store guidelines, marketing, and distribution.", unread = true),
            Post(postId = "d01_post_1", id_course = "d01", date = LocalDate.of(2023, 10, 7).toDate(), title = "Welcome to 'Sachen machen'!", description = "Let's kickstart our journey in 'Sachen machen' and explore exciting hands-on projects together.", unread = true),
            Post(postId = "d01_post_2", id_course = "d01", date = LocalDate.of(2023, 10, 8).toDate(), title = "Project Ideas and Brainstorming", description = "Share your project ideas and participate in brainstorming sessions to decide what 'Sachen' we'll 'machen'!", unread = true),
            Post(postId = "d01_post_3", id_course = "d01", date = LocalDate.of(2023, 10, 9).toDate(), title = "Materials and Resources", description = "Find out where to access materials and resources for your 'Sachen machen' projects. We've got you covered!", unread = true),
            Post(postId = "cs101_post_1", id_course = "cs101", date = LocalDate.of(2023, 10, 7).toDate(), title = "Welcome to 'Introduction to Computer Science'!", description = "Get ready to explore the world of computer science and coding. We'll start with the basics.", unread = true),
            Post(postId = "cs101_post_2", id_course = "cs101", date = LocalDate.of(2023, 10, 8).toDate(), title = "Understanding Algorithms and Data Structures", description = "Learn about the fundamental concepts of algorithms and data structures that power computing.", unread = true),
            Post(postId = "webdev01_post_1", id_course = "webdev01", date = LocalDate.of(2023, 10, 8).toDate(), title = "Welcome to 'Web Development'!", description = "Discover the exciting world of web development and the technologies we'll be working with in this course.", unread = true),
            Post(postId = "webdev01_post_2", id_course = "webdev01", date = LocalDate.of(2023, 10, 9).toDate(), title = "Creating Your First Web Page", description = "Let's dive right in and create your very first web page using HTML and CSS.", unread = true),
            Post(postId = "dbms01_post_1", id_course = "dbms01", date = LocalDate.of(2023, 10, 9).toDate(), title = "Welcome to 'Database Management Systems'!", description = "Explore the world of databases, from relational databases to NoSQL solutions, in this course.", unread = true),
            Post(postId = "ai2023_post_1", id_course = "ai2023", date = LocalDate.of(2023, 10, 10).toDate(), title = "Welcome to 'Artificial Intelligence'!", description = "Dive into the fascinating field of artificial intelligence and its real-world applications.", unread = true),
            Post(postId = "ai2023_post_2", id_course = "ai2023", date = LocalDate.of(2023, 10, 11).toDate(), title = "Machine Learning Basics", description = "Get started with machine learning and understand its core concepts in this post.", unread = true)
        ))

        TasksDb.getInstance().save(listOf(
            Task(taskId = "se21_task_1", id_course = "se21", id_post = "se21_post_1", description = "Create Demo Data", date = LocalDate.of(2023, 10, 6).toDate(), isDone = false),
            Task(taskId = "se21_task_3", id_course = "se21", id_post = "se21_post_3", description = "Design a Mobile App UI", date = LocalDate.of(2023, 10, 8).toDate(), isDone = false),
            Task(taskId = "se21_task_4", id_course = "se21", id_post = "se21_post_4", description = "Implement Kotlin Features", date = LocalDate.of(2023, 10, 9).toDate(), isDone = false),
            Task(taskId = "se21_task_5", id_course = "se21", id_post = "se21_post_5", description = "Set Up Local Data Storage", date = LocalDate.of(2023, 10, 10).toDate(), isDone = false),
            Task(taskId = "cs101_task_1", id_course = "cs101", id_post = "cs101_post_1", description = "Install Python and IDE", date = LocalDate.of(2023, 10, 7).toDate(), isDone = false),
            Task(taskId = "cs101_task_2", id_course = "cs101", id_post = "cs101_post_2", description = "Implement Sorting Algorithms", date = LocalDate.of(2023, 10, 8).toDate(), isDone = false),
            Task(taskId = "webdev01_task_1", id_course = "webdev01", id_post = "webdev01_post_1", description = "Set Up Development Environment", date = LocalDate.of(2023, 10, 8).toDate(), isDone = false),
            Task(taskId = "webdev01_task_2", id_course = "webdev01", id_post = "webdev01_post_2", description = "Create a Simple Web Page", date = LocalDate.of(2023, 10, 9).toDate(), isDone = false),
            Task(taskId = "ai2023_task_1", id_course = "ai2023", id_post = "ai2023_post_1", description = "Explore AI Applications", date = LocalDate.of(2023, 10, 10).toDate(), isDone = false),
            Task(taskId = "ai2023_task_2", id_course = "ai2023", id_post = "ai2023_post_2", description = "Implement Linear Regression", date = LocalDate.of(2023, 10, 11).toDate(), isDone = false),
            Task(taskId = "d01_task_1", id_course = "d01", id_post = "d01_post_1", description = "Choose a Project Idea", date = LocalDate.of(2023, 10, 7).toDate(), isDone = false),
            Task(taskId = "d01_task_2", id_course = "d01", id_post = "d01_post_1", description = "Gather Necessary Materials", date = LocalDate.of(2023, 10, 7).toDate(), isDone = false),
            Task(taskId = "d01_task_3", id_course = "d01", id_post = "d01_post_2", description = "Participate in Brainstorming", date = LocalDate.of(2023, 10, 8).toDate(), isDone = false),
            Task(taskId = "dbms01_task_1", id_course = "dbms01", id_post = "dbms01_post_1", description = "Explore Relational Databases", date = LocalDate.of(2023, 10, 9).toDate(), isDone = false)
        ))

        FileAttachmentsDb.getInstance().save(listOf(
            FileAttachment("ai2023_post_2_attach_1", "ai2023", "ai2023_post_2", "Shanghai at Night", LocalDate.of(2023, 10, 11).toDate(), "https://images.unsplash.com/photo-1696142990758-581061f2801d", "4 MB", "jpeg", true, null),
            FileAttachment("webdev01_post_2_attach_1", "webdev01", "webdev01_post_2", "SE_22 Self-Assessment", LocalDate.of(2023, 10, 8).toDate(), "https://docs.google.com/document/d/1qhwxb_HSE4oBKiGDgJu2XCnzSICIjYNvwUSaF8qA8Ug/export?format=pdf", "44 KB", "pdf", true, null),
        ))

        LinkAttachmentsDb.getInstance().save(listOf(
            LinkAttachment("se21_post_1_attach_1", "se21", "se21_post_1", "Florian's Website ;)", LocalDate.of(2023, 10, 6).toDate(), "https://koeni.dev", true, null)
        ))

        HolidaysDb().save(Holiday("fall23", LocalDate.of(2023, 10, 7).toDate(), LocalDate.of(2023, 10, 11).toDate(), "herbstferien", "2023"))
        HolidaysDb().save(Holiday("christmas23-1", LocalDate.of(2023, 12, 25).toDate(), LocalDate.of(2023, 12, 25).toDate(), "1. Weihnachtstag", "2023"))
        HolidaysDb().save(Holiday("christmas23-2", LocalDate.of(2023, 12, 26).toDate(), LocalDate.of(2023, 12, 26).toDate(), "2. Weihnachtstag", "2023"))
        HolidaysDb().save(Holiday("winter23", LocalDate.of(2023, 12, 23).toDate(), LocalDate.of(2024, 1, 6).toDate(), "weihnachtsferien", "2023"))
        HolidaysDb().save(Holiday("spring24", LocalDate.of(2024, 3, 25).toDate(), LocalDate.of(2024, 4, 6).toDate(), "osterferien", "2024"))
        HolidaysDb().save(Holiday("summer24", LocalDate.of(2024, 7, 1).toDate(), LocalDate.of(2024, 8, 31).toDate(), "sommerferien", "2024"))

    }

    private fun LocalDate.toDate(): Date {
        return Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())
    }

}