package de.koenidv.sph.adapters

import android.app.Activity
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.color.colorChooser
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.database.PostsDb
import de.koenidv.sph.database.TasksDb
import de.koenidv.sph.objects.Course
import de.koenidv.sph.parsing.Utility

//  Created by koenidv on 18.12.2020.
class CoursesAdapter(private val courses: MutableList<Course>,
                     private val activity: Activity,
                     private val onClick: (Course) -> Unit) :
        RecyclerView.Adapter<CoursesAdapter.ViewHolder>() {

    // Show a bottom sheet for managing the course on long click
    private val onLongClick = { course: Course, position: Int ->

        val sheet = BottomSheetDialog(activity)
        sheet.setContentView(R.layout.sheet_manage_course)

        val markRead = sheet.findViewById<TextView>(R.id.readTextView)
        val changeColor = sheet.findViewById<TextView>(R.id.colorTextView)

        // Mark all posts as read
        markRead?.setOnClickListener {
            PostsDb.getInstance().markCourseAsRead(course.courseId)
            notifyItemChanged(position)
            sheet.dismiss()
        }

        // Change course color
        changeColor?.setOnClickListener {
            // Get all colors used by default
            val colorPresets = Utility.parseStringArray(R.array.course_colors).map {
                Color.parseColor(it.value)
            }.toIntArray()

            // Show a color picker dialog
            MaterialDialog(activity, BottomSheet()).show {
                colorChooser(
                        colors = colorPresets,
                        initialSelection = course.color,
                        allowCustomArgb = true) { _: MaterialDialog, color: Int ->

                    // Save color to db
                    CoursesDb.setColor(course.courseId, color)
                    // Update dataset and recycler
                    course.color = color
                    courses[position] = course
                    notifyItemChanged(position)
                }
                positiveButton(R.string.save)
                negativeButton(R.string.cancel)
                title(text = course.fullname)
            }

            sheet.dismiss()
        }


        sheet.show()

    }

    /**
     * Provides a reference to the type of view
     * (custom ViewHolder).
     */
    class ViewHolder(view: View, val onClick: (Course) -> Unit, onLongClick: (Course, Int) -> Unit) : RecyclerView.ViewHolder(view) {
        val layout: LinearLayout = view.findViewById(R.id.itemLayout)
        private val nameText: TextView = view.findViewById(R.id.courseNameTextView)
        private val infoText: TextView = view.findViewById(R.id.courseInfoTextView)
        private var currentCourse: Course? = null

        init {
            // Set onClickListener from attribute
            layout.setOnClickListener {
                currentCourse?.let {
                    onClick(it)
                }
            }
            layout.setOnLongClickListener {
                currentCourse?.let {
                    onLongClick(it, adapterPosition)
                }
                true
            }
        }

        fun bind(course: Course) {
            currentCourse = course

            val openPosts = PostsDb.getInstance().getUnreadByCourseIdCount(course.courseId)
            val openTasks = TasksDb.getInstance().getUndoneByCourseIdCount(course.courseId)

            /*
             * Text
             */

            // Set data
            var name = course.fullname
            if (course.isLK == true) name += SphPlanner.applicationContext().getString(R.string.course_appendix_lk)
            nameText.text = name
            // Create info text
            var infotext = ""
            if (openTasks > 0) {
                if (infotext != "") infotext += "\n"
                infotext += SphPlanner.applicationContext().resources.getQuantityString(R.plurals.course_info_undone_tasks, openTasks, openTasks)
            }
            if (openPosts > 0) {
                if (infotext != "") infotext += "\n"
                infotext += SphPlanner.applicationContext().resources.getQuantityString(R.plurals.course_info_unread_posts, openPosts, openPosts)
            }
            // Set info text if it is not empty
            if (infotext != "") {
                infoText.text = infotext
                infoText.visibility = View.VISIBLE
            } else {
                infoText.visibility = View.GONE
            }

            /*
             * Background color
             */

            // Set background color, about 70% opacity
            Utility.tintBackground(layout, course.color ?: 6168631, 0xb4000000.toInt())
        }
    }

    // Creates new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_course, viewGroup, false)

        return ViewHolder(view, onClick, onLongClick)
    }

    // Replaces the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Bind data to ViewHolder
        viewHolder.bind(courses[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = courses.size

}