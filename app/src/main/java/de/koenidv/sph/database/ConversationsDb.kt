package de.koenidv.sph.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import de.koenidv.sph.objects.Conversation

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
        cv.put("unread", if (conversation.unread) 1 else 0)
        cv.put("archived", if (conversation.archived) 1 else 0)

        writable.replace("conversations", null, cv)
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
     * Marks a conversation as unread or read
     */
    fun setUnread(convId: String, unread: Boolean) {
        writable.execSQL("UPDATE conversations SET unread = " +
                (if (unread) 1 else 0) + " WHERE conversation_id=\"$convId\"")
    }


}