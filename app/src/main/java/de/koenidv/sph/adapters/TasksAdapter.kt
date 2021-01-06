package de.koenidv.sph.adapters

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.database.TasksDb
import de.koenidv.sph.objects.Task
import de.koenidv.sph.parsing.Utility
import java.text.SimpleDateFormat
import java.util.*


//  Created by koenidv on 20.12.2020.
class TasksAdapter(private val tasks: MutableList<Task>,
                   private val activity: Activity,
                   private val onDateClick: (postId: String) -> Unit,
                   private val onCourseClick: (courseId: String) -> Unit,
                   private val onTaskCheckedChanged: (postId: String, courseId: String, isDone: Boolean) -> Unit) :
        RecyclerView.Adapter<TasksAdapter.ViewHolder>() {

    // Show a bottom sheet with options when long clicking a task
    val onLongClick = { task: Task, position: Int ->
        val sheet = BottomSheetDialog(activity)
        sheet.setContentView(R.layout.sheet_manage_task)

        val pin = sheet.findViewById<TextView>(R.id.pinTextView)
        val unpin = sheet.findViewById<TextView>(R.id.unpinTextView)

        // Only show applicable options
        if (task.isPinned) pin?.visibility = View.GONE
        else unpin?.visibility = View.GONE

        // Function for pinning / unpinning a task
        val setPinned = { pinned: Boolean ->
            // Pin/Unpin the task in the db
            TasksDb.getInstance().setPinned(task.taskId, pinned)
            // Update adapter dataset
            task.isPinned = pinned
            tasks[position] = task
            // Notify the adapter about the changed item
            notifyItemChanged(position)
        }

        // Pin a task
        // This will put it on the top the next time tasks are shown
        // And highlight it with some color now
        pin?.setOnClickListener {
            setPinned(true)
            sheet.dismiss()
        }

        // Unpin a task
        unpin?.setOnClickListener {
            setPinned(false)
            sheet.dismiss()
        }

        sheet.show()
    }

    /**
     * Provides a reference to the type of view
     * (custom ViewHolder).
     */
    class ViewHolder(view: View,
                     onDateClick: (postId: String) -> Unit,
                     onCourseClick: (String) -> Unit,
                     onTaskCheckedChanged: (postId: String, courseId: String, isDone: Boolean) -> Unit,
                     onTaskLongClick: (Task, Int) -> Unit) : RecyclerView.ViewHolder(view) {
        private val layout = view.findViewById<ConstraintLayout>(R.id.taskLayout)
        private val checkbox = view.findViewById<CheckBox>(R.id.taskCheckBox)
        private val description = view.findViewById<TextView>(R.id.taskTextView)
        private val date = view.findViewById<TextView>(R.id.dateTextView)
        private val dateLayout = view.findViewById<LinearLayout>(R.id.dateLayout)
        private val course = view.findViewById<TextView>(R.id.courseTextView)
        private val courseLayout = view.findViewById<LinearLayout>(R.id.courseLayout)

        private val dateFormat = SimpleDateFormat("d. MMM yyyy", Locale.getDefault())
        private var currentTask: Task? = null
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
            layout.setOnLongClickListener {
                currentTask?.let {
                    onTaskLongClick(it, adapterPosition)
                }
                true
            }
        }

        fun bind(task: Task) {
            currentTask = task

            // Set checkbox checked
            checkboxset = false
            checkbox.isChecked = TasksDb.getInstance().taskDone(task.id_post)
            checkboxset = true

            // Set data
            description.text = task.description
            date.text = dateFormat.format(task.date)

            // Set course
            course.text = CoursesDb.getInstance().getFullname(task.id_course)
            // Adjust course background color
            // Set background color, about 70% opacity
            Utility().tintBackground(course, CoursesDb.getInstance().getColor(task.id_course), 0xb4000000.toInt())

            // Tint background with theme color at 15% if task is pinned
            if (task.isPinned)
                Utility().tintBackground(layout, SphPlanner.applicationContext()
                        .getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)
                        .getInt("themeColor", 0), 0x26000000)
            else
                layout.background.clearColorFilter()

        }

    }

    // Creates new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_task, viewGroup, false)
        return ViewHolder(view, onDateClick, onCourseClick, onTaskCheckedChanged, onLongClick)
    }

    // Replaces the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Bind data to ViewHolder
        viewHolder.bind(tasks[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = tasks.size
}