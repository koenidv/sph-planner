package de.koenidv.sph.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.adapters.PostsAdapter
import de.koenidv.sph.database.PostAttachmentsDb
import de.koenidv.sph.database.PostTasksDb
import de.koenidv.sph.database.PostsDb
import de.koenidv.sph.networking.AttachmentManager
import me.saket.bettermovementmethod.BetterLinkMovementMethod


// Created by koenidv on 18.12.2020.
class CourseOverviewFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_course_overview, container, false)
        val prefs: SharedPreferences = SphPlanner.applicationContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)

        val postsRecycler = view.findViewById<RecyclerView>(R.id.postsRecycler)
        val postsTitleText = view.findViewById<TextView>(R.id.postsTitleTextView)
        val postsLoading = view.findViewById<ProgressBar>(R.id.postsLoading)
        val loadMorePostsButton = view.findViewById<MaterialButton>(R.id.loadMorePostsButton)

        // Get passed course id argument
        val courseId = arguments?.getString("courseId") ?: ""

        val posts = PostsDb.getInstance().getByCourseId(courseId)
        val tasks = PostTasksDb.getInstance().getByCourseId(courseId)
        val attachments = PostAttachmentsDb.getInstance().getPostByCourseId(courseId)

        /*
         * Posts recycler
         * Set up with first 2 posts only
         * Then load more on button click
         */

        if (posts.isNotEmpty()) {
            val postsToShow = posts.take(2).toMutableList()
            val taskstoShow = tasks.filter { it.id_post == postsToShow[0].postId || it.id_post == postsToShow.getOrNull(1)?.postId }.toMutableList()
            val attachmentsToShow = attachments.filter { it.id_post == postsToShow[0].postId || it.id_post == postsToShow.getOrNull(1)?.postId }.toMutableList()

            // Movement method to open links in-app
            val movementMethod = BetterLinkMovementMethod.newInstance()
            movementMethod.setOnLinkClickListener { _, url ->
                if (prefs.getBoolean("open_links_inapp", true)) {
                    // Open WebViewFragment with respective url on click
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                            .navigate(R.id.webviewFromPostsAction, bundleOf("url" to url))
                    true
                } else false
            }

            val postsAdapter = PostsAdapter(
                    postsToShow,
                    taskstoShow,
                    attachmentsToShow,
                    movementMethod,
                    AttachmentManager().onAttachmentClick(requireActivity()),
                    AttachmentManager().onAttachmentLongClick(requireActivity())
            )
            postsRecycler.adapter = postsAdapter

            loadMorePostsButton.setOnClickListener {
                // Load all posts
                // Replace posts adapter data with all posts
                postsToShow.clear()
                postsToShow.addAll(posts)
                taskstoShow.clear()
                taskstoShow.addAll(tasks)
                attachmentsToShow.clear()
                attachmentsToShow.addAll(attachments)
                // Show loading symbol and hide button
                postsLoading.visibility = View.VISIBLE
                loadMorePostsButton.visibility = View.GONE
                // Update RecyclerView and hide ProgessBar
                // todo only notify for updated items
                postsAdapter.notifyDataSetChanged()
                postsLoading.visibility = View.GONE
                // Mark all posts as read
                PostsDb.getInstance().markAsRead(courseId)
            }

            // Mark displayed posts as read
            PostsDb.getInstance().markAsRead(postsToShow[0].postId, postsToShow.getOrNull(1)?.postId)

            // Hide load more button if there are no further posts
            if (posts.size == 2) {
                loadMorePostsButton.visibility = View.GONE
            }
        } else {
            postsTitleText.text = getString(R.string.posts_no_data)
            postsRecycler.visibility = View.GONE
            loadMorePostsButton.visibility = View.GONE
        }

        return view
    }
}