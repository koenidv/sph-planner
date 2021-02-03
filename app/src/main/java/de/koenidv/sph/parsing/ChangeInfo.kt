package de.koenidv.sph.parsing

import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import de.koenidv.sph.objects.Change

//  Created by koenidv on 31.12.2020.
object ChangeInfo {

    /**
     * Get a color associated to the provided change type
     */
    fun getTypeColor(type: Int): Int = when (type) {
        Change.TYPE_EVA -> applicationContext().getColor(R.color.change_type_eva)
        Change.TYPE_CANCELLED -> applicationContext().getColor(R.color.change_type_cancelled)
        Change.TYPE_FREED -> applicationContext().getColor(R.color.change_type_freed)
        Change.TYPE_SUBSTITUTE -> applicationContext().getColor(R.color.change_type_substitute)
        Change.TYPE_CARE -> applicationContext().getColor(R.color.change_type_care)
        Change.TYPE_ROOM -> applicationContext().getColor(R.color.change_type_room)
        Change.TYPE_SWITCHED -> applicationContext().getColor(R.color.change_type_switched)
        Change.TYPE_EXAM -> applicationContext().getColor(R.color.change_type_exam)
        Change.TYPE_HOLIDAYS -> applicationContext().getColor(R.color.change_type_holidays)
        else -> applicationContext().getColor(R.color.change_type_other)
    }

    /**
     * Get a short descriptor a change's type
     */
    fun getTypeNameAbbreviation(type: Int): String = when (type) {
        Change.TYPE_EVA -> applicationContext().getString(R.string.changes_type_short_eva)
        Change.TYPE_CANCELLED -> applicationContext().getString(R.string.changes_type_short_cancelled)
        Change.TYPE_FREED -> applicationContext().getString(R.string.changes_type_short_freed)
        Change.TYPE_SUBSTITUTE -> applicationContext().getString(R.string.changes_type_short_substitute)
        Change.TYPE_CARE -> applicationContext().getString(R.string.changes_type_short_cancelled)
        Change.TYPE_ROOM -> applicationContext().getString(R.string.changes_type_short_room)
        Change.TYPE_SWITCHED -> applicationContext().getString(R.string.changes_type_short_switched)
        Change.TYPE_EXAM -> applicationContext().getString(R.string.changes_type_short_exam)
        else -> applicationContext().getString(R.string.changes_type_short_other)
    }
}