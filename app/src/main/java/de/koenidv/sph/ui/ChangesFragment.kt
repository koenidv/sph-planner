package de.koenidv.sph.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.adapters.ChangesAdapter
import de.koenidv.sph.database.ChangesDb
import de.koenidv.sph.database.HolidaysDb
import de.koenidv.sph.objects.Change
import java.util.*


// Created by koenidv on 18.12.2020.
class ChangesFragment : Fragment() {

    var favorites = false
    var displayed = true
    lateinit var changes: MutableList<Change>
    lateinit var noDataText: TextView
    lateinit var changesRecycler: RecyclerView

    // Refresh whenever the broadcast "uichange" is received
    private val uichangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Only do something if changes were updated
            if (intent.getStringExtra("content") == "changes") {
                if (displayed && ::changes.isInitialized) {
                    // Update with new changes
                    setDataset()
                    // Notify recyclerview
                    changesRecycler.adapter?.notifyDataSetChanged()
                } else {
                    // If the recycler was never set up, recreate the fragment
                    parentFragmentManager.beginTransaction()
                            .replace(R.id.nav_host_fragment,
                                    ChangesFragment().also {
                                        it.arguments = bundleOf("favorites" to favorites)
                                    })
                            .commit()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Register to receive messages.
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(uichangeReceiver,
                IntentFilter("uichange"))
    }

    fun setDataset() {
        // Get changes
        if (::changes.isInitialized) {
            changes.clear()
        } else {
            changes = mutableListOf()
        }
        // Add favorite or all changes to the list
        changes.addAll(if (favorites) ChangesDb.instance!!.getFavorites()
        else ChangesDb.instance!!.getAllCurrent())

        // Add the next holiday
        val nextHoliday = HolidaysDb().next
        if (nextHoliday != null) {
            // Get the number of days left until this holiday
            val daysLeft = (nextHoliday.start.time - Date().time) / (86400 * 1000)
            // Only display if the next holiday is within 6 weeks / 42 days
            if (daysLeft <= 42) {
                changes.add(Change(
                        date = nextHoliday.start,
                        lessons = emptyList(),
                        type = Change.TYPE_HOLIDAYS,
                        className = nextHoliday.name.capitalize(Locale.getDefault()) +
                                " " + nextHoliday.year,
                        description = getString(R.string.changes_holidays_description, daysLeft)
                ))
            }
        }

        // If no changes are to be displayed, show a text explaining that
        noDataText.visibility = if (changes.isEmpty()) View.VISIBLE
        else View.GONE
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_changes, container, false)

        noDataText = view.findViewById<TextView>(R.id.noDataTextView)
        changesRecycler = view.findViewById(R.id.changesRecycler)
        val favoritesSwitch = view.findViewById<SwitchMaterial>(R.id.favoritesSwitch)

        // Set open in browser url
        SphPlanner.openInBrowserUrl = getString(R.string.url_changes)

        // Get passed argument
        if (arguments?.getBoolean("favorites") != null) {
            favorites = arguments?.getBoolean("favorites")!!
        }

        // Set favorites switch
        favoritesSwitch.isChecked = favorites

        // Get changes
        setDataset()

        // Check if there are any changes to be displayed
        // If no changes should be displayed, also hide switch
        if (changes.isEmpty()) {
            favoritesSwitch.visibility = View.GONE
            displayed = false
            return view
        }

        // Set up changes recycler
        val adapter = ChangesAdapter(changes) { courseId: String ->
            // Navigate to course
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                    .navigate(R.id.overviewFromChangesAction, bundleOf("courseId" to courseId))
        }
        changesRecycler.adapter = adapter

        // Favorites switch
        favoritesSwitch.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
            favorites = checked
            // Update data
            setDataset()
            // Notify recyclerview
            // Should do this with notifyItemRemoved/Inserted..
            changesRecycler.adapter?.notifyDataSetChanged()
        }

        return view
    }

    override fun onDestroy() {
        // Unregister broadcast receiver
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(uichangeReceiver)
        super.onDestroy()
    }
}