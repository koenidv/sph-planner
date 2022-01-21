package de.koenidv.sph.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.adapters.HolidaysAdapter
import de.koenidv.sph.database.HolidaysDb

class HolidaysFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_holidays, container, false)

        // Setup adapter
        val holidaysRecycler = view.findViewById<RecyclerView>(R.id.holidaysRecycler)
        val futureHolidays = HolidaysDb().future
        //Reihenfolge aendern
        holidaysRecycler.adapter = HolidaysAdapter(futureHolidays)

        return view
    }
}