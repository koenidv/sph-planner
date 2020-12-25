package de.koenidv.sph.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.google.android.material.switchmaterial.SwitchMaterial
import de.koenidv.sph.R

class TimetableFragment : Fragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(android.R.transition.move)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_timetable, container, false)

        // Show timetable
        val ft = parentFragmentManager.beginTransaction()
        val timetableFragment = TimetableViewFragment()
        timetableFragment.arguments = bundleOf("expanded" to true, "viewAll" to false, "openOnClick" to false)
        ft.replace(R.id.timetableFragment, timetableFragment).commit()

        val filterSwitch = view.findViewById<SwitchMaterial>(R.id.timetableFilterSwitch)
        filterSwitch.setOnCheckedChangeListener { _, isChecked -> timetableFragment.setViewAll(!isChecked) }

        return view
    }
}