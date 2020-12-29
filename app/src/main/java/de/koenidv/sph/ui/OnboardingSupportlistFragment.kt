package de.koenidv.sph.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.koenidv.sph.MainActivity
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.database.FunctionTilesDb
import de.koenidv.sph.networking.NetworkManager
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
        val indexLoading = view.findViewById<ProgressBar>(R.id.indexLoading)
        val nextFab = view.findViewById<FloatingActionButton>(R.id.nextFab)

        // Get supported features
        NetworkManager().loadSiteWithToken("https://start.schulportal.hessen.de/index.php", onComplete = { success: Int, response: String? ->

            if (success != NetworkManager.SUCCESS) {
                // Display network error
                // Might display if sph is being maintained
                // Credentials should be valid as we just checked them in the last onboarding step
                featuresLoading.visibility = View.GONE
                titleText.text = when (success) {
                    NetworkManager.FAILED_NO_NETWORK -> getString(R.string.onboard_supported_error_network)
                    NetworkManager.FAILED_MAINTENANCE -> getString(R.string.onboard_supported_error_maintenance)
                    NetworkManager.FAILED_SERVER_ERROR -> getString(R.string.onboard_supported_error_server)
                    else -> getString(R.string.onboard_supported_error_unknown)
                }
                warningText.visibility = View.VISIBLE
                warningText.setTextColor(requireContext().getColor(R.color.colorAccent))
                warningText.setOnClickListener {
                    val ft = parentFragmentManager.beginTransaction()
                    ft.detach(this@OnboardingSupportlistFragment).attach(this@OnboardingSupportlistFragment).commit()
                }
                return@loadSiteWithToken
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

            if (context == null) return@loadSiteWithToken

            // Supported tags
            val schoolTested = requireContext().resources.getStringArray(R.array.tested_schools).contains(prefs.getString("schoolid", ""))
            var allFeatures = true
            var usableFeatures = true
            var manualFeatures = false
            var someFeatures = false
            var featurelistText = getString(R.string.onboard_supported_featurelist)
            val checkmarkText = getString(R.string.emoji_check)
            val crossmarkText = getString(R.string.emoji_cross)

            // todo Better check for compatibility
            if (!features.contains("Mein Unterricht")) {
                allFeatures = false
                usableFeatures = false
                featurelistText = featurelistText.replace("%mycourses", crossmarkText)
            } else {
                someFeatures = true
                featurelistText = featurelistText.replace("%mycourses", checkmarkText)
            }
            if (!features.contains("Nachrichten")) {
                allFeatures = false
                featurelistText = featurelistText.replace("%messages", crossmarkText)
            } else {
                someFeatures = true
                featurelistText = featurelistText.replace("%messages", checkmarkText)
            }
            if (!features.contains("Lerngruppen")) {
                allFeatures = false
                manualFeatures = true
                featurelistText = featurelistText.replace("%studygroups", crossmarkText)
            } else {
                featurelistText = featurelistText.replace("%studygroups", checkmarkText)
            }
            if (!features.contains("Stundenplan")) {
                allFeatures = false
                usableFeatures = false
                featurelistText = featurelistText.replace("%timetable", crossmarkText)
            } else {
                someFeatures = true
                featurelistText = featurelistText.replace("%timetable", checkmarkText)
            }
            if (!features.contains("Vertretungsplan") && !features.contains("Testphase Vertretungsplan")) {
                allFeatures = false
                usableFeatures = false
                featurelistText = featurelistText.replace("%changes", crossmarkText)
            } else {
                someFeatures = true
                featurelistText = featurelistText.replace("%changes", checkmarkText)
            }

            // Get title text from supported tags
            val featureTitleText: String
            featureTitleText = when {
                schoolTested -> getString(R.string.onboard_supported_schooltested)
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
            if (someFeatures) { // todo show contact options othwerwise
                warningText.visibility = View.VISIBLE
                // todo start indexing
                indexLoading.visibility = View.VISIBLE


                /*
                 * Start indexing
                 */

                // Resolve tile urls
                var tilesResolved = 0
                for (feature in featureList) {
                    NetworkManager().resolveUrl(feature.location, onComplete = { successUrl: Int, resolvedUrl: String ->
                        kotlin.run {
                            // Save new url to object
                            // todo handle errors
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
                                NetworkManager().indexAll {
                                    if (it == NetworkManager.SUCCESS) {
                                        indexLoading.visibility = View.GONE
                                        nextFab.visibility = View.VISIBLE
                                        prefs.edit().putBoolean("introComplete", true).apply()
                                    }
                                }


                            }
                        }
                    })
                }
            }
        })

        // Continue button
        nextFab.setOnClickListener { startActivity(Intent(context, MainActivity().javaClass)); requireActivity().finish() }


        return view
    }

}