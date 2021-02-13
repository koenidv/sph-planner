package de.koenidv.sph.ui

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.reddit.indicatorfastscroll.FastScrollItemIndicator
import com.reddit.indicatorfastscroll.FastScrollerThumbView
import com.reddit.indicatorfastscroll.FastScrollerView
import de.koenidv.sph.R
import de.koenidv.sph.adapters.UsersAdapter
import de.koenidv.sph.database.UsersDb
import java.util.*


// Created by koenidv on 09.01.2021.
class UsersFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_users, container, false)

        // Disable swipe to refresh to avoid conflicts with fastscroll
        requireActivity().findViewById<SwipeRefreshLayout>(R.id.swipeRefresh).isEnabled = false

        // Set up adapter to show all users

        val usersRecycler = view.findViewById<RecyclerView>(R.id.usersRecycler)
        val users = UsersDb.all()
        usersRecycler.adapter = UsersAdapter(users, requireActivity())

        // Set up fast scroll
        val fastScroller = view.findViewById<FastScrollerView>(R.id.fastscroller)
        fastScroller.setupWithRecyclerView(
                usersRecycler,
                { position ->
                    val item = users[position] // Get your model object
                    // or fetch the section at [position] from your database
                    if (item.isPinned) {
                        // Show pin icon for pinned users
                        FastScrollItemIndicator.Icon(R.drawable.ic_pin) // Return an icon indicator
                    } else {
                        val firstChar = item.lastname?.substring(0, 1)?.toUpperCase(Locale.ROOT)
                        // Only show indicator if it's not some weird numbered user
                        if (firstChar != null && !TextUtils.isDigitsOnly(firstChar)) {
                            FastScrollItemIndicator.Text(firstChar) // Return a text indicator
                        } else null
                    }
                }
        )
        view.findViewById<FastScrollerThumbView>(R.id.fastscroller_thumb)
                .setupWithFastScroller(fastScroller)

        return view
    }

    override fun onStop() {
        // Re-enable swipe to refresh
        requireActivity().findViewById<SwipeRefreshLayout>(R.id.swipeRefresh).isEnabled = true
        super.onStop()
    }
}