package de.koenidv.sph.database

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import de.koenidv.sph.objects.Conversation

//  Created by koenidv on 09.01.2021.
class ConversationsDb {

    var writable: SQLiteDatabase = DatabaseHelper.getInstance().writableDatabase

    /**
     * Dummy
     * Saves a conversation
     */
    fun save(conversation: Conversation) {

    }

    /**
     * Dummy
     * Returns the last message id within a conversation, or null,
     * if there is no such conversation
     */
    fun lastMessage(convId: String): String? {
        // Query message id
        val queryString = "SELECT last_id_message FROM conversations WHERE conversation_id = \"$convId\""
        val cursor: Cursor = writable.rawQuery(queryString, null)
        // If result is empty, return null
        if (!cursor.moveToFirst()) {
            cursor.close()
            return null
        }
        // Else return the queried message id
        val messId = cursor.getString(0)
        cursor.close()
        return messId
    }

}