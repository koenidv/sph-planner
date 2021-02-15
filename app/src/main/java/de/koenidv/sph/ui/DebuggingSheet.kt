package de.koenidv.sph.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.debugging.Debugger
import de.koenidv.sph.networking.TokenManager

//  Created by koenidv on 16.12.2020.
// Bottom sheet to let the user select a dark/light theme and accent color combination
class DebuggingSheet internal constructor() : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.sheet_debugging, container, false)

        val debuggerSwitch = view.findViewById<SwitchMaterial>(R.id.debugLogSwitch)
        val debuggerView = view.findViewById<MaterialButton>(R.id.debugViewLog)
        val debuggerShare = view.findViewById<MaterialButton>(R.id.debugShareLog)

        // Adjust debugger switch to reflect logging state
        debuggerSwitch.isChecked = Debugger.DEBUGGING_ENABLED

        // Disable log view/share buttons if log is empty
        if (Debugger.isEmpty()) {
            debuggerView.isEnabled = false
            debuggerShare.isEnabled = false
        }

        // Enable or disable debugging
        debuggerSwitch.setOnCheckedChangeListener { _, enabled ->
            Debugger.setEnabled(enabled)
            if (enabled) dismiss()
        }

        // Copy the log and open the online debugger view
        debuggerView.setOnClickListener {
            Debugger.view()
            dismiss()
        }

        // Upload debug log to dogbin and share the link
        debuggerShare.setOnClickListener {
            Toast.makeText(requireContext(),
                    R.string.debugger_uploading, Toast.LENGTH_LONG).show()
            Debugger.share()
            dismiss()
        }

        // Export the default database
        view.findViewById<MaterialButton>(R.id.debugShareDatabase).setOnClickListener {
            // Get a uri to the file
            val fileToShare = requireContext().getDatabasePath("database")
            val uri = FileProvider.getUriForFile(requireContext(), requireContext().packageName + ".provider", fileToShare)
            // Create a chooser intent
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            // Display the chooser
            startActivity(Intent.createChooser(intent, ""))
            dismiss()
        }

        // Generate a new session token and share it
        view.findViewById<MaterialButton>(R.id.debugShareToken).setOnClickListener {
            Toast.makeText(requireContext(),
                    R.string.debug_token_generating, Toast.LENGTH_LONG).show()
            // Get a fresh token
            TokenManager.getToken(true) { _, token ->
                dismiss()
                // Share it
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, token.toString())
                    this.type = "text/plain"
                }
                val chooser = Intent.createChooser(sendIntent,
                        SphPlanner.applicationContext().getString(R.string.debugger_share_long))
                        .apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                requireContext().startActivity(chooser)
            }
        }

        // Done button
        view.findViewById<MaterialButton>(R.id.doneButton).setOnClickListener { dismiss() }

        return view
    }
}