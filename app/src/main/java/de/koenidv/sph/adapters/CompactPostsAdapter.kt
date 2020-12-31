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
import de.koenidv.sph.objects.Post
import de.koenidv.sph.parsing.Utility


//  Created by koenidv on 20.12.2020.
class CompactPostsAdapter(private val posts: List<Post>,
                          private var maxSize: Int? = null,
                          private val onClick: (Post, View) -> Unit) :
        RecyclerView.Adapter<CompactPostsAdapter.ViewHolder>() {

    /**
     * Provides a reference to the type of view
     * (custom ViewHolder).
     */
    class ViewHolder(view: View, onClick: (Post, View) -> Unit) : RecyclerView.ViewHolder(view) {
        private val layout = view.findViewById<ConstraintLayout>(R.id.postLayout)
        private val title = view.findViewById<TextView>(R.id.titleTextView)
        private val unread = view.findViewById<TextView>(R.id.unreadTextView)
        private val course = view.findViewById<TextView>(R.id.courseTextView)
        private val attachs = view.findViewById<TextView>(R.id.attachmentsTextView)
        private val task = view.findViewById<TextView>(R.id.taskTextView)
        private var currentPost: Post? = null

        init {
            layout.setOnClickListener {
                currentPost?.let {
                    onClick(it, view)
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
            else unread.visibility = View.GONE

            if (attachCount > 0) {
                attachs.visibility = View.VISIBLE
                attachs.text = applicationContext().resources.getQuantityString(R.plurals.posts_info_attachments, attachCount, attachCount)
            } else attachs.visibility = View.GONE
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
            } else task.visibility = View.GONE

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
        // Bind data to ViewHolder
        viewHolder.bind(posts[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() =
            if (maxSize != null && maxSize!! <= posts.size)
                maxSize!! else posts.size

    override fun getItemId(position: Int): Long {
        // This will in almost all cases show distinct items,
        // so we can use the position as id
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    fun setMaxSize(size: Int?) {
        val postsSize = posts.size
        val prevMaxSize = maxSize ?: postsSize
        maxSize = size
        if (size ?: postsSize > prevMaxSize)
            notifyItemRangeInserted(prevMaxSize, size ?: postsSize)
        else if (size ?: postsSize < prevMaxSize)
            notifyItemRangeChanged(size ?: postsSize, prevMaxSize)
    }
}