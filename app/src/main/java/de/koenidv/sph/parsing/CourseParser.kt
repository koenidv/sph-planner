package de.koenidv.sph.parsing

//  Created by koenidv on 08.12.2020.
class CourseParser {
    fun getCourseFullnameFromInternald(courseId : String) : String {
        require(IdParser().getCourseIdType(courseId) == TYPE_INTERNAL)
        val subject = courseId.substring(courseId.indexOf("-") + 1)
        return "Testing"
        TODO("Dictionary for subject ids")
    }
}