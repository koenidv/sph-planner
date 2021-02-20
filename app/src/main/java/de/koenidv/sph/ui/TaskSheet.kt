package de.koenidv.sph.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.os.bundleOf
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.koenidv.sph.R
import de.koenidv.sph.adapters.TasksAdapter
import de.koenidv.sph.database.TasksDb
import de.koenidv.sph.networking.Tasks

//  Created by koenidv on 29.12.2020.
class TaskSheet internal constructor(private val taskId: String) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.sheet_task, container, false)

        val taskRecycler = view.findViewById<RecyclerView>(R.id.taskRecycler)
        val doneButton = view.findViewById<Button>(R.id.doneButton)

        val task = TasksDb.getInstance().getByTaskId(taskId)

        // Set up recycler view with just one item
        taskRecycler.adapter = TasksAdapter(
                task,
                requireActivity(),
                onDateClick = {
                    // Show single post bottom sheet
                    dismiss()
                    PostSheet(it).show(parentFragmentManager, "post")
                },
                onCourseClick = {
                    dismiss()
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                            .navigate(R.id.overviewAction, bundleOf("courseId" to it))
                },
                onTaskCheckedChanged = Tasks().onCheckedChanged(requireActivity())
        )

        doneButton.setOnClickListener { dismiss() }

        return view
    }
}