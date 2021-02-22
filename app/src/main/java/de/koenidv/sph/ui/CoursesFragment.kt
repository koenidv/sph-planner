package de.koenidv.sph.ui

//  Created by koenidv on 12.12.2020.
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import de.koenidv.sph.R
import de.koenidv.sph.adapters.CoursesAdapter
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.objects.Course


class CoursesFragment : Fragment() {

    lateinit var courses: List<Course>
    lateinit var coursesRecycler: RecyclerView

    // Refresh whenever the broadcast "uichange" is received
    private val uichangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Notify adapter about all changed courses
            if (!intent.getStringArrayExtra("courses").isNullOrEmpty()
                    && ::courses.isInitialized) {
                val courseids = intent.getStringArrayExtra("courses")!!
                for (courseid in courseids) {
                    coursesRecycler.adapter?.notifyItemChanged(
                            courses.indexOfFirst { it.courseId == courseid })
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Register to receive messages.
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(uichangeReceiver,
                IntentFilter("uichange"))
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_courses, container, false)

        coursesRecycler = view.findViewById(R.id.coursesRecycler)

        // Get favorite courses and sort by isLK and fullname
        courses = CoursesDb.getFavorites().sortedBy { it.fullname }.sortedByDescending { it.isLK }
        // Set up courses recycler
        val coursesAdapter = CoursesAdapter(courses.toMutableList(), requireActivity()) {
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                    .navigate(R.id.overviewFromCoursesAction, bundleOf("courseId" to it.courseId))
        }
        coursesRecycler.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        coursesRecycler.adapter = coursesAdapter


        return view
    }

    override fun onDestroy() {
        // Unregister broadcast receiver
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(uichangeReceiver)
        super.onDestroy()
    }
}