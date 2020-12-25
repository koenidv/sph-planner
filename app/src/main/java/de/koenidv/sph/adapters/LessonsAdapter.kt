package de.koenidv.sph.adapters

import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.objects.Course
import de.koenidv.sph.objects.TimetableEntry
import de.koenidv.sph.parsing.Utility

//  Created by koenidv on 18.12.2020.
class LessonsAdapter(private val dataset: List<List<TimetableEntry>>, private val onClick: (Course) -> Unit) :
        RecyclerView.Adapter<LessonsAdapter.ViewHolder>() {

    /**
     * Provides a reference to the type of view
     * (custom ViewHolder).
     */
    class ViewHolder(view: View, val onClick: (Course) -> Unit) : RecyclerView.ViewHolder(view) {
        val layout: LinearLayout = view.findViewById(R.id.itemLayout)
        private val nameText: TextView = view.findViewById(R.id.courseNameTextView)
        private var currentCourse: Course? = null

        init {
            // Set onClickListener from attribute
            layout.setOnClickListener {
                currentCourse?.let {
                    onClick(it)
                }
            }
        }

        fun bind(entry: TimetableEntry, hourcount: Int) {
            currentCourse = entry.course

            // Set data
            nameText.text = entry.course?.fullname

            // Enlarge if same as next lesson
            if (hourcount > 1) {
                val params = layout.layoutParams
                params.height = Utility().dpToPx(hourcount * 32f + 4f).toInt()
                layout.layoutParams = params
            }

            // Set background color with 40% opacity
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                (layout.background as StateListDrawable).colorFilter = BlendModeColorFilter(
                        (entry.course?.color
                                ?: 6168631) and 0x00FFFFFF or 0x66000000, BlendMode.SRC_ATOP)
            } else {
                @Suppress("DEPRECATION") // not in < Q
                (layout.background as StateListDrawable)
                        .setColorFilter((entry.course?.color
                                ?: 6168631) and 0x00FFFFFF or 0x66000000, PorterDuff.Mode.SRC_ATOP)
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
        // Bind data to ViewHolder
        if (dataset[position].isNotEmpty()
                && dataset[position][0].lesson.isDisplayed != true) {

            // Check if the next lessons are the same
            // Hide them if they are
            var hourcount = 1
            val thisLesson = dataset[position][0].lesson
            var nextLesson = dataset.getOrNull(position + hourcount)?.getOrNull(0)?.lesson
            while (nextLesson != null
                    && nextLesson.idCourse == thisLesson.idCourse
                    && nextLesson.room == thisLesson.room) {
                // Hide next lesson
                dataset[position + hourcount][0].lesson.isDisplayed = true
                // Get the next, next lessen
                hourcount++
                nextLesson = dataset.getOrNull(position + hourcount)?.getOrNull(0)?.lesson
            }

            // Bind lesson to view
            viewHolder.bind(dataset[position][0], hourcount)

        } else if (dataset[position].isNotEmpty()
                && dataset[position][0].lesson.isDisplayed == true)
        // Lesson is hidden
            viewHolder.layout.visibility = View.GONE
        else
        // No lesson for this hour
            viewHolder.layout.visibility = View.INVISIBLE
        // Todo support concurrent lessons
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataset.size

}