package de.koenidv.sph.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.adapters.ChangesAdapter
import de.koenidv.sph.database.ChangesDb


// Created by koenidv on 18.12.2020.
class ChangesFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_changes, container, false)

        // Set open in browser url
        SphPlanner.openInBrowserUrl = getString(R.string.url_changes)

        var favorites = false

        val favoritesSwitch = view.findViewById<SwitchMaterial>(R.id.favoritesSwitch)
        val changesRecycler = view.findViewById<RecyclerView>(R.id.changesRecycler)

        // Get passed argument
        if (arguments?.getBoolean("favorites") != null) {
            favorites = arguments?.getBoolean("favorites")!!
        }

        // Set favorites switch
        favoritesSwitch.isChecked = favorites

        // Get changes
        val changes = if (favorites) ChangesDb.instance!!.getFavorites().toMutableList()
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
}