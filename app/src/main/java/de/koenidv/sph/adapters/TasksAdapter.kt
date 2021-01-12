package de.koenidv.sph.adapters

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.datetime.datePicker
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
                   private val onTaskCheckedChanged: (task: Task, isDone: Boolean) -> Unit) :
        RecyclerView.Adapter<TasksAdapter.ViewHolder>() {

    // Show a bottom sheet with options when long clicking a task
    private val onLongClick = { task: Task, position: Int ->
        val sheet = BottomSheetDialog(activity)
        sheet.setContentView(R.layout.sheet_manage_task)

        val pin = sheet.findViewById<TextView>(R.id.pinTextView)
        val unpin = sheet.findViewById<TextView>(R.id.unpinTextView)
        val share = sheet.findViewById<TextView>(R.id.shareTextView)

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

        share?.setOnClickListener {
            sheet.dismiss()
            // Share task description as plaintext
            val text = SphPlanner.applicationContext().getString(R.string.tasks_share_template)
                    .replace("%course", CoursesDb.getInstance().getFullname(task.id_course))
                    .replace("%description", task.description)
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                this.type = "text/plain"
            }
            activity.startActivity(Intent.createChooser(sendIntent, null))
        }

        sheet.show()
    }

    // Let the user add or edit a task's due date
    private val onDueClick: (Task, Int) -> Unit = { task: Task, position: Int ->
        var currentCalendar: Calendar? = null
        if (task.dueDate != null) {
            currentCalendar = Calendar.getInstance()
            currentCalendar.time = task.dueDate!!
        }

        MaterialDialog(activity, BottomSheet()).show {
            datePicker(currentDate = currentCalendar) { _, calendar ->
                val newDate = Date(calendar.timeInMillis)
                // Update db and dataset
                TasksDb.getInstance().setDueDate(task.taskId, newDate)
                task.dueDate = newDate
                tasks[position] = task
                // Notify about changed item
                notifyItemChanged(position)
            }
        }

    }

    /**
     * Provides a reference to the type of view
     * (custom ViewHolder).
     */
    class ViewHolder(view: View,
                     onDateClick: (postId: String) -> Unit,
                     onCourseClick: (String) -> Unit,
                     onTaskCheckedChanged: (task: Task, isDone: Boolean) -> Unit,
                     onTaskLongClick: (Task, Int) -> Unit,
                     onDueClick: (Task, Int) -> Unit) : RecyclerView.ViewHolder(view) {
        private val layout = view.findViewById<ConstraintLayout>(R.id.taskLayout)
        private val checkbox = view.findViewById<CheckBox>(R.id.taskCheckBox)
        private val description = view.findViewById<TextView>(R.id.taskTextView)
        private val dueInfo = view.findViewById<TextView>(R.id.dueInfoTextView)
        private val due = view.findViewById<TextView>(R.id.dueTextView)
        private val dueLayout = view.findViewById<LinearLayout>(R.id.dueLayout)
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
                        onTaskCheckedChanged(it, isChecked)

                        // Send broadcast to update ui
                        val uiBroadcast = Intent("uichange")
                        uiBroadcast.putExtra("content", "taskDone")
                        uiBroadcast.putExtra("taskId", it.taskId)
                        uiBroadcast.putExtra("postId", it.id_post)
                        uiBroadcast.putExtra("isDone", isChecked)
                        LocalBroadcastManager.getInstance(SphPlanner.applicationContext()).sendBroadcast(uiBroadcast)
                    }
            }
            layout.setOnLongClickListener {
                currentTask?.let {
                    onTaskLongClick(it, adapterPosition)
                }
                true
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
            dueLayout.setOnClickListener {
                currentTask?.let {
                    onDueClick(it, adapterPosition)
                }
            }
        }

        private val themeColor = SphPlanner.applicationContext()
                .getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)
                .getInt("themeColor", 0)

        fun bind(task: Task) {
            currentTask = task

            // Set checkbox checked
            checkboxset = false
            checkbox.isChecked = TasksDb.getInstance().taskDoneByPost(task.id_post)
            checkboxset = true

            // Set data
            description.text = task.description
            date.text = dateFormat.format(task.date)

            // Set due date or show option to set one
            val timenow = Date().time
            when {
                task.dueDate == null -> {
                    due.visibility = View.GONE
                    dueInfo.setText(R.string.tasks_due_setdate)
                }
                task.dueDate!!.time - timenow <= -24 * 60 * 60 * 1000 -> {
                    // Due date not within the last 24 hours, overdue
                    due.visibility = View.VISIBLE
                    dueInfo.visibility = View.GONE
                    due.setText(R.string.tasks_due_overdue)
                    Utility.tintBackground(
                            due,
                            SphPlanner.applicationContext().getColor(R.color.design_default_color_error),
                            0xb4000000.toInt())
                }
                task.dueDate!!.time - timenow <= 0 -> {
                    // Date is within the last 24 hours, which means its today (only day, no time)
                    due.visibility = View.VISIBLE
                    dueInfo.visibility = View.VISIBLE
                    dueInfo.setText(R.string.tasks_due_info_relative)
                    due.setText(R.string.tasks_due_today)
                    Utility.tintBackground(due, themeColor, 0xb4000000.toInt())
                }
                task.dueDate!!.time - timenow <= 24 * 60 * 60 * 1000 -> {
                    // Tomorrow
                    due.visibility = View.VISIBLE
                    dueInfo.visibility = View.VISIBLE
                    dueInfo.setText(R.string.tasks_due_info_relative)
                    due.setText(R.string.tasks_due_tomorrow)
                    Utility.tintBackground(due, themeColor, 0x60000000)
                }
                else -> {
                    // Other day
                    due.visibility = View.VISIBLE
                    dueInfo.visibility = View.VISIBLE
                    dueInfo.setText(R.string.tasks_due_info_absolute)

                    due.text = SimpleDateFormat("d. MMM", Locale.getDefault())
                            .format(task.dueDate!!)
                    Utility.tintBackground(due,
                            SphPlanner.applicationContext().getColor(R.color.grey_800),
                            0xb4000000.toInt())
                }
            }

            // Set course
            course.text = CoursesDb.getInstance().getFullname(task.id_course)
            // Adjust course background color
            // Set background color, about 70% opacity
            Utility.tintBackground(course, CoursesDb.getInstance().getColor(task.id_course), 0xb4000000.toInt())

            // Tint background with theme color at 15% if task is pinned
            if (task.isPinned)
                Utility.tintBackground(layout, themeColor, 0x26000000)
            else
                layout.background.clearColorFilter()

        }

    }

    // Creates new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_task, viewGroup, false)
        return ViewHolder(view, onDateClick, onCourseClick, onTaskCheckedChanged, onLongClick, onDueClick)
    }

    // Replaces the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Bind data to ViewHolder
        viewHolder.bind(tasks[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = tasks.size
}