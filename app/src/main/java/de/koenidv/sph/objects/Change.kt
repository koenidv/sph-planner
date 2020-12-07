package de.koenidv.sph.objects

import java.util.*

//  Created by koenidv on 07.12.2020.
data class Change(
        // Important stuff
        var changeId : String, // i.e. m_bar_1_change-2017-12-07_1
        var id_course : String, // i.e. m_bar_1
        var id_course_external : String, // i.e. M-GK-3
        var date : Date, // Date of the change, day only, no time
        var lessons : List<Int>, // Affected lessons, i.e [1,2]
        var type : String, // Type of change, i.e. todo: change type constants
        // Not so important stuff
        var className : String?, // Affected school class, i.e. Q34
        var className_before : String?, // If the schedule was changed
        var id_teacher : String?, // Affected teacher id, i.e. bar
        var id_subsTeacher : String?, // Substitute teacher id, if any
        var room : String?, // New room, i.e. M115
        var room_before : String?, // Old room
        var description : String? // Description of the event
)
