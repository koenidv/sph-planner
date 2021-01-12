package de.koenidv.sph.parsing

//  Created by koenidv on 06.12.2020.
object TeacherInfo {

    /**
     * Retrieves a teacher id from an internal id
     * @param id The teacher's course's internal id (i.e. "m_bar_1")
     * @return The teacher's id (i.e "bar")
     */
    fun teacherIdFromInternalCourseId(id: String): String {
        require(IdParser().getCourseIdType(id) == TYPE_INTERNAL)
        // Example for internal ids: m_bar_1 (Maths Barth 1)
        // Return only part between the underscores
        return id.substring(id.indexOf("_"), id.lastIndexOf("_"))
    }
}