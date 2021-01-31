package de.koenidv.sph.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.koenidv.sph.R

//  Created by koenidv on 29.12.2020.
// Bottom sheet displaying an image and text
class InfoSheet internal constructor(
        @DrawableRes private val image: Int,
        @StringRes private val text: Int) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.sheet_info, container, false)

        // Set image
        view.findViewById<ImageView>(R.id.infoImageView).setImageDrawable(
                ContextCompat.getDrawable(requireContext(), image)
        )

        // Set text
        view.findViewById<TextView>(R.id.infoTextView).setText(text)

        // Done button
        view.findViewById<Button>(R.id.doneButton).setOnClickListener { dismiss() }

        return view
    }
}