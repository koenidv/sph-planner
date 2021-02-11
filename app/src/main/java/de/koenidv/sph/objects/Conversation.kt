package de.koenidv.sph.objects

//  Created by koenidv on 10.02.2021.
data class Conversation(
        val convId: String, // Conversation id, like 33868
        val firstIdMess: String, // Id of the conversation's first message
        val subject: String, // Conversation's subject
        val recipientCount: Int, // Number of recipients according to sph's "private" property
        val answerType: String, // none/private/all, merged from different values from sph:
        // Papierkorb, noAnswerAllowed, privateOnly, groupOnly
        val originalSenderId: String,
        val unread: Boolean,
        val archived: Boolean = false
) {
    companion object {
        const val ANSWER_TYPE_NONE = "none"
        const val ANSWER_TYPE_PRIVATE = "private"
        const val ANSWER_TYPE_ALL = "all"
    }
}
