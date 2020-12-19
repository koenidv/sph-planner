package de.koenidv.sph.parsing

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import androidx.core.graphics.toColorInt
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.objects.*
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.net.URL
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
                courseInternalId = IdParser().getCourseIdWithGmb(courseGmbId, teacherId, courses)
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
        var sphId: String
        var teacherId: String
        var internalId: String
        val nameColorMap = Utility().parseStringArray(R.array.course_colors)

        // Get data from each table row and save the courses
        for (entry in rawContents) {
            namedId = entry.substring(Utility().ordinalIndexOf(entry, "<td>", 1) + 4, entry.indexOf("<small>") - 1).trim()
            sphId = entry.substring(entry.indexOf("<small>") + 8, entry.indexOf("</small>") - 1)
            teacherId = entry.substring(Utility().ordinalIndexOf(entry, "<td>", 2))
            teacherId = teacherId.substring(teacherId.indexOf("(") + 1, teacherId.indexOf(")")).toLowerCase(Locale.ROOT)
            internalId = IdParser().getCourseIdWithSph(sphId, teacherId, entry.contains("LK"))

            courses.add(Course(
                    courseId = internalId,
                    sph_id = sphId,
                    named_id = namedId,
                    id_teacher = teacherId,
                    fullname = namedId.substring(0, namedId.indexOf(" ")),
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
        var courseId: String
        var courseWithNamedId: Course?
        // Get values from list
        for (entry in rawContents) {
            if (entry.contains("a=sus_view&id=")) {
                courseId = entry.substring(entry.indexOf("a=sus_view&id=") + 14)
                courseId = courseId.substring(0, courseId.indexOf("\""))
                courseName = entry.substring(entry.indexOf("</span>") + 7, entry.indexOf("</a>")).trim()

                // todo create new courses with teacher_id instead of using old ones: might not be available
                /* todo don't use brute method to recognize wrongly named courses:
                 * Info (instead of Informatik) for example will not be recognized
                 * for Q34, sometimes 13 is replaced with Q34 */

                courseWithNamedId = CoursesDb.getInstance().getByNamedId(courseName.replace("Info GK Q34", "Informatik GK 13"))
                if (courseWithNamedId != null) {
                    courseWithNamedId.number_id = courseId
                    courses.add(courseWithNamedId)
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
    fun parsePosts(courseId: String, rawResponse: String, currentPosts: List<Post> = listOf(),
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
        var postTitle: String
        var postDescription: String?
        // Task-specific
        var taskId: String
        var taskDescription: String
        var taskDone: Boolean
        // Attachment-specific
        var attachIndex: Int
        var attachId: String
        var attachName: String
        var attachUrl: URL
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
                postTitle = cells[1].select("b")[0].text()
                // Description might include html. We'll just get the text for now. Todo parse lists
                postDescription = try {
                    //Jsoup.clean(cells[1].select("i[title=\"Ausführlicher Inhalt\"]").parents()[0].toString(), Whitelist.basic())
                    cells[1].select("i[title=\"Ausführlicher Inhalt\"]").parents()[0].text()
                } catch (iobe: IndexOutOfBoundsException) {
                    // No description available
                    null
                }
                // If description containes a list, add a newline to all list markers
                if (postDescription != null && cells[1].select("i[title=\"Ausführlicher Inhalt\"]").parents()[0].toString().contains("<ul>"))
                    postDescription = postDescription.replace(" - ", "\n•")
                // Add new post to posts list
                posts.add(Post(
                        postId,
                        courseId,
                        date,
                        postTitle,
                        postDescription,
                        true // We know it's unread because it wasn't in the current posts list
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
                taskDescription = cells[1].select("span.homework").nextAll("span.markup")[0].text()
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
                    attachIndex = 1
                    do {
                        attachId = courseId + "_attach-" + internalDateFormat.format(date) + "_" + attachIndex
                    } while (attachIds.contains(attachId))

                    // Get file info
                    attachName = file.toString().substring(file.toString().indexOf("</span>") + 7,
                            file.toString().indexOf("<small>"))
                            .replace("_", " ").replace("-", " ").removeSuffix(".pdf")
                    attachSize = file.select("small").text()

                    // Parse file url
                    attachUrl = URL(
                            "https://start.schulportal.hessen.de/meinunterricht.php?a=downloadFile&id="
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
                            null,
                            attachSize
                    ))
                }
            }

            // todo Parse submissions

        }

        onParsed(posts, attachments, tasks, links)
    }
}