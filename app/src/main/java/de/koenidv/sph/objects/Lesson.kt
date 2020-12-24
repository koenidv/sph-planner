package de.koenidv.sph.objects

//  Created by koenidv on 24.12.2020.
data class Lesson(
        var idCourse: String,
        var day: Int, // 0: Monday, 4: Friday
        var hour: Int, // First lesson, second, ..
        var room: String
)
