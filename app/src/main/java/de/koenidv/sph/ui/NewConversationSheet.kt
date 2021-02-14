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
import com.google.android.material.textfield.TextInputLayout
import de.koenidv.sph.R
import de.koenidv.sph.adapters.UserSelectionAdapter
import de.koenidv.sph.database.UsersDb


//  Created by koenidv on 29.12.2020.
// Bottom sheet displaying an image and text
class NewConversationSheet internal constructor() : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.sheet_newconversation, container, false)

        val subjectContainer = view.findViewById<TextInputLayout>(R.id.subjectInputLayout)
        val subject = view.findViewById<EditText>(R.id.subjectEditText)
        val recipients = view.findViewById<EditText>(R.id.recipientsEditText)
        val recipientsRecycler = view.findViewById<RecyclerView>(R.id.recipientsRecycler)

        // Display users list
        val users = UsersDb.all()
        val recipAdapter = UserSelectionAdapter(users) { recipients.setText("") }
        recipientsRecycler.adapter = recipAdapter

        // Filter the recyclerview on user input
        recipients.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                recipAdapter.filter(s.toString().trim())
            }
        })

        subject.requestFocus()

        return view
    }
}