package de.koenidv.sph.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import de.koenidv.sph.objects.Message
import java.util.*

//  Created by koenidv on 09.01.2021.
object MessagesDb {

    private var writable: SQLiteDatabase = DatabaseHelper.getInstance().writableDatabase

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
        return cursor.getMessage(true)
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
     * Get a list of messages from the cursor and close it
     */
    private fun toMessagesList(cursor: Cursor): List<Message> {
        val returnList = mutableListOf<Message>()

        if (!cursor.moveToFirst()) return returnList

        do {
            returnList.add(cursor.getMessage())
        } while (cursor.moveToNext())

        cursor.close()
        return returnList

    }

    /**
     * Get a message from a cursor pointing at such
     */
    private fun Cursor.getMessage(close: Boolean = false): Message {
        val message = Message(
                messId = this.getString(0),
                idConv = this.getString(1),
                idSender = this.getString(2),
                senderName = this.getString(3),
                senderType = this.getString(4),
                date = Date(this.getInt(5) * 1000L),
                subject = this.getString(6),
                content = this.getString(7),
                unread = this.getInt(8) == 1,
                recipients = this.getString(9).split(";"),
                recipientCount = this.getInt(10),
        )
        if (close) this.close()
        return message
    }

}