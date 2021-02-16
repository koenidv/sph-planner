package de.koenidv.sph.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import de.koenidv.sph.objects.Message
import java.util.*

//  Created by koenidv on 09.01.2021.
object MessagesDb {

    var writable: SQLiteDatabase = DatabaseHelper.getInstance().writableDatabase

    fun getMessage(messageId: String): Message? {
        val cursor: Cursor = writable.rawQuery(
                "SELECT * FROM messages WHERE message_id=\"$messageId\"",
                null)
        // If result is empty, return null
        if (!cursor.moveToFirst()) {
            cursor.close()
            return null
        }
        // Else return the queried conversation
        val conversation = toMessage(cursor)
        cursor.close()
        return conversation
    }

    fun getConversation(conversationId: String): List<Message> =
            toMessagesList(writable.rawQuery(
                    "SELECT * FROM messages WHERE id_conversation=\"$conversationId\"",
                    null))

    fun getConversationId(messageId: String): String? {
        val cursor: Cursor = writable.rawQuery(
                "SELECT id_conversation FROM messages WHERE message_id=\"$messageId\"",
                null)
        // If result is empty, return null
        if (!cursor.moveToFirst()) {
            cursor.close()
            return null
        }
        // Else return the queried conversation
        val id = cursor.getString(0)
        cursor.close()
        return id
    }

    /**
     * Saves/replaces a message
     */
    fun save(message: Message) {
        val cv = ContentValues()
        cv.put("message_id", message.messId)
        cv.put("id_conversation", message.idConv)
        cv.put("id_sender", message.idSender)
        cv.put("sendername", message.senderName)
        cv.put("sendertype", message.senderType)
        cv.put("date", message.date.time / 1000)
        cv.put("subject", message.subject)
        cv.put("content", message.content)
        cv.put("unread", if (message.unread) 1 else 0)
        cv.put("recipients", message.recipients?.joinToString(";"))
        cv.put("recipientsCount", message.recipientCount)

        writable.replace("messages", null, cv)
    }

    /**
     * Get a message from a cursor pointing at such
     */
    private fun toMessage(cursor: Cursor): Message {
        return Message(
                messId = cursor.getString(0),
                idConv = cursor.getString(1),
                idSender = cursor.getString(2),
                senderName = cursor.getString(3),
                senderType = cursor.getString(4),
                date = Date(cursor.getInt(5) * 1000L),
                subject = cursor.getString(6),
                content = cursor.getString(7),
                unread = cursor.getInt(8) == 1,
                recipients = cursor.getString(9).split(";"),
                recipientCount = cursor.getInt(10),
        )
    }

    /**
     * Get a list of messages from the cursor and close it
     */
    private fun toMessagesList(cursor: Cursor): List<Message> {
        val returnList = mutableListOf<Message>()

        if (!cursor.moveToFirst()) return returnList

        do {
            returnList.add(toMessage(cursor))
        } while (cursor.moveToNext())

        cursor.close()
        return returnList

    }

}