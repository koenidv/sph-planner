package de.koenidv.sph.objects

import java.util.*

//  Created by koenidv on 07.12.2020.
data class Change(
        // Important stuff
        var changeId : String, // i.e. m_bar_1_change-2017-12-07_1 - Lowercase
        var id_course : String, // i.e. m_bar_1 - Lowercase
        var id_course_external : String, // i.e. M-GK-3 - Uppercase
        var date : Date, // Date of the change, day only, no time
        var lessons : List<Int>, // Affected lessons, i.e [1,2]
        var type : String, // Type of change, i.e. - Uppercase todo: change type constants
        // Not so important stuff
        var id_course_external_before : String?, // If the schedule was changed
        var className : String?, // Affected school class, i.e. Q34
        var className_before : String?, // If the schedule was changed
        var id_teacher : String?, // Affected teacher id, i.e. bar - Lowercase
        var id_subsTeacher : String?, // Substitute teacher id, if any - Lowercase
        var room : String?, // New room, i.e. M115 - Uppercase
        var room_before : String? = null, // Old room. Per SPH implementation as of now, this will always be null - Uppercase
        var description : String? // Description of the event
) {

    // todo documentation
    init {
        // todo apply case conventions
        // todo remove empty values
    }

    // todo documentation
    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Change -> {
                other.changeId == this.changeId
                        && other.lessons == this.lessons
            }
            is String -> {
                other == this.changeId
            }
            else -> false
        }
    }
}
