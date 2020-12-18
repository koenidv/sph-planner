package de.koenidv.sph.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import de.koenidv.sph.R


class HomeFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val prefs = requireContext().getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)

        // Only for demonstration

        val loginButton = view.findViewById<Button>(R.id.signinButton)
        loginButton.setOnClickListener {
            Toast.makeText(requireContext(), "Heute nicht, Kartoffel", Toast.LENGTH_SHORT).show()
        }

        loginButton.setOnLongClickListener {
            when (prefs.getInt("themeRes", R.style.Theme_SPH_Electric)) {
                R.style.Theme_SPH_Electric -> prefs.edit().putInt("themeRes", R.style.Theme_SPH_Autumn).apply()
                R.style.Theme_SPH_Autumn -> prefs.edit().putInt("themeRes", R.style.Theme_SPH_Summer).apply()
                R.style.Theme_SPH_Summer -> prefs.edit().putInt("themeRes", R.style.Theme_SPH_Monochrome).apply()
                R.style.Theme_SPH_Monochrome -> prefs.edit().putInt("themeRes", R.style.Theme_SPH_Electric).apply()
                else -> prefs.edit().putInt("themeRes", R.style.Theme_SPH_Electric).apply()
            }
            requireActivity().recreate()
            true
        }

        return view

    }
}