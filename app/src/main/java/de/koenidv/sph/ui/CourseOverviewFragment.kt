package de.koenidv.sph.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.adapters.PostsAdapter
import de.koenidv.sph.database.PostAttachmentsDb
import de.koenidv.sph.database.PostTasksDb
import de.koenidv.sph.database.PostsDb
import de.koenidv.sph.objects.Post
import de.koenidv.sph.objects.PostAttachment
import de.koenidv.sph.objects.PostTask
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


// Created by koenidv on 18.12.2020.
class CourseOverviewFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_course_overview, container, false)
        val prefs: SharedPreferences = SphPlanner.applicationContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)

        val postsRecycler = view.findViewById<RecyclerView>(R.id.postsRecycler)
        val postsTitleText = view.findViewById<TextView>(R.id.postsTitleTextView)
        val postsLoading = view.findViewById<ProgressBar>(R.id.postsLoading)

        // Get passed course id argument
        val courseId = arguments?.getString("courseId") ?: ""

        val posts = PostsDb.getInstance().getByCourseId(courseId)
        val tasks = PostTasksDb.getInstance().getByCourseId(courseId)
        val attachments = PostAttachmentsDb.getInstance().getPostByCourseId(courseId)

        /*
         * Posts recycler
         * Set up with empty data and reload later
         * Show fragment before RecyclerView is populated
         */

        val postsLateInit = mutableListOf<Post>()
        val tasksLateInit = mutableListOf<PostTask>()
        val attachmentsLateInit = mutableListOf<PostAttachment>()

        val postsAdapter = PostsAdapter(
                postsLateInit,
                tasksLateInit,
                attachmentsLateInit
        )
        postsRecycler.adapter = postsAdapter
        GlobalScope.launch {
            // Set up posts recycler
            postsLateInit.addAll(posts)
            tasksLateInit.addAll(tasks)
            attachmentsLateInit.addAll(attachments)
            // Populate recyclerview 100ms delayed to avoid visible lag
            // Not the best solution..
            delay(110)
            requireActivity().runOnUiThread {
                // Check if there are any posts for this course
                if (postsLateInit.isNotEmpty()) {
                    // Update RecyclerView and hide ProgessBar
                    postsAdapter.notifyDataSetChanged()
                    postsLoading.visibility = View.GONE
                    postsRecycler.visibility = View.VISIBLE
                } else {
                    // Display no posts message
                    postsTitleText.text = getString(R.string.posts_no_data)
                    postsLoading.visibility = View.GONE
                }
            }
        }

        return view
    }
}