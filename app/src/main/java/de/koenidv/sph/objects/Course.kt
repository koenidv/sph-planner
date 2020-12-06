package de.koenidv.sph.objects

import de.koenidv.sph.parsing.CourseInfoParser
import de.koenidv.sph.parsing.TeacherParser

//  Created by koenidv on 06.12.2020.
data class Course(
        var id: String,
        var gmb_id: String?,
        var sph_id: String?,
        var named_id: String?,
        var number_id: String?,
        var fullname: String,
        var id_teacher: String,
        var isFavorite: Boolean?,
        var isLK: Boolean?) {


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
                other.id == this.id
            }
            is String -> {
                (other == this.id
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
        return id.hashCode()
    }

    /**
     * Create a course object with only the necessary values using an internal id
     * @param internalCourseId An internal course id
     */
    constructor(internalCourseId: String) : this(
            internalCourseId,
            null, null, null, null,
            CourseInfoParser().getCourseFullnameFromInternald(internalCourseId),
            TeacherParser().teacherIdFromInternalCourseId(internalCourseId),
            null, null)


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