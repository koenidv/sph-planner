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
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.objects.User
import de.koenidv.sph.parsing.Utility
import de.koenidv.sph.ui.NewConversationSheet

//  Created by koenidv on 09.01.2021.
class UsersAdapter(private val users: List<User>,
                   private val activity: FragmentActivity) :
        RecyclerView.Adapter<UsersAdapter.ViewHolder>() {

    private val prefs = SphPlanner.applicationContext()
            .getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)
    private val themeColor = prefs.getInt("themeColor", 0)

    /**
     * Send an email to this user on click
     */
    val onclick: (User) -> Unit = {
        // Show a bottom sheet to start a new conversation, but also show an option to send an email
        NewConversationSheet(it) { subject, recipients ->
            Navigation.findNavController(activity, R.id.nav_host_fragment)
                    .navigate(R.id.newChatAction, bundleOf(
                            "subject" to subject,
                            "recipients" to recipients
                    ))
        }.show(activity.supportFragmentManager, "newconversation")
    }

    /**
     * Provides a reference to the type of view
     * (custom ConversationViewHolder).
     */
    class ViewHolder(view: View, onclick: (User) -> Unit) : RecyclerView.ViewHolder(view) {
        private val layout = view.findViewById<ConstraintLayout>(R.id.userLayout)
        private val name = view.findViewById<TextView>(R.id.nameTextView)
        lateinit var currentUser: User

        init {
            view.setOnClickListener {
                onclick(currentUser)
            }
        }

        fun bind(user: User, themeColor: Int) {
            currentUser = user

            // Set user name
            name.text = SphPlanner.applicationContext().getString(R.string.users_name_template_last)
                    .replace("%firstname", user.firstname.toString())
                    .replace("%lastname", user.lastname.toString())

            // Tint background with theme color at 15% if task is pinned
            if (user.isPinned)
                Utility.tintBackground(layout, themeColor, 0x26000000)
            else
                layout.background.clearColorFilter()

        }

    }

    // Creates new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_user, viewGroup, false)
        return ViewHolder(view, onclick)
    }

    // Replaces the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Bind data to ConversationViewHolder
        viewHolder.bind(users[position], themeColor)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = users.size
}