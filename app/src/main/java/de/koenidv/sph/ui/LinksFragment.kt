package de.koenidv.sph.ui

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import de.koenidv.sph.R


class LinksFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_links, container, false)


        (view.findViewById<TextView>(R.id.iconTextView).background as StateListDrawable).setColorFilter(Color.parseColor("#f44336"), PorterDuff.Mode.SRC_ATOP)
        view.findViewById<TextView>(R.id.nameTextView).text = "Testelement!"

        view.findViewById<LinearLayout>(R.id.itemLayout).setOnClickListener { Toast.makeText(context, "Hey", Toast.LENGTH_SHORT).show() }


        return view
    }


}