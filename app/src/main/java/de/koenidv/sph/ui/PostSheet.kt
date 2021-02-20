package de.koenidv.sph.ui

import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.koenidv.sph.R
import de.koenidv.sph.adapters.PostsAdapter
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.database.PostsDb
import de.koenidv.sph.networking.AttachmentManager
import de.koenidv.sph.networking.Tasks
import de.koenidv.sph.objects.Attachment

//  Created by koenidv on 29.12.2020.
class PostSheet internal constructor(private val id: String) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.sheet_post, container, false)

        val titleLayout = view.findViewById<LinearLayout>(R.id.titleLayout)
        val courseTextView = view.findViewById<TextView>(R.id.courseTextView)
        val postsRecycler = view.findViewById<RecyclerView>(R.id.postsRecycler)
        val doneButton = view.findViewById<Button>(R.id.doneButton)

        // Get post
        val post = PostsDb.getInstance().getByPostId(id)

        // Set course text
        courseTextView.text = CoursesDb.getInstance().getFullname(post.id_course)
        // Adjust course background color
        // Set background color, about 70% opacity
        val opacity: Int = 0xb4000000.toInt()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            (courseTextView.background as StateListDrawable).colorFilter = BlendModeColorFilter(
                    CoursesDb.getInstance().getColor(post.id_course) and 0x00FFFFFF or opacity, BlendMode.SRC_ATOP)
        } else {
            @Suppress("DEPRECATION") // not in < Q
            (courseTextView.background as StateListDrawable).setColorFilter(
                    CoursesDb.getInstance().getColor(post.id_course) and 0x00FFFFFF or opacity, PorterDuff.Mode.SRC_ATOP)
        }

        postsRecycler.adapter = PostsAdapter(
                listOf(post),
                AttachmentManager().movementMethod(requireActivity(), R.id.frag_webview),
                AttachmentManager().onAttachmentClick(requireActivity()) { _: Int, _: Attachment -> },
                AttachmentManager().onAttachmentLongClick(requireActivity()) { action: Int, _: Attachment ->
                    // Update ui when attachment was renamed
                    if (action == AttachmentManager.ATTACHMENT_RENAMED
                            || action == AttachmentManager.ATTACHMENT_RENAMED_PIN) {
                        // Notify posts recycler about changed data
                        // This will only ever be 1 item
                        postsRecycler.adapter?.notifyItemChanged(0)
                    }
                },
                Tasks().onCheckedChanged(requireActivity())
        )

        titleLayout.setOnClickListener {
            dismiss()
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                    .navigate(R.id.frag_course_overview, bundleOf("courseId" to post.id_course))
        }

        doneButton.setOnClickListener { dismiss() }

        return view
    }
}