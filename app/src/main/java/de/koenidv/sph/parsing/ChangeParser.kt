package de.koenidv.sph.parsing

import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.objects.Change

//  Created by koenidv on 31.12.2020.
class ChangeParser {

    /**
     * Get a color associated to the provided change type
     */
    fun getTypeColor(type: Int): Int = when (type) {
        Change.TYPE_EVA -> SphPlanner.applicationContext().getColor(R.color.change_type_eva)
        Change.TYPE_CANCELLED -> SphPlanner.applicationContext().getColor(R.color.change_type_cancelled)
        Change.TYPE_FREED -> SphPlanner.applicationContext().getColor(R.color.change_type_freed)
        Change.TYPE_SUBSTITUTE -> SphPlanner.applicationContext().getColor(R.color.change_type_substitute)
        Change.TYPE_CARE -> SphPlanner.applicationContext().getColor(R.color.change_type_care)
        Change.TYPE_ROOM -> SphPlanner.applicationContext().getColor(R.color.change_type_room)
        Change.TYPE_SWITCHED -> SphPlanner.applicationContext().getColor(R.color.change_type_switched)
        Change.TYPE_EXAM -> SphPlanner.applicationContext().getColor(R.color.change_type_exam)
        else -> SphPlanner.applicationContext().getColor(R.color.change_type_other)
    }
}