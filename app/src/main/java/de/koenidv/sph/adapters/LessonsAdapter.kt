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
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.objects.Change
import de.koenidv.sph.objects.TimetableEntry
import de.koenidv.sph.parsing.ChangeInfo
import de.koenidv.sph.parsing.CourseInfo
import de.koenidv.sph.parsing.Utility


//  Created by koenidv on 18.12.2020.
class LessonsAdapter(private var dataset: List<List<TimetableEntry>>,
                     private var expanded: Boolean = false,
                     private var multiple: Boolean = false, private var maxConcurrent: Int = 1,
                     private val onClick: (List<TimetableEntry>) -> Unit) :
        RecyclerView.Adapter<LessonsAdapter.ViewHolder>() {

    private var recyclerView: RecyclerView? = null

    /**
     * Provides a reference to the type of view
     * (custom ConversationViewHolder).
     */
    class ViewHolder(view: View, val onClick: (List<TimetableEntry>) -> Unit) : RecyclerView.ViewHolder(view) {
        val outerlayout: LinearLayout = view.findViewById(R.id.outerLayout)
        val layout: LinearLayout = view.findViewById(R.id.itemLayout)
        private val textView: TextView = view.findViewById(R.id.courseNameTextView)
        private var currentEntry: List<TimetableEntry>? = null

        init {
            // Set onClickListener from attribute
            layout.setOnClickListener {
                currentEntry?.let {
                    onClick(it)
                }
            }
        }

        fun bind(entries: List<TimetableEntry>, hourcount: Int, expanded: Boolean, multiple: Boolean, maxConcurrent: Int = 1) {
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
                    title += "${CourseInfo.getShortnameFromInternald(it.lesson.idCourse)} (${it.course?.id_teacher})"
                    if (expanded) title += "<br><small>${it.lesson.room}<br></small>"
                    title += "<br>"
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                textView.text = Html.fromHtml(title, Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION")
                textView.text = Html.fromHtml(title)
            }

            /*
             * Size
             */

            // Set size
            // Enlarge to show rooms
            // Or span multiple hours if consecutive lessons are the same
            val height = if (expanded && !multiple) 64f else if (multiple) maxConcurrent * 32f else 32f
            val extrapadding = (hourcount - 1) * 4f
            layout.layoutParams.height = Utility.dpToPx(hourcount * height + extrapadding).toInt()

            /*
             * Background color
             */

            val color: Int = if (!multiple) {
                (entries[0].course?.color
                        ?: 6168631)
            } else {
                SphPlanner.applicationContext().getColor(R.color.grey_800)
            }
            // Set background color with 40% opacity
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                (layout.background as StateListDrawable).colorFilter = BlendModeColorFilter(
                        color and 0x00FFFFFF or 0x66000000, BlendMode.SRC_ATOP)
            } else {
                @Suppress("DEPRECATION") // not in < Q
                (layout.background as StateListDrawable)
                        .setColorFilter(color and 0x00FFFFFF or 0x66000000, PorterDuff.Mode.SRC_ATOP)
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

    // Replaces the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Bind data to ConversationViewHolder
        // sph will not mix different rowspans, therefore we can just check the first item
        if (!dataset.getOrNull(position).isNullOrEmpty()
                && dataset[position][0].lesson.isDisplayed != true) {

            // Check if the next lessons and changes are the same
            // Ignore rooms if not expanded
            // Ignore changes if concurrent courses are shown
            // Hide them if they are
            var hourcount = 1
            val firstEntry = dataset[position][0]
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
                viewHolder.outerlayout.setPadding(0, 0, 0, Utility.dpToPx(4f).toInt())

            // Bind lesson to view
            viewHolder.bind(dataset[position], hourcount, expanded, multiple, maxConcurrent)

        } else if (!dataset.getOrNull(position).isNullOrEmpty()
                && dataset[position][0].lesson.isDisplayed == true) {
            // Lesson is already displayed, hide completely
            viewHolder.outerlayout.visibility = View.GONE
        } else {
            // No lesson for this hour
            viewHolder.outerlayout.visibility = View.INVISIBLE
            // We still need to apply the correct size
            val height = if (expanded) 64f else 32f
            viewHolder.layout.layoutParams.height = Utility.dpToPx(height).toInt()
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataset.size

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    fun setExpanded(expanded: Boolean) {
        this.expanded = expanded
        notifyDataSetChanged()
    }

    fun setDataAndMultiple(newDataset: List<List<TimetableEntry>>, multiple: Boolean, maxConcurrent: Int) {
        this.dataset = newDataset
        this.multiple = multiple
        this.maxConcurrent = maxConcurrent
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
                        text.text = SphPlanner.applicationContext()
                                .getString(R.string.timetable_change_template)
                                .replace("%shortname", CourseInfo.getNameAbbreviation(change.id_course!!))
                                .replace("%type", change.room.toString())
                    } else {
                        text.text = SphPlanner.applicationContext()
                                .getString(R.string.timetable_change_template)
                                .replace("%shortname", CourseInfo.getNameAbbreviation(change.id_course!!))
                                .replace("%type", ChangeInfo.getTypeNameAbbreviation(change.type))
                    }
                }
            }
        }
    }

}