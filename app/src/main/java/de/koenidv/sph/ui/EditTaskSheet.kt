package de.koenidv.sph.ui

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.datetime.datePicker
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.prefs
import de.koenidv.sph.database.TasksDb
import de.koenidv.sph.objects.Task
import java.util.*
import kotlin.math.roundToInt


//  Created by koenidv on 14.02.2021.
/**
 * Bottom sheet showing options to edit or create a task
 * @param task Specify to edit a task, null for creating a new one
 */

class EditTaskSheet(private val task: Task? = null,
                    private val callback: (Task) -> Unit) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.sheet_edit_task, container, false)

        var date: Calendar
        var due: Calendar? = null

        val description = view.findViewById<EditText>(R.id.descriptionText)
        val dateButton = view.findViewById<MaterialButton>(R.id.dateButton)
        val dueButton = view.findViewById<MaterialButton>(R.id.dueButton)
        val pinned = view.findViewById<CheckBox>(R.id.pinnedCheckbox)
        val save = view.findViewById<MaterialButton>(R.id.saveButton)

        if (task != null) {
            // Set values from task to edit
            description.setText(task.description)
            // Set dates and update the according buttons
            date = Calendar.getInstance().apply { timeInMillis = task.date.time }
            updateDateButton(date, dateButton)
            if (task.dueDate != null) {
                due = Calendar.getInstance().apply { timeInMillis = task.dueDate!!.time }
                updateDateButton(due, dueButton)
            }
            pinned.isChecked = task.isPinned
        } else {
            // If no task was specified, use today as date for the new task
            date = Calendar.getInstance()
            updateDateButton(date, dateButton)
        }

        // Edit date
        dateButton.setOnClickListener {
            MaterialDialog(requireContext(), BottomSheet()).show {
                datePicker(currentDate = date) { _, calendar ->
                    date = calendar
                    updateDateButton(date, dateButton)
                }
            }
        }

        // Edit due date
        dueButton.setOnClickListener {
            MaterialDialog(requireContext(), BottomSheet()).show {
                datePicker(currentDate = due) { _, calendar ->
                    due = calendar
                    updateDateButton(due, dueButton)
                }
            }
        }

        // Save this task
        save.setOnClickListener {
            if (description.text.isNullOrEmpty()) {
                description.error = getString(R.string.tasks_create_description)
                return@setOnClickListener
            }

            // If new task shall be created, callback with a new task
            // Else update the task to be edited and call back with it
            if (task == null) {
                Task(
                        taskId = "custom_" + (Date().time / 1000.toDouble()).roundToInt(),
                        description = description.text.toString(),
                        date = date.time,
                        dueDate = due?.time,
                        isPinned = pinned.isChecked,
                        isDone = false,
                        id_course = null,
                        id_post = null
                ).also {
                    // Save to db
                    TasksDb.getInstance().save(it, true)
                    // Call back
                    callback(it)
                }
            } else {
                // Apply changes and call back
                task.description = description.text.toString()
                task.date = date.time
                task.dueDate = due?.time
                task.isPinned = pinned.isChecked
                task.also {
                    TasksDb.getInstance().save(it, true)
                    callback(it)
                }
            }

            dismiss()
        }

        description.requestFocus()
        return view
    }

    /**
     * Set text on date selector button
     */
    private fun updateDateButton(time: Calendar?, button: MaterialButton) {
        if (time != null) {
            button.text = getRelativeDay(time)
            // Apply theme color with 15% opacity
            button.background.setTint(prefs.getInt("themeColor", 0)
                    and 0x00FFFFFF or 0x26000000)
        } else {
            button.text = getText(R.string.tasks_create_date)
            button.background.setTint(0)
        }
    }

    private fun getRelativeDay(time: Calendar): String {
        val now = Calendar.getInstance()
        return DateUtils.getRelativeTimeSpanString(
                time.timeInMillis,
                now.timeInMillis,
                DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_ALL
        ).toString()
    }

}