package de.koenidv.sph.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.objects.Change
import de.koenidv.sph.parsing.ChangeParser
import de.koenidv.sph.parsing.CourseParser
import de.koenidv.sph.parsing.Utility
import java.text.SimpleDateFormat
import java.util.*


//  Created by koenidv on 20.12.2020.
class ChangesAdapter(private val changes: List<Change>,
                     private val onCourseClick: (courseId: String) -> Unit) :
        RecyclerView.Adapter<ChangesAdapter.ViewHolder>() {

    private var currentDate = Date()

    // todo date is not shown when filtering for favorites and change didn't show date before

    /**
     * Provides a reference to the type of view
     * (custom ViewHolder).
     */
    class ViewHolder(view: View, onCourseClick: (String) -> Unit) : RecyclerView.ViewHolder(view) {
        private val layout = view.findViewById<LinearLayout>(R.id.changeLayout)
        private val date = view.findViewById<TextView>(R.id.dateTextView)
        private val title = view.findViewById<TextView>(R.id.titleTextView)
        private val description = view.findViewById<TextView>(R.id.descriptionTextView)
        private val course = view.findViewById<TextView>(R.id.courseTextView)
        private val courseLayout = view.findViewById<LinearLayout>(R.id.courseLayout)

        private var currentChange: Change? = null
        private val themeColor = applicationContext()
                .getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)
                .getInt("themeColor", 0)
        private val otherDateFormat = SimpleDateFormat("EEEE", Locale.getDefault())

        init {
            courseLayout.setOnClickListener {
                currentChange?.id_course?.let {
                    onCourseClick(it)
                }
            }
        }

        fun bind(change: Change, currentDate: Date) {
            currentChange = change

            // Check if the attached course is favorite
            val isFavorite = CoursesDb.getInstance().isFavorite(change.id_course)

            // Set title
            var titletext = applicationContext().getString(
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
                    .replace("%substitute", (change.id_subsTeacher
                            ?: "?").capitalize(Locale.getDefault()))
                    .replace("%room", change.room ?: "?")
                    .replace("%nameOld", change.id_course_external_before ?: "?")

            titletext = if (isFavorite == true) {
                titletext.replace("%name",
                        CoursesDb.getInstance().getFullname(change.id_course) ?: "?")
            } else {
                titletext.replace("%name",
                        CourseParser().getFullnameFromInternald(change.id_course ?: "?")
                                + if (change.id_teacher != null) " (${change.id_teacher!!.capitalize(Locale.getDefault())})" else "")
            }

            title.text = titletext

            // Set description
            if (change.description != null) {
                description.visibility = View.VISIBLE
                description.text = change.description
            } else description.visibility = View.GONE

            // Set date
            if (change.date != currentDate) {
                date.visibility = View.VISIBLE
                val time = Date().time
                date.text = when {
                    change.date.time - time <= -24 * 60 * 60 * 1000 ->
                        // Past date
                        applicationContext().getString(R.string.changes_date_past)
                    change.date.time - time <= 0 ->
                        // Date is within the last 24 hours, which means its today
                        applicationContext().getString(R.string.changes_date_today)
                    change.date.time - time <= 24 * 60 * 60 * 1000 ->
                        // Tomorrow
                        applicationContext().getString(R.string.changes_date_tomorrow)
                    else -> otherDateFormat.format(change.date)
                }
            } else {
                date.visibility = View.GONE
            }

            // Set course
            if (isFavorite != true || change.id_course == null) courseLayout.visibility = View.GONE
            else {
                courseLayout.visibility = View.VISIBLE
                course.text = CoursesDb.getInstance().getFullname(change.id_course)
                // Adjust course background color
                // Set background color, about 70% opacity
                Utility().tintBackground(course, CoursesDb.getInstance().getColor(change.id_course), 0xb4000000.toInt())
            }

            // Set background color according to change type (15% opacity)
            Utility().tintBackground(layout, ChangeParser().getTypeColor(change.type), 0x26000000)

        }
    }

    // Creates new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_change, viewGroup, false)
        return ViewHolder(view, onCourseClick)
    }

    // Replaces the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Bind data to ViewHolder
        viewHolder.bind(changes[position], currentDate)
        currentDate = changes[position].date
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = changes.size
}