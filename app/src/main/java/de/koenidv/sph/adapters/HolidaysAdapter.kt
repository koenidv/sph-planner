package de.koenidv.sph.adapters

import android.annotation.SuppressLint
import android.os.Build
import android.system.Os.bind
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.objects.Holiday
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter


//  Created by LbTobi on 10.02.2021
class HolidaysAdapter(private val holidays: List<Holiday>) :
    RecyclerView.Adapter<HolidaysAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val name = view.findViewById<TextView>(R.id.holidayDateTextView)
    private val date = view.findViewById<TextView>(R.id.holidayDateTextView)
    private val remaining = view.findViewById<TextView>(R.id.holidayRemainingTextView)
    lateinit var currentHoliday: Holiday


    fun bind(holiday: Holiday) {
        currentHoliday = holiday

        //place data in current holiday item

        //name
        val currentName = currentHoliday.name

        name.text = when (currentName) {
            "osterferien" -> applicationContext().getString(R.string.holidays_spring)
            "sommerferien" -> applicationContext().getString(R.string.holidays_summer)
            "herbstferien" -> applicationContext().getString(R.string.holidays_autumn)
            "weihnachtsferien" -> applicationContext().getString(R.string.holidays_winter)
            else -> currentName
        }

        //date
        val formatter = SimpleDateFormat.getDateInstance()
        val startDate = formatter.format(currentHoliday.start)


       //date.text = startDate




        // TODO: 10/02/2021 implement date stuff



    }

}

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_holiday, viewGroup,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.bind(holidays[position])
    }

    override fun getItemCount(): Int {
        return holidays.size
    }
}