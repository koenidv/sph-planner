package de.koenidv.sph.objects

//  Created by koenidv on 24.12.2020.
data class Timetable(
        var monday: List<List<Lesson>>,
        var tuesday: List<List<Lesson>>,
        var wednesday: List<List<Lesson>>,
        var thursday: List<List<Lesson>>,
        var friday: List<List<Lesson>>,
        var it: List<List<List<Lesson>>> = listOf(monday, tuesday, wednesday, thursday, friday)
)