package de.koenidv.sph.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.objects.Post
import de.koenidv.sph.objects.PostAttachment
import de.koenidv.sph.objects.PostTask
import java.text.SimpleDateFormat
import java.util.*

//  Created by koenidv on 20.12.2020.
class PostsAdapter(private val posts: List<Post>, private val tasks: List<PostTask>, private val attachments: List<PostAttachment>) :
        RecyclerView.Adapter<PostsAdapter.ViewHolder>() {

    val attachmentsViewPool = RecyclerView.RecycledViewPool()

    /**
     * Provides a reference to the type of view
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val layout: ConstraintLayout = view.findViewById(R.id.postLayout)
        private val card: MaterialCardView = view.findViewById(R.id.materialCardView)
        private val dateText: TextView = view.findViewById(R.id.dateTextView)
        private val titleText: TextView = view.findViewById(R.id.titleTextView)
        private val descriptionText: TextView = view.findViewById(R.id.descriptionTextView)
        private val taskCheckBox: MaterialCheckBox = view.findViewById(R.id.taskCheckBox)
        private val taskText: TextView = view.findViewById(R.id.taskTextView)
        private val taskHighlight: View = view.findViewById(R.id.taskHighlightView)
        private val attachmentsRecycler: RecyclerView = view.findViewById(R.id.attachmentsRecycler)
        private val dateFormat = SimpleDateFormat("d. MMM yyyy", Locale.getDefault())

        fun bind(post: Post, task: PostTask?, attachments: List<PostAttachment>,
                 attachmentsViewPool: RecyclerView.RecycledViewPool) {

            val themeColor = SphPlanner.applicationContext()
                    .getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)
                    .getInt("themeColor", 0)

            // Set data
            dateText.text = dateFormat.format(post.date)
            if (post.title != null) titleText.text = post.title
            else titleText.visibility = View.GONE
            if (post.description != null) descriptionText.text = post.description
            else descriptionText.visibility = View.GONE

            if (post.unread) {
                // Apply theme color with 15% opacity to background
                card.backgroundTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_enabled)), intArrayOf(themeColor and 0x00FFFFFF or 0x26000000))
            }

            // Task
            if (task != null) {
                taskCheckBox.isChecked = task.isDone
                taskCheckBox.visibility = View.VISIBLE
                taskText.text = task.description
                taskText.visibility = View.VISIBLE

                if (!task.isDone && !post.unread) {
                    // If task not done and post doesn't already have a colored background
                    // Apply theme color with 20% opacity to background
                    taskHighlight.setBackgroundColor(themeColor and 0x00FFFFFF or 0x33000000)
                }
            }

            // Attachments
            if (attachments.isNullOrEmpty()) {
                // Re-set bottom margin if there are no attachments
                val constraintSet = ConstraintSet()
                constraintSet.clone(layout)
                constraintSet.setMargin(R.id.materialCardView, ConstraintSet.BOTTOM, 0)
                constraintSet.applyTo(layout)
            } else {
                // Set up attachments recycler
                attachmentsRecycler.setHasFixedSize(true)
                attachmentsRecycler.setRecycledViewPool(attachmentsViewPool)
                attachmentsRecycler.adapter = AttachmentsAdapter(attachments)
            }


            /*
            // Set background color
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                (layout.background as StateListDrawable).colorFilter = BlendModeColorFilter(course.color
                        ?: 6168631, BlendMode.SRC_ATOP)
            } else {
                @Suppress("DEPRECATION") // not in < Q
                (layout.background as StateListDrawable)
                        .setColorFilter(course.color ?: 6168631, PorterDuff.Mode.SRC_ATOP)
            }
            */
        }
    }

    // Creates new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_post, viewGroup, false)
        return ViewHolder(view)
    }

    // Replaces the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val post = posts[position]
        // Bind data to ViewHolder
        viewHolder.bind(
                post,
                tasks.firstOrNull { it.id_post == post.postId }, // There will always only be one task per post
                attachments.filter { it.id_post == post.postId },
                attachmentsViewPool) // Filter attachments for post
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = posts.size

}