package de.koenidv.sph.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.debugging.DebugLog
import me.saket.bettermovementmethod.BetterLinkMovementMethod

//  Created by koenidv in December 2020.
@Suppress("unused") // Is actually used by OnboardingActivity
class OnboardingLegalFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_onboarding_legal, container, false)
        val prefs = SphPlanner.appContext().getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)

        // If user already agreed to the legal stuff but then cancelled the setup,
        // continue to the next step
        if (prefs.getBoolean("legalVerified", false)) {
            // If logging is enabled, log this
            DebugLog("LegalFrag", "Legal already verified, skipping")
            val ft = parentFragmentManager.beginTransaction()
            ft.replace(R.id.fragment, OnboardingSigninFragment()).commit()
            return null
        }

        val liabilityCheck = view.findViewById<CheckBox>(R.id.legalLiabilityCheckBox)
        val sourcesCheck = view.findViewById<CheckBox>(R.id.legalSourcesCheckBox)
        val privacyCheck = view.findViewById<CheckBox>(R.id.legalPrivacyCheckBox)

        privacyCheck.movementMethod = BetterLinkMovementMethod.getInstance()
        val ft = parentFragmentManager.beginTransaction()

        // If all checkboxes are checked, update sharedprefs and head to next fragment
        val continueCheck = { _: CompoundButton, _: Boolean ->
            if (liabilityCheck.isChecked && sourcesCheck.isChecked && privacyCheck.isChecked) {
                prefs.edit().putBoolean("legalVerified", true).apply()
                // If logging is enabled, log this
                DebugLog("LegalFrag", "Verified legal conditions")
                ft.replace(R.id.fragment, OnboardingSigninFragment()).commit()
            }
        }

        // Continue to the next fragment once all conditions are checked
        liabilityCheck.setOnCheckedChangeListener(continueCheck)
        sourcesCheck.setOnCheckedChangeListener(continueCheck)
        privacyCheck.setOnCheckedChangeListener(continueCheck)

        return view
    }

}