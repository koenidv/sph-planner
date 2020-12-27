package de.koenidv.sph.objects

import java.util.*

//  Created by koenidv on 07.12.2020.
data class LinkAttachment(
        var attachment_id: String, // courseId_link-date_index
        var id_course: String, // Course to be attached to, i.e. m_bar_48 (48 is sph post id)
        var id_post: String, // Post to be attached to, i.e. m_bar_1_post-2020-12-07_48
        var name: String, // Could be used after resolving
        var date: Date, // Date of the post to be attached to. Should be day only, no time
        var url: String, // Url that has been found
        var pinned: Boolean, // Whether the external link is pinned by user
        var lastUse: Date? // Last usage of this link
)
