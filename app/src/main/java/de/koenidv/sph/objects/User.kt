package de.koenidv.sph.objects

//  Created by koenidv on 09.01.2021.
data class User(
        val userId: String,
        var teacherId: String?,
        var firstname: String?,
        var lastname: String?,
        var type: String?, // lul or sus
        var isPinned: Boolean
)
