package de.koenidv.sph.database

import de.koenidv.sph.objects.Attachment

//  Created by koenidv on 29.12.2020.
object AttachmentsDb {

    val files: FileAttachmentsDb = FileAttachmentsDb.getInstance()
    val links: LinkAttachmentsDb = LinkAttachmentsDb.getInstance()

    /**
     * Get the count of file and link attachments for a post
     */
    fun countForPost(postId: String) = files.countForPost(postId) + links.countForPost(postId)

    /**
     * Get file and links attachments (in this order) by post id
     */
    fun byPostId(postId: String): MutableList<Attachment> {
        val attachments = files.getByPostId(postId)
        attachments.addAll(links.getByPostId(postId))
        return attachments
    }

    /**
     * Get all pinned attachments, ordered by last use
     */
    fun pins(): MutableList<Attachment> {
        val pins = files.pins
        pins.addAll(links.pins)
        pins.sortByDescending { it.lastuse() }
        return pins
    }

}