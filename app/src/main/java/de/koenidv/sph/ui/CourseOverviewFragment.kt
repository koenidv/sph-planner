package de.koenidv.sph.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.adapters.PostsAdapter
import de.koenidv.sph.database.PostAttachmentsDb
import de.koenidv.sph.database.PostsDb
import de.koenidv.sph.objects.PostTask


// Created by koenidv on 18.12.2020.
class CourseOverviewFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_course_overview, container, false)
        val prefs: SharedPreferences = SphPlanner.applicationContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)

        val postsRecycler = view.findViewById<RecyclerView>(R.id.postsRecycler)
        val postsTitleText = view.findViewById<TextView>(R.id.postsTitleTextView)

        // Get passed course id argument
        val courseId = arguments?.getString("courseId") ?: ""

        val posts = PostsDb.getInstance().getByCourseId(courseId)
        val tasks = listOf<PostTask>()
        val attachments = PostAttachmentsDb.getInstance().getPostByCourseId(courseId)

        if (!posts.isEmpty()) {
            // Set up posts recycler
            val postsAdapter = PostsAdapter(
                    posts,
                    tasks,
                    attachments
            )
            postsRecycler.layoutManager = LinearLayoutManager(requireContext())
            postsRecycler.adapter = postsAdapter
        } else {
            // Display no posts message
            postsTitleText.text = getString(R.string.posts_no_data)
        }

        return view
    }
}