package de.koenidv.sph.objects

//  Created by koenidv on 25.12.2020.
data class TimetableEntry(
        val lesson: Lesson,
        val course: Course?,
        val changes: List<Change>?,
)
