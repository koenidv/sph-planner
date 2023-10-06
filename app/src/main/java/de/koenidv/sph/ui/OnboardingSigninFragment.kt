package de.koenidv.sph.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.StringRequestListener
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.toptoche.searchablespinnerlibrary.SearchableSpinner
import de.koenidv.sph.MainActivity
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.debugging.DebugLog
import de.koenidv.sph.debugging.Debugger
import de.koenidv.sph.networking.NetworkManager
import de.koenidv.sph.networking.TokenManager
import de.koenidv.sph.parsing.RawParser
import java.util.*


class OnboardingSigninFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // If user is verified but app was closed before data could finish loading,
        // we can just skip to the next fragment
        val prefs = SphPlanner.appContext().getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)
        if (prefs.getBoolean("credsVerified", false)) {
            // If logging is enabled, log this
                DebugLog("SigninFrag", "Already signed in, skipping")
            val ft = parentFragmentManager.beginTransaction()
            ft.replace(R.id.fragment, OnboardingSupportlistFragment()).commit()
            return null
        } else {

            val view = inflater.inflate(R.layout.fragment_onboarding_signin, container, false)
            var schoolIds: List<Pair<String, String>> = listOf()

            val loadicon = view.findViewById<ProgressBar>(R.id.signinLoading)
            val title = view.findViewById<TextView>(R.id.signinTitleTextView)
            val description = view.findViewById<TextView>(R.id.signinDescriptionTextView)
            val schoolid = view.findViewById<SearchableSpinner>(R.id.schoolIdSpinner)
            val userText = view.findViewById<EditText>(R.id.userEditText)
            val passText = view.findViewById<EditText>(R.id.passwordEditText)
            val textlayout1 = view.findViewById<TextInputLayout>(R.id.textInputLayout)
            val textlayout2 = view.findViewById<TextInputLayout>(R.id.textInputLayout2)
            val signinButton = view.findViewById<ExtendedFloatingActionButton>(R.id.signinButton)
            val demodataButton = view.findViewById<Button>(R.id.demodataButton)

            signinButton.shrink()


            // If logging is enabled, log loading the start page
            DebugLog("SigninFrag", "Loading list of schools")

            // Load school names and ids to display them in a spinner
            // Hide loading icon and show contents once done
            AndroidNetworking.get("https://start.schulportal.hessen.de/")
                    .setPriority(Priority.LOW)
                    .build()
                    .getAsString(object : StringRequestListener {
                        override fun onResponse(response: String) {
                            // If app was closed don't continue
                            if (context == null) return
                            // no matter the outcome, display demo data button
                            demodataButton.visibility = View.VISIBLE
                            // Parse schools from response
                            schoolIds = RawParser().parseSchoolIds(response)
                            // Fill spinner if response was valid, else show error
                            if (response.contains("Wartungsarbeiten") || schoolIds.isEmpty()) {
                                // Maintenance work
                                // Show error
                                loadicon.visibility = View.GONE
                                title.visibility = View.VISIBLE
                                description.visibility = View.VISIBLE
                                description.text = getString(R.string.onboard_welcome_error_maintenance)
                                description.setTextColor(requireContext().getColor(R.color.colorAccent))
                                description.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                                // If logging is enabled, log maintenance
                                DebugLog("SigninFrag",
                                        "School list loading failed, maintenance",
                                        bundleOf("response" to response))
                                description.setOnClickListener {
                                    // Log trying again
                                    DebugLog("SigninFrag",
                                            "User Input: Trying again after maintenance")
                                    val ft = parentFragmentManager.beginTransaction()
                                    ft.detach(this@OnboardingSigninFragment).attach(
                                            this@OnboardingSigninFragment).commit()
                                }
                            } else {
                                // Show sign in ui
                                // Load spinner
                                schoolid.adapter = SpinAdapter(requireContext(), schoolIds)
                                schoolid.setPositiveButton(getString(R.string.cancel))
                                schoolid.setTitle(getString(R.string.onboard_select_school))
                                schoolid.setSelection(schoolIds.indexOf(
                                        "Gymnasium am Mosbacher Berg, Wiesbaden" to "5146"))

                                // Set component visibility
                                loadicon.visibility = View.GONE
                                title.visibility = View.VISIBLE
                                description.visibility = View.VISIBLE
                                schoolid.visibility = View.VISIBLE
                                textlayout1.visibility = View.VISIBLE
                                textlayout2.visibility = View.VISIBLE
                                signinButton.visibility = View.VISIBLE

                                // Log schools loaded
                                DebugLog("SigninFrag",
                                        "School list loading: Success",
                                        bundleOf("listSize" to schoolIds.size))
                            }
                        }

                        override fun onError(anError: ANError) {
                            // Log network error
                            DebugLog("SigninFrag",
                                    "NetError loading schools list",
                                    anError)

                            // Error occurred, very high chance of no network
                            // Show error
                            loadicon.visibility = View.GONE
                            title.visibility = View.VISIBLE
                            description.visibility = View.VISIBLE
                            demodataButton.visibility = View.VISIBLE
                            description.text = getString(R.string.onboard_welcome_error_connection)
                            description.setTextColor(requireContext().getColor(R.color.colorAccent))
                            description.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                            description.setOnClickListener {
                                // Log trying again
                                DebugLog("SigninFrag",
                                        "User Input: Trying again after net error")
                                // Recreate fragment
                                val ft = parentFragmentManager.beginTransaction()
                                ft.detach(this@OnboardingSigninFragment).attach(this@OnboardingSigninFragment).commit()
                            }
                        }

                    })

            // Enable and expand sign in button if there's an input
            userText.doOnTextChanged { _, _, _, _ ->
                if (userText.text.isNotEmpty() && passText.text.isNotEmpty()) {
                    signinButton.isEnabled = true
                    signinButton.extend()
                } else {
                    signinButton.isEnabled = false
                    //signinButton.shrink()
                }
            }
            passText.doOnTextChanged { _, _, _, _ ->
                if (userText.text.isNotEmpty() && passText.text.isNotEmpty()) {
                    signinButton.isEnabled = true
                    signinButton.extend()
                } else {
                    signinButton.isEnabled = false
                }
            }
            // Sign in using keyboard mime action
            passText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE
                        && userText.text.isNotEmpty()
                        && passText.text.isNotEmpty()) {
                    signinButton.callOnClick()
                }
                false
            }

            // Setup sign in fab
            signinButton.setOnClickListener {
                // Set component visibility
                loadicon.visibility = View.VISIBLE
                title.visibility = View.GONE
                description.visibility = View.GONE
                schoolid.visibility = View.GONE
                textlayout1.visibility = View.GONE
                textlayout2.visibility = View.GONE
                signinButton.visibility = View.GONE
                // Hide keyboard
                (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow(view.windowToken, 0)
                // Hide the password
                passText.transformationMethod = PasswordTransformationMethod()

                val school = schoolIds[schoolid.selectedItemPosition].second
                prefs.edit()
                        .putString("user", userText.text.toString()
                                .trim()
                                .toLowerCase(Locale.ROOT)
                                .replace(" ", ".")
                                .replace("ä", "ae")
                                .replace("ö", "oe")
                                .replace("ü", "ue"))
                        .putString("password", passText.text.toString())
                        // We'll assume schools have been loaded, as ui won't let the user get here otherwise
                        // We could mitigate this by checking for schoolIds.isNotEmpty()..
                        .putString("schoolid", school)
                        .apply()

                // Log signing in
                DebugLog("SigninFrag", "Signing in with $school")

                // Check if credentials are valid
                // We'll only get a token if login was successfull
                TokenManager.getToken(true) { success: Int, token: String? ->

                    // Log signing in
                        DebugLog("SigninFrag", "Signin cb: $success",
                                type = Debugger.LOG_TYPE_VAR)

                    if (success == NetworkManager.SUCCESS && token != null && token != "") {
                        // User signed in successfully - NOW DO SOMETHING WITH IT :)
                        prefs.edit().putBoolean("credsVerified", true).apply()

                        // If this is a teacher account, meaning no "." in the user name,
                        // remember this. Might be the cause of some future issues.
                        if (!userText.text.contains(".")) {
                            // Mark teacher account for Crashlytics
                            FirebaseCrashlytics.getInstance().setCustomKey("is_teacher", true)
                        }
                        // Mark school id in crashlytics, for more effective debugging
                        FirebaseCrashlytics.getInstance().setCustomKey(
                                "school_id", prefs.getString("schoolid", "0")!!)

                        val analytics = FirebaseAnalytics.getInstance(requireContext())
                        // Log school id GA as user property
                        analytics.setUserProperty(
                                "school",
                                prefs.getString("schoolid", "0")!!)
                        // Log conversion to GA
                        analytics.logEvent("login_complete", bundleOf())

                        // Move to next onboarding fragment
                        val ft = parentFragmentManager.beginTransaction()
                        ft.replace(R.id.fragment, OnboardingSupportlistFragment()).commit()
                    } else {
                        // Sign in failed, reshow ui
                        loadicon.visibility = View.GONE
                        title.visibility = View.VISIBLE
                        description.visibility = View.VISIBLE
                        schoolid.visibility = View.VISIBLE
                        textlayout1.visibility = View.VISIBLE
                        textlayout2.visibility = View.VISIBLE
                        signinButton.visibility = View.VISIBLE

                        // Show an error message
                        description.setTextColor(requireContext().getColor(R.color.colorAccent))
                        description.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                        if (success == NetworkManager.FAILED_INVALID_CREDENTIALS) {
                            // Incorrect user/password
                            description.text = getString(R.string.onboard_signin_error_credentials)
                        } else if (success == NetworkManager.FAILED_NO_NETWORK
                                || success == NetworkManager.FAILED_CANCELLED) {
                            // No connectivity, request timed out or got cancelled
                            description.text = getString(R.string.onboard_signin_error_network)
                        } else if (success == NetworkManager.FAILED_MAINTENANCE) {
                            // sph is under maintenance
                            description.text = getString(R.string.onboard_signin_error_maintenance)
                        } else if (success == NetworkManager.FAILED_SERVER_ERROR) {
                            // An server error occurred
                            description.text = getString(R.string.onboard_signin_error_server)
                        } else {
                            // Some other error occurred
                            // StKl - 04.12.2021 - Hier gehe ich manchmal rein. App stuerzt ab. Bei Neustart bin ich dann drin
                            DebugLog("Onboarding", "BlueScreen? $success")
                            description.text = getString(R.string.onboard_signin_error_unknown)
                        }
                    }
                }
                DebugLog("Onboarding", "BlueScreen? Tolenmanager1")
            }
            DebugLog("Onboarding", "BlueScreen? signinButton 1")

            demodataButton.setOnClickListener {
                // Enable demo mode
                prefs.edit()
                    .putBoolean("demoMode", true)
                    .putBoolean("introComplete", true)
                    .putBoolean("credsVerified", true)
                    .putString("user", "Demo User")
                    .apply()
                startActivity(Intent(context, MainActivity().javaClass)); requireActivity().finish()
            }


            // Restore credentials if sign in failed before
            userText.setText(prefs.getString("user", ""))
            passText.setText(prefs.getString("password", ""))

            return view
        }
    }
}

// Adapter for school id spinner
class SpinAdapter(context: Context, private val values: List<Pair<String, String>>) : ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, values.map { it.first }) {

    override fun getCount(): Int {
        return values.size
    }

    override fun getItem(position: Int): String {
        return values[position].first + " (" + values[position].second + ")"
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    // SearchableSpinner will only get this for closed view
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val label = super.getView(position, convertView, parent) as TextView
        label.text = values[position].first
        return label
    }

}