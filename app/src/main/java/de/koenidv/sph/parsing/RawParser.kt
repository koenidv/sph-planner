package de.koenidv.sph.parsing

import android.graphics.Color
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.os.bundleOf
import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.TAG
import de.koenidv.sph.SphPlanner.Companion.appContext
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.debugging.DebugLog
import de.koenidv.sph.debugging.Debugger
import de.koenidv.sph.objects.*
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

//  Created by koenidv on 08.12.2020.
//  Adapted by StKl JAN-2022
class RawParser {

    /**
     * Parse changes from raw changes sph site
     */
    @Suppress("LocalVariableName")
    fun parseChanges(rawResponse: String): List<Change> {
        val changes = mutableListOf<Change>()

        // we could check the week type (a/b) here: <span class="badge woche">

        // Log to Crashlytics if failed
        try {
            // Get the document
            val doc = Jsoup.parse(rawResponse)

            // For getting the date
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)

            // We'll need those below to construct our Change object
            var dateString: String?
            var date: Date
            var internalId: String?
            var id_course_external: String? = null
            var id_course_external_before: String? = null
            var lessons: List<Int> = listOf()
            var type: Int = Change.TYPE_OTHER
            var className: String? = null
            var className_before: String? = null
            var id_teacher: String? = null
            var id_subsTeacher: String? = null
            var room: String? = null
            var description: String? = null

            // Extract every changes table, i.e. every available day
            for (panel in doc.select("div.panel:not(#menue_tag) div.panel-body")) {
                // Parse date
                // Only get "11.01.2021" from "Vertretungen am 11.01.2021" -> Chars 16-26
                dateString = Regex("""\d{1,2}\.\d{1,2}\.\d{2,4}""")
                        .find(panel.selectFirst("h3").text())?.value
                date = if (dateString != null)
                    dateFormat.parse(dateString)!!
                else Date() // Use current date as fallback if no date was found

                // We are left with a table, each row contains these columns:
                // title (not needed), lessons (11 11 - 12), classname (Q34), old classname (Q34, mostly empty),
                // substitute teacher id (Bar, mostly empty), teacher id (Bar), type (EVA), course (M-GK-3),
                // old course (M-GK-3, mostly empty), room (M119), description

                // Get the change's data for every table row
                for (row in panel.select("tbody tr")) {

                    // Skip this day if text contains "Keine Einträge" (no entries)
                    if (!row.text().contains("Keine Einträge!")) {
                        // Process every cell
                        for ((dayIndex, cell) in row.select("td").withIndex()) {
                            when (dayIndex) {
                                0 -> {
                                } // Ignore first cell
                                1 -> { // Affected lessons
                                    lessons = if (cell.text().contains(" - ")) {
                                        // Get start and end lesson and put everything in between in a list
                                        // Also, make sure a lesson is never null but 0
                                        val fromLesson = cell.text().substring(
                                                0, cell.text().indexOf(" "))
                                                .trim().toIntOrNull() ?: 0
                                        val toLesson = cell.text().substring(
                                                cell.text().lastIndexOf(" ") + 1)
                                                .trim().toIntOrNull() ?: 0
                                        (fromLesson..toLesson).toList()
                                    } else {
                                        listOf(cell.text().toIntOrNull() ?: 0)
                                    }
                                }
                                2 -> className = if (cell.text() == "") null else cell.text()
                                3 -> className_before = if (cell.text() == "") null else cell.text()
                                4 -> id_subsTeacher =
                                        if (cell.text() == "") null
                                        else cell.text().toLowerCase(Locale.ROOT)
                                5 -> id_teacher =
                                        if (cell.text() == "") null
                                        else cell.text().toLowerCase(Locale.ROOT)
                                6 -> type = when (cell.text()) {
                                    "EVA", "Eigenverantwortliches Arbeiten" -> Change.TYPE_EVA
                                    "Entfall" -> Change.TYPE_CANCELLED
                                    "Freisetzung" -> Change.TYPE_FREED
                                    "Vertretung", "Statt-Vertretung",
                                    "Vertr.", "Vertr" -> Change.TYPE_SUBSTITUTE
                                    "Betreuung" -> Change.TYPE_CARE
                                    "Raum", "Raumwechsel" -> Change.TYPE_ROOM
                                    "Verlegung", "Tausch" -> Change.TYPE_SWITCHED
                                    "Klausur" -> Change.TYPE_EXAM
                                    else -> Change.TYPE_OTHER
                                }
                                7 -> id_course_external =
                                        if (cell.text() == "") null
                                        else cell.text().toLowerCase(Locale.ROOT)
                                8 -> id_course_external_before =
                                        if (cell.text() == "") null
                                        else cell.text().toLowerCase(Locale.ROOT)
                                9 -> room =
                                        if (cell.text() == "") null
                                        else cell.text().toLowerCase(Locale.ROOT)
                                10 -> description =
                                        if (cell.text() == "") null
                                        else cell.text().toLowerCase(Locale.ROOT)
                                                // Remove duplicate whitespaces
                                                .replace("""\s+""".toRegex(), " ")
                                                .capitalize(Locale.getDefault())
                            }
                            // Next cell
                        }

                        // Try to get an internal id
                        internalId = if (id_course_external != null && id_teacher != null)
                            IdParser().getCourseIdWithGmb(id_course_external, id_teacher)
                        else if (id_course_external_before != null && id_teacher != null)
                            IdParser().getCourseIdWithGmb(id_course_external_before, id_teacher)
                        else if (id_course_external != null && IdParser().getCourseIdWithGmb(id_course_external) != null)
                            IdParser().getCourseIdWithGmb(id_course_external)
                        else if (id_course_external_before != null && IdParser().getCourseIdWithGmb(id_course_external_before) != null)
                            IdParser().getCourseIdWithGmb(id_course_external_before)
                        else null

                        // Add parsed change to list
                        changes.add(Change(
                                internalId,
                                id_course_external,
                                date,
                                lessons,
                                type,
                                id_course_external_before,
                                className,
                                className_before,
                                id_teacher,
                                id_subsTeacher,
                                room,
                                null, // room before is not currently supported by sph
                                description
                        ))
                    }
                }

                // Next day

            }
        } catch (e: Error) {
            // Log this error to Firebase Crashlytics
            FirebaseCrashlytics.getInstance().log("Failed to parse raw changes")
            FirebaseCrashlytics.getInstance().recordException(e)
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
                        isLK = courseGmbId.contains("(lk|pf)".toRegex())
                ))
            }
        }

        return courses
    }

    /**
     * Parse courses from raw study groups webpage
     * StKL: Parse also Arbeiten/ KLausuren for schedule db
     * @param rawResponse Html repsonse from SPH
     * @return List of all found courses
     */
    //fun parseCoursesFromStudygroups(rawResponse: String): List<Course> {
    fun parseCoursesFromStudygroups(rawResponse: String): Pair<List<Course>, List<Schedule>> {
        val courses = mutableListOf<Course>()
        val schdls  = mutableListOf<Schedule>()

        // Return empty lists if content is invalid
        try {

            //#1
            // Remove stuff we don't need
            // There are multiple <tbody> tables in this page, we'll just take the first one for courses
            val rawContent = rawResponse.substring(rawResponse.indexOf("<tbody>") + 7, rawResponse.indexOf("</tbody>"))

            //#2
            // And the second <tbody> tble for schedule, because in the second <tbody> table are only entries in with Klausren/ Arbeiten
            var schdlCntnt = rawResponse.substring(rawResponse.indexOf("</tbody>") + 8)
            schdlCntnt = schdlCntnt.substring(schdlCntnt.indexOf("<tbody>") + 7, schdlCntnt.indexOf("</tbody>"))

            //#1
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
            val nameColorMap = Utility.parseStringArray(R.array.course_colors)

            // Get data from each table row and save the courses
            for (entry in rawContents) {
                namedId = entry.substring(Utility.ordinalIndexOf(entry, "<td>", 1) + 4, entry.indexOf("<small>") - 1).trim()
                uniformNamedId = CourseInfo.parseNamedId(namedId)
                // Get the sph id from <small> and remove "lk" (ignore case)
                sphId = entry.substring(entry.indexOf("<small>") + 8, entry.indexOf("</small>") - 1)
                        .replace("(?i)lk".toRegex(), "")
                teacherId = entry.substring(Utility.ordinalIndexOf(entry, "<td>", 2))
                teacherId = teacherId.substring(teacherId.indexOf("(") + 1, teacherId.indexOf(")")).toLowerCase(Locale.ROOT)
                // Get an internal id from sph's sphid
                internalId = IdParser().getCourseIdWithSph(
                        sphId,
                        teacherId,
                        entry.toLowerCase(Locale.ROOT).contains("(lk|pf)".toRegex()))

                courses.add(Course(
                        courseId = internalId,
                        sph_id = sphId,
                        named_id = uniformNamedId,
                        id_teacher = teacherId,
                        fullname = CourseInfo.getFullnameFromInternald(internalId),
                        isFavorite = true,
                        isLK = entry.toLowerCase(Locale.ROOT).contains("(lk|pf)".toRegex()),
                        color = (nameColorMap[uniformNamedId.substring(0, uniformNamedId.indexOf(" "))]
                                ?: nameColorMap["default"])!!.toColorInt()
                ))
            }

            //#2
            /*
            Example:
            ...
            <tbody>
             <tr>
             <td colspan="6"> <b>November</b></td>
             </tr>
                            v
             <tr data-type="klausur" data-id="892" data-lerngruppe="2914" >
                      v
             <td>Do, 25.11.2021</td>
                   v
             <td>Biologie 05f1 <small>(051BIO01-F)</small> </td>
                     v
             <td>Lernkontrolle </td>
                    v
             <td>1., 2.</td>
                   v
             <td>45 Min.</td>
             </tr>
             ...
            </tbody>
            ...
            */

            //divide string in the parts between <tr> and </tr>
            val schdlCntntArr = schdlCntnt.split("<tr").toMutableList()
            schdlCntntArr.removeFirst()
            val c = Calendar.getInstance()
            c.time = Date()

            for (entry1 in schdlCntntArr) {
                //ignore the month headline by investigating for Klausur in the starting tr tag
                if ("klausur" in entry1) {
                    //good entry
                    val tmSchedule = Schedule()
                    val tdCntntArr = entry1.split("<td").toMutableList()
                    tdCntntArr.removeFirst()
                    var spprtStr: String

                    if (tdCntntArr.getOrNull(0) != null) {
                        //first <td> contains date - e.g. <td>Do, 25.11.2021</td>
                        spprtStr = tdCntntArr[0].substring(
                            tdCntntArr[0].indexOf(".") - 2,
                            tdCntntArr[0].indexOf(".") + 8
                        )
                        if (spprtStr.isNotEmpty()) {
                            tmSchedule.nme = spprtStr
                            val dtArr = spprtStr.split(".").toMutableList()
                            if ( (dtArr.getOrNull(0) != null) && (dtArr.getOrNull(1) != null) && (dtArr.getOrNull(2) != null) ) {
                                c.set(dtArr[2].toInt(), dtArr[1].toInt() - 1, dtArr[0].toInt())
                            }
                            else {
                                c.time = Date(0)
                            }
                            tmSchedule.strt = c.time
                            tmSchedule.nd = tmSchedule.strt
                        }
                    }

                    if (tdCntntArr.getOrNull(1) != null) {
                        //second <td> contains form - e.g. <td>Biologie 05f1 <small>(051BIO01-F)</small> </td>
                        //another example: <td> Religion - evangelisch 5 <small>(051REV01-)</small> </td>
                        //from ">" plus 0..n spaces => #1#
                        spprtStr = tdCntntArr[1].replace("[>]{1}+[\\s]{0,}".toRegex(), "#1#")
                        //to spaces followed by letter-numbers combintion followed by spaces {0,} followed by "<"
                        spprtStr = spprtStr.replace(
                            "[\\s]{1,}+[\\w]{1,}+[\\s]{0,}+[<]{1,}".toRegex(),
                            "#2#"
                        )
                        if (spprtStr.isNotEmpty()) {
                            tmSchedule.crs =
                                spprtStr.substring(
                                    spprtStr.indexOf("#1#") + 3,
                                    spprtStr.indexOf("#2#")
                                )
                            tmSchedule.nme += "_" + tmSchedule.crs
                        }
                    }

                    if (tdCntntArr.getOrNull(2) != null) {
                        //third <td> contains txt - e.g. <td>Lernkontrolle </td>
                        tmSchedule.txt = tdCntntArr[2].substring(
                            tdCntntArr[2].indexOf(">") + 1,
                            tdCntntArr[2].indexOf("</td>")
                        )
                        tmSchedule.txt = tmSchedule.txt.replace("[ ]".toRegex(), "")
                        tmSchedule.nme += "_" + tmSchedule.txt
                        if ((tmSchedule.txt == "Arbeit") || (tmSchedule.txt == "Lernkontrolle")) {
                            tmSchedule.ctgr = "Prüfungen"
                        }
                    }

                    if (tdCntntArr.getOrNull(3) != null) {
                        //fourth <td> contains lessons - e.g. <td>1., 2.</td>
                        spprtStr = tdCntntArr[3].substring(
                            tdCntntArr[2].indexOf(">") + 1,
                            tdCntntArr[3].indexOf("</td>")
                        )
                        if (spprtStr.isNotEmpty()) {
                            val lssnArr = spprtStr.split(",").toMutableList()
                            val lssnArr2 = mutableListOf<Int>()
                            for (entry2 in lssnArr) if ("[\\d]".toRegex()
                                    .containsMatchIn(entry2)
                            ) lssnArr2.add(entry2.replace("[\\D]".toRegex(), "").toInt())
                            for (nmbr in lssnArr2) tmSchedule.hr += "$nmbr#"
                        }
                    }

                    if (tdCntntArr.getOrNull(4) != null) {
                        //fifth <td> contains duration - e.g. <td>45 Min.</td>
                        spprtStr = tdCntntArr[4].substring(
                            tdCntntArr[4].indexOf(">") + 1,
                            tdCntntArr[4].indexOf("</td>")
                        )
                        if (spprtStr.isNotEmpty()) {
                            spprtStr = spprtStr.replace("[\\D]".toRegex(), "")
                            tmSchedule.drtn = spprtStr.toInt()
                        }
                    }

                    schdls.add(tmSchedule)

                }
                //else - we can ignore the entry
            }

        } catch (e: Exception) {
            Log.w(TAG, "Studygroups parsing failed!")
            Log.w(TAG, e.stackTraceToString())
        }

        //return courses
        return Pair(courses, schdls)
    }

    /**
     * Parse courses from raw post overview webpage
     * @param rawResponse Html repsonse from SPH
     * @return List of all found courses
     */
    fun parseCoursesFromPostsoverview(rawResponse: String): List<Course> {
        val courses = mutableListOf<Course>()
        // Get the courses table
        val table = Jsoup.parse(rawResponse).select("#aktuell tbody")

        var courseName: String
        var numberId: String
        var teacherId: String
        var isLK: Boolean
        var courseToAdd: Course?
        val nameColorMap = Utility.parseStringArray(R.array.course_colors)
        // Get values from table
        for (row in table.select("tr")) {
            numberId = row.attr("data-book")
            courseName = row.select("span.name").text()
            teacherId = row.select("span.teacher button")
                    .first().ownText().toLowerCase(Locale.ROOT)
            isLK = courseName.toLowerCase(Locale.ROOT).contains("(lk|pf|prfgk)".toRegex())

            // Add the course to the list
            courseToAdd = getCourseFromPostsoverviewData(courseName, teacherId, isLK, numberId, nameColorMap)
            if (courseToAdd != null)
                courses.add(courseToAdd)
        }

        return courses
    }

    /**
     * Get or create course from the data provided by sph's post overview page
     */
    fun getCourseFromPostsoverviewData(courseName: String,
                                       teacherId: String,
                                       isLK: Boolean,
                                       numberId: String,
                                       nameColorMap: Map<String, String>): Course? {
        // Make named id from post overview uniform
        val uniformNamedId = CourseInfo.parseNamedId(courseName)

        // Get courses that might be the same as this one
        var similiarCourses = CoursesDb.getByNamedId(uniformNamedId).toMutableList()

        // If no similiar course was found, try getting all courses with the same subject and teacher
        var courseIdPrefix: String? = null
        if (similiarCourses.isEmpty()) {
            courseIdPrefix = IdParser().getCourseIdPrefixWithNamedId(uniformNamedId, teacherId)
            if (courseIdPrefix != null)
                similiarCourses.addAll(CoursesDb.getByInternalPrefix(courseIdPrefix)
                        .filter { it.isLK == null || it.isLK == isLK })
        }


        // If contains text in brackets and no colon in between,
        // we'll assume that's a sph id and try to find a matching course
        if (courseName.contains("""\([^,]+\)""".toRegex())) {
            val courseToAdd = CoursesDb.getBySphId(
                    courseName.substring(
                            courseName.indexOf("(") + 1,
                            courseName.lastIndexOf(")"))
                            .replace("-GYM", " - GYM"))
            if (courseToAdd != null) similiarCourses.add(courseToAdd)
        }

        // Make sure no course is in the list twice
        // This might happen because we add both by NamedId and SphId
        similiarCourses = similiarCourses.distinct().toMutableList()

        when {
            similiarCourses.size == 1 -> {
                // Only one similiar course, this should be it.
                similiarCourses[0].apply {
                    number_id = numberId
                    named_id = uniformNamedId
                    isFavorite = true
                    fullname = CourseInfo.getFullnameFromInternald(similiarCourses[0].courseId)
                    color = (nameColorMap[
                            uniformNamedId
                                    .substring(0, uniformNamedId.indexOf(" "))]
                            ?: nameColorMap["default"])!!.toColorInt()
                }
                return similiarCourses[0]
            }
            similiarCourses.isEmpty() -> {
                // todo handle null courseIdPrefix
                // Create new course with this namedid & numberid
                // If no course could be found, there is no other course with the same internal prefix
                // (We tried getting every course by that)
                // This means we can just use the prefix - if available with an index 1
                if (courseIdPrefix != null) {
                    return Course(
                            courseId = courseIdPrefix + "_1",
                            named_id = uniformNamedId,
                            fullname = CourseInfo.getFullnameFromInternald(courseIdPrefix + "_1"),
                            number_id = numberId,
                            id_teacher = teacherId,
                            isFavorite = true,
                            isLK = isLK,
                            color = (nameColorMap[uniformNamedId.substringBefore(" ")]
                                    ?: nameColorMap["default"])!!.toColorInt()
                    )
                }
                Log.d(TAG, "No valid course for $uniformNamedId")
            }
            else /*similiarCourses.size > 1*/ -> {
                // todo handle multiple similiar courses
                Toast.makeText(appContext(), "Too many courses", Toast.LENGTH_LONG).show()
                Log.d(TAG, "Too many courses for $uniformNamedId")
                // currently adds first course
                similiarCourses[0].number_id = numberId
                return similiarCourses[0]
            }
        }
        // This should never be called
        Log.e(TAG, "Couldn't get course from post overview data")
        return null
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
    fun parseFeatureList(rawResponse: String): List<FunctionTile> {
        val functions = mutableListOf<FunctionTile>()
        val ids = mutableListOf<String>()

        // Get "accordion", list of all feature tiles
        val accordion = Jsoup.parse(rawResponse).selectFirst("div#accordion")

        // Name-type map
        val hrefTypeMap = Utility.parseStringArray(R.array.tiles_href_type)

        var id: String
        var name: String
        var locationTemp: String
        var type: String?
        var icon: String
        var color: Int
        var styles: String
        // Get every list element within the accordion
        for (element in accordion.select("li")) {
            // sph removed the elements' ids, but classes will contain it - at least for now
            // This will break should they opt to assign different classes to the same tile,
            // once on start and once on their correct group
            id = element.className()
            // Only if tile with id hasn't been added yet
            if (!ids.contains(id)) {
                // Get the feature's name
                name = element.select("div.textheight h3").last().ownText()
                // Get the url this tile is directing to
                // This (now changed: might) be something like /meinunterricht.php?a=X&e=XXX,
                // we'll resolve those redirects later
                locationTemp = element.select("div.textheight a").last()
                        .attr("href")
                // Add https://start.schulportal.hessen.de/ if that's a relative location
                if (!locationTemp.contains("http")) {
                    locationTemp = appContext().getString(R.string.url_start) + locationTemp
                }
                // Get the tile's logo from its logo view's class
                // The .fa- or .glyphicon- icon will be the second-to-last class before .logo
                icon = Regex(""".*((fa|glyphicon)-\S*)\s+logo""").find(
                        element.selectFirst("div.logoview span.logo").className()
                )!!.groupValues[1]
                // replace some icons that we know don't work
                icon = icon.replace("mail-bulk", "comment-alt") // This does not even work on desktop
                        .replace("video-camera", "play") // This does work but looks horrible (Edupool)
                        .replace("project-diagram", "sitemap")
                        .replace("file-contract", "file-alt")
                        .replace("equals", "calculator")
                // Try to get the tile's color
                // sph sometimes uses rgb, sometimes hex and sometimes provides invalid hex values
                color = try {
                    styles = element.selectFirst("div.box").attr("style")
                    Color.parseColor(styles.substring(
                            styles.indexOf("background-color: #") + 18,
                            styles.indexOf("background-color: #") + 25))
                } catch (nfe: java.lang.Exception) {
                    // If parsing the color failed for some reason, use the current theme color
                    // NumberFormatException or StringIndexOutOfBoundsException if uses rba
                    appContext()
                            .getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)
                            .getInt("themeColor", 0)
                }

                // Don't save the tile if it's for logging out
                if (!locationTemp.contains("index.php?logout")
                        && !name.contains("Logout")
                        && !name.contains("Abmelden")) {

                    // Check the tile's type using its href
                    type = null
                    type@ for (href in hrefTypeMap) {
                        if (locationTemp.contains(href.key)) {
                            type = href.value
                            break@type
                        }
                    }
                    // If no matching href to type mapentry was found, use "other"
                    if (type == null) type = "other"

                    // Create a feature tile with these values and add it to the return list
                    functions.add(FunctionTile(name, locationTemp, type, icon, color))
                    // Remember tile id
                    ids.add(id)
                }
            }

        }

        return functions
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
                              attachments: List<FileAttachment>,
                              tasks: List<Task>,
                              linkAttachments: List<LinkAttachment>) -> Unit) {
        val posts = mutableListOf<Post>()
        val files = mutableListOf<FileAttachment>()
        val tasks = mutableListOf<Task>()
        val links = mutableListOf<LinkAttachment>()
        val attachIds = mutableListOf<String>()
        val linkIds = mutableListOf<String>()
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
        // File attachment-specific
        var fileId: String
        var fileName: String
        var fileType: String
        var fileUrl: String
        var fileSize: String
        // Link attachment-specific
        var linkId: String
        var linkMatches: List<String>?
        // For getting the date
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
        val internalDateFormat = SimpleDateFormat("2020-MM-dd", Locale.ROOT)

        // Parse data for every row
        for (row in rawPosts) {
            try {
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
                    postTitle = cells[1].selectFirst("b")?.wholeText()?.trim() ?: ""
                    // Correct weird stuff that sph does
                    postTitle = postTitle.replace("""&amp;amp;quot;""", "\"")
                    if (postTitle == "kein Thema") postTitle = null
                    // Description might include html. We'll just get the text for now.
                    postDescription = try {
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
                }

                /*
                 * Tasks
                 */

                // If post contains a task
                if (cells[1].toString().contains("<span class=\"homework\">")) {
                    // There can only be one task per post, it seems
                    taskId = courseId + "_task-" + internalDateFormat.format(date) + "_1"
                    taskDone = !cells[1].select("span.homework span.done").hasClass("hidden")
                    taskDescription = cells[1].select("span.homework").nextAll("span.markup")[0].wholeText()
                    // Add new task to tasks list
                    tasks.add(Task(
                            taskId,
                            courseId,
                            postId,
                            taskDescription,
                            date,
                            taskDone
                    ))
                }

                /*
                 * File attachments
                 */

                // If post contains attachments
                if (cells[1].select("div.files").size != 0) {
                    // For every file attachment
                    for (file in cells[1].select("div.files div.file")) {
                        fileId = IdParser().getFileAttachmentId(courseId, date, attachIds)
                        attachIds.add(fileId)

                        // Get file info
                        fileName = file.toString().substringAfter("</span>")
                                .substringBefore("<small>")
                                .replace("_", " ").replace("-", " ").trim()
                        fileSize = file.select("small").text()
                        fileSize = fileSize.substring(1, fileSize.length - 1) // Remove brackets
                        //StKl: Take care file type is using last "." in the string!
                        fileType = fileName.substringAfterLast('.', "").toLowerCase(Locale.ROOT)
                        fileName = fileName.substringBeforeLast('.')

                        // Parse file url
                        fileUrl = ("https://start.schulportal.hessen.de/meinunterricht.php?a=downloadFile&id="
                                + doc.select("h1[data-book]")[0].attr("data-book") // NumberId of this course
                                + "&e=" + sphPostId
                                + "&f=" + URLEncoder.encode(file.attr("data-file"), "utf-8"))

                        // Add new Attachment to lst
                        files.add(FileAttachment(
                                fileId,
                                courseId,
                                postId,
                                fileName,
                                date,
                                fileUrl,
                                fileSize,
                                fileType,
                                // Set post as not pinned
                                // This will not overwrite if the attachment was pinned before
                                false,
                                null
                        ))
                    }
                }

                /*
             * Links
             * sph returns links as plain text and converts them using js,
             * so we'll have to use some markdown
             */

                // For description and homework
                for (content in cells[1].select("span.markup")) {
                    // Get links from content
                    linkMatches = Regex("""(http[^\s|<|"]*)""".trimMargin()).find(content.toString())?.groupValues
                    if (linkMatches != null) {
                        // Remove doubled entries
                        linkMatches = linkMatches.distinct()
                        for (link in linkMatches) {
                            // Filter out entire match and some more stuff
                            if (link.startsWith("http")) {
                                // Add link
                                linkId = IdParser().getLinkAttachmentId(courseId, date, linkIds)
                                linkIds.add(linkId)
                                links.add(LinkAttachment(
                                        linkId,
                                        courseId,
                                        postId,
                                        link.trim(),
                                        date,
                                        link.trim(),
                                        false,
                                        null
                                ))
                            }
                        }
                    }
                }

                // todo Parse submissions
            } catch (npe: NullPointerException) {
                DebugLog("Parser", "NPE on parsing posts for $courseId",
                        bundleOf("row" to row.toString()), Debugger.LOG_TYPE_ERROR)
                npe.printStackTrace()
                FirebaseCrashlytics.getInstance().recordException(npe)
            }
        }

        onParsed(posts, files, tasks, links)
    }

    /**
     * Parse all lessons from timetable
     * @param rawResponse Raw timetable site from sph
     * @return List of courses
     */
    fun parseTimetable(rawResponse: String): List<Lesson> {
        val returnList = mutableListOf<Lesson>()

        // Return empty list if content is not what we expected
        try {

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
                                gmbId = lesson.select("b").text().trim().toLowerCase(Locale.ROOT)
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
        } catch (e: Exception) {
            Log.w(TAG, "Timetable parsing failed!")
            Log.w(TAG, e.stackTraceToString())
        }

        return returnList
    }
}