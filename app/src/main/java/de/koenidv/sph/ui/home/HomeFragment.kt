package de.koenidv.sph.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import de.koenidv.sph.R
import de.koenidv.sph.networking.NetworkManager

class HomeFragment : Fragment() {

    //private var homeViewModel: HomeViewModel? = null

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        /*homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        val textView = root.findViewById<TextView>(R.id.text_home)
        homeViewModel!!.text.observe(viewLifecycleOwner, Observer { s -> textView.text = s })*/

        //val binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false);
        val view = inflater.inflate(R.layout.fragment_home, container, false);

        val loginButton = view.findViewById<Button>(R.id.loginButton);
        loginButton.setOnClickListener {
            val networkManager = NetworkManager()
            networkManager.getAccessToken()
        }

        return view;

    }
}