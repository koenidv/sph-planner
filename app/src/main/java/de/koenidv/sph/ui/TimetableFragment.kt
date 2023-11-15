package de.koenidv.sph.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionInflater
import com.google.android.material.switchmaterial.SwitchMaterial
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.adapters.LessonsAdapter
import de.koenidv.sph.adapters.TimebarAdapter
import java.util.*

class TimetableFragment : Fragment() {

    private var expanded: Boolean = true
    private var viewAll: Boolean = false
    private var openOnClick: Boolean = false
    private var withChanges: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.move)
        arguments?.let {
            expanded = it.getBoolean("expanded")
            viewAll = it.getBoolean("viewAll")
            openOnClick = it.getBoolean("openOnClick")
            withChanges = it.getBoolean("withChanges")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_timetable, container, false)

        // Update open in browser url
        SphPlanner.openInBrowserUrl = getString(R.string.url_timetable)

        // Show timetable
        val ft = parentFragmentManager.beginTransaction()
        val timetableFragment = TimetableViewFragment()
        timetableFragment.arguments = bundleOf(
                "expanded" to expanded,
                "viewAll" to viewAll,
                "openOnClick" to openOnClick,
                "withChanges" to withChanges)
        ft.replace(R.id.timetableFragment, timetableFragment).commit()

        val filterSwitch = view.findViewById<SwitchMaterial>(R.id.timetableFilterSwitch)
        filterSwitch.isChecked = !viewAll
        filterSwitch.setOnCheckedChangeListener { _, isChecked -> timetableFragment.setViewAll(!isChecked) }

        return view
    }
}