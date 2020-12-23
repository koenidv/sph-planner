package de.koenidv.sph.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import de.koenidv.sph.R

// Created by koenidv on 18.12.2020.
class MessagesFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_messages, container, false)

        // Just for convenience while developing, open sph's front page using the WebViewFragment
        // But first, use a very bad but simple workaround to set explore tab as active tab

        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                .navigate(R.id.nav_links)
        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                .navigate(R.id.frag_webview, bundleOf("url" to "https://start.schulportal.hessen.de"))

        return view
    }


}