package de.koenidv.sph.adapters

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.networking.AttachmentManager
import de.koenidv.sph.objects.PostAttachment
import java.io.File

//  Created by koenidv on 20.12.2020.
class AttachmentsAdapter(private val attachments: List<PostAttachment>,
                         private val onAttachmentClick: (PostAttachment, View) -> Unit,
                         private val onAttachmentLongClick: (PostAttachment, View) -> Unit) :
        RecyclerView.Adapter<AttachmentsAdapter.ViewHolder>() {

    /**
     * Provides a reference to the type of view
     * (custom ViewHolder).
     */
    class ViewHolder(view: View, onAttachmentClick: (PostAttachment, View) -> Unit, onAttachmentLongClick: (PostAttachment, View) -> Unit) : RecyclerView.ViewHolder(view) {
        private val nameText: TextView = view.findViewById(R.id.nameTextView)
        private val iconText: TextView = view.findViewById(R.id.iconTextView)
        private val card: MaterialCardView = view.findViewById(R.id.attachmentCard)
        var currentAttachment: PostAttachment? = null

        // Setup onClickListener
        init {
            card.setOnClickListener {
                currentAttachment?.let {
                    onAttachmentClick(it, view)
                }
            }
            card.setOnLongClickListener {
                currentAttachment?.let {
                    onAttachmentLongClick(it, view)
                }
                true
            }
        }

        fun bind(attachment: PostAttachment) {

            // Set current attachment for onClick
            currentAttachment = attachment

            // Set name
            var name = attachment.name
            val type = attachment.fileType
            // Replace attachment name if its an image and the name consists of only digits and whitespaces
            if (listOf("jpg", "jpeg", "png", "gif").contains(type) && TextUtils.isDigitsOnly(name.replace(" ", "")))
                name = SphPlanner.applicationContext().getString(R.string.attachments_namereplace_picture)
            nameText.text = name

            // Set downloaded and filetype icons
            var icon = ""
            if (File(SphPlanner.applicationContext().filesDir.toString() + "/" + attachment.localPath()).exists())
                icon += "check-circle "
            icon += AttachmentManager().getIconForFiletype(type)
            iconText.text = icon
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
        // Bind data to ViewHolder
        viewHolder.bind(attachments[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = attachments.size
}