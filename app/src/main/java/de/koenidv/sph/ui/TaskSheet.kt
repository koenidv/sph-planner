package de.koenidv.sph.ui

import androidx.core.os.bundleOf
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.adapters.TasksAdapter
import de.koenidv.sph.database.TasksDb
import de.koenidv.sph.networking.Tasks

//  Created by koenidv on 29.12.2020.
class TaskSheet internal constructor(private val taskId: String) : RecyclerSheet() {

    override fun setupRecycler(recycler: RecyclerView) {
        val task = TasksDb.getInstance().getByTaskId(taskId)

        // Set up recycler view with just one item
        recycler.adapter = TasksAdapter(
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
    }

}