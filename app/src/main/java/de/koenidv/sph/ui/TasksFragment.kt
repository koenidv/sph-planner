package de.koenidv.sph.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.adapters.TasksAdapter
import de.koenidv.sph.database.PostTasksDb
import de.koenidv.sph.database.PostsDb
import de.koenidv.sph.networking.AttachmentManager
import de.koenidv.sph.objects.PostTask


// Created by koenidv on 18.12.2020.
class TasksFragment : Fragment() {

    var undone = false
    lateinit var tasks: MutableList<PostTask>
    lateinit var noDataText: TextView
    lateinit var tasksRecycler: RecyclerView

    // Refresh whenever the broadcast "uichange" is received
    private val uichangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Only do something changes were updated
            if (intent.getStringExtra("content") == "posts") {
                if (::tasks.isInitialized) {
                    // Get tasks
                    tasks.clear()
                    tasks.addAll(if (undone) PostTasksDb.getInstance().undone.toMutableList()
                    else PostTasksDb.getInstance().all.toMutableList())
                    // Show message if there are no tasks
                    if (tasks.isEmpty()) {
                        if (undone) {
                            noDataText.setText(R.string.tasks_filter_none_undone)
                        } else {
                            noDataText.setText(R.string.tasks_filter_none)
                        }
                        noDataText.visibility = View.VISIBLE
                    } else noDataText.visibility = View.GONE
                    // Notify recyclerview
                    tasksRecycler.adapter?.notifyDataSetChanged()
                } else {
                    // If the recycler was never set up, recreate the fragment
                    @Suppress("DEPRECATION")
                    parentFragmentManager.beginTransaction()
                            .replace(R.id.nav_host_fragment,
                                    instantiate(context, TasksFragment().javaClass.name,
                                            bundleOf("undone" to undone)))
                            .commit()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Register to receive messages.
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(uichangeReceiver,
                IntentFilter("uichange"))
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_tasks, container, false)

        noDataText = view.findViewById(R.id.noDataTextView)
        tasksRecycler = view.findViewById(R.id.tasksRecycler)
        val undoneSwitch = view.findViewById<SwitchMaterial>(R.id.undoneSwitch)

        // Firstly, check if there are any tasks to be displayed
        if (!PostTasksDb.getInstance().existAny()) {
            noDataText.visibility = View.VISIBLE
            undoneSwitch.visibility = View.GONE
            tasksRecycler.visibility = View.GONE
            return view
        }

        // Set open in browser url
        SphPlanner.openInBrowserUrl = getString(R.string.url_allposts)

        // Get passed argument
        if (arguments?.getBoolean("undone") != null) {
            undone = arguments?.getBoolean("undone")!!
        }

        // Set undone switch
        undoneSwitch.isChecked = undone

        // Get tasks
        tasks = if (undone) PostTasksDb.getInstance().undone.toMutableList()
        else PostTasksDb.getInstance().all.toMutableList()
        // Show message if there are no tasks
        if (tasks.isEmpty()) {
            if (undone) {
                noDataText.setText(R.string.tasks_filter_none_undone)
            } else {
                noDataText.setText(R.string.tasks_filter_none)
            }
            noDataText.visibility = View.VISIBLE
        }

        // Set up tasks recycler
        val adapter = TasksAdapter(
                tasks,
                requireActivity(),
                onDateClick = {
                    // Show single post bottom sheet
                    PostSheet(
                            PostsDb.getInstance().getByPostId(it)
                    ).show(parentFragmentManager, "post")
                },
                onCourseClick = {
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                            .navigate(R.id.overviewFromTasksAction, bundleOf("courseId" to it))
                },
                onTaskCheckedChanged = AttachmentManager().onTaskCheckedChanged(requireActivity())
        )
        tasksRecycler.adapter = adapter

        // Favorites switch
        undoneSwitch.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
            undone = checked
            // Update data
            tasks.clear()
            tasks.addAll(if (undone) PostTasksDb.getInstance().undone.toMutableList()
            else PostTasksDb.getInstance().all.toMutableList())
            if (tasks.isEmpty()) {
                if (undone) {
                    noDataText.setText(R.string.tasks_filter_none_undone)
                } else {
                    noDataText.setText(R.string.tasks_filter_none)
                }
                noDataText.visibility = View.VISIBLE
            } else noDataText.visibility = View.GONE
            // Notify recyclerview
            // Should do this with notifyItemRemoved/Inserted..
            tasksRecycler.adapter?.notifyDataSetChanged()
        }

        return view
    }

    override fun onDestroy() {
        // Unregister broadcast receiver
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(uichangeReceiver)
        super.onDestroy()
    }
}