package de.koenidv.sph.parsing

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.widget.Toast
import androidx.core.graphics.toColorInt
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.objects.*
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

//  Created by koenidv on 08.12.2020.
class RawParser {

    // todo documentation
    @Suppress("LocalVariableName")
    @SuppressLint("DefaultLocale")
    fun parseChanges(rawResponse: String): List<Change> {
        val changes = mutableListOf<Change>()

        // todo check if response is valid
        // we could check the week type (a/b) here: <span class="badge woche">
        // todo get last refresh


        // Remove stuff that we don't need
        var rawContent = rawResponse.substring(rawResponse.indexOf("<div class=\"panel panel-primary\""))
        rawContent = rawContent.substring(0, rawContent.indexOf("<link"))
        // Remove newlines, we don't need them (But also isn't necessary atm and makes debugging horrible)
        // rawContent = rawContent.replace("\n", "").replace("\t", "")

        // For remembering where we left off :)
        var rawToday: String
        var rawChange: String
        var rawCell: String
        var dayInContent = 0
        var cellInRow: Int

        // For getting the date
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)

        // We'll need those below to construct our Change object
        var date: Date
        var internalId: String
        var id_course_external: String? = ""
        var id_course_external_before: String?
        var lessons: List<Int>
        var type: String
        var className: String?
        var className_before: String?
        var id_teacher: String? = null
        var id_subsTeacher: String?
        var room: String?
        var description: String?

        // Extract every changes table, i.e. every available day
        while (rawContent.contains("<table class=\"table")) {
            // Get today's table only
            rawToday = rawContent.substring(rawContent.indexOf("<table class=\"table"), rawContent.indexOf("<div class=\"fixed-table-footer"))
            // Remove the extracted table from rawContent
            rawContent = rawContent.substring(rawContent.indexOf("<div class=\"fixed-table-footer") + 30)
            // Extract only the table's body
            // We don't need <thead> as long as the table order remains the same
            // This might vary for different schools
            rawToday = rawToday.substring(rawToday.indexOf("<tbody>") + 7, rawToday.indexOf("</tbody>"))

            // Parse date
            val dateIndex = Utility().ordinalIndexOf(rawContent, "Vertretungen am ", dayInContent) + 16
            date = dateFormat.parse(rawContent.substring(dateIndex, dateIndex + 10))!!

            // We are left with a table, each row contains these columns:
            // title (not needed), lessons (11 11 - 12), classname (Q34), old classname (Q34, mostly empty),
            // substitute teacher id (Bar, mostly empty), teacher id (Bar), type (EVA), course (M-GK-3),
            // old course (M-GK-3, mostly empty), room (M119), description

            // Get the change's data for every table row
            while (rawToday.contains("<tr")) {
                // Extract change from today's changes
                rawChange = rawToday.substring(rawToday.indexOf("<tr>") + 4, rawToday.indexOf("</tr>"))
                rawToday = rawToday.substring(rawToday.indexOf("</tr>") + 5)

                // Process every cell
                cellInRow = 0
                while (rawChange.contains("<td")) {
                    // Extract cell from table row
                    rawCell = rawChange.substring(rawChange.indexOf("<td"))
                    rawCell = rawCell.substring(rawCell.indexOf(">") + 1, rawCell.indexOf("</td>"))

                    when (cellInRow) {
                        0 -> {
                        } // Ignore first cell
                        1 -> { // Affected lessons
                            if (rawCell.contains(" - ")) {
                                // Get start and end lesson and put everything in between in a list
                                val fromLesson = Integer.getInteger(rawCell.substring(0, rawCell.indexOf(" ")))!!
                                val toLesson = Integer.getInteger(rawCell.substring(0, rawCell.indexOf(" ")))!!
                                lessons = (fromLesson..toLesson).toList()
                            } else {
                                lessons = listOf(Integer.getInteger(rawCell))
                            }
                        }
                        2 -> className = rawCell
                        3 -> className_before = rawCell
                        4 -> id_subsTeacher = rawCell.toLowerCase()
                        5 -> id_teacher = rawCell.toLowerCase()
                        6 -> type = rawCell.toUpperCase()
                        7 -> id_course_external = rawCell.toUpperCase()
                        8 -> id_course_external_before = rawCell.toUpperCase()
                        9 -> room = rawCell.toUpperCase()
                        10 -> description = rawCell
                    }
                    // Next cell
                    cellInRow++
                }


                /*

                TODO Do not use change ids with course and teacher, change might not have one
                 Use rolling change ids instead, guess course ids (from timetable) and search for favorite course

                if ()

                // Create Change with the extracted values and add it to the list
                if (id_teacher != null) {
                    internalId = IdParser().getCourseId(id_course_external, TYPE_GMB, id_teacher)
                } else {
                    // todo find course by external id. Must have seen it before, maybe full timetable
                    internalId = ""
                }

                    changes.add(Change(
                            changeId = IdParser().getChangeId(internalId, date, changes),

                    ))

                Log.d(SphPlanner.TAG, rawChange)*/
            }


            // This will crash if no course is specified
            // Might happen before holidays


            Log.d(SphPlanner.TAG, rawToday)
            // Next day
            dayInContent++
        }

        return changes
    }

    /**
     * Parse courses from raw timetable webpage
     * @param rawResponse Html repsonse from SPH
     * @return List of all found courses
     */
    fun parseCoursesFromTimetable(rawResponse: String): List<Course> {
        val courses = mutableListOf<Course>()

        // Split response into every entry
        val lessons = rawResponse.split("<div class=\"stunde").toMutableList()
        lessons[lessons.size - 1] = lessons.last().substring(0, lessons.last().indexOf("</div>"))
        lessons.removeFirst() // Remove first element, it's not a lesson

        var courseGmbId: String
        var teacherId: String
        var courseInternalId: String
        // Add course from each lesson if not yet added
        for (lesson in lessons) {
            // Get gmb id from raw data
            courseGmbId = lesson
                    .substring(lesson.indexOf("<b>") + 3, lesson.indexOf("</b>"))
                    .replace("\n", "")
                    .replace(" ", "")
                    .toLowerCase(Locale.ROOT)
                    .take(14)

            // Check if the course list already contains this id
            if (courses.none { course -> course.gmb_id == courseGmbId }) {
                // Get teacher id between <small> tags
                teacherId = lesson
                        .substring(lesson.indexOf("<small>") + 7, lesson.indexOf("</small>"))
                        .replace("\n", "")
                        .replace(" ", "")
                        .replace("-", "")
                        .toLowerCase(Locale.ROOT)
                        .take(3)
                // Create internal course id
                courseInternalId = IdParser().getCourseIdWithGmb(courseGmbId, teacherId, true, courses)
                // Add created course to list
                courses.add(Course(
                        courseId = courseInternalId,
                        gmb_id = courseGmbId,
                        id_teacher = teacherId,
                        isLK = courseGmbId.contains("lk")
                ))
            }
        }

        return courses
    }

    /**
     * Parse courses from raw study groups webpage
     * @param rawResponse Html repsonse from SPH
     * @return List of all found courses
     */
    fun parseCoursesFromStudygroups(rawResponse: String): List<Course> {
        val courses = mutableListOf<Course>()
        // Remove stuff we don't need
        // There are multiple tables in this page, we'll just take the first one
        val rawContent = rawResponse.substring(rawResponse.indexOf("<tbody>") + 7, rawResponse.indexOf("</tbody>"))

        /*
         * It seems as if the sph id index is in the same order as the timetable
         * i.e. Q3Gvac03 - GYM is the 3rd History GK in the timetable
         * This means that the internal id's should be in the same order as they're taken from it
         * Let's just hope the best
         */

        val rawContents = rawContent.split("<tr").toMutableList()
        rawContents.removeFirst()

        var namedId: String
        var uniformNamedId: String
        var sphId: String
        var teacherId: String
        var internalId: String
        val nameColorMap = Utility().parseStringArray(R.array.course_colors)

        // Get data from each table row and save the courses
        for (entry in rawContents) {
            namedId = entry.substring(Utility().ordinalIndexOf(entry, "<td>", 1) + 4, entry.indexOf("<small>") - 1).trim()
            uniformNamedId = CourseParser().parseNamedId(namedId)
            sphId = entry.substring(entry.indexOf("<small>") + 8, entry.indexOf("</small>") - 1)
            teacherId = entry.substring(Utility().ordinalIndexOf(entry, "<td>", 2))
            teacherId = teacherId.substring(teacherId.indexOf("(") + 1, teacherId.indexOf(")")).toLowerCase(Locale.ROOT)
            internalId = IdParser().getCourseIdWithSph(sphId, teacherId, entry.contains("LK"))

            courses.add(Course(
                    courseId = internalId,
                    sph_id = sphId,
                    named_id = uniformNamedId,
                    id_teacher = teacherId,
                    fullname = CourseParser().getFullnameFromInternald(internalId),
                    isFavorite = true,
                    isLK = entry.contains("LK"),
                    color = (nameColorMap[namedId.substring(0, namedId.indexOf(" "))]
                            ?: nameColorMap["default"])!!.toColorInt()
            ))
        }

        return courses
    }

    /**
     * Parse courses from raw post overview webpage
     * @param rawResponse Html repsonse from SPH
     * @return List of all found courses
     */
    fun parseCoursesFromPostsoverview(rawResponse: String): List<Course> {
        val courses = mutableListOf<Course>()
        // Remove stuff we don't need, get second menu <ul>
        @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER") var rawContent = rawResponse.substring(rawResponse.indexOf("<ul role=\"menu\"") + 15)
        rawContent = rawContent.substring(rawContent.indexOf("<ul role=\"menu\""))
        rawContent = rawContent.substring(0, rawContent.indexOf("</ul>"))

        val rawContents = rawContent.split("<li >").toMutableList()
        rawContents.removeFirst() // Trash
        rawContents.removeFirst() // Overview link

        var courseName: String
        var uniformCourseName: String
        var numberId: String
        var similiarCourses: List<Course>
        // Get values from list
        for (entry in rawContents) {
            if (entry.contains("a=sus_view&id=")) {
                numberId = entry.substring(entry.indexOf("a=sus_view&id=") + 14)
                numberId = numberId.substring(0, numberId.indexOf("\""))
                courseName = entry.substring(entry.indexOf("</span>") + 7, entry.indexOf("</a>")).trim()

                // todo create new courses with teacher_id instead of using old ones: might not be available
                /* todo don't use brute method to recognize wrongly named courses:
                 * Info (instead of Informatik) for example will not be recognized
                 * for Q34, sometimes 13 is replaced with Q34 */

                // Make named id from post overview uniform
                uniformCourseName = CourseParser().parseNamedId(courseName)

                // Get courses that might be the same as this one
                similiarCourses = CoursesDb.getInstance().getByNamedId(uniformCourseName).toMutableList()
                if (courseName.contains("""\(.*\)""".toRegex())) { // if contains text in brackets
                    val courseToAdd = CoursesDb.getInstance().getBySphId(courseName.substring(courseName.indexOf("(") + 1, courseName.lastIndexOf(")")).replace("-GYM", " - GYM"))
                    if (courseToAdd != null) similiarCourses.add(courseToAdd)
                }

                // Make sure no course is in the list twice
                // This might happen because we add both by NamedId and SphId
                similiarCourses = similiarCourses.distinct()

                when {
                    similiarCourses.size == 1 -> {
                        // Only one similiar course, this should be it.
                        similiarCourses[0].number_id = numberId
                        courses.add(similiarCourses[0])
                    }
                    similiarCourses.isEmpty() -> {
                        // todo create new course with this namedid & numberid
                        Toast.makeText(applicationContext(), "No valid course", Toast.LENGTH_SHORT).show()
                    }
                    similiarCourses.size > 1 -> {
                        // wetodo handle multiple similiar courses
                        Toast.makeText(applicationContext(), "Too many courses", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        return courses
    }

    /**
     * Parse a list of school ids from raw select school webpage
     * @param rawResponse Html repsonse from SPH
     * @return List of all found schools and ids
     */
    fun parseSchoolIds(rawResponse: String): List<Pair<String, String>> {
        val ids = mutableListOf<Pair<String, String>>()

        // Split String into list items and remove stuff we don't need
        val rawContents = rawResponse.replace("\n", "").split("<a class=\"list-group-item\"").toMutableList()
        rawContents.removeFirst()

        var id: String
        for (content in rawContents) {
            id = content.substring(content.indexOf("data-id=") + 9, content.indexOf("data-id=") + 13)
            if (!id.startsWith("200") && !id.contains("\"")) { // 5-digit ids starting with 200 for companies or 3 only 3 digits
                ids.add(Pair(
                        content.substring(
                                content.indexOf("\">") + 2, content.indexOf("</a>"))
                                .replace(" <small>", ", ")
                                .replace("</small>", ""),
                        id))
            }
        }

        ids.sortBy { it.first }
        return ids
    }

    /**
     * Parse a list of supported tiles from sph start page
     * @param rawResponse Html repsonse from SPH
     * @return List of all found tiles with temporary urls as location
     */
    fun parseFeatureList(rawResponse: String): List<Tile> {
        val tiles = mutableListOf<Tile>()
        val ids = mutableListOf<String>()

        // Split String into list items and remove stuff we don't need
        val rawContents = rawResponse.replace("\n", "")
                .substring(rawResponse.indexOf("id=\"accordion\">") + 15, rawResponse.indexOf("id=\"menuelist\"") - 5)
                .split("<li class").toMutableList()
        rawContents.removeFirst()

        // Name-type map
        val nametypeMap = Utility().parseStringArray(R.array.tiles_name_type)

        var id: String
        var name: String
        var locationTemp: String
        var type: String
        var icon: String
        var color: Int
        for (content in rawContents) {
            id = content.substring(content.indexOf("id=\"") + 4)
            id = id.substring(0, id.indexOf("\""))
            // Only if tile with id hasn't been added yet
            if (!ids.contains(id)) {

                name = content.substring(content.indexOf("<h3><span class=\"glyphicon \"></span>") + 36)
                name = name.substring(0, name.indexOf("<")).trim()
                locationTemp = content.substring(content.indexOf("<div class=\"textheight\"> <a href=\"") + 34)
                locationTemp = "https://start.schulportal.hessen.de/" + locationTemp.substring(0, locationTemp.indexOf("\""))

                icon = Regex(""".*((fa|glyphicon)-\S*)\s+logo"""").find(content)!!.groupValues[1]
                /*
                SPH spits out rgb() on desktop but hex values on mobile. interesting
                colorTemp = content.substring(content.indexOf("background-color: rgb(") + 22)
                colorSplits = colorTemp.substring(0, colorTemp.indexOf(")")).split(", ")
                color = Color.rgb(colorSplits[0].toInt(), colorSplits[1].toInt(), colorSplits[2].toInt())
                */
                color = Color.parseColor(content.substring(content.indexOf("background-color: #") + 18, content.indexOf("background-color: #") + 25))

                type = nametypeMap[name] ?: "other"

                tiles.add(Tile(name, locationTemp, type, icon, color))

                // Remember tile id
                ids.add(id)
            }

        }

        return tiles
    }

    /**
     * Parse Posts, PostAttachments, PostTasks and PostLinks from raw course posts site
     * @param courseId Course which the posts belong to
     * @param rawResponse Raw html response from sph
     * @param currentPosts Current list of posts (all or this course) to check if a post is unread
     */
    fun parsePosts(courseId: String,
                   rawResponse: String,
                   currentPosts: List<Post> = listOf(),
                   markAsRead: Boolean = false,
                   onParsed: (posts: List<Post>,
                              attachments: List<PostAttachment>,
                              tasks: List<PostTask>,
                              links: List<PostLink>) -> Unit) {
        val posts = mutableListOf<Post>()
        val attachments = mutableListOf<PostAttachment>()
        val tasks = mutableListOf<PostTask>()
        val links = mutableListOf<PostLink>()
        val attachIds = mutableListOf<String>()
        // Extract table using jsoup
        val doc = Jsoup.parse(rawResponse)
        val rawPosts = doc.select("div#old tbody").select("tr")

        val currentPostIds = currentPosts.map { it.postId }

        // Extract data from table rows
        var sphPostId: String
        var postId: String
        var date: Date
        var cells: Elements
        // Post-specific
        var postTitle: String?
        var postDescription: String?
        // Task-specific
        var taskId: String
        var taskDescription: String
        var taskDone: Boolean
        // Attachment-specific
        var attachIndex: Int
        var attachId: String
        var attachName: String
        var attachType: String
        var attachUrl: String
        var attachSize: String
        // For getting the date
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
        val internalDateFormat = SimpleDateFormat("2020-MM-dd", Locale.ROOT)

        // Parse data for every row
        for (row in rawPosts) {
            cells = row.select("td")
            // Cells: 0: Some metadata, 1: Content, 2: Attendance (encrypted)
            sphPostId = row.select("tr[data-entry]").attr("data-entry")
            date = dateFormat.parse(cells[0].childNodes()[1].toString())!!
            postId = courseId + "_post-" + internalDateFormat.format(date) + "_" + sphPostId

            /*
             * Posts
             */

            // Check if current post was already in the db and if so, use it again
            // This is mainly to set the unread attribute
            if (currentPostIds.contains(postId)) {
                posts.add(currentPosts[currentPostIds.indexOf(postId)])
            } else {
                // Get information from html
                postTitle = cells[1].select("b")[0].wholeText().trim()
                // Correct weird stuff that sph does
                postTitle = postTitle.replace("""&amp;amp;quot;""", "\"")
                if (postTitle == "kein Thema") postTitle = null
                // Description might include html. We'll just get the text for now. Todo parse lists
                postDescription = try {
                    //Jsoup.clean(cells[1].select("i[title=\"Ausführlicher Inhalt\"]").parents()[0].toString(), Whitelist.basic())
                    cells[1].select("i[title=\"Ausführlicher Inhalt\"]").parents()[0].wholeText().trim()
                } catch (iobe: IndexOutOfBoundsException) {
                    // No description available
                    null
                }
                // Correct weird stuff that sph does
                postDescription = postDescription?.replace("&amp;amp;quot;", "\"")
                // Add new post to posts list
                posts.add(Post(
                        postId,
                        courseId,
                        date,
                        postTitle,
                        postDescription,
                        !markAsRead // We know it's unread because it wasn't in the current posts list
                ))
                // todo check if post includes link
            }

            /*
             * Tasks
             */

            // If post contains a task
            if (cells[1].toString().contains("<span class=\"homework\">")) {
                // There can only be one task per post, it seems
                taskId = courseId + "_task-" + internalDateFormat.format(date) + "_1"
                taskDone = cells[1].toString().contains("<span class=\"done \"")
                taskDescription = cells[1].select("span.homework").nextAll("span.markup")[0].wholeText()
                // Add new task to tasks list
                tasks.add(PostTask(
                        taskId,
                        courseId,
                        postId,
                        taskDescription,
                        date,
                        taskDone
                ))
                // todo check if homework includes link
            }

            /*
             * Attachments
             */

            // Todo dont replace old files, localpath will be lost

            // If post contains attachments
            if (cells[1].select("div.files").size != 0) {
                // For every file attachment
                for (file in cells[1].select("div.files div.file")) {
                    // Get a not before used attachment id
                    attachIndex = 0
                    do {
                        attachIndex++ // first index 1
                        attachId = courseId + "_attach-" + internalDateFormat.format(date) + "_" + attachIndex
                    } while (attachIds.contains(attachId))
                    attachIds.add(attachId)

                    // Get file info
                    attachName = file.toString().substring(file.toString().indexOf("</span>") + 7,
                            file.toString().indexOf("<small>"))
                            .replace("_", " ").replace("-", " ").trim()
                    attachSize = file.select("small").text()
                    attachSize = attachSize.substring(1, attachSize.length - 1) // Remove brackets
                    attachType = attachName.substring(attachName.lastIndexOf(".") + 1)
                    attachName = attachName.substring(0, attachName.lastIndexOf("."))

                    // Parse file url
                    attachUrl = ("https://start.schulportal.hessen.de/meinunterricht.php?a=downloadFile&id="
                            + doc.select("h1[data-book]")[0].attr("data-book") // NumberId of this course
                            + "&e=" + sphPostId
                            + "&f=" + URLEncoder.encode(file.attr("data-file"), "utf-8"))

                    // Add new Attachment to lst
                    attachments.add(PostAttachment(
                            attachId,
                            courseId,
                            postId,
                            attachName,
                            date,
                            attachUrl,
                            attachSize,
                            attachType,
                            // Set post as not pinned
                            // This will not overwrite if the attachment was pinned before
                            false,
                            null
                    ))
                }
            }

            // todo Parse submissions
            // todo Don't replace existing attachments

        }

        onParsed(posts, attachments, tasks, links)
    }

    /**
     * Parse all lessons from timetable
     * @param rawResponse Raw timetable site from sph
     * @return List of courses
     */
    fun parseTimetable(rawResponse: String): List<Lesson> {
        val returnList = mutableListOf<Lesson>()
        // Extract table using jsoup
        val doc = Jsoup.parse(rawResponse)
        val table = doc.select("div.plan tbody")[0]
        // Table is split up into rows of hours (one or multiple)
        // There is one row for each hour, even if there aren't any lesson
        // However, there isn't a cell if values overlap from the previous hour
        // And there also insn't any attribute to let us know which day the lesson is on, if a cell has been skipped
        // The rowspan attribute (number of hours) will be the same for all entries within a cell
        // Remember the number of rows to skip for each day..
        val hoursSkipped = mutableListOf(0, 0, 0, 0, 0)
        var rowspanRaw: String
        var rowspan: Int
        var rowspanInner: Int
        var currentHour = 1
        var currentDay: Int
        var currentDayRaw: Int
        var cells: Elements
        var lessons: Elements
        var gmbId: String
        var courseId: String
        var teacherId: String
        var room: String

        // For each hour
        for (row in table.select("tr")) {
            cells = row.select("td")
            // First column is always a description, including time
            // todo get lesson times
            currentDay = -1
            currentDayRaw = 1
            // For every day, also skip non-existent columns
            while (currentDay <= 4) {
                // Skip first column
                if (currentDay != -1) {
                    // Only if this hour was not overridden by last hour
                    if (hoursSkipped[currentDay] == 0) {
                        // Remember if this overrides the next hour
                        rowspanRaw = cells[currentDayRaw].attr("rowspan") ?: "1"
                        if (rowspanRaw == "") rowspanRaw = "1"
                        rowspan = rowspanRaw.toInt() - 1
                        hoursSkipped[currentDay] = rowspan

                        // Find lessons within this cell
                        lessons = cells[currentDayRaw].select("div.stunde")
                        currentDayRaw++

                        // Add each lesson
                        for (lesson in lessons) {
                            rowspanInner = 0

                            // Extract data
                            gmbId = lesson.select("b").text().trim()
                            teacherId = lesson.select("small").text().trim()
                            room = lesson.toString().substring(
                                    lesson.toString().indexOf("</b>") + 4,
                                    lesson.toString().indexOf("<small")
                            ).replace("<br>", "").trim()
                            courseId = IdParser().getCourseIdWithGmb(gmbId, teacherId, false)

                            // Add each lesson to the return list
                            // Individually, if rowspan is larger than 1
                            while (rowspanInner <= rowspan) {
                                // Check for duplicate courses with different rooms
                                if (returnList.any {
                                            it.idCourse == courseId
                                                    && it.day == currentDay
                                                    && it.hour == currentHour + rowspanInner
                                        }) {
                                    // Course was already added, but with a different room
                                    // Update the course with this room added
                                    returnList.find {
                                        it.idCourse == courseId
                                                && it.day == currentDay
                                                && it.hour == currentHour + rowspanInner
                                    }!!.room += ", $room"
                                } else {
                                    // Just add the lesson to the list
                                    returnList.add(Lesson(
                                            courseId,
                                            currentDay,
                                            currentHour + rowspanInner,
                                            room
                                    ))
                                }
                                rowspanInner++
                            }
                        }
                    } else hoursSkipped[currentDay]--
                }
                currentDay++
            }
            currentHour++
        }

        return returnList
    }
}