package de.koenidv.sph.objects

import java.net.URL
import java.nio.file.Path
import java.util.*

//  Created by koenidv on 07.12.2020.
data class PostAttachment(
        var attachmentId : String, // i.e. m_bar_1_attach-2020-12-07_1
        var course_id : String, // Course to be attached to, i.e. m_bar_1
        var name : String, // Name given by SPH, i.e. "Arbeitsblatt Nr. 3 Einfuehrung in das Testen von Hypothesen" (.pdf removed and hyphens replaced)
        var date : Date, // Date of the post to be attached to. Should be day only, no time
        var url : URL, // External url where the file can be loaded
        var deviceLocation : Path? // For future updates - Save file locally for later use, nullable
)
