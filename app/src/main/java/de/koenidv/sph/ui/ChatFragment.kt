package de.koenidv.sph.ui

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import de.koenidv.sph.R
import de.koenidv.sph.adapters.ChatAdapter
import de.koenidv.sph.database.ConversationsDb
import de.koenidv.sph.database.MessagesDb
import de.koenidv.sph.objects.Conversation
import de.koenidv.sph.parsing.EmojiExcludeFilter

// Created by koenidv on 18.12.2020.
class ChatFragment : Fragment() {

    private lateinit var conversationId: String

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
        val messages = MessagesDb.getConversation(conversationId)

        // Display messages
        val info = conversation!!.getInfo()
        val adapter = ChatAdapter(messages, info)
        messagesRecycler.adapter = adapter

        /*
         * Input
         */

        val inputContainer = view.findViewById<TextInputLayout>(R.id.textInputLayout)
        val input = view.findViewById<EditText>(R.id.messageEditText)
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

            inputContainer.setEndIconOnClickListener {
                Toast.makeText(this.context, input.text, Toast.LENGTH_LONG).show()
            }

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

}