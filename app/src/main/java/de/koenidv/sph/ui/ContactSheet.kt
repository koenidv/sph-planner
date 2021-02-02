package de.koenidv.sph.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner

//  Created by koenidv on 29.12.2020.
class ContactSheet internal constructor() : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.sheet_contact, container, false)

        val telegramLayout = view.findViewById<LinearLayout>(R.id.contactTelegramLayout)
        val instagramLayout = view.findViewById<LinearLayout>(R.id.contactInstagramLayout)
        val githubLayout = view.findViewById<LinearLayout>(R.id.contactGithubLayout)
        val mailLayout = view.findViewById<LinearLayout>(R.id.contactMailLayout)
        val doneButton = view.findViewById<Button>(R.id.doneButton)


        // Send telegram message
        telegramLayout.setOnClickListener {
            try {
                dismiss()
                startActivity(Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.contact_telegram_data))))
            } catch (e: Exception) {
            }
        }

        // Open Instagram profile to send a message
        instagramLayout.setOnClickListener {
            try {
                dismiss()
                startActivity(Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.contact_instagram_data))))
            } catch (e: Exception) {
            }
        }

        // Open GitHub site to open an issue
        githubLayout.setOnClickListener {
            try {
                dismiss()
                startActivity(Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.contact_github_data))))
            } catch (e: Exception) {
            }
        }

        // Send email with android and version code
        mailLayout.setOnClickListener {
            val prefs = requireContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
            // Get app version
            val version: String
            version = try {
                val pInfo = SphPlanner.applicationContext().packageManager.getPackageInfo(SphPlanner.applicationContext().packageName, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pInfo.longVersionCode.toString()
                } else {
                    @Suppress("DEPRECATION")
                    pInfo.versionCode.toString()
                }
            } catch (nme: PackageManager.NameNotFoundException) {
                "?"
            }
            // Send an email
            val emailIntent = Intent(Intent.ACTION_SENDTO)
                    .setData(Uri.parse("mailto:"))
                    .putExtra(Intent.EXTRA_EMAIL, arrayOf("koenidv@gmail.com"))
                    .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject))
                    .putExtra(Intent.EXTRA_TEXT, getString(R.string.feedback_body)
                            .replace("%app", version)
                            .replace("%android", Build.VERSION.SDK_INT.toString())
                            .replace("%school", prefs.getString("school", "0")!!))
            // Only open if email client is installed
            if (emailIntent.resolveActivity(SphPlanner.applicationContext().packageManager) != null) startActivity(emailIntent)
            dismiss()
        }

        doneButton.setOnClickListener { dismiss() }

        return view
    }
}