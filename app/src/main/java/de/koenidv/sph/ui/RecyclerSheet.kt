package de.koenidv.sph.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.koenidv.sph.R

//  Created by koenidv on 29.12.2020.
abstract class RecyclerSheet : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.sheet_recycler, container, false)

        val recycler = view.findViewById<RecyclerView>(R.id.recycler)
        setupRecycler(recycler)

        val doneButton = view.findViewById<Button>(R.id.doneButton)
        doneButton.setOnClickListener { dismiss() }

        return view
    }

    abstract fun setupRecycler(recycler: RecyclerView)
}