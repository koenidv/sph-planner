package de.koenidv.sph.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import de.koenidv.sph.R
import de.koenidv.sph.database.TimetableDb
import de.koenidv.sph.networking.NetworkManager
import de.koenidv.sph.parsing.RawParser


class HomeFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val prefs = requireContext().getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)

        // Only for demonstration

        val loginButton = view.findViewById<Button>(R.id.signinButton)
        loginButton.setOnClickListener {
            //NetworkManager().loadAndSavePosts { Toast.makeText(SphPlanner.applicationContext(), "Heute schon, Kartoffel", Toast.LENGTH_SHORT).show() }
            NetworkManager().loadSiteWithToken("https://start.schulportal.hessen.de/stundenplan.php", onComplete = { success: Int, result: String? ->
                val test = RawParser().parseTimetable(result!!)
                TimetableDb.instance!!.clear()
                TimetableDb.instance!!.save(test)
            })
        }

        return view

    }
}