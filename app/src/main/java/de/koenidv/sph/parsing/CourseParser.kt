package de.koenidv.sph.parsing

import de.koenidv.sph.R

//  Created by koenidv on 08.12.2020.
class CourseParser {
    fun getCourseFullnameFromInternald(courseId: String): String {
        require(IdParser().getCourseIdType(courseId) == TYPE_INTERNAL)
        val subject = courseId.substring(courseId.indexOf("-") + 1)
        return "Testing"
        TODO("Dictionary for subject ids")
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