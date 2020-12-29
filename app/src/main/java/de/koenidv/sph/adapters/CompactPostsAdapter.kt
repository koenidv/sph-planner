package de.koenidv.sph.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import de.koenidv.sph.database.AttachmentsDb
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.database.PostTasksDb
import de.koenidv.sph.database.PostsDb
import de.koenidv.sph.objects.Post
import de.koenidv.sph.parsing.Utility


//  Created by koenidv on 20.12.2020.
class CompactPostsAdapter(private val posts: List<Post>,
                          private val onClick: (Post) -> Unit) :
        RecyclerView.Adapter<CompactPostsAdapter.ViewHolder>() {

    /**
     * Provides a reference to the type of view
     * (custom ViewHolder).
     */
    class ViewHolder(view: View, onClick: (Post) -> Unit) : RecyclerView.ViewHolder(view) {
        private val layout = view.findViewById<ConstraintLayout>(R.id.postLayout)
        private val title = view.findViewById<TextView>(R.id.titleTextView)
        private val unread = view.findViewById<TextView>(R.id.unreadTextView)
        private val course = view.findViewById<TextView>(R.id.courseTextView)
        private val attachs = view.findViewById<TextView>(R.id.attachmentsTextView)
        private val task = view.findViewById<TextView>(R.id.taskTextView)
        private val info = view.findViewById<TextView>(R.id.infoTextView)
        private var currentPost: Post? = null

        init {
            layout.setOnClickListener {
                currentPost?.let {
                    onClick(it)
                    if (it.unread) {
                        unread.visibility = View.GONE
                        PostsDb.getInstance().markAsRead(it.postId)
                    }
                }
            }
        }

        fun bind(post: Post) {
            currentPost = post

            val taskDone: Boolean? = PostTasksDb.getInstance().taskDone(post.postId)
            val attachCount = AttachmentsDb.countForPost(post.postId)

            // Set data
            title.text = post.title ?: post.description
                    ?: applicationContext().getString(R.string.posts_no_text)

            course.text = CoursesDb.getInstance().getFullname(post.id_course)

            if (post.unread) unread.visibility = View.VISIBLE

            if (attachCount > 0) {
                attachs.visibility = View.VISIBLE
                attachs.text = applicationContext().resources.getQuantityString(R.plurals.posts_info_attachments, attachCount, attachCount)
            }
            if (taskDone != null) {
                task.visibility = View.VISIBLE
                val color: Int
                if (taskDone) {
                    task.text = applicationContext().getString(R.string.posts_info_task_done)
                    // todo use theme color
                    //color = Utility().getThemedColor(R.attr.tagBackground)
                    color = applicationContext().getColor(R.color.grey_800)
                } else {
                    task.text = applicationContext().getString(R.string.posts_info_task_undone)
                    //color = Utility().getThemedColor(R.attr.tagBackgroundWarning)
                    color = applicationContext().getColor(R.color.pink_a700)
                }
                Utility().tintBackground(task, color, 0xb4000000.toInt())
            }

            // Adjust course background color
            // Set background color, about 70% opacity
            Utility().tintBackground(course, CoursesDb.getInstance().getColor(post.id_course), 0xb4000000.toInt())

        }
    }

    // Creates new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_post_compact, viewGroup, false)
        return ViewHolder(view, onClick)
    }

    // Replaces the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val post = posts[position]
        // Bind data to ViewHolder
        viewHolder.bind(post)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = posts.size
}