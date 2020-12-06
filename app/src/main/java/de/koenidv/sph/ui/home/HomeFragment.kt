package de.koenidv.sph.ui.home

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import de.koenidv.sph.networking.NetworkManager


class HomeFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val prefs: SharedPreferences = applicationContext().getSharedPreferences("sharedPrefs", MODE_PRIVATE)

        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Only for demonstration

        val loginButton = view.findViewById<Button>(R.id.loginButton)
        loginButton.setOnClickListener {
            val networkManager = NetworkManager()
            networkManager.getAccessToken()
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