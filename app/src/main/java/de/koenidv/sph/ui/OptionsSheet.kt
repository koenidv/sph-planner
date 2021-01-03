package de.koenidv.sph.ui

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.switchmaterial.SwitchMaterial
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.database.DatabaseHelper
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

//  Created by koenidv on 16.02.2020.
// Sorry for horrible code - was imported from GMB-Planner
// Might rework this some day

class OptionsSheet internal constructor() : BottomSheetDialogFragment() {
    private var appnameTitle: String? = null
    private var greetingTitle: String? = null
    private var refreshedShort: String? = null
    private var refreshedInfo: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.sheet_options, container, false)
        val prefs = SphPlanner.applicationContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val timeFormatter: DateFormat = SimpleDateFormat(getString(R.string.dateformat_hours), Locale.GERMAN)
        val dateFormatter: DateFormat = SimpleDateFormat(getString(R.string.dateformat_coursesrefreshed), Locale.GERMAN)

        // Append version to app name
        try {
            val pInfo = SphPlanner.applicationContext().packageManager.getPackageInfo(SphPlanner.applicationContext().packageName, 0)
            val version = pInfo.versionName
            appnameTitle = getString(R.string.info_app)
            appnameTitle = appnameTitle!!.replace("%version", version)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        try {
            var firstname = prefs.getString("realname", "")
            firstname = firstname!!.substring(0, firstname.indexOf(" "))
            greetingTitle = getString(R.string.info_greeting).replace("%name", firstname)
        } catch (e: StringIndexOutOfBoundsException) {
            greetingTitle = getString(R.string.app_name)
        }
        refreshedInfo = getString(R.string.info_last_refresh)
                .replace("%refresh", if (prefs.getLong("lastRefresh", 0) == 0L) "..." else timeFormatter.format(prefs.getLong("lastRefresh", 0)))
                .replace("%change", prefs.getString("lastChange", "...")!!)
                .replace("%courses", if (prefs.getLong("lastCourseRefresh", 0) == 0L) "..." else dateFormatter.format(prefs.getLong("lastCourseRefresh", 0)))
        refreshedShort = if (Calendar.getInstance().timeInMillis - prefs.getLong("lastRefresh", 0) < 900000) getString(R.string.last_refreshed_shortly) else if (Calendar.getInstance().timeInMillis - prefs.getLong("lastRefresh", 0) < 3600000) getString(R.string.last_refreshed_hourly) else getString(R.string.last_refreshed_other)
        val titleTextView = view.findViewById<TextView>(R.id.titleTextView)
        val authorTextView = view.findViewById<TextView>(R.id.authorTextView)
        val refreshTextView = view.findViewById<TextView>(R.id.lastRefreshedTextView)
        titleTextView.text = greetingTitle
        authorTextView.visibility = View.GONE
        refreshTextView.text = refreshedShort
        authorTextView.setOnClickListener { v: View? ->
            // Link to my instagram
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse("https://instagram.com/halbunsichtbar")
            startActivity(i)
        }

        // Expand button to show more info
        view.findViewById<View>(R.id.expandButton).setOnClickListener { v: View? ->
            val expandLayout = view.findViewById<LinearLayout>(R.id.expandLayout)
            val expandButton = view.findViewById<ImageButton>(R.id.expandButton)
            if (expandLayout.visibility == View.GONE) {
                expandLayout.visibility = View.VISIBLE
                titleTextView.text = appnameTitle
                authorTextView.visibility = View.VISIBLE
                refreshTextView.text = refreshedInfo
                refreshTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, getDrawable(requireContext(), R.drawable.ic_refresh), null)
                expandButton.setImageResource(R.drawable.ic_less)
            } else {
                expandLayout.visibility = View.GONE
                titleTextView.text = greetingTitle
                authorTextView.visibility = View.GONE
                refreshTextView.text = refreshedShort
                refreshTextView.setCompoundDrawablesRelative(null, null, null, null)
                expandButton.setImageResource(R.drawable.ic_more)
            }
        }
        /*refreshTextView.setOnClickListener { v: View? ->
            // Force refresh all changes, courses and lessons
            prefs.edit()
                    .putLong("lastCourseRefresh", 0)
                    .putLong("lastTimetableRefresh", 0) /*.putString("courses", "") Do not delete courses by default; would remove added grades */
                    .apply()
            ChangesManager().refreshChanges(context)
            dismiss()
        }*/
        /*view.findViewById<View>(R.id.coursesButton).setOnClickListener { v: View? ->
            // Show a bottom sheet to edit favorite courses
            coursesSheet = CoursesSheet()
            coursesSheet.show(activity!!.supportFragmentManager, "coursesSheet")
            dismiss()
        }*/


        view.findViewById<View>(R.id.logoutButton).setOnClickListener {
            // Ask if user actually wants to log out
            AlertDialog.Builder(context)
                    .setTitle(R.string.menu_option_logout)
                    .setMessage(R.string.menu_option_logout_question)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        run {
                            // Clear SharedPrefs
                            // todo check if we still need something
                            prefs.edit().clear().apply()
                            // todo Delete database
                            // requireContext().deleteDatabase("database")
                            DatabaseHelper.newInstance()
                            // Switch to OnboardingActivity
                            startActivity(Intent(context, OnboardingActivity().javaClass).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                            requireActivity().finish()
                        }
                    }
                    .setNegativeButton(R.string.cancel) { _, _ -> super.dismiss() }
                    .show()
        }


        // Background update toggle group
        /*val backgroundToggleGroup: MaterialButtonToggleGroup = view.findViewById(R.id.backgroundToggleGroup)
        if (prefs.getBoolean("backgroundRefresh", true)) backgroundToggleGroup.check(R.id.backgroundOnButton) else backgroundToggleGroup.check(R.id.backgroundOffButton)
        backgroundToggleGroup.addOnButtonCheckedListener { group: MaterialButtonToggleGroup?, checkedId: Int, isChecked: Boolean ->
            if (isChecked) {
                if (checkedId == R.id.backgroundOnButton) {
                    prefs.edit().putBoolean("backgroundRefresh", true).apply()

                    // Enqueue background workers
                    val workConstraints: Constraints = Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    val workRequest: PeriodicWorkRequest = Builder(RefreshWorker::class.java, 60, TimeUnit.MINUTES)
                            .setInitialDelay(45 - Calendar.getInstance()[Calendar.MINUTE], TimeUnit.MINUTES)
                            .setConstraints(workConstraints)
                            .addTag("changesRefresh")
                            .build()
                    WorkManager.getInstance(context).enqueueUniquePeriodicWork("changesRefresh", ExistingPeriodicWorkPolicy.KEEP, workRequest)
                    val morningWorkRequest: PeriodicWorkRequest = Builder(RefreshWorker::class.java, 15, TimeUnit.MINUTES)
                            .setConstraints(workConstraints)
                            .addTag("morningReinforcement")
                            .build()
                    WorkManager.getInstance(context).enqueueUniquePeriodicWork("morningReinforcement", ExistingPeriodicWorkPolicy.KEEP, morningWorkRequest)
                } else {
                    prefs.edit().putBoolean("backgroundRefresh", false).apply()

                    // Cancel background workers
                    WorkManager.getInstance(context).cancelUniqueWork("changesRefresh")
                    WorkManager.getInstance(context).cancelUniqueWork("morningReinforcement")
                }
                dismiss()
            }
        }*/

        /**
         * Enable/disable use of AutoSPH
         */
        view.findViewById<SwitchMaterial>(R.id.autosphSwitch).isChecked =
                prefs.getBoolean("open_sph_accepted_auto", false)
        view.findViewById<SwitchMaterial>(R.id.autosphSwitch).setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("open_sph_accepted_auto", isChecked).apply()
        }

        /**
         * Choose a theme
         * Open a bottom sheet to let the user choose a theme combination
         */
        view.findViewById<View>(R.id.chooseThemeButton).setOnClickListener {
            ThemeSheet().show(parentFragmentManager, "themeSheet")
            dismiss()
        }

        /**
         * Contact
         */
        view.findViewById<View>(R.id.contactButton).setOnClickListener {
            dismiss()
            ContactSheet().show(parentFragmentManager, "contact")
        }
        view.findViewById<View>(R.id.rateButton).setOnClickListener {
            // Open Play Store to let the user rate the app
            val appPackageName = SphPlanner.applicationContext().packageName
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
            } catch (anfe: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
            }
            dismiss()
        }
        view.findViewById<View>(R.id.doneButton).setOnClickListener {
            // Dismiss the sheet
            dismiss()
        }
        return view
    }
}