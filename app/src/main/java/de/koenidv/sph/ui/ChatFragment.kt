package de.koenidv.sph.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.adapters.ChatAdapter
import de.koenidv.sph.database.ConversationsDb
import de.koenidv.sph.database.MessagesDb

// Created by koenidv on 18.12.2020.
class ChatFragment : Fragment() {

    private lateinit var conversationId: String

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)

        // Get passed course id argument
        conversationId = arguments?.getString("conversationId") ?: ""

        val messagesRecycler = view.findViewById<RecyclerView>(R.id.messagesRecycler)

        val conversation = ConversationsDb().get(conversationId, false)
        val messages = MessagesDb().getConversation(conversationId)

        // Display messages
        val adapter = ChatAdapter(messages, conversation!!)
        messagesRecycler.adapter = adapter

        // Set action bar title
        (activity as AppCompatActivity).supportActionBar?.title = conversation.subject

        return view
    }


}