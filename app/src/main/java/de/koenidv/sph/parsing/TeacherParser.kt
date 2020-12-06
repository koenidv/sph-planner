package de.koenidv.sph.parsing

//  Created by koenidv on 06.12.2020.
class TeacherParser {

    /**
     * Retrieves a teacher id from an internal id
     * @param id The teacher's course's internal id (i.e. "m-bar")
     * @return The teacher's id (i.e "bar")
     */
    fun teacherIdFromInternalCourseId(id: String) : String{
        require(CourseInfoParser().getCourseIdType(id) == TYPE_INTERNAL)
        // Example for internal ids: m-bar (Maths Barth)
        // Return only part after the hyphen
        return id.substring(0, id.indexOf("-"))
    }

}