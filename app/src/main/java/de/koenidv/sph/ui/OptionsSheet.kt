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
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.database.DatabaseHelper
import java.io.File

//  Created by koenidv on 16.02.2020.
// Sorry for horrible code - was imported from GMB-Planner
// Might rework this some day

class OptionsSheet internal constructor() : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.sheet_options, container, false)
        val prefs = SphPlanner.applicationContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)

        // Append version to app name
        try {
            val pInfo = SphPlanner.applicationContext().packageManager.getPackageInfo(SphPlanner.applicationContext().packageName, 0)
            val appnameTitle = getString(R.string.info_app).replace("%version", pInfo.versionName)
            view.findViewById<TextView>(R.id.titleTextView).text = appnameTitle
        } catch (e: PackageManager.NameNotFoundException) {
        }

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
                            // Clear cookies (especcially sid token)
                            de.koenidv.sph.networking.CookieStore.clearCookies()
                            // Delete all downloaded attachments
                            File(requireContext().filesDir.toString() + "/attachments/").deleteRecursively()
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
         * Choose a theme
         * Open a bottom sheet to let the user choose a theme combination
         */
        view.findViewById<View>(R.id.chooseThemeButton).setOnClickListener {
            ThemeSheet().show(parentFragmentManager, "themeSheet")
            dismiss()
        }

        /**
         * Debugging
         */
        view.findViewById<View>(R.id.debuggingButton).setOnClickListener {
            dismiss()
            DebuggingSheet().show(parentFragmentManager, "debugging")
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