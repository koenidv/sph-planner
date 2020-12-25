package de.koenidv.sph.parsing

import de.koenidv.sph.R
import java.util.*

//  Created by koenidv on 08.12.2020.
class CourseParser {

    /**
     * Get a subjects full name from a course id
     */
    fun getFullnameFromInternald(courseId: String): String {
        // Check if the provided id is in fact an internal one
        require(IdParser().getCourseIdType(courseId) == TYPE_INTERNAL)
        // Get subject from course id
        val subject = courseId.substring(0, courseId.indexOf("_"))
        // Get a map of full names
        val nameMap = Utility().parseStringArray(R.array.courseId_fullName)
        // Return map value if available, else subject
        return if (nameMap.containsKey(subject)) nameMap.getValue(subject)
        else subject.capitalize(Locale.getDefault())
    }

    /**
     * Get a subjects short name from a course id
     */
    fun getShortnameFromInternald(courseId: String): String {
        // Check if the provided id is in fact an internal one
        require(IdParser().getCourseIdType(courseId) == TYPE_INTERNAL)
        // Get subject from course id
        val subject = courseId.substring(0, courseId.indexOf("_"))
        // Get a map of short and full names
        val shortMap = Utility().parseStringArray(R.array.courseId_shortName_override)
        val nameMap = Utility().parseStringArray(R.array.courseId_fullName)
        // Return map value if available, else subject
        return when {
            shortMap.containsKey(subject) -> shortMap.getValue(subject)
            nameMap.containsKey(subject) -> nameMap.getValue(subject)
            else -> subject.capitalize(Locale.getDefault())
        }
    }

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
        val replaceMap = Utility().parseStringArray(R.array.namedid_replacements)
        replaceMap.forEach { (key, value) -> uniformId = uniformId.replace(key, value) }

        return uniformId.trim()
    }
}