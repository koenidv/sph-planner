package de.koenidv.sph.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.koenidv.sph.R
import de.koenidv.sph.objects.Task


//  Created by koenidv on 14.02.2021.
/**
 * Bottom sheet showing options to edit or create a task
 * @param task Specify to edit a task, null for creating a new one
 */

class EditTaskSheet(private val task: Task? = null,
                    private val callback: (Task) -> Unit) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.sheet_edit_task, container, false)



        return view
    }

}