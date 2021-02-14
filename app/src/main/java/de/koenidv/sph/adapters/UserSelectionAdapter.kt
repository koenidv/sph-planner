package de.koenidv.sph.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.objects.User
import de.koenidv.sph.parsing.Utility

//  Created by koenidv on 14.02.2021.
class UserSelectionAdapter(users: List<User>, private val onSelection: () -> Unit) :
        RecyclerView.Adapter<UserSelectionAdapter.ViewHolder>() {

    data class UserData(
            val user: User,
            var selected: Boolean = false)

    private val userData = users.map { UserData(it) }
    private val displayedUsers = userData.toMutableList()
    private var previousfilter = ""

    private val onSelect: (UserData) -> Unit = {
        onSelection()
        it.selected = !it.selected
        val displayindex = displayedUsers.indexOf(it)
        notifyItemChanged(displayindex)
    }

    /**
     * Provides a reference to the type of view
     * (custom ViewHolder).
     */
    class ViewHolder(view: View, onSelect: (UserData) -> Unit) : RecyclerView.ViewHolder(view) {
        private val name = view.findViewById<TextView>(android.R.id.text1)
        private lateinit var currentUser: UserData

        init {
            view.setOnClickListener {
                onSelect(currentUser)
            }
        }

        fun bind(user: UserData) {
            currentUser = user

            // Set user name
            name.text = SphPlanner.applicationContext().getString(R.string.users_name_template_last)
                    .replace("%firstname", user.user.firstname.toString())
                    .replace("%lastname", user.user.lastname.toString())

            if (user.selected) {
                name.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_done, 0, 0, 0)
                name.compoundDrawablePadding = Utility.dpToPx(8f).toInt()
            } else {
                name.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
            }
        }

    }

    // Creates new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(android.R.layout.simple_dropdown_item_1line, viewGroup, false)
        return ViewHolder(view, onSelect)
    }

    // Replaces the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Bind data to ViewHolder
        viewHolder.bind(displayedUsers[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = displayedUsers.size

    /**
     * Only display items that contain the filter text
     */
    fun filter(filter: String) {
        // If the filter string is longer, items will be removed only
        if (filter.length > previousfilter.length) {
            for (user in userData.withIndex()) {
                val contains = ("${user.value.user.lastname}, ${user.value.user.firstname} " +
                        "(${user.value.user.teacherId}) ${user.value.user.userId}")
                        .contains(filter)

                if (!contains && displayedUsers.contains(user.value)) {
                    val index = displayedUsers.indexOf(user.value)
                    displayedUsers.removeAt(index)
                    notifyItemRemoved(index)
                } else if (contains && !displayedUsers.contains(user.value)) {
                    // Fallback for pasted filter, filter is longer but other items should be shown
                    // Will add them at the end of the list
                    displayedUsers.add(user.value)
                    notifyItemInserted(displayedUsers.size)
                }
            }
        } else {
            // If filter got shorter, just replace items
            // This way, alphabetical order will be preserved without fancy re-sorting
            displayedUsers.clear()
            displayedUsers.addAll(userData.filter {
                ("${it.user.lastname}, ${it.user.firstname} " +
                        "(${it.user.teacherId}) ${it.user.userId}")
                        .contains(filter)
            })
            notifyDataSetChanged()
        }
        previousfilter = filter
    }

    /**
     * Get all selected users
     */
    fun getSelected() = userData.filter { it.selected }.map { it.user }
}