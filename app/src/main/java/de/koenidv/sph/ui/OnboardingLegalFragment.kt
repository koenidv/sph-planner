package de.koenidv.sph.ui

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner

class OnboardingLegalFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_onboarding_legal, container, false)
        val prefs = SphPlanner.applicationContext().getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)

        // If user already agreed to the legal stuff but then cancelled the setup,
        // continue to the next step
        if (prefs.getBoolean("legalVerified", false)) {
            val ft = parentFragmentManager.beginTransaction()
            ft.replace(R.id.fragment, OnboardingSigninFragment()).commit()
            return null
        }

        val liabilityCheck = view.findViewById<CheckBox>(R.id.legalLiabilityCheckBox)
        val sourcesCheck = view.findViewById<CheckBox>(R.id.legalSourcesCheckBox)
        val privacyCheck = view.findViewById<CheckBox>(R.id.legalPrivacyCheckBox)

        privacyCheck.movementMethod = LinkMovementMethod.getInstance()
        val ft = parentFragmentManager.beginTransaction()

        // Continue to the next fragment once all conditions are checked
        liabilityCheck.setOnCheckedChangeListener { _, _ ->
            if (liabilityCheck.isChecked && sourcesCheck.isChecked && privacyCheck.isChecked) {
                prefs.edit().putBoolean("legalVerified", true).apply()
                ft.replace(R.id.fragment, OnboardingSigninFragment()).commit()
            }
        }
        sourcesCheck.setOnCheckedChangeListener { _, _ ->
            if (liabilityCheck.isChecked && sourcesCheck.isChecked && privacyCheck.isChecked) {
                prefs.edit().putBoolean("legalVerified", true).apply()
                ft.replace(R.id.fragment, OnboardingSigninFragment()).commit()
            }
        }
        privacyCheck.setOnCheckedChangeListener { _, _ ->
            if (liabilityCheck.isChecked && sourcesCheck.isChecked && privacyCheck.isChecked) {
                prefs.edit().putBoolean("legalVerified", true).apply()
                ft.replace(R.id.fragment, OnboardingSigninFragment()).commit()
            }
        }

        // Prefetch school picker
        // Next stop will load faster if user stayed long enough in this fragment
        /*AndroidNetworking.get("https://start.schulportal.hessen.de/")
                .setPriority(Priority.LOW)
                .build()
                .prefetch()*/
        // Useless, sph disallows cache, even on public pages

        return view
    }

}