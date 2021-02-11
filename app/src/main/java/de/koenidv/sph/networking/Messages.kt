package de.koenidv.sph.networking

import android.util.Log
import androidx.core.os.bundleOf
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.database.ConversationsDb
import de.koenidv.sph.debugging.DebugLog
import de.koenidv.sph.debugging.Debugger
import de.koenidv.sph.objects.Conversation
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
                                var dbUnread: Boolean?
                                var sphUnread: Boolean
                                var firstMessageId: String
                                var answerType: String
                                var origSenderId: String
                                var isArchived: Boolean

                                // List of firstMessageIds to load all messages for
                                val loadMessagesList = mutableListOf<String>()

                                // Process each message header
                                for (header in JsonParser.parseString(headers).asJsonArray) {
                                    data = header.asJsonObject

                                    conversationId = data.get("Id").asString
                                    firstMessageId = data.get("Uniquid").asString
                                    dbUnread = conversations.isUnread(conversationId)
                                    sphUnread = data.get("unread").asBoolean
                                            || data.get("ungelesen").asBoolean
                                    origSenderId = data.get("Sender").asString
                                    isArchived = if (archived)
                                        data.get("Papierkorb").asString == "ja" else false
                                    // todo add user if new and not self

                                    // If the conversation does not yet exist, create it,
                                    // and load all messages from them
                                    if (dbUnread == null) {
                                        // Check if we could answer to this coonversation
                                        answerType = when {
                                            data.get("noanswer").asBoolean ||
                                                    data.get("noAnswerAllowed").asBoolean ->
                                                Conversation.ANSWER_TYPE_NONE
                                            data.get("privateAnswerOnly").asString == "ja" ->
                                                Conversation.ANSWER_TYPE_PRIVATE
                                            data.get("groupOnly").asString == "ja" ->
                                                Conversation.ANSWER_TYPE_ALL
                                            else -> Conversation.ANSWER_TYPE_ALL
                                        }

                                        // Save this conversation
                                        conversations.save(Conversation(
                                                conversationId,
                                                firstMessageId,
                                                data.get("Betreff").asString,
                                                data.get("private").asInt,
                                                answerType,
                                                origSenderId,
                                                sphUnread,
                                                isArchived
                                        ))

                                        // Load this conversation, if not only headers
                                        if (!onlyHeaders) loadMessagesList.add(firstMessageId)

                                    } else if (dbUnread != sphUnread || forceRefresh) {
                                        // If the read status of this conversation has changed,
                                        // or if force refresh is enabled, update this conversation
                                        // Values other than unread should not have changed,
                                        // so we'll just have to update that
                                        conversations.setUnread(conversationId, sphUnread)

                                        // Load this conversation, if not only headers
                                        if (!onlyHeaders) loadMessagesList.add(firstMessageId)
                                    }

                                }

                                // If no messages should be loaded, callback, else load them
                                if (loadMessagesList.isEmpty()) {
                                    callback(NetworkManager.SUCCESS)
                                } else {
                                    var index = 0
                                    val callbackIfLast: (Int) -> Unit = {
                                        index++
                                        if (index >= loadMessagesList.size ||
                                                it != NetworkManager.SUCCESS) {
                                            // If this was the last conversation, callback
                                            // If an error occurred, callback regardless
                                            callback(it)
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
            Log.e(SphPlanner.TAG, "Fetching messages failed")
            Log.e(SphPlanner.TAG, e.stackTraceToString())
            FirebaseCrashlytics.getInstance().recordException(e)
            // Log error
            if (Debugger.DEBUGGING_ENABLED)
                DebugLog("Messages", "Error fetching messages",
                        bundleOf("exception" to e.stackTraceToString()),
                        Debugger.LOG_TYPE_ERROR).log()
            // Still continue with a success,
            // sph's messages page is just too unreliable
            // and this data not critical
            if (FirebaseRemoteConfig.getInstance()
                            .getBoolean("messages_mandatory")) {
                callback(NetworkManager.SUCCESS)
            } else {
                callback(NetworkManager.FAILED_UNKNOWN)
            }
            return
        }
    }

    /**
     * Fetch and save all messages for a conversation
     * SPH identifies the conversation by it's first message id
     */
    fun fetchConversation(firstMessageId: String,
                          cryption: Cryption,
                          callback: (success: Int) -> Unit) {
        callback(NetworkManager.SUCCESS)
        // Post to sph to get messages data
        /*NetworkManager().postJsonAuthed(SphPlanner.applicationContext().getString(R.string.url_messages),
                body = mapOf("a" to "read", "uniqid" to firstMessageId)) { netSuccess, json ->
            if (netSuccess == NetworkManager.SUCCESS && json != null) {

            } else {
            log
        }*/
    }


}