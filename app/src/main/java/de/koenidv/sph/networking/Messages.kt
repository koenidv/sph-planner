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
     */
    fun fetch(markAsRead: Boolean = false, onlyHeaders: Boolean = false, callback: (success: Int) -> Unit) {

        /*
         * We currently cannot get messages, decryption isn't implemented yet.
         * What we can do is get the number of visible messages and check
         * if it is larger than last time we checked.
         * The user will then be directed to sph in their browser,
         * where decryption works.
         * This will require them to sign in again, should they opt
         * not to use the AutoSPH signin service.
         */

        // Log fetching messages
        if (Debugger.DEBUGGING_ENABLED)
            DebugLog("Messages", "Fetching messages").log()

        // Now post messages.php with a few parameters
        // a=headers - Titles only (read for entire message)
        // getType=visibleOnly - Only get visible messages (could also be unvisibleOnly)
        // last=0 - Not yet sure what that does, but it is needed to not get an error
        NetworkManager().postJsonAuthed(SphPlanner.applicationContext().getString(R.string.url_messages),
                body = mapOf("a" to "headers", "getType" to "visibleOnly", "last" to "0")) { netSuccess, json ->
            if (netSuccess == NetworkManager.SUCCESS && json != null) {
                // The response should be a json object with two values:
                // total - The number of messages matching our request
                // rows - The encrypted headers for each message

                try {
                    // Get a decryptor
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

                        cryption.decrypt(json.get("rows").toString()) { headers ->
                            if (headers != null) {
                                val conversations = ConversationsDb()

                                var data: JsonObject
                                var conversationId: String
                                var dbLastMessageId: String?
                                var lastMessageId: String
                                var answerType: String

                                // Process each message header
                                for (header in JsonParser.parseString(headers).asJsonArray) {
                                    data = header.asJsonObject

                                    conversationId = data.get("Id").asString
                                    lastMessageId = data.get("Uniquid").asString
                                    dbLastMessageId = conversations.lastMessage(conversationId)

                                    // If the conversation does not yet exist, create it,
                                    // and load all messages from them
                                    if (dbLastMessageId == null) {
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

                                        // Save this conversation
                                        conversations.save(Conversation(
                                                conversationId,
                                                lastMessageId,
                                                data.get("Betreff").asString,
                                                data.get("private").asInt,
                                                answerType
                                        ))

                                        // Load this conversation
                                        if (!onlyHeaders) {
                                            fetchConversation(conversationId, cryption)
                                        }

                                        // TODO HEADERS DO NOT CONTAIN A CONVERSATION'S LAST MESSAGE; BUT IT'S FIRST
                                    }

                                }
                            } else {
                                // For some reason the decrypted data is null
                                callback(NetworkManager.FAILED_UNKNOWN)
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
                    return@postJsonAuthed
                }
            }
        }
    }

    /**
     * Fetch and save all messages for a conversation
     * SPH identifies the conversation by it's first message id
     */
    fun fetchConversation(conversationId: String, cryption: Cryption) {
        // Post to sph to get messages data
        NetworkManager().postJsonAuthed(SphPlanner.applicationContext().getString(R.string.url_messages),
                body = mapOf("a" to "read", "uniqid" to "visibleOnly", "last" to "0")) { netSuccess, json ->
            if (netSuccess == NetworkManager.SUCCESS && json != null) {
            }
        }
    }


}