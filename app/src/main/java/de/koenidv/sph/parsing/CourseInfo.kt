package de.koenidv.sph.parsing

import androidx.core.os.bundleOf
import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.koenidv.sph.R
import de.koenidv.sph.debugging.DebugLog
import de.koenidv.sph.debugging.Debugger
import java.util.*

//  Created by koenidv on 08.12.2020.
object CourseInfo {

    /**
     * Apply some manipulations to gmb ids to make them match with sph and internal ids
     */
    fun gmbid(gmbId: String) = gmbId
            // Lowercase
            .toLowerCase(Locale.ROOT)
            // Remove bilingual indicator
            .replace("bi-", "-")

    /**
     * Get a subjects full name from a course id
     */
    fun getFullnameFromInternald(internalCourseId: String): String {
        // Get subject from course id
        val subject = internalCourseId.substringBefore("_")
        // Get a map of full names
        val nameMap = Utility.parseStringArray(R.array.courseId_fullName)
        // Return map value if available, else subject
        return nameMap.getOrElse(subject, { subject.capitalize(Locale.getDefault()) })
    }

    /**
     * Get a subjects short name from a course id
     */
    fun getShortnameFromInternald(courseId: String): String {
        // Check if the provided id is in fact an internal one
        if (IdParser().getCourseIdType(courseId) != TYPE_INTERNAL) {
            DebugLog("CrsInf",
                    "Shortname: Id does not seem to be of type internal",
                    bundleOf("id" to courseId),
                    Debugger.LOG_TYPE_WARNING)
            // Throw a new exception to be able to log this to crashlytics
            try {
                throw Exception(
                        "CourseInfo#getShortnameFromInternalId: Failed internal id requirement for id $courseId")
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().log("shortname: Failed internal id requirement for id $courseId")
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
        // Get subject from course id
        val subject = courseId.substringBefore("_")
        // Get a map of short and full names
        val shortMap = Utility.parseStringArray(R.array.courseId_shortName_override)
        val nameMap = Utility.parseStringArray(R.array.courseId_fullName)
        // Return map value if available, else subject
        return when {
            shortMap.containsKey(subject) -> shortMap.getValue(subject)
            nameMap.containsKey(subject) -> nameMap.getValue(subject)
            else -> subject.capitalize(Locale.getDefault())
        }
    }

    /**
     * Get an abbreviation for an internal course id
     * Currently just its first character
     */
    fun getNameAbbreviation(courseId: String) = courseId.first().toUpperCase().toString()

    /**
     * Convert different NamedIds for the same course to an uniform value
     * @param namedId NamedId to convert
     * @return Uniform NamedID
     */
    fun parseNamedId(namedId: String): String {
        var uniformId = namedId

        // Remove anything within brackets
        uniformId = uniformId.replace("""\(.*\)""".toRegex(), "")

        // Replace keys from R.array.namedid_replacements
        val replaceMap = Utility.parseStringArray(R.array.namedid_replacements)
        replaceMap.forEach { (key, value) -> uniformId = uniformId.replace(key, value) }

        return uniformId.trim()
    }
}