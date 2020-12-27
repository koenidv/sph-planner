package de.koenidv.sph.objects

//  Created by koenidv on 27.12.2020.
data class Attachment(
        private val link: LinkAttachment?,
        private val file: FileAttachment?,
        private val type: String
) {
    constructor(link: LinkAttachment) : this(link, null, "link")
    constructor(file: FileAttachment) : this(null, file, "file")

    fun attachId() = if (type == "link") link!!.attachmentId else file!!.attachmentId
    fun courseId() = if (type == "link") link!!.id_course else file!!.id_course
    fun postId() = if (type == "link") link!!.id_post else file!!.id_post
    fun name() = if (type == "link") link!!.name else file!!.name
    fun date() = if (type == "link") link!!.date else file!!.date
    fun url() = if (type == "link") link!!.url else file!!.url
    fun pinned() = if (type == "link") link!!.pinned else file!!.pinned
    fun lastuse() = if (type == "link") link!!.lastUse else file!!.lastUse
    fun fileType() = if (type == "link") "link" else file!!.fileType
    fun link() = link!!
    fun file() = file!!
    fun type() = type

}