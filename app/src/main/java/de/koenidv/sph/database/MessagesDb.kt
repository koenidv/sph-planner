package de.koenidv.sph.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import de.koenidv.sph.objects.Message

//  Created by koenidv on 09.01.2021.
class MessagesDb {

    var writable: SQLiteDatabase = DatabaseHelper.getInstance().writableDatabase

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
        cv.put("recipients", message.recipients.toString())
        cv.put("recipientsCount", message.recipientCount)

        writable.replace("messages", null, cv)
    }


}