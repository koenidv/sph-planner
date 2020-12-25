package de.koenidv.sph.ui

//  Created by koenidv on 12.12.2020.
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import de.koenidv.sph.R
import de.koenidv.sph.adapters.CoursesAdapter
import de.koenidv.sph.database.CoursesDb

class CoursesFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_courses, container, false)


        val coursesRecycler = view.findViewById<RecyclerView>(R.id.coursesRecycler)

        // Get favorite tiles and sort by isLK and fullname
        val courses = CoursesDb.getInstance().favorites.sortedBy { it.fullname }.sortedByDescending { it.isLK }
        // Set up courses recycler
        val coursesAdapter = CoursesAdapter(courses) {
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                    .navigate(R.id.postsFromCoursesAction, bundleOf("courseId" to it.courseId))
        }
        coursesRecycler.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        coursesRecycler.adapter = coursesAdapter


        return view
    }
}