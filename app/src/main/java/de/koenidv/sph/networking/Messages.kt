package de.koenidv.sph.networking

import android.util.Log
import androidx.core.os.bundleOf
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.SphPlanner.Companion.TAG
import de.koenidv.sph.database.ConversationsDb
import de.koenidv.sph.database.MessagesDb
import de.koenidv.sph.database.UsersDb
import de.koenidv.sph.debugging.DebugLog
import de.koenidv.sph.debugging.Debugger
import de.koenidv.sph.objects.Conversation
import de.koenidv.sph.objects.Message
import org.jsoup.nodes.Entities
import java.text.SimpleDateFormat
import java.util.*

//  Created by koenidv on 31.01.2021.
class Messages {

    /**
     * Check if the number of visible messages has increased
     * @param onlyHeaders Load only conversations, no content
     * @param archived Also load messages that are marked as invisible in sph
     * @param forceRefresh Refresh all conversations, even if they seem to not have changed
     */
    fun fetch(onlyHeaders: Boolean = false,
              archived: Boolean = false,
              forceRefresh: Boolean = false,
              callback: (success: Int) -> Unit) {

        // Log fetching messages
        if (Debugger.DEBUGGING_ENABLED)
            DebugLog("Messages", "Fetching messages").log()

        val typeBody = if (!archived) "visibleOnly" else "All"

        try {
            // Get a decryptor
            // We need to complete the rsa handshake before requesting messages
            Cryption.start { success, cryption ->

                if (success != NetworkManager.SUCCESS || cryption == null) {
                    // Return if network manager could not be started
                    // Log error
                    if (Debugger.DEBUGGING_ENABLED)
                        DebugLog("Messages", "Could not start Cryption",
                                bundleOf("success" to success),
                                Debugger.LOG_TYPE_ERROR).log()
                    callback(success)
                    return@start
                }

                // Now post messages.php with a few parameters
                // a=headers - Titles only (read for entire message)
                // getType=visibleOnly - Only get visible messages (could also be unvisibleOnly)
                // last=0 - Not yet sure what that does, but it is needed to not get an error
                NetworkManager().postJsonAuthed(SphPlanner.applicationContext().getString(R.string.url_messages),
                        body = mapOf("a" to "headers", "getType" to typeBody, "last" to "0")) { netSuccess, json ->
                    if (netSuccess == NetworkManager.SUCCESS && json != null) {
                        // The response should be a json object with two values:
                        // total - The number of messages matching our request
                        // rows - The encrypted headers for each message

                        cryption.decrypt(json.get("rows").toString()) { headers ->
                            if (headers != null) {
                                val conversations = ConversationsDb()

                                var data: JsonObject
                                var conversationId: String
                                var sphUnread: Boolean
                                var firstMessageId: String
                                var answerType: String
                                var origSenderId: String
                                var isArchived: Boolean
                                var date: Date

                                val dateformat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT)

                                // List of conversations to load all messages for
                                val loadMessagesList = mutableListOf<Conversation>()

                                // Process each message header
                                for (header in JsonParser.parseString(headers).asJsonArray) {
                                    data = header.asJsonObject

                                    conversationId = data.get("Id").asString
                                    firstMessageId = data.get("Uniquid").asString
                                    sphUnread = data.get("unread").asBoolean
                                            || data.get("ungelesen").asBoolean
                                    origSenderId = data.get("Sender").asString
                                    isArchived = if (archived)
                                        data.get("Papierkorb").asString == "ja" else false

                                    date = dateformat.parse(data.get("Datum").asString)!!

                                    // Check if we could answer to this coonversation
                                    answerType = when {
                                        data.get("noanswer").asBoolean ||
                                                data.get("noAnswerAllowed").asBoolean ||
                                                data.get("Papierkorb").asString == "ja" ->
                                            Conversation.ANSWER_TYPE_NONE
                                        data.get("privateAnswerOnly").asString == "ja" ->
                                            Conversation.ANSWER_TYPE_PRIVATE
                                        data.get("groupOnly").asString == "ja" ->
                                            Conversation.ANSWER_TYPE_ALL
                                        else -> Conversation.ANSWER_TYPE_ALL
                                    }

                                    // Add user if not known yet
                                    if (!UsersDb.exists(data.get("Sender").asString)
                                            && data.get("SenderArt").asString == "Betreuer") {
                                        Users().addTeacherFromMessage(
                                                data.get("Sender").asString,
                                                data.get("username").asString
                                        )
                                    }

                                    var conversation = conversations.get(conversationId, false)

                                    // If the conversation does not yet exist, create it,
                                    // and load all messages from it
                                    if (conversation == null) {

                                        // Save this conversation
                                        conversation = Conversation(
                                                conversationId,
                                                firstMessageId,
                                                data.get("Betreff").asString,
                                                data.get("private").asInt,
                                                answerType,
                                                origSenderId,
                                                date,
                                                sphUnread,
                                                isArchived
                                        )
                                        conversations.save(conversation)

                                        // Load this conversation, if not only headers
                                        if (!onlyHeaders) loadMessagesList.add(conversation)

                                    } else if (conversation.date != date ||
                                            conversation.answerType != answerType ||
                                            forceRefresh) {
                                        // If the read status of this conversation has changed,
                                        // or if force refresh is enabled, update this conversation
                                        // Values other than unread, date and answertype
                                        // should not have changed, so we'll just have to update that
                                        conversations.setDate(conversationId, date)
                                        conversations.setUnread(conversationId, sphUnread)
                                        conversations.setAnswertype(conversationId, answerType)

                                        // Load this conversation, if not only headers
                                        if (!onlyHeaders) loadMessagesList.add(conversation)
                                    }

                                }

                                // If no messages should be loaded, callback, else load them
                                if (loadMessagesList.isEmpty()) {
                                    callback(NetworkManager.SUCCESS)
                                    cryption.stop()
                                } else {
                                    var index = 0
                                    val callbackIfLast: (Int) -> Unit = {
                                        index++
                                        if (index >= loadMessagesList.size ||
                                                it != NetworkManager.SUCCESS) {
                                            // If this was the last conversation, callback
                                            // If an error occurred, callback regardless
                                            callback(it)
                                            // Stop the js vm
                                            cryption.stop()
                                        }
                                    }
                                    // Load each conversation
                                    for (messages in loadMessagesList) {
                                        fetchConversation(messages, cryption, callbackIfLast)
                                    }
                                }
                            } else {
                                // For some reason the decrypted data is null
                                callback(NetworkManager.FAILED_UNKNOWN)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // If messages fetching failed, log and return
            Log.e(TAG, "Fetching messages failed")
            Log.e(TAG, e.stackTraceToString())
            FirebaseCrashlytics.getInstance().recordException(e)
            // Log error
            if (Debugger.DEBUGGING_ENABLED)
                DebugLog("Messages", "Error fetching messages",
                        bundleOf("exception" to e.stackTraceToString()),
                        Debugger.LOG_TYPE_ERROR).log()

            // Call back with unknown error
            callback(NetworkManager.FAILED_UNKNOWN)
        }
    }

    /**
     * Fetch and save all messages for a conversation
     * SPH identifies the conversation by it's first message id
     */
    fun fetchConversation(conversation: Conversation,
                          cryption: Cryption,
                          callback: (success: Int) -> Unit) {
        // We need to encrypt the conversation's first message id for sph
        cryption.encrypt(conversation.firstIdMess) { firstMessageId ->
            if (firstMessageId != null) {
                // Post to sph to get messages data
                NetworkManager().postJsonAuthed(SphPlanner.applicationContext().getString(R.string.url_messages),
                        body = mapOf("a" to "read", "uniqid" to firstMessageId)) { netSuccess, json ->
                    if (netSuccess == NetworkManager.SUCCESS && json != null) {
                        // If net request was successfull, decrypt the message
                        // Well messages, but it's one with replies
                        cryption.decrypt(json.get("message").toString()) {
                            // Parse decrypted data
                            val data = JsonParser.parseString(it).asJsonObject
                            // Disallow replies if the first message was trashed
                            if (data.get("Papierkorb").asString == "ja"
                                    && data.get("Sender").asString != json.getString("userId")
                                    && conversation.answerType != Conversation.ANSWER_TYPE_NONE) {
                                // Update answertype in db
                                ConversationsDb().setAnswertype(conversation.convId, Conversation.ANSWER_TYPE_NONE)
                            }
                            // Save the message with all its replies
                            saveMessage(data, conversation)

                            callback(NetworkManager.SUCCESS)
                        }
                    } else {
                        Log.d("$TAG msg", "Msgs is ${json.toString()}")
                        callback(if (netSuccess != NetworkManager.SUCCESS)
                            netSuccess else NetworkManager.FAILED_UNKNOWN)
                    }
                }
            } else callback(NetworkManager.FAILED_UNKNOWN)
        }
    }

    /**
     * Saves a message and all its replies
     */
    fun saveMessage(msg: JsonObject, conv: Conversation, messages: MessagesDb = MessagesDb()) {
        val dateformat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT)

        // Add each recipient from "empf"
        val recipients = mutableListOf<String>()
        var recipText: String
        if (msg.get("empf") is JsonArray) {
            try {
                var userid: String?
                val users = Users()
                for (recipient in msg.getAsJsonArray("empf")) {
                    // Try to get a users id, else add their name
                    recipText = recipient.asString
                            .substringAfter("/i> ")
                            .substringBefore("<")

                    userid = users.getTeacherUserId(recipText)
                    if (userid != null) recipients.add(userid)
                    else recipients.add(recipText)
                }
            } catch (e: java.lang.Exception) {
                Log.e(TAG, e.stackTraceToString())
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }

        val message = Message(
                messId = msg.get("Uniquid").asString,
                idConv = conv.convId,
                idSender = msg.get("Sender").asString,
                senderType = msg.get("SenderArt").asString,
                senderName = msg.get("username").asString,
                date = dateformat.parse(msg.get("Datum").asString)!!,
                subject = Entities.unescape(msg.get("Betreff").asString.trim()),
                content = Entities.unescape(
                        msg.get("Inhalt").asString.replace("<br />", "").trim()
                ),
                recipients = recipients,
                recipientCount = msg.get("private").asInt,
                unread = msg.get("ungelesen").asBoolean
        )

        messages.save(message)

        // Save each reply
        for (reply in msg.getAsJsonArray("reply")) {
            saveMessage(reply.asJsonObject, conv, messages)
        }

    }


}