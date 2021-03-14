package de.koenidv.sph.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.adapters.UserSelectionAdapter
import de.koenidv.sph.database.UsersDb
import de.koenidv.sph.networking.Users
import de.koenidv.sph.objects.User


//  Created by koenidv on 14.02.2021.
// Bottom sheet showing options to start a new conversation
class CreateConversationSheet(private val preselect: User? = null,
                              private val callback: (String, List<String>) -> Unit) :
        BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.sheet_create_conversation, container, false)

        val subject = view.findViewById<EditText>(R.id.subjectEditText)
        val recipients = view.findViewById<EditText>(R.id.recipientsEditText)
        val recipientsRecycler = view.findViewById<RecyclerView>(R.id.recipientsRecycler)
        val createButton = view.findViewById<MaterialButton>(R.id.createButton)

        // Display users list
        val users = UsersDb.all()
        val recipAdapter = UserSelectionAdapter(users, preselect?.userId) { recipients.setText("") }
        recipientsRecycler.adapter = recipAdapter

        // Filter the recyclerview on user input
        recipients.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                recipAdapter.filter(s.toString().trim())
            }
        })

        createButton.setOnClickListener {
            val subjectText = subject.text.toString()
            val recipList = recipAdapter.getSelected()

            // Require a subject
            if (subjectText.isEmpty()) {
                subject.error = getString(R.string.messages_new_subject)
                return@setOnClickListener
            }

            // Require recipients
            if (recipList.isEmpty()) {
                recipients.error = getString(R.string.messages_new_recipients)
                return@setOnClickListener
            }

            // Call back to start new conversation
            callback(subjectText, recipList.map {
                if (it.userId.startsWith("l-")) it.userId
                else "l-${it.userId}"
            })
            dismiss()

        }

        subject.requestFocus()

        // If there is a preset user set, also display an email button
        if (preselect != null) {
            val fragmentManager = parentFragmentManager
            val emailButton = view.findViewById<MaterialButton>(R.id.emailButton)
            emailButton.visibility = View.VISIBLE
            emailButton.setOnClickListener {
                // If email address template has been set
                if (SphPlanner.prefs.getString("users_mail_template", null) != null) {
                    dismiss()
                    Users().sendEmail(preselect)
                } else {
                    dismiss()
                    // Else show two sheets prompting the user to add a template
                    InfoSheet(R.drawable.img_email, R.string.email_template_info) {
                        EmailTemplateSheet().show(fragmentManager, "email-preset-sheet")
                    }.show(fragmentManager, "info-email")
                }
            }
        }

        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(STYLE_NORMAL, R.style.Theme_SPH_Sheet_NoBackground)
        super.onCreate(savedInstanceState)
    }
}