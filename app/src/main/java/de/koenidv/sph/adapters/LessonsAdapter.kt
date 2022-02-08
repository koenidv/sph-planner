package de.koenidv.sph.adapters

import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.date.dayOfMonth
import com.afollestad.date.month
import com.afollestad.date.year
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.SphPlanner.Companion.lssn_ps
import de.koenidv.sph.database.HolidaysDb
import de.koenidv.sph.database.SchedulesDb
import de.koenidv.sph.debugging.DebugLog
import de.koenidv.sph.objects.Change
import de.koenidv.sph.objects.TimetableEntry
import de.koenidv.sph.parsing.ChangeInfo
import de.koenidv.sph.parsing.CourseInfo
import de.koenidv.sph.parsing.Utility
import de.koenidv.sph.parsing.Utility.toPx
import java.time.LocalTime
import java.util.*


//  Created by koenidv on 18.12.2020.
//  Extended by StKl 05.12.2021
class LessonsAdapter(private var dt: Date, //Current date from Mon till Fri of the relevant week, already adapted in TimetableViewFragment.kt
                     private var dataset: List<List<TimetableEntry>>,
                     private var tmbr: Array<Array<LocalTime>>,
                     private var expanded: Boolean = false,
                     private var multiple: Boolean = false,
                     private var maxConcurrent: Int = 1,
                     private val onClick: (List<TimetableEntry>) -> Unit) :
        RecyclerView.Adapter<LessonsAdapter.ViewHolder>() {

    private var recyclerView: RecyclerView? = null

    /**
     * Provides a reference to the type of view
     * (custom ConversationViewHolder).
     */
    inner class ViewHolder(view: View, val onClick: (List<TimetableEntry>) -> Unit) : RecyclerView.ViewHolder(view) {
        //Contains the box of each course with another linear layout and text view therein
        val outerlayout: LinearLayout = view.findViewById(R.id.outerLayout)
        //Contains the text view and ist part of outer layout above
        val layout: LinearLayout = view.findViewById(R.id.itemLayout)
        //Is the course name
        val textView: TextView = view.findViewById(R.id.courseNameTextView)
        //Stundenplan Liste: Leer
        private var currentEntry: List<TimetableEntry>? = null

        init {
            // Set onClickListener from attribute
            layout.setOnClickListener {
                currentEntry?.let {
                    onClick(it)
                }
            }
        }

        //could work with dataset variable as well, but could lead to incosistencies => Only work with function prameters!
        fun bind(entries: List<TimetableEntry>, dat: Date, position: Int, hourcount: Int, expanded: Boolean, multiple: Boolean, maxConcurrent: Int = 1) {
            val checkCal = Calendar.getInstance()
            checkCal.time = dt

            if (!multiple) currentEntry = entries

            /*
             * Text
             */

            // Set data
            var title = ""
            if (!multiple) {
                title = CourseInfo.getShortnameFromInternald(entries[0].lesson.idCourse)
                if (expanded) title += "<br><small>${entries[0].lesson.room}</small>"
            } else {
                entries.forEach {
                    title += CourseInfo.getShortnameFromInternald(it.lesson.idCourse)
                    if (it.course?.id_teacher != "") {
                        title += "(${it.course?.id_teacher})"
                    }
                    if (expanded) title += "<br><small>${it.lesson.room}<br></small>"
                    title += "<br>"
                }
            }

            // In case of exam => Set red triangle flag " \uD83D\uDEA9"
            val schedules = SchedulesDb.getSchedWithStartDate(dat)//Exam tdy?
            var exam = false
            if (schedules.isNotEmpty()) {
                for (entries1 in schedules) {
                    if ((entries1.hr != "") && (entries1.src == "portal")) {
                        val hArr = entries1.hr.split("#").toMutableList()
                        for (h in hArr) {
                            try {
                                if (h.toInt() == (entries[0].lesson.hour/*position + 1*/)) {
                                    exam = true
                                }
                            } catch (nfe: NumberFormatException) {
                                //nfe.printStackTrace()
                            }
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                textView.text = Html.fromHtml(title, Html.FROM_HTML_MODE_LEGACY).toString()
            } else {
                @Suppress("DEPRECATION")
                textView.text = Html.fromHtml(title).toString()
            }
            if (exam) {
                var spprtStr = textView.text
                spprtStr = "$spprtStr \uD83D\uDEA9"//red triangle flag
                textView.text = spprtStr
            }

            /*
             * Size
             */

            // Set size
            // Enlarge to show rooms
            // Or span multiple hours if consecutive lessons are the same
            val height =
                if (expanded && !multiple) 64f else if (multiple) maxConcurrent * 32f else 32f
            val extrapadding = (hourcount - 1) * 4f
            //layout.layoutParams.height = Utility.dpToPx(hourcount * height + extrapadding).toInt()
            layout.layoutParams.height = (hourcount * height + extrapadding).toInt().toPx()

            //Current Lesson? => Thin red stroke
            val nw =
                Date() //We use only hour and minutes, so year, month, day is still used from dt
            val start = tmbr[0][0]
            var end = tmbr[0][1]
            var i = 0
            while (tmbr[i][1] != LocalTime.of(0, 0)) {
                end = tmbr[i][1]
                i++
            }
            val c = Calendar.getInstance()
            c.time = Date()
            c.set(Calendar.HOUR_OF_DAY, start.hour)
            c.set(Calendar.MINUTE, start.minute - 1)
            val cmpStart = c.time
            c.set(Calendar.HOUR_OF_DAY, end.hour)
            c.set(Calendar.MINUTE, end.minute)
            val cmpEnd = c.time

            if ((nw.day == dat.day) /*&& (nw.month == dat.month)  && (nw.year == dat.year)*/
            ) {
                //textView.text = position.toString() + lssn_ps.toString() + cmpStart.toString() + ":" + cmpEnd.toString()//stkl
                if ((((position + 1) <= lssn_ps) && (lssn_ps <= (position + hourcount))) &&
                    (nw.after(cmpStart) && nw.before(cmpEnd)) &&
                    (lssn_ps > 0)
                ) {
                    layout.setBackgroundResource(R.drawable.background_red)
                }
            }

            /*
             * Background color
             */

            val color: Int = if (!multiple) {
                (entries[0].course?.color
                    ?: 6168631)
            } else {
                SphPlanner.appContext().getColor(R.color.grey_800)
            }
            // Set background color with 40% opacity
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //Freistunde = course with entry free = Set to 100% alpha to see background only, no specific color
                if (entries[0].course?.courseId == "free") {
                    (layout.background as StateListDrawable).colorFilter = BlendModeColorFilter(
                        color and 0x00FFFFFF or 0x00000000, BlendMode.SRC_ATOP
                    )
                } else {
                    (layout.background as StateListDrawable).colorFilter = BlendModeColorFilter(
                        color and 0x00FFFFFF or 0x66000000, BlendMode.SRC_ATOP
                    )
                }
            } else {
                if (entries[0].course?.courseId == "free") {
                    @Suppress("DEPRECATION") // not in < Q
                    (layout.background as StateListDrawable)
                        .setColorFilter(
                            color and 0x00FFFFFF or 0x00000000,
                            PorterDuff.Mode.SRC_ATOP
                        )
                } else {
                    @Suppress("DEPRECATION") // not in < Q
                    (layout.background as StateListDrawable)
                        .setColorFilter(
                            color and 0x00FFFFFF or 0x66000000,
                            PorterDuff.Mode.SRC_ATOP
                        )
                }
            }
        }
    }

    // Creates new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_lesson, viewGroup, false)

        return ViewHolder(view, onClick)
    }

    //Example StKl 04.12.2021
    // Timetable Thursday [
    // [TimetableEntry(lesson=Lesson(
    // idCourse=bio_tur_1, day=3, hour=1, room=215, isDisplayed=null), course=Course(courseId=bio_tur_1, gmb_id=bio, sph_id=051BIO01 - F, named_id=Bio 05f1, number_id=null, fullname=Biology, id_teacher=tur, isFavorite=true, isLK=false, color=-10608585), changes=null)],
    // [TimetableEntry(lesson=Lesson(
    // idCourse=d_wz_1, day=3, hour=2, room=025, isDisplayed=null), course=Course(courseId=d_wz_1, gmb_id=d, sph_id=051D03 - F, named_id=Deutsch 05f1, number_id=null, fullname=German, id_teacher=wz, isFavorite=true, isLK=false, color=-2097152), changes=null)],
    // [TimetableEntry(lesson=Lesson(
    // idCourse=d_wz_1, day=3, hour=3, room=025, isDisplayed=null), course=Course(courseId=d_wz_1, gmb_id=d, sph_id=051D03 - F, named_id=Deutsch 05f1, number_id=null, fullname=German, id_teacher=wz, isFavorite=true, isLK=false, color=-2097152), changes=null)],
    // [TimetableEntry(lesson=Lesson(
    // idCourse=gl_koc_1, day=3, hour=4, room=025, isDisplayed=null), course=Course(courseId=gl_koc_1, gmb_id=gl, sph_id=051GL01 - F, named_id=Gesellschaftslehre 05f1, number_id=null, fullname=Gl, id_teacher=koc, isFavorite=true, isLK=false, color=-10608585), changes=null)],
    // [TimetableEntry(lesson=Lesson(
    // idCourse=gl_koc_1, day=3, hour=5, room=025, isDisplayed=null), course=Course(courseId=gl_koc_1, gmb_id=gl, sph_id=051GL01 - F, named_id=Gesellschaftslehre 05f1, number_id=null, fullname=Gl, id_teacher=koc, isFavorite=true, isLK=false, color=-10608585), changes=null)]]
    // Bundle[{}]

    /*
    Called by RecyclerView to display the data at the specified position.
    This method should update the contents of the RecyclerView.ViewHolder.itemView to reflect the item at the given position.
    Invoked (aufrufen/ aktivieren) by the layout manager for every recyclerview => for every day
     */
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {//position := position in recycler view := +1 = lesson
        // Bind data to ConversationViewHolder
        // sph will not mix different rowspans, therefore we can just check the first item
        DebugLog("LssnAdpt", "LssnsAdpt - Data: " + dataset[position][0].toString())

        val tdy = dt
        val c = Calendar.getInstance()
        c.time = tdy
        //Check if holidays are active and set content accordingly
        val futureHolidays  = HolidaysDb().getNext(tdy)
        val currentHolidays = HolidaysDb().getCurrent(tdy)

        val d = Calendar.getInstance()
        val e = Calendar.getInstance()
        if(futureHolidays != null) {
            d.time = futureHolidays.start
        }
        else if (currentHolidays != null) {
            e.time = currentHolidays.start
        }

        if(
            (
            ( (futureHolidays != null) &&
              (    (tdy.after(futureHolidays.start) && tdy.before(futureHolidays.end))
                || ((c.year == d.year) && (c.month == d.month) && (c.dayOfMonth == d.dayOfMonth))
              )
            ) ||
            (   (currentHolidays != null) &&
              (    (tdy.after(currentHolidays.start) && tdy.before(currentHolidays.end))
                || ((c.year == e.year) && (c.month == e.month) && (c.dayOfMonth == e.dayOfMonth))
              )
            )
            )
            //&& ((dataset[position][0].lesson.day + 2) == c.get(Calendar.DAY_OF_WEEK) ) /* MON := 0, ... VS. SUN := 1, MON := 2, ... */
        ) {
            //Prepare some random addition to holidays
            val hldyAdder = listOf(
                "\uD83C\uDF04", "\uD83C\uDF08", "\uD83C\uDF1E",
                "\uD83C\uDF89", "\uD83C\uDF3B", "\uD83C\uDF41",
                "\uD83C\uDF53", "\uD83C\uDF69", "\uD83C\uDF94",
                "\uD83C\uDF9D"
            )
            val rnds = (0 until (hldyAdder.size - 1)).random()

            if ((futureHolidays != null) &&
                (    (tdy.after(futureHolidays.start) && tdy.before(futureHolidays.end))
                        || ((c.year == d.year) && (c.month == d.month) && (c.dayOfMonth == d.dayOfMonth))
                        )
            ) { //e.g. 23.12.2021 - 09-01.2022
                //Today is a holiday, no school => Change dataset
                if (position == 0) {//1st hour shows holidays
                    //Replace idCourse with "hols"
                    dataset[position][0].lesson.idCourse =
                        futureHolidays.name + hldyAdder[rnds] //vac x
                    dataset[position][0].course?.courseId = futureHolidays.name //vac
                    dataset[position][0].lesson.room = "" //x
                    dataset[position][0].course?.gmb_id = "hols"
                    dataset[position][0].course?.sph_id = "0"
                    dataset[position][0].course?.named_id = "hols"
                    dataset[position][0].course?.id_teacher = "" //x
                    dataset[position][0].lesson.isDisplayed = null
                    //Set alpha value to 0 (transparent)
                    viewHolder.layout.background.alpha = 0
                } else {//Other hours will not be displayed in this case
                    dataset[position][0].lesson.isDisplayed = true
                }
            } else if ((currentHolidays != null) &&
                (    (tdy.after(currentHolidays.start) && tdy.before(currentHolidays.end))
                        || ((c.year == e.year) && (c.month == e.month) && (c.dayOfMonth == e.dayOfMonth))
                        )
            ) {
                //Today is a holiday, no school => Change dataset
                if (position == 0) {//1st hour shows holidays
                    //Replace idCourse with "hols"
                    dataset[position][0].lesson.idCourse =
                        currentHolidays.name + hldyAdder[rnds] //vac x
                    dataset[position][0].course?.courseId = currentHolidays.name //vac
                    dataset[position][0].lesson.room = "" //x
                    dataset[position][0].course?.gmb_id = "hols"
                    dataset[position][0].course?.sph_id = "0"
                    dataset[position][0].course?.named_id = "hols"
                    dataset[position][0].course?.id_teacher = "" //x
                    dataset[position][0].lesson.isDisplayed = null
                    //Set alpha value to 0 (transparent)
                    viewHolder.layout.background.alpha = 0
                } else {//Other hours will not be displayed in this case
                    dataset[position][0].lesson.isDisplayed = true
                }
            }
        }
        //else => Unchanged dataset to use (NO holidays)

        if (!dataset.getOrNull(position).isNullOrEmpty() /*List not empty*/
                && (dataset[position][0].lesson.isDisplayed != true) /*Standard null*/
                //&& ((dataset[position][0].lesson.day + 2) == c.get(Calendar.DAY_OF_WEEK) ) /* MON := 0, ... VS. SUN := 1, MON := 2, ... */
        ) {

            // Check if the next lessons and changes are the same
            // Ignore rooms if not expanded
            // Ignore changes if concurrent (uebereinstimmende) courses are shown
            // Hide them if they are
            var hourcount = 1
            val firstEntry = dataset[position][0] //first lesson
            while (dataset.getOrNull(position + hourcount)?.find {
                        // Same lessons require the same course
                        // We can just check for the first entry as sph will not mix different rowspans
                        it.lesson.idCourse == firstEntry.lesson.idCourse
                                // Check room only if expanded, else it won't be shown
                                && (!expanded || it.lesson.room == firstEntry.lesson.room)
                                // Check changes only for first item, we're not going to show changes on concurrent lessons
                                && (multiple || it.changes == firstEntry.changes)
                    } != null) {
                // Hide next lesson
                dataset[position + hourcount][0].lesson.isDisplayed = true
                // Get the next, next lessen
                hourcount++
            }

            // Make sure ConversationViewHolder is visible after layout changed
            viewHolder.outerlayout.visibility = View.VISIBLE

            // Remove padding if this is the last visible element
            if (position == dataset.size - hourcount)
                viewHolder.outerlayout.setPadding(0, 0, 0, 0)
            // Make sure a recycled item does have padding
            else if (viewHolder.outerlayout.paddingBottom == 0)
                viewHolder.outerlayout.setPadding(0, 0, 0, (4f).toInt().toPx()) //Distance between the hours, If you change this, you have to pay attention to timebar also
                //viewHolder.outerlayout.setPadding(0, 0, 0, Utility.dpToPx(4f).toInt()) //Distance between the hours, If you change this, you have to pay attention to timebar also



            // Bind lesson to view
            viewHolder.bind(dataset[position], dt, position, hourcount, expanded, multiple, maxConcurrent)

        } else if (!dataset.getOrNull(position).isNullOrEmpty()
                && (dataset[position][0].lesson.isDisplayed == true)
                //&& ((dataset[position][0].lesson.day + 2) == c.get(Calendar.DAY_OF_WEEK) ) /* MON := 0, ... VS. SUN := 1, MON := 2, ... */
        ) {
            // Lesson is already displayed, hide completely
            viewHolder.outerlayout.visibility = View.GONE
        } else {
            // No lesson for this hour
            viewHolder.outerlayout.visibility = View.INVISIBLE
            // We still need to apply the correct size
            val height = if (expanded) 64f else 32f
            //viewHolder.layout.layoutParams.height = Utility.dpToPx(height).toInt()
            viewHolder.layout.layoutParams.height = (height).toInt().toPx()
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataset.size

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    /* Never used
    fun setExpanded(expanded: Boolean) {
        this.expanded = expanded
        notifyDataSetChanged()
    }
    */

    fun setDataAndMultiple(nwDt: Date, newDataset: List<List<TimetableEntry>>, expanded: Boolean, multiple: Boolean, maxConcurrent: Int) {
        this.dataset = newDataset
        this.multiple = multiple
        this.maxConcurrent = maxConcurrent
        this.dt = nwDt
        this.expanded = expanded
        notifyDataSetChanged()
    }

    fun setTiming(nwDt: Date) {
        //this.dataset = newDataset
        this.dt = nwDt
        notifyDataSetChanged()
    }

    /**
     * Display a list of changes on the timetable
     */
    fun applyChanges(changes: List<Change>) {
        // Get the RecyclerView's layout manager
        val layoutManager = recyclerView?.layoutManager
        if (layoutManager != null) {
            var color: Int
            var layout: View?
            var text: TextView?
            // Apply each change
            for (change in changes) {
                // Only display EVA right now
                color = ChangeInfo.getTypeColor(change.type)
                //color = SphPlanner.applicationContext().getColor(R.color.grey_800)
                // Try to find the corresponding view
                layout = layoutManager.findViewByPosition(change.lessons.first() - 1)
                text = layout?.findViewById(R.id.courseNameTextView)
                // If the view was found
                if (layout != null && text != null && change.id_course != null) {
                    // Apply a background color per type
                    Utility.tintBackground(
                            layout.findViewById(R.id.itemLayout),
                            color,
                            0x80000000.toInt())
                    // Add change type to text
                    if (change.type == Change.TYPE_ROOM) {
                        text.text = SphPlanner.appContext()
                                .getString(R.string.timetable_change_template)
                                .replace("%shortname", CourseInfo.getNameAbbreviation(change.id_course!!))
                                .replace("%type", change.room.toString())
                    } else {
                        text.text = SphPlanner.appContext()
                                .getString(R.string.timetable_change_template)
                                .replace("%shortname", CourseInfo.getNameAbbreviation(change.id_course!!))
                                .replace("%type", ChangeInfo.getTypeNameAbbreviation(change.type))
                    }
                }
            }
        }
    }

}