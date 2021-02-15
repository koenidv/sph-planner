package de.koenidv.sph.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import de.koenidv.sph.database.UsersDb
import de.koenidv.sph.parsing.Utility
import java.text.SimpleDateFormat
import java.util.*


//  Created by koenidv on 12.02.2021.
class ConversationsAdapter(private val conversations: List<ConversationInfo>,
                           private val activity: FragmentActivity,
                           private val onSelectModeChange: (Boolean) -> Unit) :
        RecyclerView.Adapter<ConversationsAdapter.ViewHolder>() {

    class ConversationInfo(
            val id: String,
            val subject: String,
            val partner: String,
            val partnerCount: Int,
            val date: Date,
            val isOwn: Boolean,
            val isUnread: Boolean
    )

    private val selectedItems = mutableListOf<Int>()
    private var selectMode = false

    private fun selectItem(position: Int) {
        if (selectedItems.contains(position)) {
            // Items was already selected, remove selection
            selectedItems.remove(position)
            // Disable select mode if this was the last item
            if (selectedItems.isEmpty()) {
                selectMode = false
                onSelectModeChange(false)
            }
        } else {
            selectedItems.add(position)
            selectMode = true
            onSelectModeChange(true)
        }
        notifyItemChanged(position)
    }

    private val onClick: (ConversationInfo, Int) -> Unit = { conversation, position ->
        if (selectMode) {
            selectItem(position)
        } else {
            Navigation.findNavController(activity, R.id.nav_host_fragment)
                    .navigate(R.id.chatFromConversationsAction,
                            bundleOf("conversationId" to conversation.id))
        }
    }
    private val onLongClick: (Int) -> Unit = {
        selectItem(it)
    }

    // Get theme color
    private val prefs = applicationContext()
            .getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)
    private val themeColor = prefs.getInt("themeColor", 0)

    /**
     * Provides a reference to the type of view
     * (custom ViewHolder).
     */
    class ViewHolder(view: View,
                     onClick: (ConversationInfo, Int) -> Unit,
                     onLongClick: (Int) -> Unit) : RecyclerView.ViewHolder(view) {
        private val layout = view.findViewById<ConstraintLayout>(R.id.conversationLayout)
        private val subject = view.findViewById<TextView>(R.id.subjectTextView)
        private val participants = view.findViewById<TextView>(R.id.participantsTextView)
        private val date = view.findViewById<TextView>(R.id.dateTextView)
        private val unread = view.findViewById<TextView>(R.id.unreadTextView)

        private var currentConversation: ConversationInfo? = null

        init {
            layout.setOnClickListener {
                currentConversation?.let {
                    onClick(it, adapterPosition)
                }
            }

            layout.setOnLongClickListener {
                onLongClick(adapterPosition)
                true
            }

        }


        fun bind(conversation: ConversationInfo, isSelected: Boolean, themeColor: Int) {
            currentConversation = conversation

            subject.text = conversation.subject
            participants.text = getRecipientText(conversation)
            date.text = getRelativeDate(conversation.date)

            // If item is selected, adjust background color
            if (isSelected) {
                // Item now selected
                Utility.tintBackground(layout, themeColor, 0x52000000)
            } else {
                // Item now unselected
                layout.background.clearColorFilter()
            }

            // If item is unread, show unread marker
            unread.visibility =
                    if (conversation.isUnread) View.VISIBLE
                    else View.GONE

        }

        private fun getRelativeDate(date: Date): String {
            val now = Date()

            return if (now.date == date.date &&
                    now.time - date.time < 24 * 360000) {
                // If now is the same day in month and maximum of 24hours ago
                SimpleDateFormat(applicationContext().getString(R.string.messages_dateformat_today),
                        Locale.getDefault())
                        .format(date)
            } else if (now.time - date.time < 48 * 360000) {
                SimpleDateFormat(applicationContext().getString(R.string.messages_dateformat_yesterday),
                        Locale.getDefault())
                        .format(date)
            } else {
                // todo proper relative dates
                SimpleDateFormat(applicationContext().getString(R.string.messages_dateformat_other),
                        Locale.getDefault())
                        .format(date)
            }

        }

        private fun getRecipientText(conversation: ConversationInfo): String {

            var text = applicationContext().getString(when {
                conversation.isOwn && conversation.partnerCount == 1 ->
                    // From self and only one recipient
                    R.string.messages_partic_fromself
                conversation.isOwn ->
                    // From self with multiple recipients
                    R.string.messages_partic_fromself_more
                conversation.partnerCount == 0 ->
                    // Not from self, only one recipient
                    R.string.messages_partic_toself
                else ->
                    // Not from self and multiple recipients
                    R.string.messages_partic_toself_more
            })

            // Replace placeholders
            text = text.replace("%partner", UsersDb.getName(conversation.partner))
                    .replace("%count", conversation.partnerCount.toString())

            return text
        }

    }

    // Creates new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_conversation, viewGroup, false)
        return ViewHolder(view, onClick, onLongClick)
    }

    // Replaces the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Bind data to ViewHolder
        viewHolder.bind(conversations[position], selectedItems.contains(position), themeColor)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = conversations.size

    /**
     * Get currently selected items
     */
    fun getSelected() = selectedItems.map { conversations[it] }

    /**
     * Clear list of selected items, will not update ui
     * (Called when items are removed anyway)
     */
    fun clearSelected() {
        selectedItems.clear()
        selectMode = false
        onSelectModeChange(false)
    }
}