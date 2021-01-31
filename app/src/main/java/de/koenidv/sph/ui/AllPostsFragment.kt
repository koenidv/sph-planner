package de.koenidv.sph.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.adapters.CompactPostsAdapter
import de.koenidv.sph.database.PostsDb
import de.koenidv.sph.database.TasksDb
import de.koenidv.sph.objects.Post


// Created by koenidv on 18.12.2020.
class AllPostsFragment(private var filters: MutableList<String> = mutableListOf()) : Fragment() {

    private lateinit var filter: () -> Unit

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_allposts, container, false)

        // Set open in browser url
        SphPlanner.openInBrowserUrl = getString(R.string.url_mycourses)


        val filterText = view.findViewById<TextView>(R.id.filterPostsTextView)
        val postsRecycler = view.findViewById<RecyclerView>(R.id.postsRecycler)
        val noDataText = view.findViewById<TextView>(R.id.noDataTextView)
        val postsUnfiltered = PostsDb.getInstance().allOrderedByUnread
        val posts = postsUnfiltered.toMutableList()

        // Filter posts function
        filter = {
            val filterTask = filters.any { it.startsWith("task_") }
            posts.clear()
            posts.addAll(postsUnfiltered.filterNot {
                var remove = false
                // Filter for unread
                if (filters.contains("unread") && !it.unread) remove = true
                else if (filters.contains("read") && it.unread) remove = true
                if (filterTask) {
                    // Filter for tasks
                    val taskDone: Boolean? = TasksDb.getInstance().taskDoneByPost(it.postId)
                    when {
                        filters.contains("task_none") && taskDone != null -> remove = true
                        filters.contains("task_any") && taskDone == null -> remove = true
                        filters.contains("task_undone") && taskDone != false -> remove = true
                        filters.contains("task_done") && taskDone != true -> remove = true
                    }
                }
                remove
            })
            postsRecycler.adapter?.notifyDataSetChanged()
            if (filters.isEmpty()) filterText.text = getString(R.string.posts_filter)
            else filterText.text = getString(R.string.posts_filtered)
            if (posts.isEmpty()) noDataText.visibility = View.VISIBLE
            else noDataText.visibility = View.GONE
        }

        // Get passed course id argument
        if (arguments?.getStringArray("filters") != null)
            filters = arguments?.getStringArray("filters")!!.toMutableList()
        // Filter posts with passed argument
        if (filters.isNotEmpty()) filter()

        // Set up posts recycler with all posts
        val adapter = CompactPostsAdapter(posts) { post: Post, itemview: View ->
            // Show single post bottom sheet
            PostSheet(post).show(parentFragmentManager, "post")
            // Remove unread indicator and mark as read
            if (post.unread) {
                itemview.findViewById<TextView>(R.id.unreadTextView).visibility = View.GONE
                PostsDb.getInstance().markAsRead(post.postId)
            }
        }
        adapter.setHasStableIds(true)
        postsRecycler.adapter = adapter
        postsRecycler.addItemDecoration(DividerItemDecoration(requireContext(), RecyclerView.VERTICAL))

        filterText.setOnClickListener {
            FilterPostsSheet(this, filters).show(parentFragmentManager, "filter_posts")
        }

        return view
    }

    fun filterPosts(filters: MutableList<String>) {
        this.filters = filters
        filter()
    }
}