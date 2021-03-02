package de.koenidv.sph.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.adapters.LessonsAdapter
import de.koenidv.sph.database.ChangesDb
import de.koenidv.sph.database.TimetableDb
import de.koenidv.sph.objects.Change
import de.koenidv.sph.objects.TimetableEntry
import de.koenidv.sph.parsing.Utility
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*


// Created on 24.12.2020 by koenidv.
class TimetableViewFragment : Fragment() {
    private var expanded: Boolean = false
    private var viewAll: Boolean = false
    private var openOnClick: Boolean = true
    private var withChanges: Boolean = true

    // List of days of hours of concurrent lessons
    private lateinit var timetable: List<List<List<TimetableEntry>>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            expanded = it.getBoolean("expanded")
            viewAll = it.getBoolean("viewAll")
            openOnClick = it.getBoolean("openOnClick")
            withChanges = it.getBoolean("withChanges")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_view_timetable, container, false)
        val prefs = requireContext().getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)

        // Get favorites if viewAll is false, all lessons otherwise
        val time1 = Date().time
        timetable = TimetableDb.instance!!.get(!viewAll)
        Log.d(SphPlanner.TAG, "Time used for timetable mapping: " + (Date().time - time1) + "ms")
        val recyclerViewPool = RecyclerView.RecycledViewPool()

        // Get the 5 recyclerviews
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

        // OnClick function for all timetable entries
        val onClick: (List<TimetableEntry>) -> Unit = {
            if (openOnClick) {
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                        .navigate(R.id.overviewAction, bundleOf("courseId" to it[0].lesson.idCourse))
            }
        }

        // Set up lessons adapters
        if (!timetable.isNullOrEmpty()) {
            val time2 = Date().time
            monday.adapter = LessonsAdapter(timetable[0], expanded, viewAll, onClick = onClick)
            tuesday.adapter = LessonsAdapter(timetable[1], expanded, viewAll, onClick = onClick)
            wednesday.adapter = LessonsAdapter(timetable[2], expanded, viewAll, onClick = onClick)
            thursday.adapter = LessonsAdapter(timetable[3], expanded, viewAll, onClick = onClick)
            friday.adapter = LessonsAdapter(timetable[4], expanded, viewAll, onClick = onClick)
            Log.d(SphPlanner.TAG, "Time used for timetable recycler creation: " + (Date().time - time2) + "ms")
        }

        // Highlight today's recycler with the theme color at ~15% opacity
        val background = ContextCompat.getDrawable(requireContext(), R.drawable.background_rounded_rectangle)
        // Sometimes uses a different color if quickly fragments are quickly switched
        background?.setTint(prefs.getInt("themeColor", 0))
        background?.alpha = 40
        when (Utility.getCurrentDayAdjusted()) {
            0 -> monday.background = background
            1 -> tuesday.background = background
            2 -> wednesday.background = background
            3 -> thursday.background = background
            else -> friday.background = background
        }

        if (withChanges) {
            GlobalScope.launch {
                // Get all changes for favorite courses, sorted by date and lesson
                val changes = ChangesDb.instance!!.getFavorites()
                if (changes.isNotEmpty()) {
                    val changesByDay = mutableListOf<List<Change>?>(null, null, null, null, null)
                    val changesThisDay = mutableListOf<Change>()
                    // Having the list already ordered means we can map it more efficently,
                    // by assuming all entries for one day will be en bloc
                    var lastDate = changes[0].date

                    @Suppress("DEPRECATION")
                    var lastDay = lastDate.day - 1 // Monday is first day
                    // Add each change to the corresponding day
                    for (change in changes) {
                        // If this is the next day, save last day and continue with the next
                        if (change.date != lastDate) {
                            changesByDay.add(lastDay, changesThisDay.toList())
                            changesThisDay.clear()
                            // Update day
                            lastDate = change.date
                            @Suppress("DEPRECATION")
                            lastDay = lastDate.day - 1
                        }
                        changesThisDay.add(change)
                    }
                    // Add the last day to changesByDay list
                    changesByDay[lastDay] = changesThisDay.toList()
                    changesThisDay.clear()

                    // Apply changes to the RecyclerViews
                    // Wait 200ms to avoid blocking the ui thread
                    // Will display regular lesson first and then switch to updated lesson
                    delay(200)
                    if (activity != null)
                        requireActivity().runOnUiThread {
                            if (changesByDay.getOrNull(0) != null)
                                (monday.adapter as LessonsAdapter).applyChanges(changesByDay[0]!!)
                            if (changesByDay.getOrNull(1) != null)
                                (tuesday.adapter as LessonsAdapter).applyChanges(changesByDay[1]!!)
                            if (changesByDay.getOrNull(2) != null)
                                (wednesday.adapter as LessonsAdapter).applyChanges(changesByDay[2]!!)
                            if (changesByDay.getOrNull(3) != null)
                                (thursday.adapter as LessonsAdapter).applyChanges(changesByDay[3]!!)
                            if (changesByDay.getOrNull(4) != null)
                                (friday.adapter as LessonsAdapter).applyChanges(changesByDay[4]!!)
                        }
                }

            }

        }

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

    /**
     * Show all lessons or only favorite courses
     */
    fun setViewAll(viewAll: Boolean) {
        if (this.viewAll != viewAll) {
            this.viewAll = viewAll
            // Recreate if pesonal timetable was for some reason not shown
            if (requireView().findViewById<RecyclerView>(R.id.mondayRecycler).adapter == null) {
                parentFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment,
                                instantiate(requireContext(), TimetableFragment().javaClass.name,
                                        bundleOf(
                                                "expanded" to this.expanded,
                                                "viewAll" to this.viewAll,
                                                "openOnClick" to this.openOnClick,
                                                "withChanges" to this.withChanges)))
                        .commit()
            } else {
                // Update timetable
                timetable = TimetableDb.instance!!.get(!viewAll)
                // Get maximum amount of concurrent lessons if they are shown
                val maxConcurrent = if (viewAll) timetable.maxOf { days -> days.maxOf { it.size } } else 1
                // Cancel if timetable does not for some reason contain entries for 5 days
                if (timetable.size < 5) return
                // Notify recyclerviews
                (requireView().findViewById<RecyclerView>(R.id.mondayRecycler).adapter as LessonsAdapter).setDataAndMultiple(timetable[0], viewAll, maxConcurrent)
                (requireView().findViewById<RecyclerView>(R.id.tuesdayRecycler).adapter as LessonsAdapter).setDataAndMultiple(timetable[1], viewAll, maxConcurrent)
                (requireView().findViewById<RecyclerView>(R.id.wednesdayRecycler).adapter as LessonsAdapter).setDataAndMultiple(timetable[2], viewAll, maxConcurrent)
                (requireView().findViewById<RecyclerView>(R.id.thursdayRecycler).adapter as LessonsAdapter).setDataAndMultiple(timetable[3], viewAll, maxConcurrent)
                (requireView().findViewById<RecyclerView>(R.id.fridayRecycler).adapter as LessonsAdapter).setDataAndMultiple(timetable[4], viewAll, maxConcurrent)
            }
        }
    }

}