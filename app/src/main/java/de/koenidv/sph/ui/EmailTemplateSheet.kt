package de.koenidv.sph.ui

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import de.koenidv.sph.R
import java.util.*

//  Created by koenidv on 29.12.2020.
class EmailTemplateSheet : BottomSheetDialogFragment() {

    lateinit var input: EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.sheet_mail_template, container, false)
        val prefs = requireContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)

        input = view.findViewById(R.id.templateEditText)
        val chipGroup = view.findViewById<ChipGroup>(R.id.presetsChipGroup)
        val doneButton = view.findViewById<Button>(R.id.doneButton)

        // We're using a selection chip group as button group so new presets
        // can be easily added via xml
        chipGroup.setOnCheckedChangeListener { group, checkedId ->
            val checked = group.findViewById<Chip>(checkedId)
            if (checked != null && checked.isChecked) {
                input.append(checked.tag.toString())
                checked.isChecked = false
            }
        }
        chipGroup.isSingleSelection = true

        doneButton.setOnClickListener { dismiss() }

        // Show current template
        input.setText(prefs.getString(
                "users_mail_template", getString(R.string.email_template_default)))
        input.requestFocus()

        return view
    }

    override fun onDismiss(dialog: DialogInterface) {
        // todo validate email input
        // Validate input
        if ("""\S+@\S+\.\w{2,8}""".toRegex().matches(input.text.toString())) {
            // Save the input to SharedPrefs
            requireContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE).edit()
                    .putString("users_mail_template", input.text.toString().toLowerCase(Locale.ROOT))
                    .apply()
            super.onDismiss(dialog)
        } else {
            // Display an error
            input.error = "test"
        }
    }
}