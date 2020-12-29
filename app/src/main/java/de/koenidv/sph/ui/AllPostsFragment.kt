package de.koenidv.sph.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.adapters.CompactPostsAdapter
import de.koenidv.sph.database.PostsDb


// Created by koenidv on 18.12.2020.
class AllPostsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_allposts, container, false)

        val postsRecycler = view.findViewById<RecyclerView>(R.id.postsRecycler)
        val posts = PostsDb.getInstance().allOrderedByUnread

        // Set up posts recycler with all posts
        postsRecycler.setHasFixedSize(true)
        postsRecycler.adapter = CompactPostsAdapter(posts) {
            PostSheet(it).show(parentFragmentManager, "post")
        }
        postsRecycler.addItemDecoration(DividerItemDecoration(requireContext(), RecyclerView.VERTICAL))

        // todo performance, filtering

        return view
    }
}