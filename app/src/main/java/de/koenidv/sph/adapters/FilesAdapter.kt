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
import de.koenidv.sph.objects.FileAttachment
import java.io.File

//  Created by koenidv on 20.12.2020.
class FilesAdapter(private val files: List<FileAttachment>,
                   private val onAttachmentClick: (FileAttachment, View) -> Unit,
                   private val onAttachmentLongClick: (FileAttachment, View) -> Unit) :
        RecyclerView.Adapter<FilesAdapter.ViewHolder>() {

    /**
     * Provides a reference to the type of view
     * (custom ViewHolder).
     */
    class ViewHolder(view: View, onAttachmentClick: (FileAttachment, View) -> Unit, onAttachmentLongClick: (FileAttachment, View) -> Unit) : RecyclerView.ViewHolder(view) {
        private val nameText: TextView = view.findViewById(R.id.nameTextView)
        private val iconText: TextView = view.findViewById(R.id.iconTextView)
        private val card: MaterialCardView = view.findViewById(R.id.attachmentCard)
        var currentFile: FileAttachment? = null

        // Setup onClickListener
        init {
            card.setOnClickListener {
                currentFile?.let {
                    onAttachmentClick(it, view)
                }
            }
            card.setOnLongClickListener {
                currentFile?.let {
                    onAttachmentLongClick(it, view)
                }
                true
            }
        }

        fun bind(file: FileAttachment) {

            // Set current file for onClick
            currentFile = file

            // Set name
            var name = file.name
            val type = file.fileType
            // Replace mAttachment name if its an image and the name consists of only digits and whitespaces
            if (listOf("jpg", "jpeg", "png", "gif").contains(type) && TextUtils.isDigitsOnly(name.replace(" ", "")))
                name = SphPlanner.applicationContext().getString(R.string.attachments_namereplace_picture)
            nameText.text = name

            // Set pinned, downloaded and filetype icons
            var icon = ""
            // Add pinned icon if the mAttachment is pinned
            if (file.pinned) icon += "thumbtack "
            // Add checkmark if file is saved locally
            if (File(SphPlanner.applicationContext().filesDir.toString() + "/" + file.localPath()).exists())
                icon += "check-circle "
            // Set file icon according to file type
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
        viewHolder.bind(files[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = files.size
}