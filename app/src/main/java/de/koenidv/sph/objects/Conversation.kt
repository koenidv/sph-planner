package de.koenidv.sph.objects

import android.content.Context
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import de.koenidv.sph.database.MessagesDb
import de.koenidv.sph.database.UsersDb
import java.util.*

//  Created by koenidv on 10.02.2021.
data class Conversation(
        val convId: String, // Conversation id, like 33868
        val firstIdMess: String, // Id of the conversation's first message
        val subject: String, // Conversation's subject
        val recipientCount: Int, // Number of recipients according to sph's "private" property
        val answerType: String, // none/private/all, merged from different values from sph:
        // Papierkorb, noAnswerAllowed, privateOnly, groupOnly
        val originalSenderId: String,
        val date: Date, // The last message's date
        val unread: Boolean,
        val archived: Boolean = false,
        var firstMessage: Message? = null
) {
    companion object {
        const val ANSWER_TYPE_NONE = "none"
        const val ANSWER_TYPE_PRIVATE = "private"
        const val ANSWER_TYPE_ALL = "all"
    }

    /**
     * Get the main conversation partner name and the amount of other recipients
     */
    fun getInfo(): Pair<String, Int> {
        if (firstMessage == null) firstMessage = MessagesDb.getMessage(firstIdMess)

        val ownId = applicationContext()
                .getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
                .getString("userid", "")

        val partner = UsersDb.getName(
                if (originalSenderId == ownId) {
                    firstMessage?.recipients?.getOrNull(0).toString()
                } else {
                    originalSenderId
                })
        val recipientsCount: Int = recipientCount - 1

        return Pair(partner, recipientsCount)

    }
}
