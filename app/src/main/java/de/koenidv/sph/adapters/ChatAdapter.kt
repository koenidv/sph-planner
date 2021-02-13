package de.koenidv.sph.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.objects.Message


//  Created by koenidv on 20.12.2020.
class ChatAdapter(private val messages: List<Message>) :
        RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    // Get own user id
    private val prefs = SphPlanner.applicationContext()
            .getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)
    private val userid = prefs.getString("userid", "")!!

    /**
     * Provides a reference to the type of view
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val outerLayout = view.findViewById<ConstraintLayout>(R.id.messageOuter)
        private val layout = view.findViewById<LinearLayout>(R.id.messageLayout)
        private val content = view.findViewById<TextView>(R.id.messageTextView)


        fun bind(message: Message, ownUID: String) {

            content.text = message.content

            // Set background drawable depending on if the message is outgoing or incoming
            // Also set horizontal bias so messages show up on the correct side
            if (message.idSender == ownUID) {
                layout.setBackgroundResource(R.drawable.message_background_outgoing)
                ConstraintSet().apply {
                    clone(outerLayout)
                    setHorizontalBias(layout.id, 1f)
                }.applyTo(outerLayout)
            } else {
                layout.setBackgroundResource(R.drawable.message_background_incoming)
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
                .inflate(R.layout.item_message, viewGroup, false)
        return ViewHolder(view)
    }

    // Replaces the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Bind data to ViewHolder
        viewHolder.bind(messages[position], userid)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = messages.size

}