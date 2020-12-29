package de.koenidv.sph.adapters

import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.StateListDrawable
import android.os.Build
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
        private val info = view.findViewById<TextView>(R.id.infoTextView)
        private var currentPost: Post? = null

        init {
            layout.setOnClickListener {
                currentPost?.let {
                    onClick(it)
                }
            }
        }

        fun bind(post: Post) {

            val taskDone: Boolean? = PostTasksDb.getInstance().taskDone(post.postId)
            val attachCount = AttachmentsDb.countForPost(post.postId)

            // Set data
            title.text = post.title ?: post.description
                    ?: applicationContext().getString(R.string.posts_no_text)
            course.text = CoursesDb.getInstance().getFullname(post.id_course)
            if (post.unread) unread.visibility = View.VISIBLE
            info.text = when {
                taskDone == true && attachCount > 0 ->
                    applicationContext().resources.getQuantityString(R.plurals.posts_info_attachments_and_task_done, attachCount, attachCount)
                taskDone == false && attachCount > 0 ->
                    applicationContext().resources.getQuantityString(R.plurals.posts_info_attachments_and_task_undone, attachCount, attachCount)
                attachCount > 0 ->
                    applicationContext().resources.getQuantityString(R.plurals.posts_info_attachments, attachCount, attachCount)
                taskDone == true ->
                    applicationContext().getString(R.string.posts_info_homework_done)
                taskDone == false ->
                    applicationContext().getString(R.string.posts_info_homework_undone)
                else -> ""
            }

            // Adjust course background color
            // Set background color, about 70% opacity
            val opacity: Int = 0xb4000000.toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                (course.background as StateListDrawable).colorFilter = BlendModeColorFilter(
                        CoursesDb.getInstance().getColor(post.id_course) and 0x00FFFFFF or opacity, BlendMode.SRC_ATOP)
            } else {
                @Suppress("DEPRECATION") // not in < Q
                (course.background as StateListDrawable).setColorFilter(
                        CoursesDb.getInstance().getColor(post.id_course) and 0x00FFFFFF or opacity, PorterDuff.Mode.SRC_ATOP)
            }


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