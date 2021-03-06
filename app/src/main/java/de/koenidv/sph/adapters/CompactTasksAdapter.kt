package de.koenidv.sph.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.networking.Tasks


//  Created by koenidv on 20.12.2020.
class CompactTasksAdapter(private val tasks: List<Tasks.TaskData>,
                          private var maxSize: Int? = null,
                          private val onClick: (taskData: Tasks.TaskData) -> Unit,
                          private val onTaskCheckedChanged: (taskData: Tasks.TaskData, isDone: Boolean) -> Unit) :
        RecyclerView.Adapter<CompactTasksAdapter.ViewHolder>() {

    /**
     * Provides a reference to the type of view
     * (custom ConversationViewHolder).
     */
    class ViewHolder(view: View,
                     onClick: (taskData: Tasks.TaskData) -> Unit,
                     onTaskCheckedChanged: (taskData: Tasks.TaskData, isDone: Boolean) -> Unit) :
            RecyclerView.ViewHolder(view) {
        private val layout = view.findViewById<ConstraintLayout>(R.id.taskLayout)
        private val checkbox = view.findViewById<CheckBox>(R.id.taskCheckBox)
        private val description = view.findViewById<TextView>(R.id.taskTextView)

        private var currentTask: Tasks.TaskData? = null
        private var checkboxset = false

        init {
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (checkboxset)
                    currentTask?.let {
                        onTaskCheckedChanged(it, isChecked)
                        /*// Send broadcast to update ui
                        val uiBroadcast = Intent("uichange")
                        uiBroadcast.putExtra("content", "taskDone")
                        uiBroadcast.putExtra("postId", it.id_post)
                        uiBroadcast.putExtra("isDone", isChecked)
                        LocalBroadcastManager.getInstance(SphPlanner.applicationContext()).sendBroadcast(uiBroadcast)*/
                    }
            }
            layout.setOnClickListener {
                currentTask?.let {
                    onClick(it)
                }
            }
        }


        fun bind(task: Tasks.TaskData) {
            currentTask = task

            // Set checkbox checked
            checkboxset = false
            checkbox.isChecked = task.isDone
            checkboxset = true

            // Set data
            description.text = task.description

            // Set checkmark color per course, about 70% opacity
            checkbox.buttonTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_enabled)),
                    intArrayOf(task.color and 0x00FFFFFF or 0xb4000000.toInt()))

        }

    }

    // Creates new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_task_compact, viewGroup, false)
        return ViewHolder(view, onClick, onTaskCheckedChanged)
    }

    // Replaces the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Bind data to ConversationViewHolder
        viewHolder.bind(tasks[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() =
            if (maxSize != null && maxSize!! < tasks.size)
                maxSize!! else tasks.size
}