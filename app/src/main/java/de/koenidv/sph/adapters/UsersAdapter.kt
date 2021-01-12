package de.koenidv.sph.adapters

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.objects.User
import de.koenidv.sph.parsing.Utility

//  Created by koenidv on 09.01.2021.
class UsersAdapter(private val users: List<User>,
                   private val activity: Activity) :
        RecyclerView.Adapter<UsersAdapter.ViewHolder>() {

    private val themeColor = SphPlanner.applicationContext()
            .getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)
            .getInt("themeColor", 0)

    /**
     * Provides a reference to the type of view
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val layout = view.findViewById<ConstraintLayout>(R.id.userLayout)
        private val name = view.findViewById<TextView>(R.id.nameTextView)

        fun bind(user: User, themeColor: Int) {

            // Set user name
            name.text = SphPlanner.applicationContext().getString(R.string.users_name_template)
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
        return ViewHolder(view)
    }

    // Replaces the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Bind data to ViewHolder
        viewHolder.bind(users[position], themeColor)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = users.size
}