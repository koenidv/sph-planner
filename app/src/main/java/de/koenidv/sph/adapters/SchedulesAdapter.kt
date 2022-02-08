package de.koenidv.sph.adapters


import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.date.dayOfMonth
import com.afollestad.date.month
import com.afollestad.date.year
import de.koenidv.sph.SphPlanner.Companion.appContext
import de.koenidv.sph.objects.Schedule
import java.util.*





//  Extended by StKl JAN-2022
class SchedulesAdapter(
                            private val schedules: List<Schedule>,
                            private val filterTime: Boolean,
                            private val filterExam: Boolean
                            ) :
    RecyclerView.Adapter<SchedulesAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        //private val layout = view.findViewById<ConstraintLayout>(R.id.scheduleLayout)
        private val nameSch = view.findViewById<TextView>(de.koenidv.sph.R.id.scheduleNameTextView)
        private val dateText = view.findViewById<TextView>(de.koenidv.sph.R.id.scheduleDateTextView)
        private val remaining = view.findViewById<TextView>(de.koenidv.sph.R.id.scheduleRemainingTextView)
        private val texttext = view.findViewById<TextView>(de.koenidv.sph.R.id.scheduleText)

        private lateinit var currentSchedule: Schedule


    @SuppressLint("SetTextI18n")
    fun bind(schedule: Schedule, position: Int) {
        currentSchedule = schedule

        if(currentSchedule.crs.isEmpty() || (currentSchedule.crs == "none")) {
            nameSch.text = currentSchedule.nme
            if(currentSchedule.nme != currentSchedule.txt) {
                texttext.text = currentSchedule.txt
            }
            else {
                texttext.text = ""
            }
        }
        else {
            nameSch.text = "${currentSchedule.crs} / ${currentSchedule.txt}"
            texttext.text = ""
        }

        val c = Calendar.getInstance()

        c.time = currentSchedule.strt
        val startDate  = c.dayOfMonth.toString() + "." + (c.month + 1).toString() + "." + c.year.toString()
        //val startClock =

        c.time = currentSchedule.nd
        val endDate  = c.dayOfMonth.toString() + "." + (c.month + 1).toString() + "." + c.year.toString()
        //val endClock =

        val currentDate = Date().time
        val remainingTime = currentSchedule.strt.time - currentDate
        val remainingDays = remainingTime / 86400000

        if (startDate < endDate) {
            dateText.text = appContext().getString(de.koenidv.sph.R.string.holidays_date)
                .replace("%s", startDate)
                .replace("%e", endDate)
        }
        else {
            dateText.text = startDate

            //Assumption: Exams only on 1 day
            if(currentSchedule.hr != "") {
                val hArr = currentSchedule.hr.split("#").toMutableList()
                //var spprtStr = dateText.text
                var spprtStr = ""
                for (h in hArr) spprtStr = "$spprtStr$h, "
                spprtStr = spprtStr.substring(0, spprtStr.length-4)//delete last ", "
                val spprtStr2 = "" + appContext().getString(de.koenidv.sph.R.string.schedule_hour)
                    .replace("%l", spprtStr)
                var spprtStr3 = dateText.text
                spprtStr3 = "$spprtStr3 - $spprtStr2"
                dateText.text = spprtStr3
            }
            if (currentSchedule.drtn > 0) {
                var spprtStr = dateText.text
                spprtStr = "$spprtStr - " + appContext().getString(de.koenidv.sph.R.string.schedule_duration)
                    .replace("%m", currentSchedule.drtn.toString())
                c.time = currentSchedule.strt
                if (c.get(Calendar.HOUR_OF_DAY) < 10) spprtStr = "${spprtStr}0"
                spprtStr = "$spprtStr - ${c.get(Calendar.HOUR_OF_DAY)}:${c.get(Calendar.MINUTE)}"
                if (c.get(Calendar.MINUTE) == 0) spprtStr = "${spprtStr}0"
                spprtStr = "${spprtStr}-"
                c.add(Calendar.MINUTE, currentSchedule.drtn)
                if (c.get(Calendar.HOUR_OF_DAY) < 10) spprtStr = "${spprtStr}0"
                spprtStr = "$spprtStr${c.get(Calendar.HOUR_OF_DAY)}:${c.get(Calendar.MINUTE)}"
                if (c.get(Calendar.MINUTE) == 0) spprtStr = "${spprtStr}0"
                dateText.text = spprtStr
            }
        }

        //remaining
        if (remainingDays >= 14) {
            remaining.text = appContext().getString(de.koenidv.sph.R.string.holidays_remaining_weeks)
                    .replace("%w", (remainingDays / 7).toString())
                    .replace("%d", (remainingDays - (remainingDays / 7) * 7).toString())
        } else if (remainingDays >= 0) {
            remaining.text = appContext().getString(de.koenidv.sph.R.string.holidays_remaining_days, remainingDays)
        } else {
            remaining.text = "✔"
        }
        // TODO: 13/02/2021 make use of plurals/singulars

        /*
        // Tint background with holiday color at 15%
        val color = when (currentName) {
            "osterferien" -> appContext().getColor(R.color.holiday_color_spring)
            "sommerferien" -> appContext().getColor(R.color.holiday_color_summer)
            "herbstferien" -> appContext().getColor(R.color.holiday_color_autumn)
            "weihnachtsferien" -> appContext().getColor(R.color.holiday_color_winter)
            else -> Color.TRANSPARENT //Bank holidays and unknown holidays
        }
        if (color == Color.TRANSPARENT) {
            layout.background.clearColorFilter()
        }
        else {
            Utility.tintBackground(layout, color, 0x32000000)
        }
        */
    }

}

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(de.koenidv.sph.R.layout.item_schedule, viewGroup,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.bind(schedules[position], position)
    }

    override fun getItemCount(): Int {
        return schedules.size
    }
}