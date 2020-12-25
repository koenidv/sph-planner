package de.koenidv.sph

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.adapters.LessonsAdapter
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.database.TimetableDb

class TimetableFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_timetable, container, false)

        val timetable = TimetableDb.instance!!.get(true)
        val courses = CoursesDb.getInstance().favorites
        val recyclerViewPool = RecyclerView.RecycledViewPool()

        val monday = view.findViewById<RecyclerView>(R.id.mondayRecycler)
        val tuesday = view.findViewById<RecyclerView>(R.id.tuesdayRecycler)
        val wednesday = view.findViewById<RecyclerView>(R.id.wednesdayRecycler)
        val thursday = view.findViewById<RecyclerView>(R.id.thursdayRecycler)
        val friday = view.findViewById<RecyclerView>(R.id.fridayRecycler)

        // Share views for better performance
        monday.setRecycledViewPool(recyclerViewPool)
        tuesday.setRecycledViewPool(recyclerViewPool)
        wednesday.setRecycledViewPool(recyclerViewPool)
        thursday.setRecycledViewPool(recyclerViewPool)
        friday.setRecycledViewPool(recyclerViewPool)

        // Set up lessons adapters
        monday.adapter = LessonsAdapter(timetable[0]) {}
        tuesday.adapter = LessonsAdapter(timetable[1]) {}
        wednesday.adapter = LessonsAdapter(timetable[2]) {}
        thursday.adapter = LessonsAdapter(timetable[3]) {}
        friday.adapter = LessonsAdapter(timetable[4]) {}



        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment TimetableFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                TimetableFragment().apply {
                    arguments = Bundle().apply {

                    }
                }

    }
}