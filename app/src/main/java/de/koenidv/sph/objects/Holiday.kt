package de.koenidv.sph.objects

import java.util.*

//  Created by koenidv on 29.01.2021.
data class Holiday(
        val id: String,
        val start: Date,
        val end: Date,
        val name: String,
        val year: String
)
