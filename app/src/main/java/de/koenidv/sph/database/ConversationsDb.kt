package de.koenidv.sph.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
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
        cv.put("date", conversation.date.time / 1000)
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
        val conversation = toConversation(cursor, withFirstMessage)
        cursor.close()
        return conversation
    }

    /**
     * Get all conversations that are (not) archived
     */
    fun getAll(archived: Boolean = false) =
            toConversationList(writable.rawQuery(
                    "SELECT * FROM conversations WHERE archived=${if (archived) 1 else 0}",
                    null), true)

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
     * Sets the last date
     * Should be replaced with just a left join, but sph's data makes that quite hard
     */
    fun setDate(convId: String, date: Date) {
        writable.execSQL("UPDATE conversations SET date = " +
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
     * Get a conversation from a cursor pointing at such
     */
    private fun toConversation(cursor: Cursor, withFirstMessage: Boolean): Conversation {
        return Conversation(
                cursor.getString(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getInt(3),
                cursor.getString(4),
                cursor.getString(5),
                Date(cursor.getInt(6) * 1000L),
                cursor.getInt(6) == 1,
                cursor.getInt(7) == 1
        ).apply {
            if (withFirstMessage)
                firstMessage = MessagesDb().getMessage(this.firstIdMess)
        }
    }

    /**
     * Get a list of conversations from the cursor and close it
     */
    private fun toConversationList(cursor: Cursor, withFirstMessage: Boolean = false): List<Conversation> {
        val returnList = mutableListOf<Conversation>()

        if (!cursor.moveToFirst()) return returnList

        do {
            returnList.add(toConversation(cursor, withFirstMessage))
        } while (cursor.moveToNext())

        cursor.close()
        return returnList

    }

}