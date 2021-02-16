package de.koenidv.sph.networking

import android.content.Intent
import android.util.Log
import androidx.core.os.bundleOf
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.SphPlanner.Companion.TAG
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import de.koenidv.sph.database.ConversationsDb
import de.koenidv.sph.database.MessagesDb
import de.koenidv.sph.database.UsersDb
import de.koenidv.sph.debugging.DebugLog
import de.koenidv.sph.debugging.Debugger
import de.koenidv.sph.objects.Conversation
import de.koenidv.sph.objects.Message
import org.json.JSONArray
import org.json.JSONObject
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
              currentCrypt: Cryption? = null,
              callback: (success: Int) -> Unit) {

        // Log fetching messages
        if (Debugger.DEBUGGING_ENABLED)
            DebugLog("Messages", "Fetching messages").log()

        val typeBody = if (!archived) "visibleOnly" else "All"

        try {
            // Get a decryptor
            // We need to complete the rsa handshake before requesting messages
            Cryption.start(currentCrypt) { success, cryption ->

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
                NetworkManager().postJsonAuthed(applicationContext().getString(R.string.url_messages),
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
                                var dateString: String
                                var date: Date

                                val dateformat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT)
                                val replaceformat = SimpleDateFormat("dd.MM.yyyy", Locale.ROOT)

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

                                    // Make relative dates absolute
                                    dateString = data.get("Datum").asString
                                            .replace("heute",
                                                    replaceformat.format(Date()))
                                            .replace("gestern",
                                                    replaceformat.format(
                                                            Date().time - 24 * 60 * 60000))
                                    // Parse date
                                    date = dateformat.parse(dateString)!!

                                    // Check if we could answer to this coonversation
                                    answerType = when {
                                        data.get("noanswer").asBoolean ||
                                                data.get("noAnswerAllowed").asString == "ja" ||
                                                data.get("Papierkorb").asString == "ja" ->
                                            Conversation.ANSWER_TYPE_NONE
                                        data.get("privateAnswerOnly").asString == "ja" ->
                                            Conversation.ANSWER_TYPE_PRIVATE
                                        data.get("groupOnly").asString == "ja" ->
                                            Conversation.ANSWER_TYPE_GROUP
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

                                        // Notify ui about the new conversation
                                        notifyFragments(conversationId, "new")

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

                                        // Notify ui about the changed conversation
                                        notifyFragments(conversationId, "metachanged")
                                    }

                                }

                                // If no messages should be loaded, callback, else load them
                                if (loadMessagesList.isEmpty()) {
                                    callback(NetworkManager.SUCCESS)
                                    cryption.stop()
                                    // Remember the time we updated this
                                    // Not updating this the first time,
                                    // but that shouldn't be an issue
                                    SphPlanner.prefs.edit().putLong("updated_messages",
                                            Date().time).apply()
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
    private fun fetchConversation(conversation: Conversation,
                                  cryption: Cryption,
                                  callback: (success: Int) -> Unit) {
        // We need to encrypt the conversation's first message id for sph
        cryption.encrypt(conversation.firstIdMess) { firstMessageId ->
            if (firstMessageId != null) {
                // Post to sph to get messages data
                NetworkManager().postJsonAuthed(applicationContext().getString(R.string.url_messages),
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
    private fun saveMessage(msg: JsonObject, conv: Conversation, messages: MessagesDb = MessagesDb) {
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

        // Make relative dates absolute
        val replaceformat = SimpleDateFormat("dd.MM.yyyy", Locale.ROOT)
        val dateString = msg.get("Datum").asString
                .replace("heute",
                        replaceformat.format(Date()))
                .replace("gestern",
                        replaceformat.format(
                                Date().time - 24 * 60 * 60000))

        val message = Message(
                messId = msg.get("Uniquid").asString,
                idConv = conv.convId,
                idSender = msg.get("Sender").asString,
                senderType = msg.get("SenderArt").asString,
                senderName = msg.get("username").asString,
                date = dateformat.parse(dateString)!!,
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


    /**
     * Update an existing conversation using its conversationId
     */
    fun updateConversation(conversationId: String?, callback: (Int) -> Unit) {
        if (conversationId != null) {
            // Get the corresponding conversation object
            val conversation = ConversationsDb().get(conversationId, false)
            if (conversation == null) {
                callback(NetworkManager.FAILED_UNKNOWN)
                return
            }
            // Start encryption vm
            Cryption.start { success, cryption ->
                if (success != NetworkManager.SUCCESS) {
                    callback(NetworkManager.FAILED_UNKNOWN)
                    cryption?.stop()
                    return@start
                }

                // Fetch the remote conversation
                fetchConversation(conversation, cryption!!) {
                    // Stop the vm and call back
                    cryption.stop()
                    callback(it)

                    // Notify ui about the changed conversation
                    notifyFragments(conversationId, "contentchanged")

                    // Remember the time we updated this
                    SphPlanner.prefs.edit().putLong(
                            "updated_messages_$conversationId", Date().time).apply()
                }
            }
        }
    }


    /**
     * Create a conversation by sending the first message
     */
    fun sendFirstMessage(recipientIds: List<String>, subject: String, message: String, callback: (Int, String?) -> Unit) {
        // Parse all recipients to a json object each
        val values = mutableListOf<JSONObject>()
        for (recip in recipientIds) {
            values.add(JSONObject(mapOf("name" to "to[]", "value" to recip)))
        }
        // Also add subject and message
        values.add(JSONObject(mapOf("name" to "subject", "value" to subject)))
        values.add(JSONObject(mapOf("name" to "text", "value" to message)))

        // Message content needs to be encrypted
        Cryption.start { cryptsuccess, cryption ->
            if (cryptsuccess != NetworkManager.SUCCESS) {
                callback(cryptsuccess, null)
                return@start
            }

            cryption!!.encrypt(JSONArray(values).toString()) { encrypted ->

                // Post this to sph
                TokenManager.authenticate {
                    NetworkManager().postJsonAuthed(
                            applicationContext().getString(R.string.url_messages),
                            mapOf("a" to "newmessage", "c" to encrypted!!)) { success, data ->

                        if (success != NetworkManager.SUCCESS) {
                            // Some network error
                            callback(success, null)
                            cryption.stop()
                            return@postJsonAuthed
                        }

                        if (data == null || !data.getBoolean("back")) {
                            // Possibly server error
                            callback(NetworkManager.FAILED_UNKNOWN, null)
                            cryption.stop()
                            return@postJsonAuthed
                        }

                        // Now refresh messages list
                        fetch(currentCrypt = cryption) {
                            callback(it, data.getString("id"))
                        }

                    }
                }
            }
        }

    }


    /**
     * Send a reply to a conversation
     */
    fun sendReply(firstMessageId: String,
                  answerType: String,
                  message: String,
                  recipient: String = "all",
                  conversation: Conversation? = null,
                  callback: (success: Int) -> Unit) {

        if ((answerType == Conversation.ANSWER_TYPE_GROUP && recipient != "all") ||
                firstMessageId == "" || message == "") {
            callback(NetworkManager.FAILED_UNKNOWN)
            return
        }

        // Assemble the needed JSONObject
        // For groupOnly and privateAnswerOnly, sph will only check existance and ignore the values
        val json = JSONObject(mapOf(
                "to" to recipient,
                "groupOnly" to if (answerType == Conversation.ANSWER_TYPE_GROUP) "ja" else "nein",
                "privateAnswerOnly" to if (answerType == Conversation.ANSWER_TYPE_PRIVATE) "ja" else "nein",
                "message" to message,
                "replyToMsg" to firstMessageId
        ))

        // Encrypt the message
        Cryption.start { cryptsuccess, cryption ->
            if (cryptsuccess != NetworkManager.SUCCESS || cryption == null) {
                callback(cryptsuccess)
                return@start
            }

            cryption.encrypt(json.toString()) { encrypted ->
                if (encrypted == null) {
                    callback(NetworkManager.FAILED_UNKNOWN)
                    return@encrypt
                }

                // Post the encrypted message to sph
                NetworkManager().postJsonAuthed(
                        applicationContext().getString(R.string.url_messages),
                        mapOf("a" to "reply", "c" to encrypted)) { success, data ->

                    if (success != NetworkManager.SUCCESS
                            || data == null || !data.getBoolean("back")) {
                        // Some network error
                        callback(
                                if (success == NetworkManager.SUCCESS) NetworkManager.FAILED_UNKNOWN
                                else success)
                        cryption.stop()
                        return@postJsonAuthed
                    }

                    // Post successful, now save the message locally
                    if (conversation != null) {
                        MessagesDb.save(Message(
                                data.getString("id"),
                                conversation.convId,
                                TokenManager.userid,
                                Message.SENDER_TYPE_STUDENT,
                                SphPlanner.prefs.getString("real_name", "")!!,
                                Date(), // A few seconds later than actual, shouldn't be an issue
                                conversation.subject,
                                message,
                                listOf(recipient), // Accurate recipients are only really required for the first msg
                                conversation.recipientCount,
                                false
                        ))
                    }

                    callback(NetworkManager.SUCCESS)

                }
            }
        }
    }


    /**
     * Send a local broadcast with the specified conversation id to update the ui
     * @param conversationId Id of the updated conversation
     * @param type should be "new", "metachanged" or "contentchanged", ui will be updated accordingly
     */
    private fun notifyFragments(conversationId: String, type: String) {
        val uiBroadcast = Intent("uichange")
                .putExtras(bundleOf(
                        "content" to "messages",
                        "id" to conversationId,
                        "type" to type
                ))
        LocalBroadcastManager.getInstance(applicationContext()).sendBroadcast(uiBroadcast)
    }


}