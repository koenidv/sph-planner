package de.koenidv.sph.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
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
import de.koenidv.sph.database.AttachmentsDb
import de.koenidv.sph.database.ChangesDb
import de.koenidv.sph.database.PostTasksDb
import de.koenidv.sph.database.PostsDb
import de.koenidv.sph.networking.AttachmentManager
import de.koenidv.sph.networking.NetworkManager
import de.koenidv.sph.objects.Attachment
import de.koenidv.sph.objects.Post
import de.koenidv.sph.parsing.Utility
import java.util.*


class HomeFragment : Fragment() {

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

        // Only for demonstration

        val surpriseButton = view.findViewById<Button>(R.id.surpriseButton)
        surpriseButton.setOnClickListener {
            //NetworkManager().indexAll { Toast.makeText(SphPlanner.applicationContext(), "Heute schon, Kartoffel", Toast.LENGTH_SHORT).show() }
            //NetworkManager().loadAndSavePosts { Toast.makeText(SphPlanner.applicationContext(), "Kartoffelfeld abgeräumt", Toast.LENGTH_SHORT).show() }
            //DatabaseHelper.getInstance().writableDatabase.execSQL("DELETE FROM posts WHERE post_id IN (SELECT post_id FROM posts ORDER BY date DESC LIMIT 3)")
            NetworkManager().loadAndSaveChanges {
                Toast.makeText(SphPlanner.applicationContext(), "Kartoffel, in Deckung!", Toast.LENGTH_SHORT).show()
            }
        }


        /*
         * Timetable todo check if timetable feature is supported
         */

        val timetable = view.findViewById<FragmentContainerView>(R.id.timetableFragment)
        view.findViewById<LinearLayout>(R.id.timetableLayout).setOnClickListener {
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                    .navigate(R.id.timetableFromHomeAction, null, null,
                            FragmentNavigatorExtras(timetable to "timetable"))
        }


        /*
         * Personalized changes todo check if changes feature is supported
         */

        val changesTitle = view.findViewById<TextView>(R.id.changesTitleTextView)
        val changesLayout = view.findViewById<LinearLayout>(R.id.changesLayout)
        val changesRecycler = view.findViewById<RecyclerView>(R.id.changesRecycler)

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


        /**
         * Undone tasks
         */

        val tasksLayout = view.findViewById<LinearLayout>(R.id.tasksLayout)
        val tasksRecyclerLayout = view.findViewById<LinearLayout>(R.id.tasksRecyclerLayout)
        val tasksRecycler = view.findViewById<RecyclerView>(R.id.tasksRecycler)
        val tasksTitle = view.findViewById<TextView>(R.id.tasksTitleTextView)
        val moreTasksText = view.findViewById<TextView>(R.id.moreTasksTextView)

        val tasks = PostTasksDb.getInstance().undone
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


        /*
         * Unread posts
         */

        val unreadPostsLayout = view.findViewById<LinearLayout>(R.id.unreadPostsLayout)
        val unreadPostsRecycler = view.findViewById<RecyclerView>(R.id.unreadPostsRecycler)
        val moreUnreadText = view.findViewById<TextView>(R.id.moreUnreadTextView)

        // Get unread posts
        // and up to 4 read posts
        // We'll only display 4 posts,
        // but unread posts will get removed once they are read
        val posts = PostsDb.getInstance().unread.toMutableList()
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

        /**
         * Pinned attachments
         */

        val pinsLayout = view.findViewById<LinearLayout>(R.id.pinsLayout)
        val pinsTitle = view.findViewById<TextView>(R.id.pinsTitleTextView)
        val pinsRecycler = view.findViewById<RecyclerView>(R.id.pinsRecycler)

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
                        }
                    }
            )
            PagerSnapHelper().attachToRecyclerView(pinsRecycler)
        }

        // Open all attachments fragment on click
        pinsLayout.setOnClickListener {
            requireActivity().findNavController(R.id.nav_host_fragment)
                    .navigate(R.id.frag_placeholder)
            // todo attachments collection
        }

        return view

    }
}