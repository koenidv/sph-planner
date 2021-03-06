package de.koenidv.sph.ui

import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.adapters.ConversationsAdapter
import de.koenidv.sph.database.ConversationsDb
import de.koenidv.sph.networking.Messages

//  Created by koenidv on 06.03.2021.
class ConversationsArchiveSheet(
        private val onUnarchive: (ConversationsAdapter.ConversationInfo) -> Unit) : RecyclerSheet() {

    override fun setupRecycler(recycler: RecyclerView) {

        val conversations = ConversationsDb()
                .getConversationInfo("archived=1")
                .toMutableList()

        val adapter = ConversationsAdapter(conversations, requireActivity(), archived = true)

        adapter.clickCallback = { dismiss() }
        adapter.unarchiveCallback = { info, position ->
            // Mark the conversation as not archived in db and on sph
            Messages().setArchived(info.id,
                    ConversationsDb().getFirstMessageId(info.id).toString(), false)
            // Remove the conversation from the sheet
            conversations.removeAt(position)
            adapter.notifyItemRemoved(position)
            // Add the conversation to the actual list (not archived)
            onUnarchive(info)
        }

        recycler.adapter = adapter

    }

}