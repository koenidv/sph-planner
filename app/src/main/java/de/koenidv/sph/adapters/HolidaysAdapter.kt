package de.koenidv.sph.adapters


import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.appContext
import de.koenidv.sph.objects.Holiday
import de.koenidv.sph.parsing.Utility
import java.text.SimpleDateFormat
import java.util.*


//  Created by LbTobi on 10.02.2021
class HolidaysAdapter(private val holidays: List<Holiday>) :
    RecyclerView.Adapter<HolidaysAdapter.ViewHolder>() {

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
            "osterferien" -> appContext().getString(R.string.holidays_spring) + " $year"
            "sommerferien" -> appContext().getString(R.string.holidays_summer) + " $year"
            "herbstferien" -> appContext().getString(R.string.holidays_autumn) + " $year"
            "weihnachtsferien" -> appContext().getString(R.string.holidays_winter) + " $year"
            else -> currentName
        }

        //date stuff
        val formatter = SimpleDateFormat(appContext().getString(R.string.holidays_date_template), Locale.getDefault())
        val formatter2 = SimpleDateFormat(appContext().getString(R.string.holidays_date_template2), Locale.getDefault())
        val startDate = formatter.format(currentHoliday.start)
        val endDate = formatter2.format(currentHoliday.end)
        val currentDate = Date().time
        val remainingTime = currentHoliday.start.time - currentDate
        val remainingDays = remainingTime / 86400000

        date.text = appContext().getString(R.string.holidays_date)
                .replace("%s", startDate)
                .replace("%e", endDate)

        //remaining
        if (remainingDays >= 14) {
            remaining.text = appContext().getString(R.string.holidays_remaining_weeks)
                    .replace("%w", (remainingDays / 7).toString())
                    .replace("%d", (remainingDays - (remainingDays / 7) * 7).toString())
        } else {
            remaining.text = appContext().getString(R.string.holidays_remaining_days, remainingDays)
        }
        // TODO: 13/02/2021 make use of plurals/singulars

        // Tint background with holiday color at 15% for next 4 Holidays
        if (position < 4) {
            val color = when (currentName) {
                "osterferien" -> appContext().getColor(R.color.holiday_color_spring)
                "sommerferien" -> appContext().getColor(R.color.holiday_color_summer)
                "herbstferien" -> appContext().getColor(R.color.holiday_color_autumn)
                "weihnachtsferien" -> appContext().getColor(R.color.holiday_color_winter)
                else -> Color.TRANSPARENT
            }
            Utility.tintBackground(layout, color, 0x32000000)
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