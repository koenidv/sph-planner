package de.koenidv.sph.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.MainActivity
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.SphPlanner.Companion.TMLMT
import de.koenidv.sph.SphPlanner.Companion.mainMonday
import de.koenidv.sph.adapters.LessonsAdapter
import de.koenidv.sph.adapters.TimebarAdapter
import de.koenidv.sph.database.ChangesDb
import de.koenidv.sph.database.SchedulesDb
import de.koenidv.sph.database.TimebarDb
import de.koenidv.sph.database.TimetableDb
import de.koenidv.sph.objects.Change
import de.koenidv.sph.objects.TimetableEntry
import de.koenidv.sph.parsing.Utility
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.*


//  Created on 24.12.2020 by koenidv.
//  Extended by StKl Q4-2021
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

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_view_timetable, container, false)
        val nav = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        val prefs =
            requireContext().getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)

        // Get favorites if viewAll is false, all lessons otherwise
        timetable = TimetableDb.instance!!.get(!viewAll)
        val (vldDate: LocalDate, timebar: Array<Array<LocalTime>>) = TimebarDb.instance!!.get()

        val recyclerViewPool = RecyclerView.RecycledViewPool()

        // Get the 5 recyclerviews + timebar
        val monday = view.findViewById<RecyclerView>(R.id.mondayRecycler)
        val tuesday = view.findViewById<RecyclerView>(R.id.tuesdayRecycler)
        val wednesday = view.findViewById<RecyclerView>(R.id.wednesdayRecycler)
        val thursday = view.findViewById<RecyclerView>(R.id.thursdayRecycler)
        val friday = view.findViewById<RecyclerView>(R.id.fridayRecycler)
        val tmbrView = view.findViewById<RecyclerView>(R.id.lssnRecycler)
        // Get the 5 corrsponding TextViews + timebarTxt
        val mondayTxt = view.findViewById<TextView>(R.id.mondayTextView)
        val tuesdayTxt = view.findViewById<TextView>(R.id.tuesdayTextView)
        val wednesdayTxt = view.findViewById<TextView>(R.id.wednesdayTextView)
        val thursdayTxt = view.findViewById<TextView>(R.id.thursdayTextView)
        val fridayTxt = view.findViewById<TextView>(R.id.fridayTextView)
        val tmbrTxt = view.findViewById<TextView>(R.id.lssnTextView)
        // Get the 5 corrsponding dates + lssnDate
        val mondayDt = view.findViewById<TextView>(R.id.mondayDate)
        val tuesdayDt = view.findViewById<TextView>(R.id.tuesdayDate)
        val wednesdayDt = view.findViewById<TextView>(R.id.wednesdayDate)
        val thursdayDt = view.findViewById<TextView>(R.id.thursdayDate)
        val fridayDt = view.findViewById<TextView>(R.id.fridayDate)
        val lssnDt = view.findViewById<TextView>(R.id.lssnDate)

        // Share views for better performance
        monday.setRecycledViewPool(recyclerViewPool)
        tuesday.setRecycledViewPool(recyclerViewPool)
        wednesday.setRecycledViewPool(recyclerViewPool)
        thursday.setRecycledViewPool(recyclerViewPool)
        friday.setRecycledViewPool(recyclerViewPool)
        //tmbrView.setRecycledViewPool(recyclerViewPool) //Crash in case of general timetable is viewed/ switch is set accordingly

        // OnClick function for all timetable entries
        val onClick: (List<TimetableEntry>) -> Unit = {
            if (openOnClick) {
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                    .navigate(R.id.overviewAction, bundleOf("courseId" to it[0].lesson.idCourse))
            }
        }

        //Set valid dates for header - Part #1

        //Contains (after some adaptions) always the Monday we are working with
        //Start of all adaptions and parameters for the next days and into the adapters
        //var tdy = Date()
        var tdy = mainMonday
        val c = Calendar.getInstance()
        c.time = tdy
        //if Friday after 16:00 - Show next week
        if (   (c.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) && (c.get(Calendar.HOUR_OF_DAY) > 16)  ) { //16:59 := FALSE !!!
            c.add(Calendar.DATE, 3)//Fri => Mon
        } else if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {//(tdy.dayOfWeek == DayOfWeek.SATURDAY) {
            c.add(Calendar.DATE, 2)//Sat => Mon
        } else if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {//(tdy.dayOfWeek == DayOfWeek.SUNDAY) {
            c.add(Calendar.DATE, 1)//Sun => Mon
        }
        //Now we have Monday of next week or we are within the current week
        //Now we need the dates for the next days, starting Monday
        when (c.get(Calendar.DAY_OF_WEEK)) {//we do not change the week!
            Calendar.TUESDAY ->     c.add(Calendar.DATE, -1)
            Calendar.WEDNESDAY ->   c.add(Calendar.DATE, -2)
            Calendar.THURSDAY ->    c.add(Calendar.DATE, -3)
            Calendar.FRIDAY ->      c.add(Calendar.DATE, -4)
            else ->                 c.add(Calendar.DATE,  0)
        }
        // Set up lessons adapters
        tdy = c.time //The ultimative Monday
        mainMonday = tdy

        c.add(Calendar.DATE, 1)
        val tdy2 = c.time
        c.add(Calendar.DATE, 1)
        val tdy3 = c.time
        c.add(Calendar.DATE, 1)
        val tdy4 = c.time
        c.add(Calendar.DATE, 1)
        val tdy5 = c.time

        // Highlight today's recycler with the theme color at ~15% opacity
        val background =
            ContextCompat.getDrawable(requireContext(), R.drawable.background_rounded_rectangle)
        // Sometimes uses a different color if quickly fragments are quickly switched
        background?.setTint(prefs.getInt("themeColor", 0))
        background?.alpha = 40

        if (!timetable.isNullOrEmpty()) {
            monday.adapter    = LessonsAdapter(tdy,  timetable[0], timebar, expanded, viewAll, onClick = onClick)
            tuesday.adapter   = LessonsAdapter(tdy2, timetable[1], timebar, expanded, viewAll, onClick = onClick)
            wednesday.adapter = LessonsAdapter(tdy3, timetable[2], timebar, expanded, viewAll, onClick = onClick)
            thursday.adapter  = LessonsAdapter(tdy4, timetable[3], timebar, expanded, viewAll, onClick = onClick)
            friday.adapter    = LessonsAdapter(tdy5, timetable[4], timebar, expanded, viewAll, onClick = onClick)
            tmbrView.adapter  = TimebarAdapter(tdy,  background,   timebar, expanded, viewAll)
        }

        //Highlight current day
        when (Utility.getCurrentDayAdjusted()) {
            0 -> mondayTxt.background = background
            1 -> tuesdayTxt.background = background
            2 -> wednesdayTxt.background = background
            3 -> thursdayTxt.background = background
            else -> fridayTxt.background = background
        }

        if(expanded) {//No sideways navigation in expanded view
            view.findViewById<View>(R.id.bttnLeft).visibility  = View.GONE
            view.findViewById<View>(R.id.bttnRight).visibility = View.GONE
            view.findViewById<View>(R.id.bttnTdy).visibility   = View.GONE
        }
        else {
            //More than 3m/ 12weeks to past or future not allowed
            c.time = Date()
            c.add(Calendar.MONTH, -(TMLMT))
            if(mainMonday.before(c.time)) {
                view.findViewById<View>(R.id.bttnLeft).visibility = View.GONE
            }
            else {
                view.findViewById<View>(R.id.bttnLeft).visibility = View.VISIBLE
            }
            c.time = Date()
            c.add(Calendar.MONTH, TMLMT)
            if(mainMonday.after(c.time)) {
                view.findViewById<View>(R.id.bttnRight).visibility = View.GONE
            }
            else {
                view.findViewById<View>(R.id.bttnRight).visibility = View.VISIBLE
            }
        }

        view.findViewById<View>(R.id.bttnLeft).setOnClickListener {
            c.time = mainMonday
            c.add(Calendar.DATE, -7)
            mainMonday = c.time
            (requireView().findViewById<RecyclerView>(R.id.lssnRecycler).adapter        as TimebarAdapter).setTiming(c.time)
            (requireView().findViewById<RecyclerView>(R.id.mondayRecycler).adapter      as LessonsAdapter).setTiming(c.time)
            c.add(Calendar.DATE, 1)
            (requireView().findViewById<RecyclerView>(R.id.tuesdayRecycler).adapter     as LessonsAdapter).setTiming(c.time)
            c.add(Calendar.DATE, 1)
            (requireView().findViewById<RecyclerView>(R.id.wednesdayRecycler).adapter   as LessonsAdapter).setTiming(c.time)
            c.add(Calendar.DATE, 1)
            (requireView().findViewById<RecyclerView>(R.id.thursdayRecycler).adapter    as LessonsAdapter).setTiming(c.time)
            c.add(Calendar.DATE, 1)
            (requireView().findViewById<RecyclerView>(R.id.fridayRecycler).adapter      as LessonsAdapter).setTiming(c.time)

            //update
            val ft = parentFragmentManager.beginTransaction()
            val timetableFragment = TimetableViewFragment()
            timetableFragment.arguments = bundleOf(
                "expanded" to expanded,
                "viewAll" to viewAll,
                "openOnClick" to openOnClick,
                "withChanges" to withChanges)
            ft.replace(R.id.timetableFragment, timetableFragment)
                .setReorderingAllowed(true) //Optimizing state changes for better transitions
                .commit()
        }
        view.findViewById<View>(R.id.bttnRight).setOnClickListener {
            c.time = mainMonday
            c.add(Calendar.DATE, 7)
            mainMonday = c.time
            (requireView().findViewById<RecyclerView>(R.id.lssnRecycler).adapter        as TimebarAdapter).setTiming(c.time)
            (requireView().findViewById<RecyclerView>(R.id.mondayRecycler).adapter      as LessonsAdapter).setTiming(c.time)
            c.add(Calendar.DATE, 1)
            (requireView().findViewById<RecyclerView>(R.id.tuesdayRecycler).adapter     as LessonsAdapter).setTiming(c.time)
            c.add(Calendar.DATE, 1)
            (requireView().findViewById<RecyclerView>(R.id.wednesdayRecycler).adapter   as LessonsAdapter).setTiming(c.time)
            c.add(Calendar.DATE, 1)
            (requireView().findViewById<RecyclerView>(R.id.thursdayRecycler).adapter    as LessonsAdapter).setTiming(c.time)
            c.add(Calendar.DATE, 1)
            (requireView().findViewById<RecyclerView>(R.id.fridayRecycler).adapter      as LessonsAdapter).setTiming(c.time)

            //update
            val ft = parentFragmentManager.beginTransaction()
            /*ft.setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                R.anim.fade_in, R.anim.fade_out)*/
            val timetableFragment = TimetableViewFragment()
            timetableFragment.arguments = bundleOf(
                "expanded" to expanded,
                "viewAll" to viewAll,
                "openOnClick" to openOnClick,
                "withChanges" to withChanges)
            ft.replace(R.id.timetableFragment, timetableFragment)
                .setReorderingAllowed(true) //Optimizing state changes for better transitions
                .commit()
        }
        view.findViewById<View>(R.id.bttnTdy).setOnClickListener {
            tdy = Date()
            c.time = tdy
            //if Friday after 16:00 - Show next week
            if ((c.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) && (c.get(Calendar.HOUR_OF_DAY) > 16)) { //16:59 := FALSE !!!
                c.add(Calendar.DATE, 3)//Fri => Mon
            } else if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {//(tdy.dayOfWeek == DayOfWeek.SATURDAY) {
                c.add(Calendar.DATE, 2)//Sat => Mon
            } else if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {//(tdy.dayOfWeek == DayOfWeek.SUNDAY) {
                c.add(Calendar.DATE, 1)//Sun => Mon
            }
            //Now we have Monday of next week or we are within the current week
            //Now we need the dates for the next days, starting Monday
            when (c.get(Calendar.DAY_OF_WEEK)) {//we do not change the week!
                Calendar.TUESDAY -> c.add(Calendar.DATE, -1)
                Calendar.WEDNESDAY -> c.add(Calendar.DATE, -2)
                Calendar.THURSDAY -> c.add(Calendar.DATE, -3)
                Calendar.FRIDAY -> c.add(Calendar.DATE, -4)
                else -> c.add(Calendar.DATE, 0)
            }
            // Set up lessons adapters
            tdy = c.time //The ultimative Monday
            mainMonday = tdy

            (requireView().findViewById<RecyclerView>(R.id.lssnRecycler).adapter as TimebarAdapter).setTiming(
                c.time
            )
            (requireView().findViewById<RecyclerView>(R.id.mondayRecycler).adapter as LessonsAdapter).setTiming(
                c.time
            )
            c.add(Calendar.DATE, 1)
            (requireView().findViewById<RecyclerView>(R.id.tuesdayRecycler).adapter as LessonsAdapter).setTiming(
                c.time
            )
            c.add(Calendar.DATE, 1)
            (requireView().findViewById<RecyclerView>(R.id.wednesdayRecycler).adapter as LessonsAdapter).setTiming(
                c.time
            )
            c.add(Calendar.DATE, 1)
            (requireView().findViewById<RecyclerView>(R.id.thursdayRecycler).adapter as LessonsAdapter).setTiming(
                c.time
            )
            c.add(Calendar.DATE, 1)
            (requireView().findViewById<RecyclerView>(R.id.fridayRecycler).adapter as LessonsAdapter).setTiming(
                c.time
            )

            //update
            val ft = parentFragmentManager.beginTransaction()
            val timetableFragment = TimetableViewFragment()
            timetableFragment.arguments = bundleOf(
                "expanded" to expanded,
                "viewAll" to viewAll,
                "openOnClick" to openOnClick,
                "withChanges" to withChanges
            )
            ft.replace(R.id.timetableFragment, timetableFragment)
                .setReorderingAllowed(true) //Optimizing state changes for better transitions
                .commit()
        }

        //Set valid dates for header - Part #2
        //Set year and CW

        c.time = tdy
        tmbrTxt.text = c.get(Calendar.YEAR).toString()//tdy.year.toString()
        //CW calculation, starting with current Thursday
        c.add(Calendar.DATE, 3)
        var week = 0 //ISO8601: CW1 is the week with the first Thursday in the year, Start of the week is Mon
        week += c.get(Calendar.DAY_OF_YEAR) / 7
        if (   (c.get(Calendar.DAY_OF_YEAR) - (week*7)) > 3   ) {
            week++
        }
        lssnDt.text = "#${week}"

        c.time = tdy
        val dtStrMon = "${c.get(Calendar.DAY_OF_MONTH)}.${c.get(Calendar.MONTH)+1}.${c.get(Calendar.YEAR)}"
        mondayDt.text = c.get(Calendar.DAY_OF_MONTH).toString() + "." + (c.get(Calendar.MONTH)+1).toString()
        c.add(Calendar.DATE, 1)
        val tdy22 = c.time
        val dtStrTue = "${c.get(Calendar.DAY_OF_MONTH)}.${c.get(Calendar.MONTH)+1}.${c.get(Calendar.YEAR)}"
        tuesdayDt.text = c.get(Calendar.DAY_OF_MONTH).toString() + "." + (c.get(Calendar.MONTH)+1).toString()
        c.add(Calendar.DATE, 1)
        val tdy33 = c.time
        val dtStrWed = "${c.get(Calendar.DAY_OF_MONTH)}.${c.get(Calendar.MONTH)+1}.${c.get(Calendar.YEAR)}"
        wednesdayDt.text = c.get(Calendar.DAY_OF_MONTH).toString() + "." + (c.get(Calendar.MONTH)+1).toString()
        c.add(Calendar.DATE, 1)
        val tdy44 = c.time
        val dtStrThu = "${c.get(Calendar.DAY_OF_MONTH)}.${c.get(Calendar.MONTH)+1}.${c.get(Calendar.YEAR)}"
        thursdayDt.text = c.get(Calendar.DAY_OF_MONTH).toString() + "." + (c.get(Calendar.MONTH)+1).toString()
        c.add(Calendar.DATE, 1)
        val tdy55 = c.time
        val dtStrFri = "${c.get(Calendar.DAY_OF_MONTH)}.${c.get(Calendar.MONTH)+1}.${c.get(Calendar.YEAR)}"
        fridayDt.text = c.get(Calendar.DAY_OF_MONTH).toString() + "." + (c.get(Calendar.MONTH)+1).toString()

        //tmbrTxt.text = LocalDate.parse(vldDate.toString(), DateTimeFormatter.ISO_LOCAL_DATE).toString() //Date of the last update of the timetable in Schulportal

        //Set flag and schedules for the days on the name of the day
        //1v5
        if(SchedulesDb.getSchedWithStartDate(tdy).isNotEmpty() && !expanded) {
            mondayTxt.text = "${mondayTxt.text} \uD83D\uDEA9"//red triangle flag
            mondayTxt.setOnClickListener {
                nav.navigate(R.id.schedulesFromHomeAction)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment,
                        SchedulesFragment().also {
                            it.arguments = bundleOf("startS" to dtStrMon)//Date for the schedule
                        })
                    .setReorderingAllowed(true) //Optimizing state changes for better transitions
                    .commit()
            }
        }
        //2v5
        if(SchedulesDb.getSchedWithStartDate(tdy22).isNotEmpty() && !expanded) { //later on check for view and set navigate accordingly but now simly orbid it; the easy way
            tuesdayTxt.text = "${tuesdayTxt.text} \uD83D\uDEA9"//red triangle flag
            tuesdayTxt.setOnClickListener {
                nav.navigate(R.id.schedulesFromHomeAction)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment,
                        SchedulesFragment().also {
                            it.arguments = bundleOf("startS" to dtStrTue)//Date for the schedule
                        })
                    .setReorderingAllowed(true) //Optimizing state changes for better transitions
                    .commit()
            }
        }
        //3v5
        if(SchedulesDb.getSchedWithStartDate(tdy33).isNotEmpty() && !expanded) {
            wednesdayTxt.text = "${wednesdayTxt.text} \uD83D\uDEA9"//red triangle flag
            wednesdayTxt.setOnClickListener {
                nav.navigate(R.id.schedulesFromHomeAction)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment,
                        SchedulesFragment().also {
                            it.arguments = bundleOf("startS" to dtStrWed)//Date for the schedule
                        })
                    .setReorderingAllowed(true) //Optimizing state changes for better transitions
                    .commit()
            }
        }
        //4v5
        if(SchedulesDb.getSchedWithStartDate(tdy44).isNotEmpty() && !expanded) {
            thursdayTxt.text = "${thursdayTxt.text} \uD83D\uDEA9"//red triangle flag
            thursdayTxt.setOnClickListener {
                nav.navigate(R.id.schedulesFromHomeAction)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment,
                        SchedulesFragment().also {
                            it.arguments = bundleOf("startS" to dtStrThu)//Date for the schedule
                        })
                    .setReorderingAllowed(true) //Optimizing state changes for better transitions
                    .commit()
            }
        }
        //5v5
        if(SchedulesDb.getSchedWithStartDate(tdy55).isNotEmpty() && !expanded) {
            fridayTxt.text = "${fridayTxt.text} \uD83D\uDEA9"//red triangle flag
            fridayTxt.setOnClickListener {
                nav.navigate(R.id.schedulesFromHomeAction)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment,
                        SchedulesFragment().also {
                            it.arguments = bundleOf("startS" to dtStrFri)//Date for the schedule
                        })
                    .setReorderingAllowed(true) //Optimizing state changes for better transitions
                    .commit()
            }
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

    /**
     * Show all lessons or only favorite courses
     */
    fun setViewAll(viewAll: Boolean) {
        if (view == null) return // Avoid IllegalStateException if view is not done creating

        if (this.viewAll != viewAll) {
            if(viewAll) {
                //set title
                var spprtStr = SphPlanner.prefs.getString("clss_name", "")!!
                if (spprtStr.isNullOrEmpty() || (spprtStr == "0")) spprtStr = ""
                (activity as MainActivity).supportActionBar?.title = spprtStr
            }
            else {
                (activity as MainActivity).supportActionBar?.title = SphPlanner.appContext().getString(R.string.timetable_title)
            }
            this.viewAll = viewAll

            // Recreate if pesonal timetable was for some reason not shown
            if (requireView().findViewById<RecyclerView>(R.id.mondayRecycler).adapter == null) {
                parentFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment,
                                TimetableFragment().also {
                                    it.arguments = bundleOf(
                                            "expanded" to this.expanded,
                                            "viewAll" to this.viewAll,
                                            "openOnClick" to this.openOnClick,
                                            "withChanges" to this.withChanges)
                                })
                        .setReorderingAllowed(true) //Optimizing state changes for better transitions
                        .commit()
            } else {
                // Update timetable
                timetable = TimetableDb.instance!!.get(!viewAll)
                // Get maximum amount of concurrent lessons if they are shown
                val maxConcurrent = if (viewAll) timetable.maxOf { days -> days.maxOf { it.size } } else 1
                // Cancel if timetable does not for some reason contain entries for 5 days
                if (timetable.size < 5) return
                // Notify recyclerviews

                // Set up lessons adapters
                val c = Calendar.getInstance()
                c.time = mainMonday
                c.add(Calendar.DATE, 1)
                val tdy2 = c.time
                c.add(Calendar.DATE, 1)
                val tdy3 = c.time
                c.add(Calendar.DATE, 1)
                val tdy4 = c.time
                c.add(Calendar.DATE, 1)
                val tdy5 = c.time

                (requireView().findViewById<RecyclerView>(R.id.lssnRecycler).adapter        as TimebarAdapter).setDataAndMultiple(              viewAll, maxConcurrent)
                (requireView().findViewById<RecyclerView>(R.id.mondayRecycler).adapter      as LessonsAdapter).setDataAndMultiple(
                    mainMonday, timetable[0], expanded, viewAll, maxConcurrent)
                (requireView().findViewById<RecyclerView>(R.id.tuesdayRecycler).adapter     as LessonsAdapter).setDataAndMultiple(tdy2, timetable[1], expanded, viewAll, maxConcurrent)
                (requireView().findViewById<RecyclerView>(R.id.wednesdayRecycler).adapter   as LessonsAdapter).setDataAndMultiple(tdy3, timetable[2], expanded, viewAll, maxConcurrent)
                (requireView().findViewById<RecyclerView>(R.id.thursdayRecycler).adapter    as LessonsAdapter).setDataAndMultiple(tdy4, timetable[3], expanded, viewAll, maxConcurrent)
                (requireView().findViewById<RecyclerView>(R.id.fridayRecycler).adapter      as LessonsAdapter).setDataAndMultiple(tdy5, timetable[4], expanded, viewAll, maxConcurrent)
            }
        }
    }

}