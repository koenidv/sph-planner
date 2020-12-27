package de.koenidv.sph.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.database.FileAttachmentsDb
import de.koenidv.sph.database.PostTasksDb
import de.koenidv.sph.database.PostsDb


// Created by koenidv on 18.12.2020.
class AttachmentsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_attachments, container, false)
        val prefs: SharedPreferences = SphPlanner.applicationContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)

        val attachmentsRecycler = view.findViewById<RecyclerView>(R.id.attachmentsRecycler)
        val loading = view.findViewById<ProgressBar>(R.id.loading)
        val loadMoreButton = view.findViewById<MaterialButton>(R.id.loadMoreButton)

        // Get passed course id argument
        val courseId = arguments?.getString("courseId") ?: ""

        val posts = PostsDb.getInstance().getByCourseId(courseId)
        val tasks = PostTasksDb.getInstance().getByCourseId(courseId)
        val attachments = FileAttachmentsDb.getInstance().getPostByCourseId(courseId)



        return view
    }
}