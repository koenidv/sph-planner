package de.koenidv.sph.objects

import java.util.*

//  Created by koenidv on 07.12.2020.
data class Post(
        var postId: String, // Post id, i.e. m_bar_1_post-2020-12-07_48
        var id_course: String, // Course to be attached to, i.e. m_bar_1
        var date: Date, // The post's date, should be day only, no time
        var title: String, // The post's title, i.e. "Entscheidungsregel aufstellen"
        var description: String?, // The post's content, nullable
        var unread: Boolean // If the post is unread
)
