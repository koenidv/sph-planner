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
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.objects.Course
import de.koenidv.sph.objects.Post
import de.koenidv.sph.objects.PostTask

//  Created by koenidv on 18.12.2020.
class CoursesAdapter(private val courses: List<Course>,
                     private val unreadPosts: List<Post>,
                     private val tasks: List<PostTask>,
                     private val onClick: (Course) -> Unit) :
        RecyclerView.Adapter<CoursesAdapter.ViewHolder>() {

    /**
     * Provides a reference to the type of view
     * (custom ViewHolder).
     */
    class ViewHolder(view: View, val onClick: (Course) -> Unit) : RecyclerView.ViewHolder(view) {
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
        }

        fun bind(course: Course, openPosts: Int, openTasks: Int) {
            currentCourse = course

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
            }

            /*
             * Background color
             */

            // Set background color, about 70% opacity
            val opacity: Int = 0xb4000000.toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                (layout.background as StateListDrawable).colorFilter = BlendModeColorFilter((course.color
                        ?: 6168631) and 0x00FFFFFF or opacity, BlendMode.SRC_ATOP)
            } else {
                @Suppress("DEPRECATION") // not in < Q
                (layout.background as StateListDrawable)
                        .setColorFilter(course.color ?: 6168631, PorterDuff.Mode.SRC_ATOP)
            }
        }
    }

    // Creates new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_course, viewGroup, false)

        return ViewHolder(view, onClick)
    }

    // Replaces the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val courseId = courses[position].courseId
        // Get the size of unread courses within this course
        val openPosts = unreadPosts.filter { it.id_course == courseId && it.unread }.size
        // Get the size of not yet done tasks within this course
        val openTasks = tasks.filter { it.id_course == courseId && !it.isDone }.distinct().size

        // Bind data to ViewHolder
        viewHolder.bind(courses[position], openPosts, openTasks)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = courses.size

}