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
import de.koenidv.sph.database.FunctionTilesDb
import de.koenidv.sph.debugging.DebugLog
import de.koenidv.sph.debugging.Debugger
import de.koenidv.sph.networking.NetworkManager
import de.koenidv.sph.networking.TokenManager
import de.koenidv.sph.networking.UrlResolver
import de.koenidv.sph.parsing.RawParser
import kotlinx.coroutines.*

class OnboardingSupportlistFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_onboarding_supportlist, container, false)
        val prefs = SphPlanner.appContext().getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)

        val featuresLoading = view.findViewById<ProgressBar>(R.id.featuresLoading)
        val titleText = view.findViewById<TextView>(R.id.headTextView)
        val featuresText = view.findViewById<TextView>(R.id.featurelistTextView)
        val warningText = view.findViewById<TextView>(R.id.warningTextView)
        val contactButton = view.findViewById<MaterialButton>(R.id.contactButton)
        val indexLoading = view.findViewById<ProgressBar>(R.id.indexLoading)
        val statusText = view.findViewById<TextView>(R.id.statusTextView)
        val nextFab = view.findViewById<FloatingActionButton>(R.id.nextFab)
        val nextFab90s = view.findViewById<TextView>(R.id.statusTextViewForward)
        nextFab90s.visibility = View.GONE

        // Log loading features
        DebugLog("FeaturesFrag", "Loading features list")

        // Get supported features
        /* Lambda function:
            - Innerhalb der Klammern stehen zuerst die Lambda- Parameter: success: Int, response: String?
            - dann der Pfeil -> und rechts vom Pfeil der Lambda-Body
            - ...(){letzter Funktionsparameter}

         getSiteAuthed will be executed - {...} is the parameter for callback
         fun getSiteAuthed(url: String,
                              forceNewToken: Boolean = false,
                              callback: (success: Int, result: String?) -> Unit) {
        */
        NetworkManager().getSiteAuthed("https://start.schulportal.hessen.de/index.php") { success: Int, response: String? ->
            // Fragment => Must be hosted by (another fragment and finally) an activity: Here OnboardingActivity
            // Would otherwise crash if fragment has been detached in the meantime
            if (host == null) return@getSiteAuthed //return to the end of getSiteAuthed function

            if (success != NetworkManager.SUCCESS) {
                // Display error
                // Credentials should be valid as we just checked them in the last onboarding step
                featuresLoading.visibility = View.GONE
                warningText.text = when (success) {
                    NetworkManager.FAILED_NO_NETWORK -> getString(R.string.onboard_supported_error_network)
                    NetworkManager.FAILED_INVALID_CREDENTIALS -> "INVALID_CREDENTIALS"//getString(R.string.xxx)
                    NetworkManager.FAILED_MAINTENANCE -> getString(R.string.onboard_supported_error_maintenance)
                    NetworkManager.FAILED_SERVER_ERROR -> getString(R.string.onboard_supported_error_server)
                    NetworkManager.FAILED_CANCELLED -> "FAILED_CANCELLED"//getString(R.string.xxx)
                    NetworkManager.FAILED_CRYPTION -> "FAILED_CRYPTION"//getString(R.string.xxx)
                    NetworkManager.FAILED_TOKEN -> getString(R.string.onboard_token)
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
            // Could also get it from username, but we're not processing that here
            // <span class="glyphicon glyphicon-user"></span>Mustermann, Max  (Klasse)<span class="caret"></span>...
            val srch = "<span class=\"glyphicon glyphicon-user\"></span>"
            var realName = response?.substring(response.indexOf(srch))
            val famName  = realName?.substring(srch.length, realName.indexOf(", "))?.trim()
            val clssName = realName?.substring(realName.indexOf("(")+1, realName.indexOf(")"))?.trim()
            //Mustermann, Max  (Klasse)<span class="caret"></span>...
            realName = realName?.substring(realName.indexOf(", ") + 2)//Max  (Klasse)<span class="caret"></span>...
            realName = realName?.substring(0, realName.indexOf(" "))//Max
            if (realName != null) {
                prefs.edit().putString("real_name", realName).apply()
            }
            else {
                prefs.edit().putString("real_name", getString(R.string.dfltRealName)).apply()
            }
            if (famName  != null) {
                prefs.edit().putString("fam_name" , famName ).apply()
            }
            else {
                prefs.edit().putString("fam_name" , getString(R.string.dfltFamName) ).apply()
            }
            if (clssName != null) {
                prefs.edit().putString("clss_name", clssName).apply()
            }
            else {
                prefs.edit().putString("clss_name", getString(R.string.dfltClssName)).apply()
            }

            // todo all indexing in NetworkManager

            val featureList = RawParser().parseFeatureList(response!!)
            // Get string list of supported features
            // Features by => type, A lot of features are type other ...
            // Features by => name, An alternative
            val features = featureList.map { it.type }
            val featuresName = featureList.map { it.name }

            // Log supported features
            DebugLog("FeaturesFrag", "Loaded features list",
                    bundleOf("features" to featureList),
                    Debugger.LOG_TYPE_VAR)

            // Don't crash if context is null for some reason
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
            if (!features.contains("mycourses")) {
                allFeatures = false
                usableFeatures = false
                featurelistText = featurelistText.replace("%mycourses", warnmarkText)
            } else {
                someFeatures = true
                featurelistText = featurelistText.replace("%mycourses", checkmarkText)
            }
            if (!features.contains("messages")) {
                allFeatures = false
                featurelistText = featurelistText.replace("%messages", crossmarkText)
            } else {
                someFeatures = true
                featurelistText = featurelistText.replace("%messages", checkmarkText)
            }
            if (!features.contains("studygroups")) {
                allFeatures = false
                manualFeatures = true // todo manual course adding
                featurelistText = featurelistText.replace("%studygroups", crossmarkText)
            } else {
                featurelistText = featurelistText.replace("%studygroups", checkmarkText)
            }
            if (!features.contains("timetable")) {
                allFeatures = false
                featurelistText = featurelistText.replace("%timetable", crossmarkText)
            } else {
                someFeatures = true
                featurelistText = featurelistText.replace("%timetable", checkmarkText)
            }
            if (!features.contains("changes")) {
                allFeatures = false
                featurelistText = featurelistText.replace("%changes", crossmarkText)
            } else {
                someFeatures = true
                featurelistText = featurelistText.replace("%changes", checkmarkText)
            }
            if (!featuresName.contains("Kalender")) {
                allFeatures = false
                featurelistText = featurelistText.replace("%calendar", crossmarkText)
            } else {
                someFeatures = true
                featurelistText = featurelistText.replace("%calendar", checkmarkText)
            }

            // Get title text from supported tags
            val featureTitleText: String = when {
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
                indexLoading.visibility = View.VISIBLE


                /*
                 * Start indexing
                 */

                // Log index starting
                DebugLog("FeaturesFrag", "INDEXING START")
                DebugLog("FeaturesFrag", "Resolving tile urls")

                // Resolve feature tile urls if necessary
                UrlResolver().resolveFeatureUrls(featureList) {

                    // Save features in case we need them later
                    FunctionTilesDb.getInstance().save(featureList)

                    NetworkManager().indexAll({ status ->
                        // Update status text on status update
                        activity?.runOnUiThread { statusText.text = status }
                    }) { indexsuccess ->

                        /**
                         * Indexing was successful
                         */

                        // Log index status
                        DebugLog("FeaturesFrag",
                                "INDEXING DONE: $indexsuccess",
                                type = Debugger.LOG_TYPE_VAR)

                        // Continue on indexing completion
                        runBlocking {
                            statusText.visibility = View.GONE
                        }
                        if (indexsuccess == NetworkManager.SUCCESS) {
                            indexLoading.visibility = View.GONE
                            nextFab.visibility = View.VISIBLE
                            // Mark onboarding complete
                            prefs.edit().putBoolean("introComplete", true).apply()

                            Debugger.logOnboardingComplete()
                        } else {

                            /**
                             * Indexing was not successful
                             */

                            // Display error message
                            indexLoading.visibility = View.GONE
                            warningText.text = when (indexsuccess) {
                                NetworkManager.FAILED_NO_NETWORK -> getString(R.string.onboard_supported_error_network)
                                NetworkManager.FAILED_INVALID_CREDENTIALS -> "INVALID_CREDENTIALS"//getString(R.string.xxx)
                                NetworkManager.FAILED_MAINTENANCE -> getString(R.string.onboard_supported_error_maintenance)
                                NetworkManager.FAILED_SERVER_ERROR -> getString(R.string.onboard_supported_error_server)
                                NetworkManager.FAILED_CANCELLED -> "FAILED_CANCELLED"//getString(R.string.xxx)
                                NetworkManager.FAILED_CRYPTION -> "FAILED_CRYPTION"//getString(R.string.xxx)
                                NetworkManager.FAILED_TOKEN -> getString(R.string.onboard_token)
                                else -> getString(R.string.onboard_supported_error_unknown)//"Rootcause Crash #A" (anchor for docu, pls NOT delete)
                                //success := 0
                                //indexsuccess := -1
                            }
                            warningText.visibility = View.VISIBLE
                            warningText.setTextColor(requireContext().getColor(R.color.colorAccent))
                            warningText.setOnClickListener {
                                // Log retrying
                                DebugLog("FeaturesFrag",
                                        "Recreating on user input")

                                // Clear session id
                                TokenManager.reset() //tknrst
                                // Recreate to try again
                                requireActivity().recreate()
                            }
                            // Debug option to share response on unknown error
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
            } else {
                // Log unsupported school
                DebugLog("FeaturesFrag", "School unsupported",
                        bundleOf("schoolid" to prefs.getString("schoolid", "0")),
                        Debugger.LOG_TYPE_ERROR)
                // School unsupported. Log to analytics
                FirebaseAnalytics.getInstance(requireContext()).logEvent(
                        "school_unsupported",
                        bundleOf(
                                "school" to prefs.getString("schoolid", "0")!!
                        ))
            }
        } //w/o any cnds => NetworkManager().getSiteAuthed

        // Continue button
        nextFab.setOnClickListener {
            // Log onboarding complete
            DebugLog("FeaturesFrag",
                    "ONBOARDING COMPLETE",
                    type = Debugger.LOG_TYPE_SUCCESS)
            // Mark onboarding completed for Crashlytics
            FirebaseCrashlytics.getInstance().setCustomKey("onboarding_completed", true)
            // Disable debugger after indexing is complete
            Debugger.setEnabled(false)

            // Remember this version code
            prefs.edit().putInt("appVersion", 130).apply()

            startActivity(Intent(context, MainActivity().javaClass)); requireActivity().finish()
        }

        contactButton.setOnClickListener { ContactSheet().show(parentFragmentManager, "contact") }

        /*
        * After 45s, display debugging options
        */

        CoroutineScope(Dispatchers.Unconfined).launch {
            delay(45000)



            withContext(Dispatchers.Main) {
                // Set up share debug log button
                view.findViewById<MaterialButton>(R.id.debugButton).apply {
                    visibility = View.VISIBLE
                    setOnClickListener {
                        DebuggingSheet().show(parentFragmentManager, "debugging-indexing")
                    }
                }

                if(context != null) {
                    //Show hint for automatic continuation between this time point and time point for forwarding
                    nextFab90s.text = getString(R.string.forwarding_message)
                    nextFab90s.visibility = View.VISIBLE
                }

            }
        }

        /*
        * After 60s of inactivity continue automaticly
        */

        //dispatch := entsendet, gemeldet; unconfined := not limited (nicht abgegrenzt)
        CoroutineScope(Dispatchers.Unconfined).launch {
            delay(60000)
            withContext(Dispatchers.Main) {
                if(context != null) {
                    startActivity(
                        Intent(
                            context,
                            MainActivity().javaClass
                        )
                    ); requireActivity().finish()
                }
            }
        }

        return view
    }

}