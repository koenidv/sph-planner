package de.koenidv.sph.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import de.koenidv.sph.R
import de.koenidv.sph.networking.NetworkManager


class HomeFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Only for demonstration

        val loginButton = view.findViewById<Button>(R.id.signinButton)
        loginButton.setOnClickListener {
            /*TokenManager().generateAccessToken {
                Toast.makeText(applicationContext(), "Success", Toast.LENGTH_LONG).show()
            }*/
            NetworkManager().createIndex(object : NetworkManager.DoneListener {
                override fun onComplete(success: Boolean) {
                    Toast.makeText(context, "Success?", Toast.LENGTH_LONG).show()
                }
            })
        }

        return view

    }
}