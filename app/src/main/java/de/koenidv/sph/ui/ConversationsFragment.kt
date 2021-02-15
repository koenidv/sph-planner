package de.koenidv.sph.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.adapters.ConversationsAdapter
import de.koenidv.sph.database.ConversationsDb

// Created by koenidv on 18.12.2020.
class ConversationsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_conversations, container, false)

        SphPlanner.openInBrowserUrl = getString(R.string.url_messages)

        val conversationsRecycler = view.findViewById<RecyclerView>(R.id.conversationsRecycler)
        val fab = view.findViewById<ExtendedFloatingActionButton>(R.id.newConversationFab)

        var recyclerEditMode = false
        val conversations = ConversationsDb().getConversationInfo().toMutableList()

        // Display conversations
        val adapter = ConversationsAdapter(conversations, requireActivity()) { selectMode ->
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

        fab.setOnClickListener {
            if (recyclerEditMode) {
                // Archive selected conversations
                // todo archive in db
                var index: Int
                val selected = adapter.getSelected()
                adapter.clearSelected()
                for (conversation in selected) {
                    // Remove this item from the recycler view
                    index = conversations.indexOf(conversation)
                    if (index != -1) {
                        conversations.removeAt(index)
                        adapter.notifyItemRemoved(index)
                    }
                }
            } else {
                // Show a bottom sheet to start a new conversation
                NewConversationSheet { subject, recipients ->
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                            .navigate(R.id.newChatFromConversationsAction, bundleOf(
                                    "subject" to subject,
                                    "recipients" to recipients
                            ))
                }.show(parentFragmentManager, "newconversation")
            }
        }

        return view
    }


}