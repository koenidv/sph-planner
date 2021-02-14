package de.koenidv.sph.ui

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import de.koenidv.sph.R
import de.koenidv.sph.adapters.ChatAdapter
import de.koenidv.sph.database.UsersDb
import de.koenidv.sph.objects.Message


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

        inputContainer.setEndIconOnClickListener {
            Toast.makeText(this.context, input.text, Toast.LENGTH_LONG).show()
        }

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