package de.koenidv.sph.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigatorExtras
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.networking.NetworkManager


class HomeFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val prefs = requireContext().getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)

        // Only for demonstration

        val surpriseButton = view.findViewById<Button>(R.id.surpriseButton)
        surpriseButton.setOnClickListener {
            NetworkManager().loadAndSavePosts { Toast.makeText(SphPlanner.applicationContext(), "Heute schon, Kartoffel", Toast.LENGTH_SHORT).show() }
        }

        val timetable = view.findViewById<FragmentContainerView>(R.id.timetableFragment)

        view.findViewById<LinearLayout>(R.id.timetableLayout).setOnClickListener {
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                    .navigate(R.id.timetableFromHomeAction, null, null, FragmentNavigatorExtras(timetable to "timetable"))
        }

        return view

    }
}