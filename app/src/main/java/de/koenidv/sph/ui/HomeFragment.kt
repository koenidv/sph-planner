package de.koenidv.sph.ui

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.StringRequestListener
import com.facebook.stetho.okhttp3.StethoInterceptor
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import de.koenidv.sph.networking.NetworkManager
import de.koenidv.sph.networking.TokenManager
import de.koenidv.sph.parsing.RawParser
import okhttp3.OkHttpClient


class HomeFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val prefs: SharedPreferences = applicationContext().getSharedPreferences("sharedPrefs", MODE_PRIVATE)

        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Only for demonstration

        val loginButton = view.findViewById<Button>(R.id.loginButton)
        loginButton.setOnClickListener {
            /*TokenManager().generateAccessToken {
                Toast.makeText(applicationContext(), "Success", Toast.LENGTH_LONG).show()
            }*/
            // Testing load all courses:
            (NetworkManager().loadSiteWithToken("https://start.schulportal.hessen.de/stundenplan.php", object : StringRequestListener {
                override fun onResponse(response: String?) {
                    val courses = RawParser().parseCoursesFromTimetable(response!!)
                }

                override fun onError(anError: ANError?) {
                    Toast.makeText(applicationContext(), anError.toString(), Toast.LENGTH_LONG).show()
                }
            }))


            //
            // Only for testing parsing changes
            //

            // Adding an Network Interceptor for Debugging purpose :
            /*val okHttpClient = OkHttpClient.Builder()
                    .addNetworkInterceptor(StethoInterceptor())
                    .build()
            AndroidNetworking.initialize(applicationContext(), okHttpClient)

            // This will get a static copy of the sph changes page
            AndroidNetworking.get("https://gist.githubusercontent.com/koenidv/7c0b35895d1daf86b2b00f243d687fe2/raw/75be574b45556420eb0d9c6311795fbbe2b29424/gistfile1.txt")
                    .setPriority(Priority.LOW)
                    .build()
                    .getAsString(object : StringRequestListener {
                        override fun onResponse(response: String) {
                            RawParser().parseChanges(response)
                        }

                        override fun onError(error: ANError) {
                            Toast.makeText(applicationContext(), error.errorDetail, Toast.LENGTH_LONG).show()
                            Log.d(SphPlanner.TAG, error.errorBody)
                        }
                    })*/


        }

        view.findViewById<Button>(R.id.logoutButton).setOnClickListener {
            prefs.edit().remove("token").remove("token_last_success").apply()
        }

        val userEditText = view.findViewById<EditText>(R.id.userEditText)
        userEditText.setText(prefs.getString("user", ""))
        userEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                prefs.edit().putString("user", s.toString()).apply()
            }
        })

        val passwordEditText = view.findViewById<EditText>(R.id.passwordEditText)
        passwordEditText.setText(prefs.getString("password", ""))
        passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                prefs.edit().putString("password", s.toString()).apply()
            }
        })

        val idText = view.findViewById(R.id.idText) as TextView
        idText.text = prefs.getString("iCalLink", "")
        return view

    }
}