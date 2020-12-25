package de.koenidv.sph.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.adapters.LessonsAdapter
import de.koenidv.sph.database.TimetableDb
import de.koenidv.sph.objects.TimetableEntry


// Created on 24.12.2020 by koenidv.
class TimetableViewFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var expanded: Boolean = false
    private var viewAll: Boolean = false
    private var openOnClick: Boolean = true
    private lateinit var timetable: List<List<List<TimetableEntry>>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            expanded = it.getBoolean("expanded")
            viewAll = it.getBoolean("viewAll")
            openOnClick = it.getBoolean("openOnClick")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_view_timetable, container, false)

        // Get favorites if viewAll is false, all lessons otherwise
        timetable = TimetableDb.instance!!.get(!viewAll)
        val recyclerViewPool = RecyclerView.RecycledViewPool()

        val monday = view.findViewById<RecyclerView>(R.id.mondayRecycler)
        val tuesday = view.findViewById<RecyclerView>(R.id.tuesdayRecycler)
        val wednesday = view.findViewById<RecyclerView>(R.id.wednesdayRecycler)
        val thursday = view.findViewById<RecyclerView>(R.id.thursdayRecycler)
        val friday = view.findViewById<RecyclerView>(R.id.fridayRecycler)

        // Share views for better performance
        monday.setRecycledViewPool(recyclerViewPool)
        tuesday.setRecycledViewPool(recyclerViewPool)
        wednesday.setRecycledViewPool(recyclerViewPool)
        thursday.setRecycledViewPool(recyclerViewPool)
        friday.setRecycledViewPool(recyclerViewPool)

        val onClick: (List<TimetableEntry>) -> Unit = {
            if (openOnClick) {
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                        .navigate(R.id.frag_posts, bundleOf("courseId" to it[0].lesson.idCourse))
            }
        }

        // Set up lessons adapters
        monday.adapter = LessonsAdapter(timetable[0], expanded, viewAll, onClick = onClick)
        tuesday.adapter = LessonsAdapter(timetable[1], expanded, viewAll, onClick = onClick)
        wednesday.adapter = LessonsAdapter(timetable[2], expanded, viewAll, onClick = onClick)
        thursday.adapter = LessonsAdapter(timetable[3], expanded, viewAll, onClick = onClick)
        friday.adapter = LessonsAdapter(timetable[4], expanded, viewAll, onClick = onClick)

        return view
    }

    fun setExpanded(expanded: Boolean) {
        if (this.expanded != expanded) {
            this.expanded = expanded
            (requireView().findViewById<RecyclerView>(R.id.mondayRecycler).adapter as LessonsAdapter).setExpanded(expanded)
            (requireView().findViewById<RecyclerView>(R.id.tuesdayRecycler).adapter as LessonsAdapter).setExpanded(expanded)
            (requireView().findViewById<RecyclerView>(R.id.wednesdayRecycler).adapter as LessonsAdapter).setExpanded(expanded)
            (requireView().findViewById<RecyclerView>(R.id.thursdayRecycler).adapter as LessonsAdapter).setExpanded(expanded)
            (requireView().findViewById<RecyclerView>(R.id.fridayRecycler).adapter as LessonsAdapter).setExpanded(expanded)
        }
    }

    fun setViewAll(viewAll: Boolean) {
        if (this.viewAll != viewAll) {
            this.viewAll = viewAll
            // Update timetable
            timetable = TimetableDb.instance!!.get(!viewAll)
            // Get maximum amount of concurrent lessons if they are shown
            val maxConcurrent = if (viewAll) timetable.maxOf { days -> days.maxOf { it.size } } else 1
            // Notify recyclerviews
            (requireView().findViewById<RecyclerView>(R.id.mondayRecycler).adapter as LessonsAdapter).setDataAndMultiple(timetable[0], viewAll, maxConcurrent)
            (requireView().findViewById<RecyclerView>(R.id.tuesdayRecycler).adapter as LessonsAdapter).setDataAndMultiple(timetable[1], viewAll, maxConcurrent)
            (requireView().findViewById<RecyclerView>(R.id.wednesdayRecycler).adapter as LessonsAdapter).setDataAndMultiple(timetable[2], viewAll, maxConcurrent)
            (requireView().findViewById<RecyclerView>(R.id.thursdayRecycler).adapter as LessonsAdapter).setDataAndMultiple(timetable[3], viewAll, maxConcurrent)
            (requireView().findViewById<RecyclerView>(R.id.fridayRecycler).adapter as LessonsAdapter).setDataAndMultiple(timetable[4], viewAll, maxConcurrent)
        }
    }

}