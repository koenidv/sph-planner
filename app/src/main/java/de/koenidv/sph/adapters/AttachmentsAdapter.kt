package de.koenidv.sph.adapters

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.networking.AttachmentManager
import de.koenidv.sph.objects.Attachment
import de.koenidv.sph.parsing.Utility
import java.io.File

//  Created by koenidv on 20.12.2020.
class AttachmentsAdapter(private var attachments: List<Attachment>,
                         private val onAttachmentClick: (Attachment, View) -> Unit,
                         private val onAttachmentLongClick: (Attachment, View) -> Unit,
                         private val showCourse: Boolean = false) :
        RecyclerView.Adapter<AttachmentsAdapter.ViewHolder>() {

    /**
     * Provides a reference to the type of view
     * (custom ConversationViewHolder).
     */
    class ViewHolder(view: View,
                     onAttachmentClick:
                     (Attachment, View) -> Unit,
                     onAttachmentLongClick: (Attachment, View) -> Unit) : RecyclerView.ViewHolder(view) {
        private val nameText: TextView = view.findViewById(R.id.nameTextView)
        private val iconText: TextView = view.findViewById(R.id.iconTextView)
        private val layout: ConstraintLayout = view.findViewById(R.id.attachmentLayout)
        private var currentAttachment: Attachment? = null

        // Setup onClickListener
        init {
            layout.setOnClickListener {
                currentAttachment?.let {
                    onAttachmentClick(it, view)
                }
            }
            layout.setOnLongClickListener {
                currentAttachment?.let {
                    onAttachmentLongClick(it, view)
                }
                true
            }
        }

        fun bind(attachment: Attachment, showCourse: Boolean) {

            // Set current file for onClick
            currentAttachment = attachment

            // Set name
            var name = attachment.name()
            val type = attachment.fileType()
            // Replace attachment name if its an image and the name consists of only digits and whitespaces
            if (listOf("jpg", "jpeg", "png", "gif").contains(type) && TextUtils.isDigitsOnly(name.replace(" ", "")))
                name = SphPlanner.applicationContext().getString(R.string.attachments_namereplace_picture)
            nameText.text = name

            // Set pinned, downloaded and filetype icons
            var icon = ""
            // Add pinned icon if the attachment is pinned
            if (attachment.pinned()) icon += "thumbtack "
            // Add checkmark if file is saved locally
            if (attachment.type() == "file")
                if (File(attachment.file().localPath()).exists())
                    icon += "check-circle "
            // Set file icon according to file type
            icon += AttachmentManager().getIconForFiletype(type)
            iconText.text = icon

            // Show course color if wanted
            if (showCourse) {
                Utility.tintBackground(layout, CoursesDb.getColor(attachment.courseId()), 0x26000000)
            }
        }
    }

    // Creates new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_attachment, viewGroup, false)

        return ViewHolder(view, onAttachmentClick, onAttachmentLongClick)
    }

    // Replaces the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Bind data to ConversationViewHolder
        viewHolder.bind(attachments[position], showCourse)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = attachments.size

    fun updateDataset(dataset: List<Attachment>) {
        this.attachments = dataset
        notifyDataSetChanged()
    }
}