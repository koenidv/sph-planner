package de.koenidv.sph.objects

import de.koenidv.sph.networking.TokenManager
import java.util.*

//  Created by koenidv on 10.02.2021.
data class Message(
        val messId: String, // Message's unique id ("Uniquid" / )
        val idConv: String, // Conversation the message belongs to
        val idSender: String, // Sender's user id
        val senderType: String, // "Betreuer" / "Teilnehmer" user type
        val senderName: String, // Sender's name
        val date: Date, // Message timestamp
        val subject: String, // Messages's subject
        val content: String, // Message's content
        val recipients: List<String>?, // Recipients, only for own messages
        val recipientCount: Int, // Number of recipients according to sph's "private" property
        val unread: Boolean // true if unread, false if read
) {

    companion object {
        const val SENDER_TYPE_TEACHER = "Betreuer"
        const val SENDER_TYPE_STUDENT = "Teilnehmer"
    }

    fun isOwn() = idSender == TokenManager.userid
}
