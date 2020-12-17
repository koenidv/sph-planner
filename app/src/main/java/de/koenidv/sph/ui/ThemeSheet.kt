package de.koenidv.sph.ui

import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButtonToggleGroup
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner

//  Created by koenidv on 16.02.2020.
// Bottom sheet to let the user select a dark/light theme and accent color combination
class ThemeSheet internal constructor() : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.sheet_theme, container, false)
        val prefs = SphPlanner.applicationContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        // Disable follow system option below Android 10
        if (Build.VERSION.SDK_INT < 29) {
            view.findViewById<Button>(R.id.themeSystemButton).visibility = View.GONE
        }

        // Dark mode toggle group
        val darkToggleGroup: MaterialButtonToggleGroup = view.findViewById(R.id.darkmodeToggleGroup)
        // Check currently active dark theme selection
        if (prefs.contains("forceDark"))
            if (prefs.getBoolean("forceDark", true)) darkToggleGroup.check(R.id.themeDarkButton)
            else darkToggleGroup.check(R.id.themeLightButton)
        else darkToggleGroup.check(R.id.themeSystemButton)
        // Put in editor on click
        darkToggleGroup.addOnButtonCheckedListener { _: MaterialButtonToggleGroup?, checkedId: Int, isChecked: Boolean ->
            if (isChecked) {
                if (checkedId == R.id.themeSystemButton)
                    editor.remove("forceDark").apply()
                else
                    editor.putBoolean("forceDark", checkedId == R.id.themeDarkButton).apply()
            }
        }

        // Accent color toggle group
        val accentToggleGroup: MaterialButtonToggleGroup = view.findViewById(R.id.accentToggleGroup)
        // Check currently active accent color selection
        when (prefs.getInt("themeRes", R.style.Theme_SPH_Electric)) {
            R.style.Theme_SPH_Summer -> accentToggleGroup.check(R.id.accentSummerButton)
            R.style.Theme_SPH_Autumn -> accentToggleGroup.check(R.id.accentAutumnButton)
            R.style.Theme_SPH_Monochrome -> accentToggleGroup.check(R.id.accentMonochromeButton)
            R.style.Theme_SPH_Electric -> accentToggleGroup.check(R.id.accentElectricButton)
            else -> accentToggleGroup.check(R.id.accentElectricButton)
        }
        // Put in editor on click
        accentToggleGroup.addOnButtonCheckedListener { _: MaterialButtonToggleGroup?, checkedId: Int, isChecked: Boolean ->
            if (isChecked) {
                when (checkedId) {
                    R.id.accentElectricButton -> editor.putInt("themeRes", R.style.Theme_SPH_Electric).apply()
                    R.id.accentSummerButton -> editor.putInt("themeRes", R.style.Theme_SPH_Summer).apply()
                    R.id.accentAutumnButton -> editor.putInt("themeRes", R.style.Theme_SPH_Autumn).apply()
                    R.id.accentMonochromeButton -> editor.putInt("themeRes", R.style.Theme_SPH_Monochrome).apply()
                    else -> editor.putInt("themeRes", R.style.Theme_SPH_Electric).apply()
                }
            }
        }

        // Save button
        view.findViewById<Button>(R.id.doneButton).setOnClickListener { dismiss() }

        return view
    }

    override fun onDismiss(dialog: DialogInterface) {
        requireActivity().recreate()
        super.onDismiss(dialog)
    }
}