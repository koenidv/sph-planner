package de.koenidv.sph.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import de.koenidv.sph.R
import de.koenidv.sph.adapters.ConversationsAdapter
import de.koenidv.sph.database.ConversationsDb

// Created by koenidv on 18.12.2020.
class MessagesFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_messages, container, false)

        val conversationsRecycler = view.findViewById<RecyclerView>(R.id.conversationsRecycler)
        val fab = view.findViewById<ExtendedFloatingActionButton>(R.id.newConversationFab)

        // Display conversations
        conversationsRecycler.adapter = ConversationsAdapter(ConversationsDb().getAll())

        // Extend / Shrink fab on scroll
        conversationsRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && fab.isExtended) {
                    fab.shrink()
                } else if (dy < 0 && !fab.isExtended) {
                    fab.extend()
                }
            }
        })

        return view
    }


}