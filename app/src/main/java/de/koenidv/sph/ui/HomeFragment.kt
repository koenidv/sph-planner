package de.koenidv.sph.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.adapters.AttachmentsAdapter
import de.koenidv.sph.adapters.CompactChangesAdapter
import de.koenidv.sph.adapters.CompactPostsAdapter
import de.koenidv.sph.adapters.CompactTasksAdapter
import de.koenidv.sph.database.*
import de.koenidv.sph.networking.AttachmentManager
import de.koenidv.sph.objects.Attachment
import de.koenidv.sph.objects.FunctionTile
import de.koenidv.sph.objects.Post
import de.koenidv.sph.objects.Task
import de.koenidv.sph.parsing.Utility
import java.util.*


class HomeFragment : Fragment() {

    private lateinit var tasks: MutableList<Task>
    private lateinit var tasksRecycler: RecyclerView
    private lateinit var posts: MutableList<Post>
    private lateinit var unreadPostsRecycler: RecyclerView

    // Refresh whenever the broadcast "uichange" is received
    private val uichangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            // Update posts list with new unread posts
            if (intent.getStringExtra("content") == "posts") {
                // If there wasn't any post or any task before but now is,
                // it's easier to recreate the fragment
                if (unreadPostsRecycler.adapter == null
                        || tasksRecycler.adapter == null) {
                    @Suppress("DEPRECATION")
                    parentFragmentManager.beginTransaction()
                            .replace(R.id.nav_host_fragment,
                                    instantiate(context, HomeFragment().javaClass.name))
                            .commit()
                    return
                }

                // Add all new posts to the top of the posts list
                // Removed posts will not be removed
                val unreadPosts = PostsDb.getInstance().unread
                for (post in unreadPosts) {
                    if (!posts.contains(post)) {
                        posts.add(0, post)
                        unreadPostsRecycler.adapter?.notifyItemInserted(0)
                        unreadPostsRecycler.adapter?.notifyItemRemoved(3)
                    }
                }

                // Add all new tasks to the top of the tasks list
                val unreadTasks = TasksDb.getInstance().undone
                for (task in unreadTasks) {
                    if (!tasks.contains(task)) {
                        tasks.add(0, task)
                        tasksRecycler.adapter?.notifyItemInserted(0)
                        tasksRecycler.adapter?.notifyItemRemoved(5)
                    }
                }
            }

            // Update tasks and posts list when a task is done
            if (intent.getStringExtra("content") == "taskDone"
                    && ::tasks.isInitialized) {
                val postId = intent.getStringExtra("postId")

                // If tasks list contains this, notify tasks aapter
                val taskIndex = tasks.indexOfFirst { it.id_post == postId }
                if (taskIndex != -1) {
                    tasksRecycler.adapter?.notifyItemChanged(taskIndex)
                } else if (!intent.getBooleanExtra("isDone", true)) {
                    // Add task to the top of the list if it is not done and wasn't there before
                    tasks.add(0, TasksDb.getInstance().getByPostId(postId).first())
                    tasksRecycler.adapter?.notifyItemInserted(0)
                    tasksRecycler.adapter?.notifyItemRemoved(5)
                }

                // If posts list contains this, notify it
                val postIndex = posts.indexOfFirst { it.postId == postId }
                if (postIndex != -1) unreadPostsRecycler.adapter?.notifyItemChanged(postIndex)
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
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_home, container, false)
        //val prefs = requireContext().getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)

        // Set random greeting as action bar title, once per app start, or after 30 minutes
        if (SphPlanner.randomGreeting == null || Date().time - SphPlanner.randomGreetingTime > 30 * 60 * 1000) {
            SphPlanner.randomGreeting = Utility().getGreeting()
            SphPlanner.randomGreetingTime = Date().time
        }
        (activity as AppCompatActivity).supportActionBar?.title = SphPlanner.randomGreeting


        /*
         * Timetable
         */


        val timetable = view.findViewById<FragmentContainerView>(R.id.timetableFragment)
        if (FunctionTilesDb.getInstance().supports(FunctionTile.FEATURE_TIMETABLE)) {
            view.findViewById<LinearLayout>(R.id.timetableLayout).setOnClickListener {
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                        .navigate(R.id.timetableFromHomeAction, null, null,
                                FragmentNavigatorExtras(timetable to "timetable"))
            }
        } else timetable.visibility = View.GONE


        /*
         * Personalized changes
         */

        val changesTitle = view.findViewById<TextView>(R.id.changesTitleTextView)
        val changesLayout = view.findViewById<LinearLayout>(R.id.changesLayout)
        val changesRecycler = view.findViewById<RecyclerView>(R.id.changesRecycler)

        if (FunctionTilesDb.getInstance().supports(FunctionTile.FEATURE_CHANGES)) {

            // Get changes for favorite courses and display them
            val personalizedChanges = ChangesDb.instance!!.getFavorites()
            if (personalizedChanges.isNotEmpty()) {
                changesRecycler.setHasFixedSize(true)
                changesRecycler.adapter = CompactChangesAdapter(personalizedChanges) {
                    requireActivity().findNavController(R.id.nav_host_fragment)
                            .navigate(R.id.changesFromHomeAction, bundleOf("favorites" to personalizedChanges.isNotEmpty()))
                }
            } else {
                changesTitle.text = getString(R.string.changes_personalized_none)
                changesRecycler.visibility = View.GONE
            }

            // Set onclick if there are any changes, else display message
            if (personalizedChanges.isNotEmpty() || ChangesDb.instance!!.existAny()) {
                changesLayout.setOnClickListener {
                    requireActivity().findNavController(R.id.nav_host_fragment)
                            .navigate(R.id.changesFromHomeAction, bundleOf("favorites" to personalizedChanges.isNotEmpty()))
                }
            } else {
                // Hide next icon if there are no changes
                changesTitle.text = getString(R.string.changes_none)
                changesTitle.setCompoundDrawablesRelative(null, null, null, null)
            }

        } else changesLayout.visibility = View.GONE


        /**
         * Undone tasks
         */

        val tasksLayout = view.findViewById<LinearLayout>(R.id.tasksLayout)
        val tasksRecyclerLayout = view.findViewById<LinearLayout>(R.id.tasksRecyclerLayout)
        tasksRecycler = view.findViewById(R.id.tasksRecycler)
        val tasksTitle = view.findViewById<TextView>(R.id.tasksTitleTextView)
        val moreTasksText = view.findViewById<TextView>(R.id.moreTasksTextView)

        if (FunctionTilesDb.getInstance().supports(FunctionTile.FEATURE_COURSES)) {

            tasks = TasksDb.getInstance().undone
            var tasksOverflow = 0
            if (tasks.size > 6) {
                tasksOverflow = tasks.size - 6
                // Display more tasks message
                moreTasksText.text = resources.getQuantityString(R.plurals.tasks_personalized_more, tasksOverflow, tasksOverflow)
                moreTasksText.visibility = View.VISIBLE
            }

            if (tasks.isNotEmpty()) {
                tasksRecycler.adapter = CompactTasksAdapter(
                        tasks,
                        6,
                        onClick = {
                            // Show single post bottom sheet
                            PostSheet(
                                    PostsDb.getInstance().getByPostId(it)
                            ).show(parentFragmentManager, "post")
                        },
                        onTaskCheckedChanged = AttachmentManager().onTaskCheckedChanged(requireActivity()) { postId, isDone ->
                            if (isDone) {
                                val index = tasks.indexOfFirst { it.id_post == postId }
                                tasks.removeAt(index)
                                tasksRecycler.adapter?.notifyItemRemoved(index)
                                // Update overflow counter
                                if (tasksOverflow > 0) {
                                    tasksOverflow--
                                    if (tasksOverflow > 0)
                                        moreTasksText.text = resources.getQuantityString(R.plurals.tasks_personalized_more, tasksOverflow, tasksOverflow)
                                    else moreTasksText.visibility = View.GONE
                                }
                            }
                        }
                )
            } else {
                tasksTitle.setText(R.string.tasks_filter_none_undone)
                tasksRecyclerLayout.visibility = View.GONE
            }

            // Open all undone tasks on click
            tasksLayout.setOnClickListener {
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                        .navigate(R.id.tasksFromHomeAction, bundleOf("undone" to true))
            }

        } else tasksLayout.visibility = View.GONE


        /*
         * Unread posts
         */

        val unreadPostsLayout = view.findViewById<LinearLayout>(R.id.unreadPostsLayout)
        unreadPostsRecycler = view.findViewById(R.id.unreadPostsRecycler)
        val moreUnreadText = view.findViewById<TextView>(R.id.moreUnreadTextView)

        if (FunctionTilesDb.getInstance().supports(FunctionTile.FEATURE_COURSES)) {

            // Get unread posts
            // and up to 4 read posts
            // We'll only display 4 posts,
            // but unread posts will get removed once they are read
            posts = PostsDb.getInstance().unread.toMutableList()
            var postsOverflow = 0
            if (posts.size > 4) {
                postsOverflow = posts.size - 4
                // Display more unread posts text
                moreUnreadText.text = resources.getQuantityString(R.plurals.posts_more_unread, postsOverflow, postsOverflow)
                moreUnreadText.visibility = View.VISIBLE
            }
            posts.addAll(PostsDb.getInstance().getRead(4))

            if (posts.isNotEmpty()) {
                // Only show posts if there are any
                unreadPostsRecycler.setHasFixedSize(true)
                unreadPostsRecycler.adapter = CompactPostsAdapter(posts, 4) { post: Post, _: View ->
                    // Show single post bottom sheet
                    PostSheet(post).show(parentFragmentManager, "post")
                    // Remove the item from the list if it is unread
                    if (post.unread) {
                        val index = posts.indexOf(post)
                        posts.removeAt(index)
                        unreadPostsRecycler.adapter?.notifyItemRemoved(index)
                        PostsDb.getInstance().markAsRead(post.postId)
                        // Update overflow counter
                        postsOverflow--
                        if (postsOverflow != 0)
                            moreUnreadText.text = resources.getQuantityString(R.plurals.posts_more_unread, postsOverflow, postsOverflow)
                        else moreUnreadText.visibility = View.GONE
                    }
                }
            } else {
                unreadPostsLayout.visibility = View.GONE
            }

            // Open all posts on click
            unreadPostsLayout.setOnClickListener {
                val bundle =
                        if (postsOverflow != 0) bundleOf("filters" to arrayOf("unread"))
                        else bundleOf()
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                        .navigate(R.id.allPostsFromHomeAction, bundle)
            }

        } else unreadPostsLayout.visibility = View.GONE


        /**
         * Pinned attachments
         */

        val pinsLayout = view.findViewById<LinearLayout>(R.id.pinsLayout)
        val pinsTitle = view.findViewById<TextView>(R.id.pinsTitleTextView)
        val pinsRecycler = view.findViewById<RecyclerView>(R.id.pinsRecycler)

        if (FunctionTilesDb.getInstance().supports(FunctionTile.FEATURE_COURSES)) {

            val pins = AttachmentsDb.pins()

            if (pins.isEmpty()) {
                pinsTitle.text = getString(R.string.attachments_pins_none)
                pinsRecycler.visibility = View.GONE
            } else {
                // Set up pins recycler
                pinsRecycler.adapter = AttachmentsAdapter(
                        pins,
                        AttachmentManager().onAttachmentClick(requireActivity()) { _: Int, _: Attachment -> },
                        AttachmentManager().onAttachmentLongClick(requireActivity()) { action: Int, attachment: Attachment ->
                            if (action == AttachmentManager.ATTACHMENT_UNPINNED) {
                                // Attachment no longer pinned
                                // Remove it from the list
                                val index = pins.indexOf(attachment)
                                pins.removeAt(index)
                                // Notify the adapter
                                pinsRecycler.adapter?.notifyItemRemoved(index)
                                // Hide title and recycler if this was the last pin
                                if (pins.isEmpty()) {
                                    pinsTitle.text = getString(R.string.attachments_pins_none)
                                    pinsRecycler.visibility = View.GONE
                                }
                            } else if (action == AttachmentManager.ATTACHMENT_RENAMED_PIN) {
                                // Update item in recycler
                                val index = pins.indexOfFirst { it.attachId() == attachment.attachId() }
                                pins[index] = attachment
                                pinsRecycler.adapter?.notifyItemChanged(index)
                            }
                        },
                        true
                )
                PagerSnapHelper().attachToRecyclerView(pinsRecycler)
            }

            // Open all attachments fragment on click
            pinsLayout.setOnClickListener {
                requireActivity().findNavController(R.id.nav_host_fragment)
                        .navigate(R.id.frag_placeholder)
                // todo attachments collection
            }

        } else pinsLayout.visibility = View.GONE


        return view

    }
}