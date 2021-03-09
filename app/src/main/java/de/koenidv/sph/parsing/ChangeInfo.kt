package de.koenidv.sph.parsing

import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.appContext
import de.koenidv.sph.objects.Change

//  Created by koenidv on 31.12.2020.
object ChangeInfo {

    /**
     * Get a color associated to the provided change type
     */
    fun getTypeColor(type: Int): Int = when (type) {
        Change.TYPE_EVA -> appContext().getColor(R.color.change_type_eva)
        Change.TYPE_CANCELLED -> appContext().getColor(R.color.change_type_cancelled)
        Change.TYPE_FREED -> appContext().getColor(R.color.change_type_freed)
        Change.TYPE_SUBSTITUTE -> appContext().getColor(R.color.change_type_substitute)
        Change.TYPE_CARE -> appContext().getColor(R.color.change_type_care)
        Change.TYPE_ROOM -> appContext().getColor(R.color.change_type_room)
        Change.TYPE_SWITCHED -> appContext().getColor(R.color.change_type_switched)
        Change.TYPE_EXAM -> appContext().getColor(R.color.change_type_exam)
        Change.TYPE_HOLIDAYS -> appContext().getColor(R.color.change_type_holidays)
        else -> appContext().getColor(R.color.change_type_other)
    }

    /**
     * Get a short descriptor a change's type
     */
    fun getTypeNameAbbreviation(type: Int): String = when (type) {
        Change.TYPE_EVA -> appContext().getString(R.string.changes_type_short_eva)
        Change.TYPE_CANCELLED -> appContext().getString(R.string.changes_type_short_cancelled)
        Change.TYPE_FREED -> appContext().getString(R.string.changes_type_short_freed)
        Change.TYPE_SUBSTITUTE -> appContext().getString(R.string.changes_type_short_substitute)
        Change.TYPE_CARE -> appContext().getString(R.string.changes_type_short_cancelled)
        Change.TYPE_ROOM -> appContext().getString(R.string.changes_type_short_room)
        Change.TYPE_SWITCHED -> appContext().getString(R.string.changes_type_short_switched)
        Change.TYPE_EXAM -> appContext().getString(R.string.changes_type_short_exam)
        else -> appContext().getString(R.string.changes_type_short_other)
    }
}