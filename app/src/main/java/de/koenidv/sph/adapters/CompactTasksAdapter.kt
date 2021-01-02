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
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.database.PostTasksDb
import de.koenidv.sph.objects.PostTask


//  Created by koenidv on 20.12.2020.
class CompactTasksAdapter(private val tasks: List<PostTask>,
                          private var maxSize: Int? = null,
                          private val onClick: (postId: String) -> Unit,
                          private val onTaskCheckedChanged: (postId: String, courseId: String, isDone: Boolean) -> Unit) :
        RecyclerView.Adapter<CompactTasksAdapter.ViewHolder>() {

    /**
     * Provides a reference to the type of view
     * (custom ViewHolder).
     */
    class ViewHolder(view: View,
                     onClick: (postId: String) -> Unit,
                     onTaskCheckedChanged: (postId: String, courseId: String, isDone: Boolean) -> Unit) : RecyclerView.ViewHolder(view) {
        private val layout = view.findViewById<ConstraintLayout>(R.id.taskLayout)
        private val checkbox = view.findViewById<CheckBox>(R.id.taskCheckBox)
        private val description = view.findViewById<TextView>(R.id.taskTextView)

        private var currentTask: PostTask? = null
        private var checkboxset = false

        init {
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (checkboxset)
                    currentTask?.let {
                        onTaskCheckedChanged(it.id_post, it.id_course, isChecked)
                    }
            }
            layout.setOnClickListener {
                currentTask?.id_post?.let {
                    onClick(it)
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

            // Set checkmark color per course, about 70% opacity
            checkbox.buttonTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_enabled)),
                    intArrayOf(CoursesDb.getInstance().getColor(task.id_course) and 0x00FFFFFF or 0xb4000000.toInt()))

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
        // Bind data to ViewHolder
        viewHolder.bind(tasks[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() =
            if (maxSize != null && maxSize!! <= tasks.size)
                maxSize!! else tasks.size
}