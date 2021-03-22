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
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import de.koenidv.sph.R
import de.koenidv.sph.adapters.ChatAdapter
import de.koenidv.sph.database.MessagesDb
import de.koenidv.sph.database.UsersDb
import de.koenidv.sph.networking.Messages
import de.koenidv.sph.networking.NetworkManager
import de.koenidv.sph.networking.TokenManager
import de.koenidv.sph.objects.Message
import de.koenidv.sph.parsing.EmojiExcludeFilter
import java.util.*


// Created by koenidv on 18.12.2020.
class NewChatFragment : Fragment() {


    private lateinit var subject: String
    private lateinit var recipients: List<String>

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)

        // Get passed course id argument
        subject = arguments?.getString("subject") ?: ""
        recipients = arguments?.getStringArrayList("recipients") ?: listOf()

        // Set action bar title
        (activity as AppCompatActivity).supportActionBar?.title = subject

        /*
         * Messages
         */

        val messagesRecycler = view.findViewById<RecyclerView>(R.id.messagesRecycler)
        val messages = mutableListOf<Message>()

        // Empty messages list, but with header
        val adapter = ChatAdapter(messages,
                Pair(UsersDb.getName(recipients.first()), recipients.size - 1))
        messagesRecycler.adapter = adapter

        /*
         * Input
         */

        val inputContainer = view.findViewById<TextInputLayout>(R.id.textInputLayout)
        val input = view.findViewById<EditText>(R.id.messageEditText)
        val sending = view.findViewById<ProgressBar>(R.id.messageSendingProgress)
        inputContainer.isEndIconVisible = false

        // Set correct hint
        inputContainer.hint = getString(R.string.messages_new_message)

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

            // Clear input and disable it; sending another message would start a new conversation
            input.setText("")
            input.isEnabled = false
            sending.visibility = View.VISIBLE
            inputContainer.hint = getString(R.string.messages_new_sending)

            // Display message with empty values
            messages.add(Message(
                    "", "",
                    TokenManager.userid, Message.SENDER_TYPE_STUDENT, "",
                    Date(), subject, text, recipients, recipients.size, false
            ))
            adapter.notifyItemInserted(messages.size)

            // Send the message
            Messages().sendFirstMessage(recipients, subject, text) { success, id ->
                activity?.runOnUiThread {
                    if (success == NetworkManager.SUCCESS && id != null) {
                        // If it was successful, switch to the regular chat fragment
                        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                                .navigate(R.id.switchToChatAction, bundleOf(
                                        "conversationId" to MessagesDb.getConversationId(id)
                                ))
                    } else {
                        // An error occurred

                        // Remove the message
                        messages.clear()
                        adapter.notifyItemRemoved(0)

                        // Re-set text
                        input.setText(text)
                        input.isEnabled = true
                        sending.visibility = View.GONE
                        inputContainer.hint = getString(R.string.messages_new_message)

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
        }
        inputContainer.setEndIconOnClickListener(endiconClick)


        // Exclude emoji, sph doesn't support them
        input.filters = arrayOf<InputFilter>(EmojiExcludeFilter())

        input.requestFocus()

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