package de.koenidv.sph.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerDrawable
import de.koenidv.sph.R
import de.koenidv.sph.parsing.Utility


//  Created by koenidv on 20.12.2020.
class ChatMediaAdapter(private val media: List<ChatMedia>, private val isOwn: Boolean) :
        RecyclerView.Adapter<ChatMediaAdapter.ViewHolder>() {

    companion object {
        const val TYPE_IMGUR = 0

    }

    class ChatMedia(val type: Int, val url: String)

    /**
     * Provides a reference to the type of view
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val outerLayout = view.findViewById<ConstraintLayout>(R.id.messageOuter)
        private val layout = view.findViewById<LinearLayout>(R.id.messageLayout)
        private val image = view.findViewById<ImageView>(R.id.mediaImageView)


        fun bind(media: ChatMedia, isOwn: Boolean) {

            val shimmer = Shimmer.AlphaHighlightBuilder()// The attributes for a ShimmerDrawable is set by this builder
                    .setDuration(1800) // how long the shimmering animation takes to do one full sweep
                    .setHighlightAlpha(1f) // the shimmer alpha amount
                    .setAutoStart(true)
                    .build()

            // This is the placeholder for the imageView
            val shimmerDrawable = ShimmerDrawable().apply {
                setShimmer(shimmer)
            }

            Glide.with(image.context).load(media.url)
                    .placeholder(shimmerDrawable)
                    .transform(RoundedCorners(Utility.dpToPx(16f).toInt()))
                    .into(image)

            // Set background drawable depending on if the message is outgoing or incoming
            // Also set horizontal bias so messages show up on the correct side
            if (isOwn) {
                layout.setBackgroundResource(R.drawable.message_background_outgoing_media)
                ConstraintSet().apply {
                    clone(outerLayout)
                    setHorizontalBias(layout.id, 1f)
                }.applyTo(outerLayout)
            } else {
                layout.setBackgroundResource(R.drawable.message_background_incoming_media)
                ConstraintSet().apply {
                    clone(outerLayout)
                    setHorizontalBias(layout.id, 0f)
                }.applyTo(outerLayout)
            }

        }

    }

    // Creates new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_message_media_image, viewGroup, false)
        return ViewHolder(view)
    }

    // Replaces the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Bind data to ViewHolder
        viewHolder.bind(media[position], isOwn)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = media.size

}