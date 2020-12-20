package de.koenidv.sph.objects

import java.io.File
import java.net.URL
import java.util.*

//  Created by koenidv on 07.12.2020.
data class PostAttachment(
        var attachmentId: String, // i.e. m_bar_1_attach-2020-12-07_1
        var id_course: String, // Course to be attached to, i.e. m_bar_48 (48 is sph post id)
        var id_post: String,// Post to be attached to, i.e. m_bar_1_post-2020-12-07_48
        var name: String, // Name given by SPH, i.e. "Arbeitsblatt Nr. 3 Einfuehrung in das Testen von Hypothesen" (.pdf removed and hyphens replaced)
        var date: Date, // Date of the post to be attached to. Should be day only, no time
        var url: URL, // External url where the file can be loaded
        var deviceLocation: File?, // For future updates - Save file locally for later use, nullable
        var fileSize: String // Size of the file
)
