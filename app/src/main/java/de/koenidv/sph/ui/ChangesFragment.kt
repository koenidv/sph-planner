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
import de.koenidv.sph.objects.Change


// Created by koenidv on 18.12.2020.
class ChangesFragment : Fragment() {

    var favorites = false
    lateinit var changes: MutableList<Change>
    lateinit var changesRecycler: RecyclerView

    // Refresh whenever the broadcast "uichange" is received
    private val uichangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Only do something changes were updated
            if (intent.getStringExtra("content") == "changes") {
                // Get changes
                changes = if (favorites) ChangesDb.instance!!.getFavorites().toMutableList()
                else ChangesDb.instance!!.getAll().toMutableList()
                // Notify recyclerview
                changesRecycler.adapter?.notifyDataSetChanged()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Register to receive messages.
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(uichangeReceiver,
                IntentFilter("uichange"))
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_changes, container, false)

        // Set open in browser url
        SphPlanner.openInBrowserUrl = getString(R.string.url_changes)

        val favoritesSwitch = view.findViewById<SwitchMaterial>(R.id.favoritesSwitch)
        changesRecycler = view.findViewById<RecyclerView>(R.id.changesRecycler)

        // Get passed argument
        if (arguments?.getBoolean("favorites") != null) {
            favorites = arguments?.getBoolean("favorites")!!
        }

        // Set favorites switch
        favoritesSwitch.isChecked = favorites

        // Get changes
        changes = if (favorites) ChangesDb.instance!!.getFavorites().toMutableList()
        else ChangesDb.instance!!.getAll().toMutableList()

        // Set up changes recycler
        val adapter = ChangesAdapter(changes) { couseId: String ->
            // Navigate to course
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                    .navigate(R.id.overviewFromChangesAction, bundleOf("courseId" to couseId))
        }
        changesRecycler.adapter = adapter

        // Favorites switch
        favoritesSwitch.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
            favorites = checked
            // Update data
            changes.clear()
            changes.addAll(if (favorites) ChangesDb.instance!!.getFavorites()
            else ChangesDb.instance!!.getAll())
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