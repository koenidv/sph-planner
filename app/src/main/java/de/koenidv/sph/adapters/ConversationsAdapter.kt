package de.koenidv.sph.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import de.koenidv.sph.database.ConversationsDb
import de.koenidv.sph.database.UsersDb
import de.koenidv.sph.parsing.Utility
import de.koenidv.sph.ui.ConversationsArchiveSheet
import java.text.SimpleDateFormat
import java.util.*


//  Created by koenidv on 12.02.2021.
class ConversationsAdapter(val conversations: MutableList<ConversationInfo>,
                           private val activity: FragmentActivity,
                           private val archived: Boolean = false,
                           private val compactMode: Boolean = archived,
                           private val onSelectModeChange: ((Boolean) -> Unit)? = null) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class ConversationInfo(
            val id: String,
            val subject: String,
            val partner: String,
            val partnerCount: Int,
            val date: Date,
            val isOwn: Boolean,
            val isUnread: Boolean
    )

    /**
     * Optional callback when any item is clicked
     */
    var clickCallback: (() -> Unit)? = null

    /**
     * Optional callback specified if archive mode is used, called on unarchive button click
     */
    var unarchiveCallback: ((ConversationInfo, Int) -> Unit)? = null

    private val selectedItems = mutableListOf<Int>()
    private var selectMode = false
    private var archiveButtonVisible = ConversationsDb().archivedExists() && conversations.isNotEmpty()


    private fun selectItem(position: Int) {
        if (!compactMode && onSelectModeChange != null) {
            if (selectedItems.contains(position)) {
                // Items was already selected, remove selection
                selectedItems.remove(position)
                // Disable select mode if this was the last item
                if (selectedItems.isEmpty()) {
                    selectMode = false
                    onSelectModeChange.invoke(false)
                }
            } else {
                selectedItems.add(position)
                selectMode = true
                onSelectModeChange.invoke(true)
            }
            notifyItemChanged(position)
        }
    }

    // If in select mode, select this conversation, else navigate to the corresponding fragment
    private val onClick: (ConversationInfo, Int) -> Unit = { conversation, position ->
        if (selectMode) {
            selectItem(position)
        } else {
            clickCallback?.invoke()
            Navigation.findNavController(activity, R.id.nav_host_fragment)
                    .navigate(
                            if (compactMode) R.id.chatAction
                            else R.id.chatFromConversationsAction,
                            bundleOf("conversationId" to conversation.id))
        }
    }

    // Select this item and enable select mode
    private val onLongClick: (Int) -> Unit = {
        selectItem(it)
    }

    // Display the archive bottom sheet
    private val archiveButtonClick: (View) -> Unit = {
        ConversationsArchiveSheet {
            // Insert the now unarchived item at the top
            conversations.add(0, it)
            notifyItemInserted(0)
        }.show(activity.supportFragmentManager, "conv-archive")
    }

    // Get theme color
    private val prefs = applicationContext()
            .getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
    private val themeColor = prefs.getInt("themeColor", 0)

    /**
     * Provides a reference to the type of view
     * (custom ConversationViewHolder).
     */
    class ConversationViewHolder(view: View,
                                 onClick: (ConversationInfo, Int) -> Unit,
                                 onLongClick: (Int) -> Unit,
                                 unarchiveCallback: ((ConversationInfo, Int) -> Unit)?) :
            RecyclerView.ViewHolder(view) {
        private val layout = view.findViewById<ConstraintLayout>(R.id.conversationLayout)
        private val subject = view.findViewById<TextView>(R.id.subjectTextView)
        private val participants = view.findViewById<TextView>(R.id.participantsTextView)
        private val date = view.findViewById<TextView>(R.id.dateTextView)
        private val unread = view.findViewById<TextView>(R.id.unreadTextView)
        private val unarchiveButton = view.findViewById<ImageButton>(R.id.unarchiveButton)

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

            unarchiveButton?.setOnClickListener {
                currentConversation?.let {
                    unarchiveCallback?.invoke(it, adapterPosition)
                }
            }

        }


        fun bind(conversation: ConversationInfo, isSelected: Boolean, themeColor: Int, compactMode: Boolean) {
            currentConversation = conversation

            subject.text = conversation.subject
            participants.text = getRecipientText(conversation)
            date.text = getRelativeDate(conversation.date)

            // If item is selected, adjust background color
            if (!compactMode) {
                if (isSelected) {
                    // Item now selected
                    Utility.tintBackground(layout, themeColor, 0x52000000)
                } else {
                    // Item now unselected
                    layout.background.clearColorFilter()
                }
            }

            // If item is unread, show unread marker
            unread.visibility =
                    if (conversation.isUnread) View.VISIBLE
                    else View.GONE

        }

        private fun getRelativeDate(date: Date): String {
            val now = Date()

            return if (now.date == date.date &&
                    now.time - date.time < 24 * 3600000) {
                // If now is the same day in month and maximum of 24hours ago
                SimpleDateFormat(applicationContext().getString(R.string.messages_dateformat_today),
                        Locale.getDefault())
                        .format(date)
            } else if (now.time - date.time < 48 * 3600000) {
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

    class ArchiveViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val archiveButton = view.findViewById<MaterialButton>(R.id.archiveButton)

        fun bind(visible: Boolean, onClick: (View) -> Unit) {
            archiveButton.apply {
                setOnClickListener(onClick)
                visibility = if (visible) View.VISIBLE else View.GONE
            }
        }
    }

    override fun getItemViewType(position: Int): Int =
            if (!compactMode && position + 1 == itemCount) VIEW_ARCHIVE
            else VIEW_CONVERSATION

    // Creates new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // Create a new view, which defines the UI of the list item
        return if (viewType == VIEW_CONVERSATION) {
            val view = LayoutInflater.from(viewGroup.context)
                    .inflate(
                            when {
                                archived -> R.layout.item_conversation_archived
                                compactMode -> R.layout.item_conversation_compact
                                else -> R.layout.item_conversation
                            },
                            viewGroup, false)
            ConversationViewHolder(view, onClick, onLongClick, unarchiveCallback)
        } else {
            ArchiveViewHolder(
                    LayoutInflater.from(viewGroup.context).inflate(
                            R.layout.item_conversation_see_archived, viewGroup, false)
            )
        }
    }

    // Replaces the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        // Bind data to ConversationViewHolder or ArchiveViewHolder
        if (viewHolder is ConversationViewHolder) {
            viewHolder.bind(conversations[position], selectedItems.contains(position), themeColor, compactMode)
        } else if (viewHolder is ArchiveViewHolder) {
            viewHolder.bind(archiveButtonVisible, archiveButtonClick)
        }
    }

    /**
     * Whenever an item was inserted (possibly unarchived) or removed (propably archived),
     * we need to check if there are any archived conversations and show/hide
     * the see archive button accordingly
     */

    // If the current visibility of the archived button does not match the
    // existance of archived conversations, update it
    fun checkForArchived() {
        val anyArchived = ConversationsDb().archivedExists() && conversations.isNotEmpty()
        if (anyArchived != archiveButtonVisible) {
            archiveButtonVisible = anyArchived
            // Archived button is at conversations#size + 1
            notifyItemChanged(conversations.size + 1)
        }
    }

    // Check for archived conversations when the dataset changed
    private val archivedDatasetObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            checkForArchived()
            super.onItemRangeInserted(positionStart, itemCount)
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            checkForArchived()
            super.onItemRangeRemoved(positionStart, itemCount)
        }
    }

    // Attach archivedDatasetObserver
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        // Only if the button should be shown at all
        if (!compactMode) registerAdapterDataObserver(archivedDatasetObserver)
        super.onAttachedToRecyclerView(recyclerView)
    }

    // Detach the archived observer
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        if (!compactMode) unregisterAdapterDataObserver(archivedDatasetObserver)
        super.onDetachedFromRecyclerView(recyclerView)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = conversations.size + if (!compactMode) 1 else 0

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
        onSelectModeChange?.invoke(false)
    }

    private companion object {
        const val VIEW_CONVERSATION = 0
        const val VIEW_ARCHIVE = 1
    }
}