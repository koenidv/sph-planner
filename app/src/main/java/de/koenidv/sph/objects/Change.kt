package de.koenidv.sph.objects

import java.util.*

//  Created by koenidv on 07.12.2020.
data class Change(
        // Important stuff
        // Using autoincr. instead of unique text ids
        // Some changes do not have a course/teacher
        //var changeId : String, // i.e. m_bar_1_change-2017-12-07_1 - Lowercase
        var id_course: String? = null, // i.e. m_bar_1 - Lowercase
        var id_course_external: String? = null, // i.e. M-GK-3 - Uppercase
        var date: Date = Date(0), // Date of the change, day only, no time
        var lessons: List<Int>, // Affected lessons, i.e [1,2]
        var type: Int = TYPE_OTHER, // Type of change (a TYPE_... constant)
        // Not so important stuff
        var id_course_external_before: String? = null, // If the schedule was changed
        var className: String? = null, // Affected school class, i.e. Q34
        var className_before: String? = null, // If the schedule was changed
        var id_teacher: String? = null, // Affected teacher id, i.e. bar - Lowercase
        var id_subsTeacher: String? = null, // Substitute teacher id, if any - Lowercase
        var room: String? = null, // New room, i.e. M115 - Uppercase
        var room_before: String? = null, // Old room. Per SPH implementation as of now, this will always be null - Uppercase
        var description: String? = null, // Description of the event
        var sortLesson: Double? = null // Only for sql ordering
) {

    init {
        if (lessons.isNotEmpty()) {
            sortLesson =
                    if (lessons.size == 1) lessons[0].toDouble()
                    else (lessons[0] + 0.01 * lessons[lessons.size - 1])
        }
        // Remove cancelled from description, its a duplicate in sphs
        if (description == "Entfällt" || description == "Entfällt; Verlegung auf Entfall für Lehrer")
            description = null
    }

    companion object {
        const val TYPE_HOLIDAYS = -2
        const val TYPE_OTHER = -1
        const val TYPE_EVA = 0
        const val TYPE_CANCELLED = 1
        const val TYPE_FREED = 2
        const val TYPE_SUBSTITUTE = 3
        const val TYPE_CARE = 4
        const val TYPE_ROOM = 5
        const val TYPE_SWITCHED = 6
        const val TYPE_EXAM = 7
    }

    /**
     * Check if changes are the same
     */
    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Change -> {
                other.id_course == this.id_course
                        && other.date == this.date
                        && other.lessons == this.lessons
                        && other.type == this.type
            }
            else -> false
        }
    }

    override fun hashCode(): Int {
        var result = id_course?.hashCode() ?: 0
        result = 31 * result + date.hashCode()
        result = 31 * result + lessons.hashCode()
        result = 31 * result + type
        return result
    }
}
