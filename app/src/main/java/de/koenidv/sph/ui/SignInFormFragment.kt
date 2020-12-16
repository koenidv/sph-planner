package de.koenidv.sph.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.StringRequestListener
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import com.toptoche.searchablespinnerlibrary.SearchableSpinner
import de.koenidv.sph.MainActivity
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.networking.NetworkManager
import de.koenidv.sph.networking.TokenManager
import de.koenidv.sph.parsing.RawParser
import okhttp3.OkHttpClient


class SignInFormFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_sign_in_form, container, false)
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

        signinButton.shrink()
        // todo default selection for GMB

        // Load school names and ids to display them in a spinner
        // Hide loading icon and show contents once done
        AndroidNetworking.initialize(SphPlanner.applicationContext(), OkHttpClient())
        AndroidNetworking.get("https://start.schulportal.hessen.de/")
                .setPriority(Priority.LOW)
                .build()
                .getAsString(object : StringRequestListener {
                    override fun onResponse(response: String) {
                        schoolIds = RawParser().parseSchoolIds(response)
                        if (response.contains("Wartungsarbeiten") || schoolIds.isEmpty()) {
                            // Maintenance work
                            // Show error
                            loadicon.visibility = View.GONE
                            title.visibility = View.VISIBLE
                            description.visibility = View.VISIBLE
                            description.text = getString(R.string.onboard_welcome_error_maintenance)
                            description.setTextColor(context!!.getColor(R.color.colorAccent))
                            description.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                            description.setOnClickListener {
                                val ft = fragmentManager!!.beginTransaction()
                                ft.detach(this@SignInFormFragment).attach(this@SignInFormFragment).commit()
                            }
                        } else {
                            // Show sign in ui
                            // Load spinner
                            schoolid.adapter = SpinAdapter(context!!, schoolIds)
                            schoolid.setPositiveButton(getString(R.string.cancel))
                            schoolid.setTitle(getString(R.string.onboard_select_school))
                            // Set component visibility
                            loadicon.visibility = View.GONE
                            title.visibility = View.VISIBLE
                            description.visibility = View.VISIBLE
                            schoolid.visibility = View.VISIBLE
                            textlayout1.visibility = View.VISIBLE
                            textlayout2.visibility = View.VISIBLE
                            signinButton.visibility = View.VISIBLE
                        }
                    }

                    override fun onError(anError: ANError?) {
                        // Error occurred, very high chance of no network
                        // Show error
                        loadicon.visibility = View.GONE
                        title.visibility = View.VISIBLE
                        description.visibility = View.VISIBLE
                        description.text = getString(R.string.onboard_welcome_error_connection)
                        description.setTextColor(context!!.getColor(R.color.colorAccent))
                        description.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                        description.setOnClickListener {
                            // todo use new api
                            val ft = fragmentManager!!.beginTransaction()
                            ft.detach(this@SignInFormFragment).attach(this@SignInFormFragment).commit()
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
                //signinButton.shrink()
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
            (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(view.windowToken, 0)

            val prefs = SphPlanner.applicationContext().getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)

            prefs.edit()
                    .putString("user", userText.text.toString().replace(" ", "."))
                    .putString("password", passText.text.toString())
                    // We'll assume schools have been loaded, as ui won't let the user get here otherwise
                    // We could mitigate this by checking for schoolIds.isNotEmpty()..
                    .putString("schoolid", schoolIds[schoolid.selectedItemPosition].second)
                    .apply()

            // Check if credentials are valid
            // We'll only get a token if login was successfull
            TokenManager().generateAccessToken(object : TokenManager.TokenGeneratedListener {
                override fun onTokenGenerated(success: Int, token: String) {
                    if (success == NetworkManager().SUCCESS) {
                        // todo User signed in successfully - NOW DO SOMETHING WITH IT :)
                        prefs.edit().putBoolean("credsVerified", true).apply()
                        startActivity(Intent(context, MainActivity().javaClass))
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
                        description.setTextColor(context!!.getColor(R.color.colorAccent))
                        description.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                        if (success == NetworkManager().FAILED_INVALID_CREDENTIALS) {
                            // Incorrect user/password
                            description.text = getString(R.string.onboard_signin_error_credentials)
                        } else if (success == NetworkManager().FAILED_NO_NETWORK
                                || success == NetworkManager().FAILED_CANCELLED) {
                            // No connectivity, request timed out or got cancelled
                            description.text = getString(R.string.onboard_signin_error_network)
                        } else if (success == NetworkManager().FAILED_MAINTENANCE) {
                            // sph is under maintenance
                            description.text = getString(R.string.onboard_signin_error_maintenance)
                        }
                    }
                }

            }, true)
        }

        // Inflate the layout for this fragment
        return view
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