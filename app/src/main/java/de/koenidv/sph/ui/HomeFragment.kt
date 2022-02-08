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
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.adapters.*
import de.koenidv.sph.database.*
import de.koenidv.sph.networking.AttachmentManager
import de.koenidv.sph.networking.Tasks
import de.koenidv.sph.objects.Attachment
import de.koenidv.sph.objects.FunctionTile
import de.koenidv.sph.parsing.Utility
import java.util.*

class HomeFragment : Fragment() {

        private lateinit var changesAdapter: CompactChangesAdapter
        private lateinit var changesRecycler: RecyclerView
        private lateinit var tasks: MutableList<Tasks.TaskData>
        private lateinit var tasksRecycler: RecyclerView
        private lateinit var tasksTitle: TextView
        private lateinit var tasksRecyclerLayout: LinearLayout
        private lateinit var posts: MutableList<CompactPostsAdapter.PostData>
        private lateinit var unreadPostsRecycler: RecyclerView
        private lateinit var messagesLayout: LinearLayout
        private lateinit var messagesAdapter: ConversationsAdapter

        /**
         * Update tasks title to reflect the current number of undone tasks
         * Also hide or show recycler layout if needed
         */
        fun updateTasksTitle() {
            // Update title
            if (tasks.size == 0 || tasksRecycler.adapter?.itemCount == 0) {
                // If tasks list is now empty, update title to display everything is done
                tasksTitle.setText(R.string.tasks_filter_none_undone)
                // Hide recycler layout as it would just show an empty border
                tasksRecyclerLayout.visibility = View.GONE
            } else {
                // Else update the title to show the number of undone tasks
                tasksTitle.text = SphPlanner.appContext().resources.getQuantityString(
                        R.plurals.tasks_personalized_title_count,
                        tasks.size,
                        tasks.size)
                // Make sure tasks recycler layout is visible
                tasksRecyclerLayout.visibility = View.VISIBLE
            }
        }

        // Refresh whenever the broadcast "uichange" is received
        private val uichangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                // Update posts list with new unread posts
                if (intent.getStringExtra("content") == "posts") {
                    // If there wasn't any post or any task before but now is,
                    // it's easier to recreate the fragment
                    if (!::unreadPostsRecycler.isInitialized
                            || unreadPostsRecycler.adapter == null
                            || tasksRecycler.adapter == null) {
                        view?.post {
                            parentFragmentManager.beginTransaction()
                                    .replace(R.id.nav_host_fragment, HomeFragment())
                                    .setReorderingAllowed(true) //Optimizing state changes for better transitions
                                    .commit()
                        }
                        return
                    }

                    // Add all new posts to the top of the posts list
                    // Removed posts will not be removed
                    val unreadPosts = PostsDb.getInstance().getData("unread = 1")
                    for (post in unreadPosts) {
                        if (!posts.contains(post)) {
                            posts.add(0, post)
                            unreadPostsRecycler.adapter?.notifyItemInserted(0)
                            unreadPostsRecycler.adapter?.notifyItemRemoved(3)
                        }
                    }

                    // Add all new tasks to the top of the tasks list
                    val unreadTasks = TasksDb.getInstance().getUndoneData(false)
                    for (task in unreadTasks) {
                        if (tasks.indexOfFirst { it.id == task.id } == -1) {
                            tasks.add(0, task)
                            tasksRecycler.adapter?.notifyItemInserted(0)
                            tasksRecycler.adapter?.notifyItemRemoved(5)
                        }
                    }
                    // Update tasks title
                    updateTasksTitle()
                }

                // Update tasks and posts list when a task is done
                else if (intent.getStringExtra("content") == "taskDone"
                        && ::tasks.isInitialized) {
                    val taskId = intent.getStringExtra("taskId")
                    val postId = intent.getStringExtra("postId")

                    // If tasks list contains this, notify tasks aapter, else add it
                    val taskIndex = tasks.indexOfFirst { it.id == taskId }
                    if (taskIndex != -1) {
                        // Remove this task from the recycler
                        tasks.removeAt(taskIndex)
                        tasksRecycler.adapter?.notifyItemRemoved(taskIndex)
                    } else if (!intent.getBooleanExtra("isDone", true)) {
                        val taskToAdd = TasksDb.getInstance().getDataById(taskId, false)
                        if (taskToAdd != null) {
                            // Add task to the top of the list if it is not done and wasn't there before
                            tasks.add(0, taskToAdd)
                            tasksRecycler.adapter?.notifyItemInserted(0)
                            tasksRecycler.adapter?.notifyItemRemoved(5)
                        }
                    }
                    // Update title
                    updateTasksTitle()

                    if (postId != null) {
                        // If posts list contains this, notify it
                        val postIndex = posts.indexOfFirst { it.id == postId }
                        if (postIndex != -1) unreadPostsRecycler.adapter?.notifyItemChanged(postIndex)
                    }
                } else if (intent.getStringExtra("content") == "changes" &&
                        FunctionTilesDb.getInstance().supports(FunctionTile.FEATURE_CHANGES)
                        && ::changesAdapter.isInitialized) {

                    // Update changes ui

                    changesAdapter.changes = ChangesDb.instance!!.getFavorites()
                    changesAdapter.notifyDataSetChanged()

                    // Make sure the layout is visible
                    if (changesAdapter.changes.isNotEmpty())
                        changesRecycler.visibility = View.VISIBLE

                } else if (intent.getStringExtra("content") == "messages" &&
                        ::messagesAdapter.isInitialized &&
                        Firebase.remoteConfig.getBoolean("messages_enabled")) {

                    // Update messages

                    // Get the updated conversation
                    val updateId = intent.getStringExtra("id")
                    val updatedInfo = ConversationsDb()
                            .getConversationInfo(
                                    "archived=0 AND conversation_id=\"$updateId\"")
                            .firstOrNull()

                    if (updatedInfo != null) {
                        if (intent.getStringExtra("type") == "new") {
                            // New conversation, just add it to the top of the list
                            messagesAdapter.conversations.add(0, updatedInfo)
                            messagesAdapter.notifyItemInserted(0)

                            // Make sure the layout is visible
                            messagesLayout.visibility = View.VISIBLE
                        } else if (intent.getStringExtra("type") == "metachanged") {
                            // Updated conversation, find and update it in the list
                            // Move it to the top while we're at it
                            val index = messagesAdapter.conversations.indexOfFirst { it.id == updateId }
                            if (index != -1) {
                                // Remove current position and push to list
                                messagesAdapter.conversations.removeAt(index)
                                messagesAdapter.conversations.add(0, updatedInfo)
                                // Notify item moved to top and changed
                                messagesAdapter.notifyItemMoved(index, 0)
                                messagesAdapter.notifyItemChanged(0)
                            }
                        }

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
                                  container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {

            val view  = inflater.inflate(R.layout.fragment_home, container, false)
            val prefs = SphPlanner.appContext().getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)

            // Set random greeting as action bar title, once per app start, or after 30 minutes
            if (SphPlanner.randomGreeting == null || Date().time - SphPlanner.randomGreetingTime > 30 * 60 * 1000) {
                SphPlanner.randomGreeting = Utility.getGreeting()
                SphPlanner.randomGreetingTime = Date().time
            }
            (activity as AppCompatActivity).supportActionBar?.title = SphPlanner.randomGreeting

    /*
     * Timetable
     */

    val timetableLayout = view.findViewById<LinearLayout>(R.id.timetableLayout)
    val timetable = view.findViewById<FragmentContainerView>(R.id.timetableFragment)
    if (FunctionTilesDb.getInstance().supports(FunctionTile.FEATURE_TIMETABLE)) {
        parentFragmentManager.beginTransaction()
                .add(R.id.timetableFragment, TimetableViewFragment())
                .setReorderingAllowed(true) //Optimizing state changes for better transitions
                .commit()

        var spprtStr = SphPlanner.prefs.getString("clss_name", "")!!
        if (spprtStr.isNullOrEmpty() || (spprtStr == "0")) spprtStr = ""
        val timetableText = view.findViewById<TextView>(R.id.timetableTitleTextView)

        timetableText.text =
            SphPlanner.appContext().getString(R.string.timetable_personal_title)
                .replace("%class", spprtStr)

        timetableLayout.setOnClickListener {
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                    .navigate(R.id.timetableFromHomeAction, null, null,
                            FragmentNavigatorExtras(timetable to "timetable"))
        }
    } else timetableLayout.visibility = View.GONE


    /*
     * Messages
     */

    messagesLayout = view.findViewById(R.id.messagesLayout)
    val messagesRecycler = view.findViewById<RecyclerView>(R.id.messagesRecycler)

    if (FunctionTilesDb.getInstance().supports(FunctionTile.FEATURE_MESSAGES)
            && FirebaseRemoteConfig.getInstance().getBoolean("messages_enabled")) {

        // Get info for unread conversations
        val unreadMessages = ConversationsDb().getConversationInfo(
                "conversations.unread=1").toMutableList()
        messagesAdapter = ConversationsAdapter(unreadMessages, requireActivity(), compactMode = true)
        messagesRecycler.adapter = messagesAdapter

        // If no unread messages are to be displayed, hide the layout
        if (unreadMessages.isEmpty()) {
            messagesLayout.visibility = View.GONE
        }

        // Navigate to fragments tab
        messagesLayout.setOnClickListener {
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                    .navigate(R.id.messagesFromHomeAction)
        }

    }


    /*
     * Personalized changes
     */

    val changesTitle = view.findViewById<TextView>(R.id.changesTitleTextView)
    val changesLayout = view.findViewById<LinearLayout>(R.id.changesLayout)
    changesRecycler = view.findViewById(R.id.changesRecycler)

    if (FunctionTilesDb.getInstance().supports(FunctionTile.FEATURE_CHANGES)) {

        // Get changes for favorite courses and display them
        val personalizedChanges = ChangesDb.instance!!.getFavorites()
        // Create changes adapter
        changesAdapter = CompactChangesAdapter(personalizedChanges) {
            requireActivity().findNavController(R.id.nav_host_fragment)
                    .navigate(R.id.changesFromHomeAction, bundleOf("favorites" to personalizedChanges.isNotEmpty()))
        }
        changesRecycler.adapter = changesAdapter
        changesRecycler.setHasFixedSize(true)
        if (personalizedChanges.isEmpty()) {
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
    tasksRecyclerLayout = view.findViewById(R.id.tasksRecyclerLayout)
    tasksRecycler = view.findViewById(R.id.tasksRecycler)
    tasksTitle = view.findViewById(R.id.tasksTitleTextView)
    val moreTasksText = view.findViewById<TextView>(R.id.moreTasksTextView)

    if (FunctionTilesDb.getInstance().supports(FunctionTile.FEATURE_COURSES)) {

        tasks = TasksDb.getInstance().getUndoneData(false)
        var tasksOverflow = 0
        if (tasks.size > 6) {
            tasksOverflow = tasks.size - 6
            // Display more tasks message
            moreTasksText.text = resources.getQuantityString(R.plurals.tasks_personalized_more, tasksOverflow, tasksOverflow)
            moreTasksText.visibility = View.VISIBLE
        }

        if (tasks.isNotEmpty()) {
            // Set up recycler with undone tasks, shows only 5 at a time
            tasksRecycler.adapter = CompactTasksAdapter(
                    tasks,
                    6,
                    onClick = {
                        // Show single task bottom sheet
                        TaskSheet(it.id).show(parentFragmentManager, "task")
                    },
                    onTaskCheckedChanged = Tasks().onCheckedChanged(requireActivity()) { task, isDone ->
                        if (isDone) {
                            // Update tasks dataset
                            val index = tasks.indexOfFirst { it.id == task.id }
                            tasks.removeAt(index)
                            tasksRecycler.adapter?.notifyItemRemoved(index)
                            if (tasks.size < 6) (tasksRecycler.adapter as CompactTasksAdapter)
                            // Update overflow counter
                            if (tasksOverflow > 0) {
                                tasksOverflow--
                                if (tasksOverflow > 0)
                                    moreTasksText.text = resources.getQuantityString(R.plurals.tasks_personalized_more, tasksOverflow, tasksOverflow)
                                else moreTasksText.visibility = View.GONE
                            }
                            // Update title and hide if necessary
                            updateTasksTitle()
                        }
                    }
            )
        }
        // Show the number of undone tasks or that everything is done
        // Will also handle hiding tasks recycler layout if necessary
        updateTasksTitle()

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
        posts = PostsDb.getInstance().getData("posts.unread = 1")
        var postsOverflow = 0
        if (posts.size > 4) {
            postsOverflow = posts.size - 4
            // Display more unread posts text
            moreUnreadText.text = resources.getQuantityString(R.plurals.posts_more_unread, postsOverflow, postsOverflow)
            moreUnreadText.visibility = View.VISIBLE
        }
        posts.addAll(PostsDb.getInstance().getData("posts.unread = 0",
                "ORDER BY posts.date DESC LIMIT 4"))

        if (posts.isNotEmpty()) {
            // Only show posts if there are any
            unreadPostsRecycler.setHasFixedSize(true)
            unreadPostsRecycler.adapter = CompactPostsAdapter(posts, 4)
            { post: CompactPostsAdapter.PostData, _: View ->
                // Show single post bottom sheet
                PostSheet(post.id).show(parentFragmentManager, "post")

                // If the post was unread, mark as read
                if (post.unread) {
                    PostsDb.getInstance().markAsRead(post.id)
                    post.unread = false
                    // Update overflow counter
                    postsOverflow--
                    if (postsOverflow != 0)
                        moreUnreadText.text = resources.getQuantityString(R.plurals.posts_more_unread, postsOverflow, postsOverflow)
                    else moreUnreadText.visibility = View.GONE

                    // Move the post down the list until it's at its correct position by date
                    val index = posts.indexOf(post)
                    var nextIndex = index
                    var postMoved = false
                    // Skip all indizes with posts that are also unread
                    // Maximum 8 shifts to avoid long runtime
                    while (nextIndex < 8
                            && posts.getOrNull(nextIndex + 1) != null
                            && (posts[nextIndex + 1].unread
                                    || post.date.before(posts[nextIndex + 1].date))) {
                        postMoved = true
                        // Overwrite this with the next post and notify the adapter
                        posts[nextIndex] = posts[nextIndex + 1]
                        unreadPostsRecycler.adapter?.notifyItemMoved(nextIndex + 1, nextIndex)
                        nextIndex++
                    }
                    // If the post moved, the entry at nextindex is now duplicated
                    // and can be replaced by this post, again update adapter for this position
                    if (postMoved) posts[nextIndex] = post
                    unreadPostsRecycler.adapter?.notifyItemChanged(nextIndex)
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

    override fun onDestroy() {
    // Unregister broadcast receiver
    LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(uichangeReceiver)
    super.onDestroy()
    }

}