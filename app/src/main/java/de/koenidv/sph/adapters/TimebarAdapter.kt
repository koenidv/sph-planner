package de.koenidv.sph.adapters

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R.*
import de.koenidv.sph.SphPlanner.Companion.lssn_ps
import de.koenidv.sph.database.HolidaysDb
import de.koenidv.sph.parsing.Utility
import java.time.*
import java.util.*


// Created by koenidv on 18.12.2020.
class TimebarAdapter(
                     private var dt: Date,
                     private var bckgrnd: Drawable?,
                     private var dataset: Array<Array<LocalTime>>,
                     private var expanded: Boolean = false,
                     private var multiple: Boolean = false,
                     private var maxConcurrent: Int = 1
        ) :
        RecyclerView.Adapter<TimebarAdapter.ViewHolder>() {

    private var recyclerView: RecyclerView? = null

    /**
     * Provides a reference to the type of view
     * (custom ConversationViewHolder).
     */
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        //Bracketlayout
        val timebarLay: LinearLayout = view.findViewById(id.timebarlayout)
        //Is the timing in small
        private val textViewStartSmall: TextView = view.findViewById(id.timebarTimeStart)
        private val textViewEndSmall: TextView = view.findViewById(id.timebarTimeEnd)
        //Is the hour in large
        private val textViewLarge: TextView = view.findViewById(id.timebarHour)
        //Array<Array<LocalTime>>: Leer
        private var tmbr: Array<LocalTime>? = null

        //Nothing on click at the elements of timebar at the moment
        init {
            // Set onClickListener from attribute
            timebarLay.setOnClickListener {
                tmbr?.let {
                    //onClick(it)
                }
            }
        }

        fun bind(entries: Array<LocalTime>, position: Int, expanded: Boolean) {
            tmbr = entries

            /*
             * Text
             */

            val start  = entries[0]
            val end    = entries[1]
            val now    = LocalTime.now()

            // Set data
            textViewLarge.text = (position+1).toString()
            if((now >= start) && (now <= end)) {
                //Set background to indicate current lesson
                textViewLarge.background = bckgrnd
                //Set global lssn
                lssn_ps = (position+1)
            }

            textViewStartSmall.text = entries[0].toString()
            textViewEndSmall.text = entries[1].toString()

            /*
             * Size
             */

            // Set size
            // Enlarge to show rooms
            // Or span multiple hours if consecutive lessons are the same
            val height = if (expanded && !multiple) 64f  else if (multiple) maxConcurrent * 32f  else 32f
            val extrapadding = 4f
            timebarLay.layoutParams.height = Utility.dpToPx(/*hourcount * */height + extrapadding).toInt()
        }
    }

    // Creates new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(layout.item_timebar, viewGroup, false)

        return ViewHolder(view)
    }

    // Replaces the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        //Check if we are in holiday for the complete week
        //<yes> => lssnMaxWeek = 1
        //<no>  => Do nothing because at least one day contains timetable and dataset is giving the information
        //Has to be done in parallel to LessonsAdapter/ lssnMaxWeek becaue we are running late here always
        var tdy = dt //The ultimative Monday
        val c = Calendar.getInstance()
        c.time = tdy

        var lssnMaxWeek = position+1

        var hldysCmpltWk = 0 // := 5? Complete week is holiday
        while(c.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
            tdy = c.time
            val futureHolidays  = HolidaysDb().getNext(tdy)
            val currentHolidays = HolidaysDb().getCurrent(tdy)
            if(
                ( (futureHolidays != null) &&
                        !(   tdy.before(futureHolidays.start)  || tdy.after(futureHolidays.end)   )
                        ) ||
                (   (currentHolidays != null) &&
                        !(   tdy.before(currentHolidays.start) || tdy.after(currentHolidays.end)   )
                        )
            ) {
                hldysCmpltWk++
            }
            c.add(Calendar.DATE, 1)
        }
        if(hldysCmpltWk >= 5) {
            lssnMaxWeek = 1 //Assigned value has to be in sync with LessonAdapter
        }

        //In case we have a valid start time for timebar element
        if (    (dataset[position][0] != LocalTime.of(0, 0)) &&
                ((position+1) <= lssnMaxWeek)
        ) {
            //Handle this element

            // Make sure ConversationViewHolder is visible
            viewHolder.timebarLay.visibility = View.VISIBLE

            // Give it to bind for further processing
            viewHolder.bind(dataset[position], position, expanded)
        }
        //We have no valid start time => Sign for we do not need to handle it
        else {
            // Hide completely
            viewHolder.timebarLay.visibility = View.GONE
        }
    }

    fun setDataAndMultiple(multiple: Boolean, maxConcurrent: Int) {
        this.multiple = multiple
        this.maxConcurrent = maxConcurrent
        notifyDataSetChanged()
    }

    fun setTiming(nwDt: Date) {
        this.dt = nwDt
        notifyDataSetChanged()
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataset.size

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }



}