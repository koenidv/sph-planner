package de.koenidv.sph.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.objects.Change
import de.koenidv.sph.parsing.Utility
import java.text.SimpleDateFormat
import java.util.*


//  Created by koenidv on 20.12.2020.
class CompactChangesAdapter(private val changes: List<Change>,
                            private val onClick: (Change, View) -> Unit) :
        RecyclerView.Adapter<CompactChangesAdapter.ViewHolder>() {

    /**
     * Provides a reference to the type of view
     * (custom ViewHolder).
     */
    class ViewHolder(view: View, onClick: (Change, View) -> Unit) : RecyclerView.ViewHolder(view) {
        private val layout = view.findViewById<ConstraintLayout>(R.id.changeLayout)
        private val title = view.findViewById<TextView>(R.id.titleTextView)
        private val date = view.findViewById<TextView>(R.id.dateTextView)
        private val course = view.findViewById<TextView>(R.id.courseTextView)

        private var currentChange: Change? = null
        private val themeColor = applicationContext()
                .getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)
                .getInt("themeColor", 0)
        private val otherDateFormat = SimpleDateFormat("EEEE", Locale.getDefault())

        init {
            layout.setOnClickListener {
                currentChange?.let {
                    onClick(it, view)
                }
            }
        }

        fun bind(change: Change) {
            currentChange = change

            // Set title
            title.text = applicationContext().getString(
                    when (change.type) {
                        Change.TYPE_EVA -> R.string.changes_template_eva
                        Change.TYPE_CANCELLED -> R.string.changes_template_cancelled
                        Change.TYPE_FREED -> R.string.changes_template_freed
                        Change.TYPE_SUBSTITUTE -> R.string.changes_template_substitute
                        Change.TYPE_CARE -> R.string.changes_template_care
                        Change.TYPE_ROOM -> R.string.changes_template_room
                        Change.TYPE_SWITCHED -> R.string.changes_template_switched
                        Change.TYPE_EXAM -> R.string.changes_template_exam
                        else -> R.string.changes_template_other
                    })
                    .replace("%name", CoursesDb.getInstance().getFullname(change.id_course) ?: "?")
                    .replace("%substitute", (change.id_subsTeacher
                            ?: "?").capitalize(Locale.getDefault()))
                    .replace("%room", change.room ?: "?")
                    .replace("%nameOld", change.id_course_external_before ?: "?")

            // Set date
            val time = Date().time
            when {
                change.date.time - time <= -24 * 60 * 60 * 1000 -> {
                    // Past date
                    date.text = applicationContext().getString(R.string.changes_date_past)
                    Utility().tintBackground(date, applicationContext().getColor(R.color.black), 0xb4000000.toInt())
                }
                change.date.time - time <= 0 -> {
                    // Date is within the last 24 hours, which means its today
                    date.text = applicationContext().getString(R.string.changes_date_today)
                    Utility().tintBackground(date, themeColor, 0xb4000000.toInt())
                }
                change.date.time - time <= 24 * 60 * 60 * 1000 -> {
                    // Tomorrow
                    date.text = applicationContext().getString(R.string.changes_date_tomorrow)
                    Utility().tintBackground(date, applicationContext().getColor(R.color.grey_800), 0xb4000000.toInt())
                }
                else -> {
                    date.text = otherDateFormat.format(change.date)
                    Utility().tintBackground(date, applicationContext().getColor(R.color.black), 0xb4000000.toInt())
                }
            }

            // Set course
            course.text = CoursesDb.getInstance().getFullname(change.id_course)
            // Adjust course background color
            // Set background color, about 70% opacity
            Utility().tintBackground(course, CoursesDb.getInstance().getColor(change.id_course), 0xb4000000.toInt())

        }
    }

    // Creates new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_change_compact, viewGroup, false)
        return ViewHolder(view, onClick)
    }

    // Replaces the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Bind data to ViewHolder
        viewHolder.bind(changes[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = changes.size

    override fun getItemId(position: Int): Long {
        // This will in almost all cases show distinct items,
        // so we can use the position as id
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }
}