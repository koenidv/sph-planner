package de.koenidv.sph.ui

//  Created by koenidv on 12.12.2020.
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
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
        val courses = CoursesDb.getInstance().favoriteCourses.sortedBy { it.fullname }.sortedByDescending { it.isLK }
        // Set up courses recycler
        val coursesAdapter = CoursesAdapter(courses) {
            Toast.makeText(requireContext(), "Coming soon to a screen near your thumb.", Toast.LENGTH_SHORT).show()
        }
        coursesRecycler.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        coursesRecycler.adapter = coursesAdapter




        return view
    }
}