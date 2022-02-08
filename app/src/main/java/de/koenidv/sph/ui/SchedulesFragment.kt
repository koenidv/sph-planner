package de.koenidv.sph.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.adapters.SchedulesAdapter
import de.koenidv.sph.database.SchedulesDb
import de.koenidv.sph.objects.Schedule
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class SchedulesFragment : Fragment() {

    private var schedDate = Date(0)

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_schedules, container, false)

        // Setup adapter
        val schedulesRecycler = view.findViewById<RecyclerView>(R.id.schedulesRecycler)

        val dateStr = arguments?.getString("startS") ?: "01.01.1970"
        //val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY)
        //val date = LocalDate.parse(dateStr, formatter)
        schedDate = SimpleDateFormat("dd.MM.yyyy").parse(dateStr)!!//31.01.2022 - Month is already correct 31.00.2022

        val allSchedules: List<Schedule>
        if(schedDate.year == Date(0).year) {
            allSchedules = SchedulesDb.getAll().toMutableList()
        }
        else {
            allSchedules = SchedulesDb.getSchedWithStartDate(schedDate).toMutableList()
            (activity as AppCompatActivity).supportActionBar?.title = SphPlanner.appContext().getString(R.string.Tagestermine)
        }

        val layoutManager = schedulesRecycler.layoutManager as LinearLayoutManager

        val filterTime = view.findViewById<Chip>(R.id.sfChipTime)
        val filterExam = view.findViewById<Chip>(R.id.sfChipExam)

        // Setup filters
        //val filterGroup = view.findViewById<ChipGroup>(R.id.schedulesFilter)
        filterTime.setOnClickListener {
            // Responds to chip click

            filterExam.text = SphPlanner.appContext().getString(R.string.emoji_check)

            if(filterTime.isChecked) {//Only present & future
                //change the look
                filterTime.text = SphPlanner.appContext().getString(R.string.emoji_check)
                filterTime.chipIcon = SphPlanner.appContext().getDrawable(R.drawable.ic_schtimefilterno)

                //delete all old entries
                val tmpList = mutableListOf<Schedule>()
                for (entry in allSchedules) if (entry.strt >= Date()) tmpList.add(entry)
                //Show list starting with first entry
                layoutManager.scrollToPosition(0)
                //Go
                schedulesRecycler.adapter = SchedulesAdapter(tmpList, filterTime.isChecked, filterExam.isChecked)
            }
            else {//past, present and future
                //change the look
                filterTime.text = SphPlanner.appContext().getString(R.string.emoji_cross_bw)
                filterTime.chipIcon = SphPlanner.appContext().getDrawable(R.drawable.ic_schtimefilter)

                //Show list starting with first present entry -1
                var i = 0
                for (entry in allSchedules) if (entry.strt < Date()) i++
                if (i>0) layoutManager.scrollToPosition(i-1) else layoutManager.scrollToPosition(i)

                //Go with all entries
                schedulesRecycler.adapter = SchedulesAdapter(allSchedules, filterTime.isChecked, filterExam.isChecked)
            }
        }

        /*
        filterTime.setOnCloseIconClickListener {
            // Responds to chip's close icon click if one is present
        }

        filterTime.setOnCheckedChangeListener { chip, isChecked ->
            // Responds to chip checked/unchecked
        }
        */

        /*
        val checkedChipIds: List<Int> = filterGroup.checkedChipIds// Returns a list of the selected chips' IDs, if any
        for (entry in checkedChipIds) {
            val chip = filterGroup.findViewById<Button>(entry)
        }
        */

        //setup exam entries only or not
        filterExam.setOnClickListener {
            // Responds to chip click

            filterTime.text = SphPlanner.appContext().getString(R.string.emoji_cross_bw)
            filterTime.chipIcon = SphPlanner.appContext().getDrawable(R.drawable.ic_schtimefilter)

            if(filterExam.isChecked) {//Only exams
                //change the look
                filterExam.text = SphPlanner.appContext().getString(R.string.emoji_cross_bw)

                //delete all old entries
                val tmpList = mutableListOf<Schedule>()
                for (entry in allSchedules) if (entry.ctgr == "Prüfungen") tmpList.add(entry) //Arbeit, Lernkontrolle
                //Show list starting with first entry
                layoutManager.scrollToPosition(0)
                //Go
                schedulesRecycler.adapter = SchedulesAdapter(tmpList, filterTime.isChecked, filterExam.isChecked)
            }
            else {//all
                //change the look
                filterExam.text = SphPlanner.appContext().getString(R.string.emoji_check)

                //Show list starting with first present entry -1
                var i = 0
                for (entry in allSchedules) if (entry.strt < Date()) i++
                if (i>0) layoutManager.scrollToPosition(i-1) else layoutManager.scrollToPosition(i)

                //Go with all entries
                schedulesRecycler.adapter = SchedulesAdapter(allSchedules, filterTime.isChecked, filterExam.isChecked)
            }
        }

        // Setup share/ print
        val filterShare = view.findViewById<Chip>(R.id.sfChipShare)
        filterShare.setOnClickListener {
            // Responds to chip click

                //generating bmp is commented because of PDF is used
                //val bmp = Bitmap.createBitmap (view.width, view.height, Bitmap.Config.ARGB_8888)
                //val c = Canvas(bmp)
                //view.draw(c)

            // create a new document
            val document = PdfDocument()
            // create a page description - Sharing current view only, for more (Complete list) we have to define an own view
            val pageInfo = PdfDocument.PageInfo.Builder(view.width, view.height, 1).create()
            // start a page
            val page = document.startPage(pageInfo)
            // draw something on the page
            view.draw(page.canvas)
                //view.draw(c)
            // finish the page
            document.finishPage(page)
            // add more pages
            //. . .
            // fix naming, every sharing is replacing this file, so only one file exists
            val fileName = "shareSchedDisplay.pdf"//${System.currentTimeMillis()}
            val file = File(context?.filesDir, fileName) //internal storage - NO permission needed
            // write the document content
            document.writeTo(file.outputStream())
            // close the document
            document.close()

                /* printing bmp - running and OK, but using PDF
                val prntHlp = activity?.let { it1 -> PrintHelper(it1) }
                prntHlp?.scaleMode = PrintHelper.SCALE_MODE_FIT
                prntHlp?.printBitmap("nameimscheduler.png", bmp)
                */

            val uri = FileProvider.getUriForFile(SphPlanner.appContext(), SphPlanner.appContext().packageName + ".provider", file)
            val sendIntent = activity?.let { it1 ->
                ShareCompat.IntentBuilder.from(it1)
                    .setStream(uri) // uri from FileProvider
                    .setType("application/pdf")
                    .intent
                    .setAction(Intent.ACTION_SEND)
                    .setDataAndType(uri, "application/pdf")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val shareIntent = Intent.createChooser(sendIntent, "Share")
            shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            try {
                startActivity(shareIntent)
            } catch (ane: ActivityNotFoundException) {

            }
        }//end of share button coding

        var i = 0
        for (entry in allSchedules) if (entry.strt < Date()) i++
        if (i>0) layoutManager.scrollToPosition(i-1) else layoutManager.scrollToPosition(i)
        schedulesRecycler.adapter = SchedulesAdapter(allSchedules, filterTime.isChecked, filterExam.isChecked)

        return view
    }

    override fun onDestroy() {
        super.onDestroy()

        val cmpStr1 = SphPlanner.appContext().getString(R.string.title_overview)            //Home Location
        val cmpStr2 = (activity as AppCompatActivity).supportActionBar?.title.toString()    //Target location
        if(cmpStr1 == cmpStr2) {//we go back to the overview; Only in this case next lines are required
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment,
                    HomeFragment().also {
                        it.arguments = bundleOf()
                    })
                .setReorderingAllowed(true) //Optimizing state changes for better transitions
                .commit()
        }
    }

}