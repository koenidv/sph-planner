package de.koenidv.sph.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import de.koenidv.sph.adapters.ConversationsAdapter
import de.koenidv.sph.networking.TokenManager
import de.koenidv.sph.objects.Conversation
import java.util.*

//  Created by koenidv on 09.01.2021.
class ConversationsDb {

    var writable: SQLiteDatabase = DatabaseHelper.getInstance().writableDatabase

    /**
     * Saves/replaces a conversation
     */
    fun save(conversation: Conversation) {
        val cv = ContentValues()
        cv.put("conversation_id", conversation.convId)
        cv.put("first_id_message", conversation.firstIdMess)
        cv.put("subject", conversation.subject)
        cv.put("recipient_count", conversation.recipientCount)
        cv.put("answertype", conversation.answerType)
        cv.put("original_id_sender", conversation.originalSenderId)
        cv.put("lastdate", conversation.date.time / 1000)
        cv.put("unread", if (conversation.unread) 1 else 0)
        cv.put("archived", if (conversation.archived) 1 else 0)

        writable.replace("conversations", null, cv)
    }

    /**
     * Get a conversation by its id or null if it does not exist
     */
    fun get(convId: String, withFirstMessage: Boolean): Conversation? {
        val cursor: Cursor = writable.rawQuery(
                "SELECT * FROM conversations WHERE conversation_id=\"$convId\"",
                null)
        // If result is empty, return null
        if (!cursor.moveToFirst()) {
            cursor.close()
            return null
        }
        // Else return the queried conversation
        return cursor.getConversation(withFirstMessage, true)
    }

    /**
     * Get all conversations that are (not) archived
     */
    fun getAll(archived: Boolean = false) =
            toConversationList(writable.rawQuery(
                    "SELECT * FROM conversations WHERE archived=${if (archived) 1 else 0} ORDER BY date DESC",
                    null), true)

    /**
     * Gets a list of conversations as ConversationInfo for the conversations list
     */
    fun getConversationInfo(whereClause: String = "archived=0"):
            List<ConversationsAdapter.ConversationInfo> {
        val cursor = writable.rawQuery(
                "SELECT conversation_id, " +
                        "conversations.subject, conversations.recipient_count, " +
                        "original_id_sender, messages.recipients, conversations.lastdate, " +
                        "conversations.unread FROM conversations LEFT JOIN messages ON " +
                        "conversations.first_id_message = messages.message_id " +
                        "WHERE $whereClause ORDER BY date DESC", null)

        if (!cursor.moveToFirst()) {
            cursor.close()
            return listOf()
        }

        var isOwn: Boolean
        var partner: String
        var partnerCount: Int
        val returnList = mutableListOf<ConversationsAdapter.ConversationInfo>()

        do {
            isOwn = cursor.getString(3) == TokenManager.userid
            // If sender is self, use first recipient as partner, else original sender
            try {
                if (isOwn) {
                    partner = cursor.getString(4).substringBefore(";")
                    partnerCount = cursor.getInt(2)
                } else {
                    partner = cursor.getString(3)
                    partnerCount = cursor.getInt(2) - 1
                }
            } catch (npe: NullPointerException) {
                npe.printStackTrace()
                partner = "null"
                partnerCount = 0
            }

            returnList.add(ConversationsAdapter.ConversationInfo(
                    id = cursor.getString(0),
                    subject = cursor.getString(1),
                    partner = partner,
                    partnerCount = partnerCount,
                    date = Date(cursor.getInt(5) * 1000L),
                    isOwn = isOwn,
                    isUnread = cursor.getInt(6) == 1
            ))

        } while (cursor.moveToNext())

        cursor.close()
        return returnList

    }

    /**
     * Checks if any archived conversation exists
     */
    fun archivedExists(): Boolean {
        val cursor = writable.rawQuery(
                "SELECT conversation_id FROM conversations WHERE archived=1",
                null, null)
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    /**
     * Returns unread value for a conversation, or null if there is no such conversation
     */
    fun isUnread(convId: String): Boolean? {
        // Query message id
        val queryString = "SELECT unread FROM conversations WHERE conversation_id = \"$convId\""
        val cursor: Cursor = writable.rawQuery(queryString, null)
        // If result is empty, return null
        if (!cursor.moveToFirst()) {
            cursor.close()
            return null
        }
        // Else return the queried message id
        val unread = cursor.getInt(0) == 1
        cursor.close()
        return unread
    }

    /**
     * Returns the first message id for a given conversation id
     */
    fun getFirstMessageId(convId: String): String? {
        // Query message id
        val queryString = "SELECT first_id_message FROM conversations WHERE conversation_id=\"$convId\""
        val cursor: Cursor = writable.rawQuery(queryString, null)
        // If result is empty, return null
        if (!cursor.moveToFirst()) {
            cursor.close()
            return null
        }
        // Else return the queried message id
        val id = cursor.getString(0)
        cursor.close()
        return id
    }

    /**
     * Sets the last date
     * Should be replaced with just a left join, but sph's data makes that quite hard
     */
    fun setDate(convId: String, date: Date) {
        writable.execSQL("UPDATE conversations SET lastdate = " +
                date.time / 1000 + " WHERE conversation_id=\"$convId\"")
    }

    /**
     * Marks a conversation as unread or read
     */
    fun setUnread(convId: String, unread: Boolean) {
        writable.execSQL("UPDATE conversations SET unread = " +
                (if (unread) 1 else 0) + " WHERE conversation_id=\"$convId\"")
    }

    /**
     * Sets a conversation's answertype
     */
    fun setAnswertype(convId: String, answerType: String) {
        writable.execSQL("UPDATE conversations SET answertype=\"$answerType\" WHERE conversation_id=\"$convId\"")
    }

    /**
     * Sets a conversation's archived status
     */
    fun setArchived(convId: String, archived: Boolean) {
        writable.execSQL("UPDATE conversations SET archived=${if (archived) 1 else 0} WHERE conversation_id=\"$convId\"")
    }

    /**
     * Get a list of conversations from the cursor and close it
     */
    private fun toConversationList(cursor: Cursor, withFirstMessage: Boolean = false): List<Conversation> {
        val returnList = mutableListOf<Conversation>()

        if (!cursor.moveToFirst()) return returnList

        do {
            returnList.add(cursor.getConversation(withFirstMessage))
        } while (cursor.moveToNext())

        cursor.close()
        return returnList
    }

    /**
     * Get a conversation from a cursor pointing at such
     */
    private fun Cursor.getConversation(withFirstMessage: Boolean, close: Boolean = false): Conversation {
        val conversation = Conversation(
                this.getString(0),
                this.getString(1),
                this.getString(2),
                this.getInt(3),
                this.getString(4),
                this.getString(5),
                Date(this.getInt(6) * 1000L),
                this.getInt(7) == 1,
                this.getInt(8) == 1
        ).apply {
            if (withFirstMessage)
                firstMessage = MessagesDb.getMessage(this.firstIdMess)
            // todo use left join for first message
        }

        if (close) this.close()
        return conversation
    }

}