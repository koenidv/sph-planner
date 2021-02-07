package de.koenidv.sph.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.koenidv.sph.MainActivity
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.database.FunctionTilesDb
import de.koenidv.sph.networking.NetworkManager
import de.koenidv.sph.networking.TokenManager
import de.koenidv.sph.parsing.RawParser

class OnboardingSupportlistFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_onboarding_supportlist, container, false)
        val prefs = SphPlanner.applicationContext().getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)

        val featuresLoading = view.findViewById<ProgressBar>(R.id.featuresLoading)
        val titleText = view.findViewById<TextView>(R.id.headTextView)
        val featuresText = view.findViewById<TextView>(R.id.featurelistTextView)
        val warningText = view.findViewById<TextView>(R.id.warningTextView)
        val contactButton = view.findViewById<MaterialButton>(R.id.contactButton)
        val indexLoading = view.findViewById<ProgressBar>(R.id.indexLoading)
        val statusText = view.findViewById<TextView>(R.id.statusTextView)
        val nextFab = view.findViewById<FloatingActionButton>(R.id.nextFab)

        // Get supported features
        NetworkManager().getSiteAuthed("https://start.schulportal.hessen.de/index.php") { success: Int, response: String? ->

            // Would otherwise crash if fragment has been detached in the meantime
            if (host == null) return@getSiteAuthed

            if (success != NetworkManager.SUCCESS) {
                // Display error
                // Credentials should be valid as we just checked them in the last onboarding step
                featuresLoading.visibility = View.GONE
                warningText.text = when (success) {
                    NetworkManager.FAILED_NO_NETWORK -> getString(R.string.onboard_supported_error_network)
                    NetworkManager.FAILED_MAINTENANCE -> getString(R.string.onboard_supported_error_maintenance)
                    NetworkManager.FAILED_SERVER_ERROR -> getString(R.string.onboard_supported_error_server)
                    else -> getString(R.string.onboard_supported_error_unknown)
                }
                warningText.visibility = View.VISIBLE
                warningText.setTextColor(requireContext().getColor(R.color.colorAccent))
                warningText.setOnClickListener {
                    // Recreate to try again
                    requireActivity().recreate()
                }

                // Debug option to share response on unknown error
                warningText.setOnLongClickListener {
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, success.toString() + "\n" + response)
                        this.type = "text/plain"
                    }
                    requireActivity().startActivity(Intent.createChooser(sendIntent, null))
                    true
                }
                // Debug show toast if credential are somehow wrong
                if (success == NetworkManager.FAILED_INVALID_CREDENTIALS)
                    Toast.makeText(requireContext(), "Invalid credentials", Toast.LENGTH_LONG).show()
                return@getSiteAuthed
            }

            // Get real name from result
            // Could also get it from username, but we're not processing that heh
            var realName = response?.substring(response.indexOf("<span class=\"glyphicon glyphicon-user\"></span>"))
            realName = realName?.substring(realName.indexOf(", ") + 2)
            realName = realName?.substring(0, realName.indexOf(" "))
            if (realName != null) prefs.edit().putString("real_name", realName).apply()

            // todo all indexing in NetworkManager

            val featureList = RawParser().parseFeatureList(response!!)
            // Get string list of supported features
            val features = featureList.map { it.name }

            if (context == null) return@getSiteAuthed

            // Supported tags
            val schoolTested = requireContext().resources.getStringArray(R.array.tested_schools).contains(prefs.getString("schoolid", ""))
            var allFeatures = true
            var usableFeatures = true
            var manualFeatures = false
            var someFeatures = false
            var featurelistText = getString(R.string.onboard_supported_featurelist)
            val checkmarkText = getString(R.string.emoji_check)
            val crossmarkText = getString(R.string.emoji_cross)
            val warnmarkText = getString(R.string.emoji_warning)

            // todo Better check for compatibility
            if (!features.contains("Mein Unterricht")) {
                allFeatures = false
                usableFeatures = false
                featurelistText = featurelistText.replace("%mycourses", warnmarkText)
            } else {
                someFeatures = true
                featurelistText = featurelistText.replace("%mycourses", checkmarkText)
            }
            if (!features.contains("Nachrichten")) {
                //allFeatures = false
                featurelistText = featurelistText.replace("%messages", crossmarkText)
            } else {
                //someFeatures = true
                featurelistText = featurelistText.replace("%messages", checkmarkText)
            }
            if (!features.contains("Lerngruppen")) {
                allFeatures = false
                manualFeatures = true // todo manual course adding
                featurelistText = featurelistText.replace("%studygroups", crossmarkText)
            } else {
                featurelistText = featurelistText.replace("%studygroups", checkmarkText)
            }
            if (!features.contains("Stundenplan")) {
                allFeatures = false
                featurelistText = featurelistText.replace("%timetable", crossmarkText)
            } else {
                someFeatures = true
                featurelistText = featurelistText.replace("%timetable", checkmarkText)
            }
            if (!features.contains("Vertretungsplan") && !features.contains("Testphase Vertretungsplan")) {
                allFeatures = false
                featurelistText = featurelistText.replace("%changes", crossmarkText)
            } else {
                someFeatures = true
                featurelistText = featurelistText.replace("%changes", checkmarkText)
            }

            // Get title text from supported tags
            val featureTitleText: String
            featureTitleText = when {
                usableFeatures && schoolTested -> getString(R.string.onboard_supported_schooltested)
                allFeatures -> getString(R.string.onboard_supported_features_full)
                usableFeatures && manualFeatures -> getString(R.string.onboard_supported_features_partly_manual)
                usableFeatures && !manualFeatures -> getString(R.string.onboard_supported_features_partly_hidden)
                someFeatures && !usableFeatures -> getString(R.string.onboard_supported_features_partly_not)
                else -> getString(R.string.onboard_supported_features_none)
            }

            // Set contents and visibilites

            featuresLoading.visibility = View.GONE
            titleText.text = featureTitleText
            titleText.visibility = View.VISIBLE
            featuresText.text = featurelistText
            featuresText.visibility = View.VISIBLE
            contactButton.visibility = View.VISIBLE
            if (usableFeatures) {
                warningText.visibility = View.VISIBLE
                // todo start indexing
                indexLoading.visibility = View.VISIBLE


                /*
                 * Start indexing
                 */

                // Resolve tile urls
                var tilesResolved = 0
                for (feature in featureList) {
                    NetworkManager().resolveUrl(feature.location, callback = { successUrl: Int, resolvedUrl: String ->
                        kotlin.run {
                            // Save new url to object
                            if (successUrl == NetworkManager.SUCCESS
                                    || successUrl == NetworkManager.FAILED_UNKNOWN // If sph redirected back to home
                            )
                                feature.location = resolvedUrl
                            // Save number of tiles resolved
                            tilesResolved++
                            // If this was the last tile
                            if (tilesResolved == featureList.size) {
                                // Save features in case we need them later
                                FunctionTilesDb.getInstance().save(featureList)

                                // Now index everything else
                                NetworkManager().indexAll({
                                    // Update status text on status update
                                    status ->
                                    statusText.text = status
                                }) { indexsuccess ->
                                    // Continue on indexing completion
                                    statusText.visibility = View.GONE
                                    if (indexsuccess == NetworkManager.SUCCESS) {
                                        indexLoading.visibility = View.GONE
                                        nextFab.visibility = View.VISIBLE
                                        // Mark onboarding complete
                                        prefs.edit().putBoolean("introComplete", true).apply()

                                        val analytics = FirebaseAnalytics.getInstance(requireContext())
                                        // Log an school course id example to GA
                                        analytics.setUserProperty(
                                                "courseIdExample",
                                                CoursesDb.getInstance().gmbIdExample)
                                        // Log conversion to GA
                                        analytics.logEvent("onboarding_complete", bundleOf())
                                    } else {
                                        // Display error message
                                        indexLoading.visibility = View.GONE
                                        warningText.text = when (indexsuccess) {
                                            NetworkManager.FAILED_NO_NETWORK -> getString(R.string.onboard_supported_error_network)
                                            NetworkManager.FAILED_MAINTENANCE -> getString(R.string.onboard_supported_error_maintenance)
                                            NetworkManager.FAILED_SERVER_ERROR -> getString(R.string.onboard_supported_error_server)
                                            else -> getString(R.string.onboard_supported_error_unknown)
                                        }
                                        warningText.visibility = View.VISIBLE
                                        warningText.setTextColor(requireContext().getColor(R.color.colorAccent))
                                        warningText.setOnClickListener {
                                            // Clear session id
                                            TokenManager().reset()
                                            // Recreate to try again
                                            requireActivity().recreate()
                                        }
                                        // Debug option to share response on unknown error
                                        // Usign text comparison because o fthe else statement
                                        warningText.setOnLongClickListener {
                                            val sendIntent: Intent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT,
                                                        indexsuccess.toString()
                                                                + "@" + statusText.text
                                                                + "\n--- Server Response ---\n"
                                                                + response)
                                                this.type = "text/plain"
                                            }
                                            requireActivity().startActivity(Intent.createChooser(sendIntent, null))
                                            true
                                        }
                                    }
                                }


                            }
                        }
                    })
                }
            } else {
                // School unsupported. Log to analytics
                FirebaseAnalytics.getInstance(requireContext()).logEvent(
                        "school_unsupported",
                        bundleOf(
                                "school" to prefs.getString("schoolid", "0")!!
                        ))
            }
        }

        // Continue button
        nextFab.setOnClickListener {
            // Mark onboarding completed for Crashlytics
            FirebaseCrashlytics.getInstance().setCustomKey("onboarding_completed", true)
            startActivity(Intent(context, MainActivity().javaClass)); requireActivity().finish()
        }

        contactButton.setOnClickListener { ContactSheet().show(parentFragmentManager, "contact") }

        return view
    }

}