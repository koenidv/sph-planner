package de.koenidv.sph.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import de.koenidv.sph.database.UsersDb
import de.koenidv.sph.objects.Conversation


//  Created by koenidv on 20.12.2020.
class ConversationsAdapter(private val conversations: List<Conversation>) :
        RecyclerView.Adapter<ConversationsAdapter.ViewHolder>() {

    val onClick: (Conversation) -> Unit = {}

    /**
     * Provides a reference to the type of view
     * (custom ViewHolder).
     */
    class ViewHolder(view: View, onClick: (Conversation) -> Unit) : RecyclerView.ViewHolder(view) {
        private val layout = view.findViewById<ConstraintLayout>(R.id.conversationLayout)
        private val subject = view.findViewById<TextView>(R.id.subjectTextView)
        private val participants = view.findViewById<TextView>(R.id.participantsTextView)

        private var currentConversation: Conversation? = null

        init {
            layout.setOnClickListener {
                currentConversation?.let {
                    onClick(it)
                }
            }
        }


        fun bind(conversation: Conversation) {
            currentConversation = conversation

            subject.text = conversation.subject
            participants.text = getRecipientText(conversation)

        }

        fun getRecipientText(conversation: Conversation): String {
            val prefs = applicationContext()
                    .getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)

            var text = applicationContext().getString(when {
                conversation.originalSenderId == prefs.getString("userid", "") &&
                        // From self and only one recipient
                        conversation.recipientCount == 1 -> R.string.messages_partic_fromself
                conversation.originalSenderId == prefs.getString("userid", "") ->
                    // From self with multiple recipients
                    R.string.messages_partic_fromself_more
                conversation.recipientCount == 1 ->
                    // Not from self, only one recipient
                    R.string.messages_partic_toself
                else ->
                    // Not from self and multiple recipients
                    R.string.messages_partic_toself_more
            })

            // Replace placeholders
            text = text.replace("%sender", UsersDb().getName(conversation.originalSenderId))
                    .replace("%countall", conversation.recipientCount.toString())
                    .replace("%count", (conversation.recipientCount - 1).toString())
                    .replace("%recipient",
                            conversation.fistMessage?.recipients?.getOrNull(0).toString())

            return text
        }

    }

    // Creates new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_conversation, viewGroup, false)
        return ViewHolder(view, onClick)
    }

    // Replaces the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Bind data to ViewHolder
        viewHolder.bind(conversations[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = conversations.size
}