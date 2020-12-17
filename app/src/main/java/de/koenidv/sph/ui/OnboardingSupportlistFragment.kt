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
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.StringRequestListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.koenidv.sph.MainActivity
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
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
        NetworkManager().loadSiteWithToken("https://start.schulportal.hessen.de/index.php", object : StringRequestListener {
            override fun onResponse(response: String?) {
                // todo handle maintenance.. again

                val featureList = RawParser().parseFeatureList(response!!)
                // Get string list of supported features
                val features = featureList.map { it.name }

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
                    someFeatures = true
                    featurelistText = featurelistText.replace("%mycourses", crossmarkText)
                } else {
                    featurelistText = featurelistText.replace("%mycourses", checkmarkText)
                }
                if (!features.contains("Nachrichten")) {
                    allFeatures = false
                    someFeatures = true
                    featurelistText = featurelistText.replace("%messages", crossmarkText)
                } else {
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
                    someFeatures = true
                    featurelistText = featurelistText.replace("%timetable", crossmarkText)
                } else {
                    featurelistText = featurelistText.replace("%timetable", checkmarkText)
                }
                if (!features.contains("Vertretungsplan") && !features.contains("Testphase Vertretungsplan")) {
                    allFeatures = false
                    usableFeatures = false
                    someFeatures = true
                    featurelistText = featurelistText.replace("%changes", crossmarkText)
                } else {
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
                if (someFeatures) {
                    warningText.visibility = View.VISIBLE
                    // todo start indexing
                    //indexLoading.visibility = View.VISIBLE
                    nextFab.visibility = View.VISIBLE
                    // todo only activate after indexing is complete
                    prefs.edit().putBoolean("introComplete", true).apply()
                }
            }

            override fun onError(anError: ANError?) {
                featuresLoading.visibility = View.GONE
                titleText.text = getString(R.string.onboard_supported_error_network)
                warningText.visibility = View.VISIBLE
                warningText.setTextColor(requireContext().getColor(R.color.colorAccent))
                warningText.setOnClickListener {
                    val ft = parentFragmentManager.beginTransaction()
                    ft.detach(this@OnboardingSupportlistFragment).attach(this@OnboardingSupportlistFragment).commit()
                }
            }

        })


        nextFab.setOnClickListener { startActivity(Intent(context, MainActivity().javaClass)) }


        return view
    }

}