package de.koenidv.sph.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.database.PostTasksDb
import de.koenidv.sph.objects.PostTask
import de.koenidv.sph.parsing.Utility
import java.text.SimpleDateFormat
import java.util.*


//  Created by koenidv on 20.12.2020.
class TasksAdapter(private val tasks: List<PostTask>,
                   private val onDateClick: (postId: String) -> Unit,
                   private val onCourseClick: (courseId: String) -> Unit,
                   private val onTaskCheckedChanged: (postId: String, courseId: String, isDone: Boolean) -> Unit) :
        RecyclerView.Adapter<TasksAdapter.ViewHolder>() {

    /**
     * Provides a reference to the type of view
     * (custom ViewHolder).
     */
    class ViewHolder(view: View,
                     onDateClick: (postId: String) -> Unit,
                     onCourseClick: (String) -> Unit,
                     onTaskCheckedChanged: (postId: String, courseId: String, isDone: Boolean) -> Unit) : RecyclerView.ViewHolder(view) {
        private val layout = view.findViewById<ConstraintLayout>(R.id.taskLayout)
        private val checkbox = view.findViewById<CheckBox>(R.id.taskCheckBox)
        private val description = view.findViewById<TextView>(R.id.taskTextView)
        private val date = view.findViewById<TextView>(R.id.dateTextView)
        private val dateLayout = view.findViewById<LinearLayout>(R.id.dateLayout)
        private val course = view.findViewById<TextView>(R.id.courseTextView)
        private val courseLayout = view.findViewById<LinearLayout>(R.id.courseLayout)

        private val dateFormat = SimpleDateFormat("d. MMM yyyy", Locale.getDefault())
        private var currentTask: PostTask? = null
        private var checkboxset = false

        init {
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (checkboxset)
                    currentTask?.let {
                        onTaskCheckedChanged(it.id_post, it.id_course, isChecked)
                    }
            }
            dateLayout.setOnClickListener {
                currentTask?.id_post?.let {
                    onDateClick(it)
                }
            }
            courseLayout.setOnClickListener {
                currentTask?.id_course?.let {
                    onCourseClick(it)
                }
            }
        }


        fun bind(task: PostTask) {
            currentTask = task

            // Set checkbox checked
            checkboxset = false
            checkbox.isChecked = PostTasksDb.getInstance().taskDone(task.id_post)
            checkboxset = true

            // Set data
            description.text = task.description
            date.text = SphPlanner.applicationContext().getString(R.string.tasks_date_info, dateFormat.format(task.date))

            // Set course
            course.text = CoursesDb.getInstance().getFullname(task.id_course)
            // Adjust course background color
            // Set background color, about 70% opacity
            Utility().tintBackground(course, CoursesDb.getInstance().getColor(task.id_course), 0xb4000000.toInt())

        }

    }

    // Creates new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_task, viewGroup, false)
        return ViewHolder(view, onDateClick, onCourseClick, onTaskCheckedChanged)
    }

    // Replaces the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Bind data to ViewHolder
        viewHolder.bind(tasks[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = tasks.size
}