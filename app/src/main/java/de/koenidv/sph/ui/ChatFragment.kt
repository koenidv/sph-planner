package de.koenidv.sph.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import de.koenidv.sph.R
import de.koenidv.sph.adapters.ChatAdapter
import de.koenidv.sph.database.ConversationsDb
import de.koenidv.sph.database.MessagesDb
import de.koenidv.sph.networking.Messages
import de.koenidv.sph.networking.NetworkManager
import de.koenidv.sph.networking.TokenManager
import de.koenidv.sph.objects.Conversation
import de.koenidv.sph.objects.Message
import de.koenidv.sph.parsing.EmojiExcludeFilter
import java.util.*

// Created by koenidv on 18.12.2020.
class ChatFragment : Fragment() {

    private lateinit var conversationId: String
    private lateinit var adapter: ChatAdapter
    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)

        // Get passed course id argument
        conversationId = arguments?.getString("conversationId") ?: ""
        val conversation = ConversationsDb().get(conversationId, false)

        // Set action bar title
        (activity as AppCompatActivity).supportActionBar?.title = conversation?.subject

        /*
         * Messages
         */

        val messagesRecycler = view.findViewById<RecyclerView>(R.id.messagesRecycler)
        val messages = MessagesDb.getConversation(conversationId).toMutableList()

        // Display messages
        val info = conversation!!.getPartner()
        adapter = ChatAdapter(messages, info)
        messagesRecycler.adapter = adapter
        layoutManager = messagesRecycler.layoutManager as LinearLayoutManager

        /*
         * Input
         */

        val inputContainer = view.findViewById<TextInputLayout>(R.id.textInputLayout)
        val input = view.findViewById<EditText>(R.id.messageEditText)
        val sending = view.findViewById<ProgressBar>(R.id.messageSendingProgress)
        inputContainer.isEndIconVisible = false

        // Disable replying if answertype is none
        if (conversation.answerType == Conversation.ANSWER_TYPE_NONE) {
            inputContainer.visibility = View.GONE
            view.findViewById<TextView>(R.id.repliesDisabledTextView).visibility = View.VISIBLE
        } else {

            // If this is a private conversation, reflect it in the hint
            inputContainer.hint = if (info.second == 0 ||
                    conversation.answerType == Conversation.ANSWER_TYPE_PRIVATE)
                getString(R.string.messages_reply_private).replace("%name", info.first)
            else getString(R.string.messages_reply_all)

            // Show / Hide send button if there is any input
            input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    // Enable end icon if input is not empty
                    inputContainer.isEndIconVisible = !(s == null || s.trim().isEmpty())
                }

            })

            var endiconClick: (View) -> Unit = {}
            endiconClick = {
                val text = input.text.toString()

                // Clear input and disable it until the message was successfully sent
                input.setText("")
                input.isEnabled = false
                sending.visibility = View.VISIBLE
                inputContainer.hint = getString(R.string.messages_new_sending)

                // Display virtual message
                messages.add(Message(
                        "", "",
                        TokenManager.userid, Message.SENDER_TYPE_STUDENT, "",
                        Date(), conversation.subject, text, listOf(), conversation.recipientCount,
                        false
                ))
                adapter.notifyItemInserted(messages.size)

                // Send the message
                Messages().sendReply(conversation.firstIdMess,
                        conversation.answerType,
                        text, "all",
                        conversation) { success ->

                    if (success == NetworkManager.SUCCESS) {
                        // Sending the reply was successful, enable input
                        input.isEnabled = true
                        sending.visibility = View.GONE

                        // Re-set input hint
                        inputContainer.hint = if (info.second == 0 ||
                                conversation.answerType == Conversation.ANSWER_TYPE_PRIVATE)
                            getString(R.string.messages_reply_private).replace("%name", info.first)
                        else getString(R.string.messages_reply_all)

                    } else {
                        // An error occurred

                        // Remove the message
                        messages.clear()
                        adapter.notifyItemRemoved(0)

                        // Re-set text
                        input.setText(text)
                        input.isEnabled = true
                        sending.visibility = View.GONE

                        // Show a snackbar
                        Snackbar.make(input, when (success) {
                            NetworkManager.FAILED_NO_NETWORK -> getString(R.string.error_offline)
                            NetworkManager.FAILED_SERVER_ERROR -> getString(R.string.error_server)
                            NetworkManager.FAILED_MAINTENANCE -> getString(R.string.error_maintenance)
                            else -> getString(R.string.error) + ": $success"
                        }, Snackbar.LENGTH_LONG)
                                .setAnchorView(R.id.nav_view)
                                .setAction(R.string.retry) { endiconClick(it) }
                                .show()
                    }
                }
            }
            inputContainer.setEndIconOnClickListener(endiconClick)

        }

        // Exclude emoji, sph doesn't support them
        input.filters = arrayOf<InputFilter>(EmojiExcludeFilter())

        return view
    }

    /*
    * Close soft keyboard on stop
    */
    override fun onStop() {
        val imm = requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
        super.onStop()
    }

    // Update conversations on uichange broadcast
    private val uichangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // If messages content was updated for this conversation
            if (intent.getStringExtra("content") == "messages" &&
                    intent.getStringExtra("type") == "contentchanged" &&
                    intent.getStringExtra("id") == conversationId &&
                    ::adapter.isInitialized) {

                // Replace existing messages with the new ones
                adapter.messages.clear()
                adapter.messages.addAll(MessagesDb.getConversation(conversationId))
                adapter.notifyDataSetChanged()

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Register to receive messages.
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(uichangeReceiver,
                IntentFilter("uichange"))
    }

    override fun onDestroy() {
        // Unregister broadcast receiver
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(uichangeReceiver)
        super.onDestroy()
    }

}