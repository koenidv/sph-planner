package de.koenidv.sph.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import de.koenidv.sph.R
import de.koenidv.sph.objects.PostAttachment

//  Created by koenidv on 20.12.2020.
class AttachmentsAdapter(private val attachments: List<PostAttachment>,
                         private val onAttachmentClick: (PostAttachment) -> Unit) :
        RecyclerView.Adapter<AttachmentsAdapter.ViewHolder>() {

    /**
     * Provides a reference to the type of view
     * (custom ViewHolder).
     */
    class ViewHolder(view: View, onAttachmentClick: (PostAttachment) -> Unit) : RecyclerView.ViewHolder(view) {
        private val nameText: TextView = view.findViewById(R.id.nameTextView)
        private val card: MaterialCardView = view.findViewById(R.id.attachmentCard)
        var currentAttachment: PostAttachment? = null

        // Setup onClickListener
        init {
            card.setOnClickListener {
                currentAttachment?.let {
                    onAttachmentClick(it)
                }
            }
        }

        fun bind(attachment: PostAttachment) {

            // Set current attachment for onClick
            currentAttachment = attachment

            // Set data
            nameText.text = attachment.name

        }
    }

    // Creates new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_attachment, viewGroup, false)

        return ViewHolder(view, onAttachmentClick)
    }

    // Replaces the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Bind data to ViewHolder
        viewHolder.bind(attachments[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = attachments.size

}