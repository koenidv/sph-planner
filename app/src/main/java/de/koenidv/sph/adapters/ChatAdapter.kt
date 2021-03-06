package de.koenidv.sph.adapters

import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import de.koenidv.sph.database.UsersDb
import de.koenidv.sph.objects.Message
import me.saket.bettermovementmethod.BetterLinkMovementMethod


//  Created by koenidv on 12.02.2021.
class ChatAdapter(val messages: MutableList<Message>, private val conversationInfo: Pair<String, Int>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /**
     * Provides a reference to the type of view
     * (custom ConversationViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val outerLayout = view.findViewById<ConstraintLayout>(R.id.messageOuter)
        private val layout = view.findViewById<LinearLayout>(R.id.messageLayout)
        private val content = view.findViewById<TextView>(R.id.messageTextView)
        private val name = view.findViewById<TextView>(R.id.nameTextView)
        private val media = view.findViewById<RecyclerView>(R.id.messageMediaRecycler)


        fun bind(message: Message) {

            /*
             * Data
             */

            // Text content, with newlines, spaces and image links removed from the end
            content.text = message.content
                    .replace("""(Bild:|https://i.imgur.com\S*|\s|\\n)*$""".toRegex(), "")
            BetterLinkMovementMethod.linkify(Linkify.ALL, content)

            // Sender name
            if (message.isOwn()) {
                name.visibility = View.GONE
            } else {
                name.visibility = View.VISIBLE
                val sendername = UsersDb.getName(message.idSender)
                name.text = if (sendername != message.idSender) sendername else message.senderName
            }

            /*
             * Message bubble
             */
            // Set background drawable depending on if the message is outgoing or incoming
            // Also set horizontal bias so messages show up on the correct side
            if (message.isOwn()) {
                layout.setBackgroundResource(R.drawable.message_background_outgoing)
                ConstraintSet().apply {
                    clone(outerLayout)
                    setHorizontalBias(layout.id, 1f)
                    setHorizontalBias(name.id, 1f)
                }.applyTo(outerLayout)
            } else {
                layout.setBackgroundResource(
                        if (message.senderType == "Teilnehmer")
                            R.drawable.message_background_incoming_student
                        else R.drawable.message_background_incoming)
                ConstraintSet().apply {
                    clone(outerLayout)
                    setHorizontalBias(layout.id, 0f)
                    setHorizontalBias(name.id, 0f)
                }.applyTo(outerLayout)
            }

            /*
             * Media stuff
             */
            val mediaValues = mutableListOf<ChatMediaAdapter.ChatMedia>()

            // Find imgur links in message
            for (match in Regex("""https://i\.imgur\.com/\w{6,8}\.\w{2,5}""")
                    .findAll(message.content)) {
                mediaValues.add(ChatMediaAdapter.ChatMedia(
                        ChatMediaAdapter.TYPE_IMGUR,
                        "${match.value}?maxwidth=800"))
            }

            // Display media
            if (mediaValues.isNotEmpty())
                media.adapter = ChatMediaAdapter(mediaValues, message.isOwn())
            else media.adapter = null

        }
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val identicon = view.findViewById<ImageView>(R.id.headerImageView)
        private val header = view.findViewById<TextView>(R.id.headerTextView)

        fun bind(info: Pair<String, Int>) {

            header.text = applicationContext().getString(
                    if (info.second == 0)
                        R.string.messages_header_private
                    else R.string.messages_header_group)
                    .replace("%name", info.first)
                    .replace("%count", info.second.toString())


            // Get an identicon using Kwelo's API
            Glide.with(identicon.context)
                    .load("https://api.kwelo.com/v1/media/identicon/${info.first}?format=webm")
                    .circleCrop()
                    .into(identicon)

        }

    }

    // Creates new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(viewType, viewGroup, false)
        return if (viewType == R.layout.item_chat_header) HeaderViewHolder(view)
        else ViewHolder(view)
    }

    // Replaces the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        if (position == 0) {
            // Head item ("Conversation with xy")
            (viewHolder as HeaderViewHolder).bind(conversationInfo)
        } else {
            // Bind data to ConversationViewHolder
            (viewHolder as ViewHolder).bind(messages[position - 1])
        }
    }

    override fun getItemViewType(position: Int): Int {
        // Head item type on position 0, message otherwise
        return if (position == 0) {
            R.layout.item_chat_header
        } else {
            R.layout.item_message
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = messages.size + 1

}