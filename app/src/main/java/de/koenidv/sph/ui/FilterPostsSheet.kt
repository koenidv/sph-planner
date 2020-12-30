package de.koenidv.sph.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButtonToggleGroup
import de.koenidv.sph.R

//  Created by koenidv on 30.12.2020.
class FilterPostsSheet internal constructor(private val allPostsFragment: AllPostsFragment, private var filters: MutableList<String>) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.sheet_filter_posts, container, false)

        val readToggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.readToggleGroup)
        val taskToggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.taskToggleGroup)

        // Check enabled filters
        if (filters.contains("unread")) readToggleGroup.check(R.id.filterIsUnreadButton)
        else if (filters.contains("read")) readToggleGroup.check(R.id.filterIsReadButton)
        taskToggleGroup.check(
                when {
                    filters.contains("task_none") -> R.id.filterTaskNoneButton
                    filters.contains("task_any") -> R.id.filterTaskAnyButton
                    filters.contains("task_undone") -> R.id.filterTaskUndoneButton
                    filters.contains("task_done") -> R.id.filterTaskDoneButton
                    else -> 0
                }
        )

        // Listen for changes
        readToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            filters.remove("read")
            filters.remove("unread")
            if (isChecked && checkedId == R.id.filterIsUnreadButton) {
                filters.add("unread")
            } else if (isChecked && checkedId == R.id.filterIsReadButton) {
                filters.add("read")
            }
            allPostsFragment.filterPosts(filters)
        }
        taskToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            filters.removeAll(listOf("task_none", "task_any", "task_undone", "task_done"))
            if (isChecked) {
                when (checkedId) {
                    R.id.filterTaskNoneButton -> filters.add("task_none")
                    R.id.filterTaskAnyButton -> filters.add("task_any")
                    R.id.filterTaskUndoneButton -> filters.add("task_undone")
                    R.id.filterTaskDoneButton -> filters.add("task_done")
                }
                allPostsFragment.filterPosts(filters)
            }
        }

        view.findViewById<Button>(R.id.doneButton).setOnClickListener { dismiss() }

        return view
    }
}