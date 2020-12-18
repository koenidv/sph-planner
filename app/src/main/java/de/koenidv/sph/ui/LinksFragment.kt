package de.koenidv.sph.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.adapters.LinksAdapter
import de.koenidv.sph.database.TilesDb


class LinksFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_links, container, false)

        // Set up links recycler
        val linksRecycler = view.findViewById<RecyclerView>(R.id.linksRecycler)
        val linksAdapter = LinksAdapter(TilesDb.getInstance().allTiles.sortedBy { it.name }) {
            // Open WebViewFragment with respective url on click
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                    .navigate(R.id.webviewAction, bundleOf("url" to it.location))
        }
        linksRecycler.layoutManager = LinearLayoutManager(requireContext())
        linksRecycler.adapter = linksAdapter

        return view
    }


}