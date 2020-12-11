package de.koenidv.sph.objects

import de.koenidv.sph.parsing.CourseParser
import de.koenidv.sph.parsing.TeacherParser

//  Created by koenidv on 06.12.2020.
data class Course(
        var courseId: String, // i.e. m_bar_1
        var gmb_id: String? = null, // i.e. M-GK-3
        var sph_id: String? = null, // i.e. Q3Mbar01 - GYM
        var named_id: String? = null, // i.e. Mathematik GK 13
        var number_id: String? = null, // i.e. 760
        var fullname: String? = null, // i.e. Mathe todo: Set in init()
        var id_teacher: String, // i.e. bar
        var isFavorite: Boolean? = null, // i.e. true (is member of course)
        var isLK: Boolean? = null) { // i.e. false (not intensified course)


    /**
     * Check if given Course object has the same id
     * or given String equals one of the course's ids
     * Thanks to SPH for using 4 different types of ids...
     *
     * @param other Object to compare with: needs to be Course or String to return true
     * @return True, if ids match
     */
    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Course -> {
                other.courseId == this.courseId
            }
            is String -> {
                (other == this.courseId
                        || other == this.gmb_id
                        || other == this.sph_id
                        || other == this.named_id
                        || other == this.number_id)
            }
            else -> false
        }
    }


    /**
     * Generate hashcode for efficient object comparison
     * Only id value is needed as there should only be one course per id
     * @return Object hascode, containing only id value
     */
    override fun hashCode(): Int {
        return courseId.hashCode()
    }

    /* This needs some love
    /**
     * Create a course object with only the necessary values using an internal id
     * @param internalCourseId An internal course id
     */
    constructor(internalCourseId : String) : this(
            internalCourseId,
            null, null, null, null,
            CourseParser().getCourseFullnameFromInternald(internalCourseId),
            TeacherParser().teacherIdFromInternalCourseId(internalCourseId),
            null, null)

     */


    /*
    /**
     * Create a course object with only an external (if input is external) and internal course id, teacher id and fullname
     * @param courseId An external or internal course id
     * @param courseIdType The id's type (must be one of TYPE_INTERNAL, TYPE_GMB, TYPE_SPH, TYPE_NAMED, TYPE_NUMBER): Optional
     */
    constructor(courseId: String, courseIdType: Int?, teacherId: String) {
        // Get course id type if nothing is specified
        val courseType = courseIdType ?: CourseInfoParser().getCourseIdType(courseId)

        // Parse internal id
        val internalId = CourseInfoParser().parseCourseId(courseId, courseIdType, teacherId)

        // Assign external id according to its type
        var gmbId: String? = null
        var sphId: String? = null
        var namedId: String? = null
        var numberId: String? = null
        when (courseIdType) {
            TYPE_GMB -> gmbId = courseId
            TYPE_SPH -> sphId = courseId
            TYPE_NAMED -> namedId = courseId
            TYPE_NUMBER -> numberId = courseId
        }

        // Get fullname for course from internal id
        val name = CourseInfoParser().getCourseFullnameFromInternald(internalId)

        // Create course object with internal and external course id, teacher and fullname
        this(internalId, gmbId, sphId, namedId, numberId, fullname, teacherId, null, null)
    }
    */


}