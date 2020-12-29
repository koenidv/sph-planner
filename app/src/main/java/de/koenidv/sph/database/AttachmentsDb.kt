package de.koenidv.sph.database

//  Created by koenidv on 29.12.2020.
object AttachmentsDb {

    val files = FileAttachmentsDb.getInstance()
    val links = LinkAttachmentsDb.getInstance()

    fun countForPost(postId: String) = files.countForPost(postId) + links.countForPost(postId)

}