package de.koenidv.sph.objects

//  Created by koenidv on 16.12.2020.
data class FunctionTile(
        var name: String,
        var location: String,
        var type: String?,
        var icon: String,
        var color: Int
) {
    companion object {
        const val FEATURE_TIMETABLE = "timetable"
        const val FEATURE_COURSES = "mycourses"
        const val FEATURE_STUDYGROUPS = "studygroups"
        const val FEATURE_CHANGES = "changes"
        const val FEATURE_MESSAGES = "messages"
        const val FEATURE_CALENDAR = "Kalender"//duringupdate
    }
}
