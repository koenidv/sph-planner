package de.koenidv.sph.adapters


import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.objects.Holiday
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import de.koenidv.sph.debugging.Debugger.prefs
import de.koenidv.sph.parsing.Utility
import java.text.SimpleDateFormat
import java.util.*


//  Created by LbTobi on 10.02.2021
class HolidaysAdapter(private val holidays: List<Holiday>) :
    RecyclerView.Adapter<HolidaysAdapter.ViewHolder>() {

    private val themeColor = prefs.getInt("themeColor", 0)

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val layout = view.findViewById<ConstraintLayout>(R.id.holidayLayout)
    private val name = view.findViewById<TextView>(R.id.holidayNameTextView)
    private val date = view.findViewById<TextView>(R.id.holidayDateTextView)
    private val remaining = view.findViewById<TextView>(R.id.holidayRemainingTextView)
    lateinit var currentHoliday: Holiday


    fun bind(holiday: Holiday, position: Int) {
        currentHoliday = holiday

        //place data in current holiday item

        //name
        val currentName = currentHoliday.name
        val year = currentHoliday.year

        name.text = when (currentName) {
            "osterferien" -> applicationContext().getString(R.string.holidays_spring) + " " + year
            "sommerferien" -> applicationContext().getString(R.string.holidays_summer) + " $year"
            "herbstferien" -> applicationContext().getString(R.string.holidays_autumn) + " $year"
            "weihnachtsferien" -> applicationContext().getString(R.string.holidays_winter) + " $year"
            else -> currentName
        }

        //date stuff
        val formatter = SimpleDateFormat.getDateInstance()
        val startDate = formatter.format(currentHoliday.start)
        val endDate = formatter.format(currentHoliday.end)
        val currentDate = Date().time
        val remainingTime = currentHoliday.start.time - currentDate
        val remainingDays = remainingTime / 86400000

        date.text = applicationContext().getString(R.string.holidays_date)
                .replace("%s", startDate)
                .replace("%e", endDate)
        // TODO: 12/02/2021 don't display year in date Textfield

        //remaining
        if (remainingDays >= 7) {
            remaining.text = applicationContext().getString(R.string.holidays_remaining_weeks)
                    .replace("%w", (remainingDays / 7).toString())
                    .replace("%d", (remainingDays - (remainingDays / 7) * 7).toString())
        } else {
            remaining.text = applicationContext().getString(R.string.holidays_remaining_days, remainingDays)
        }

        // Tint background with holiday color at 15% for next 4 Holidays
        if (position < 4) {
            val color = when (currentName) {
                "osterferien" -> applicationContext().getColor(R.color.holiday_color_spring)
                "sommerferien" -> applicationContext().getColor(R.color.holiday_color_summer)
                "herbstferien" -> applicationContext().getColor(R.color.holiday_color_autumn)
                "weihnachtsferien" -> applicationContext().getColor(R.color.holiday_color_winter)
                else -> Color.TRANSPARENT
            }
            Utility.tintBackground(layout, color, 0x50000000)
        } else
            layout.background.clearColorFilter()
    }

}

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_holiday, viewGroup,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.bind(holidays[position], position)
    }

    override fun getItemCount(): Int {
        return holidays.size
    }
}