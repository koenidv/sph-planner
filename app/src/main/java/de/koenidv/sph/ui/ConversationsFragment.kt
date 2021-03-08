package de.koenidv.sph.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import de.koenidv.sph.MainActivity
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.adapters.ConversationsAdapter
import de.koenidv.sph.database.ConversationsDb
import de.koenidv.sph.networking.Messages

// Created by koenidv on 18.12.2020.
class ConversationsFragment : Fragment() {

    private lateinit var adapter: ConversationsAdapter
    private lateinit var layoutManager: LinearLayoutManager
    lateinit var emptyLayout: LinearLayout

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_conversations, container, false)
        val conversationsDb = ConversationsDb()

        SphPlanner.openInBrowserUrl = getString(R.string.url_messages)

        val conversationsRecycler = view.findViewById<RecyclerView>(R.id.conversationsRecycler)
        val fab = view.findViewById<ExtendedFloatingActionButton>(R.id.newConversationFab)

        var recyclerEditMode = false
        val conversations = conversationsDb.getConversationInfo().toMutableList()

        // Display conversations
        adapter = ConversationsAdapter(conversations, requireActivity()) { selectMode ->
            recyclerEditMode = selectMode
            fab.setText(
                    if (selectMode) R.string.messages_archive_conversations
                    else R.string.messages_new_button)
            fab.setIconResource(
                    if (selectMode) R.drawable.ic_archive
                    else R.drawable.ic_edit
            )
            fab.extend()
        }
        conversationsRecycler.adapter = adapter
        layoutManager = conversationsRecycler.layoutManager as LinearLayoutManager

        // Extend / Shrink fab on scroll
        conversationsRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                // Don't shrink if in edit mode
                if (!recyclerEditMode) {
                    if (dy > 0 && fab.isExtended) {
                        fab.shrink()
                    } else if (dy < 0 && !fab.isExtended) {
                        fab.extend()
                    }
                }
            }
        })

        // Button for creating a new conversation or archiving selected conversations
        fab.setOnClickListener {
            if (!recyclerEditMode) {
                // If not in edit mode, start a new conversations
                newConversation()
            } else {
                // Archive selected conversations
                var index: Int
                val selected = adapter.getSelected()
                val messages = Messages()
                adapter.clearSelected()
                for (conversation in selected) {
                    // Mark this conversation as archived in db and on sph
                    val firstMsgId = conversationsDb.getFirstMessageId(conversation.id)
                    if (firstMsgId != null)
                        messages.setArchived(conversation.id, firstMsgId, true)
                    // Remove this item from the recycler view
                    index = conversations.indexOf(conversation)
                    if (index != -1) {
                        conversations.removeAt(index)
                        adapter.notifyItemRemoved(index)
                    }

                    // If there are no conversations left, show emptyConversationsLayout
                    if (adapter.conversations.isEmpty()) emptyLayout.visibility = View.VISIBLE
                }
            }
        }

        // If there are no conversations to display, show a text explaining that
        emptyLayout = view.findViewById(R.id.noConversationsLayout)
        val newButton = view.findViewById<MaterialButton>(R.id.noConversationsNewButton)
        val refreshButton = view.findViewById<MaterialButton>(R.id.noConversationsRefreshButton)

        newButton.setOnClickListener { newConversation() }
        refreshButton.setOnClickListener {
            // Set ptr as refreshing
            try {
                (requireActivity() as MainActivity).swipeRefresh.isRefreshing = true
                Messages().fetch(forceRefresh = true, archived = true) {
                    // Sometimes this is not running on ui and would crash
                    activity?.runOnUiThread {
                        // Set ptr not refreshing
                        (activity as MainActivity).swipeRefresh.isRefreshing = false
                    }
                }
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }

        if (conversations.isEmpty()) {
            emptyLayout.visibility = View.VISIBLE
        }



        return view
    }

    private fun newConversation() {
        // Show a bottom sheet to start a new conversation
        NewConversationSheet { subject, recipients ->
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                    .navigate(R.id.newChatFromConversationsAction, bundleOf(
                            "subject" to subject,
                            "recipients" to recipients
                    ))
        }.show(parentFragmentManager, "newconversation")
    }

    // Update conversations on uichange broadcast
    private val uichangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // If messages were updated, and it was not a content change
            // We're not displaying any content in the list anyway
            if (intent.getStringExtra("content") == "messages" &&
                    intent.getStringExtra("type") != "contentchanged"
                    && ::adapter.isInitialized) {

                // Get the updated conversation
                val updateId = intent.getStringExtra("id")
                val updatedInfo = ConversationsDb()
                        .getConversationInfo(
                                "archived=0 AND conversation_id=\"$updateId\"")
                        .firstOrNull()

                if (updatedInfo != null) {
                    if (intent.getStringExtra("type") == "new") {
                        // New conversation, just add it to the top of the list
                        adapter.conversations.add(0, updatedInfo)
                        adapter.notifyItemInserted(0)
                        layoutManager.scrollToPosition(0)
                    } else if (intent.getStringExtra("type") == "metachanged") {
                        // Updated conversation, find and update it in the list
                        // Move it to the top while we're at it
                        val index = adapter.conversations.indexOfFirst { it.id == updateId }
                        if (index != -1) {
                            // Remove current position and push to list
                            adapter.conversations.removeAt(index)
                            adapter.conversations.add(0, updatedInfo)
                            // Notify item moved to top and changed
                            adapter.notifyItemMoved(index, 0)
                            adapter.notifyItemChanged(0)
                            // Scroll to the new top position
                            layoutManager.scrollToPosition(0)
                        }
                    }

                    // Make sure emptyConversationsLayout is hidden as there are conversations now
                    if (emptyLayout.visibility == View.VISIBLE)
                        emptyLayout.visibility = View.GONE
                }

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