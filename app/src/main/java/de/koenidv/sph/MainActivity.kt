package de.koenidv.sph

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import de.koenidv.sph.ui.OnboardingActivity
import de.koenidv.sph.ui.OptionsSheet

//  Created by koenidv on 05.12.2020.

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        val prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE)

        // Apply custom accent color theme
        if (prefs.contains("themeRes")) setTheme(prefs.getInt("themeRes", R.style.Theme_SPH_Electric))
        // Apply custom dark / light theme
        if (prefs.contains("forceDark")
                || AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_UNSPECIFIED)
            if (prefs.getBoolean("forceDark", true)) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        // Navigate to OnboardingActivity if user hasn't completed setup yet
        if (!prefs.getBoolean("credsVerified", false)
                || !prefs.getBoolean("introComplete", false)) {
            startActivity(Intent(this, OnboardingActivity().javaClass).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            finish()
        }

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val navView = findViewById<BottomNavigationView>(R.id.nav_view)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_courses, R.id.nav_messages, R.id.nav_explore)
                .build()
        val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment?
        val navController = navHostFragment!!.navController
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(navView, navController)
        navController.addOnDestinationChangedListener { _, _, _ ->
            // Reset browser url on destination changed
            SphPlanner.openInBrowserUrl = null
        }

        // Save theme color to use somewhere without application context
        // Get theme color
        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
        prefs.edit().putInt("themeColor", typedValue.data).apply()

    }

    // Create options menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.openInBrowserItem) {
            // Open in browser
            val nav = findNavController(R.id.nav_host_fragment)
            if (SphPlanner.openInBrowserUrl == null) {
                // Open SPH start page
                openSph(this)
            } else {
                // Open set url in browser
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SphPlanner.openInBrowserUrl)))
            }
        } else if (item.itemId == R.id.optionsItem) {
            // Show a bottom sheet with information and options
            val optionsSheet = OptionsSheet()
            optionsSheet.show(supportFragmentManager, "optionsSheet")
            // Reset token timer
            getSharedPreferences("sharedPrefs", MODE_PRIVATE).edit()
                    .putLong("token_last_success", 0).apply()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun openSph(context: Context) {
        val prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE)
        // Open koenidv's autosph to log browser in to sph
        // This will transfer user data to an external server
        // Ask user if wants this or log in manually
        val uri = Uri.parse("https://koenidv.de/autosph?direct="
                + prefs.getString("schoolid", "") + "."
                + prefs.getString("user", "")
                + "&" + prefs.getString("password", ""))
        val autoLoginIntent = Intent(Intent.ACTION_VIEW, uri)
        val manualIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://start.schulportal.hessen.de/"))

        // Auto log in if user has accepted before and network is trusted
        if (prefs.getBoolean("open_sph_accepted_auto", false)
                && NetworkCapabilities().hasCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)) {
            startActivity(autoLoginIntent)
        } else {
            AlertDialog.Builder(context)
                    .setTitle(R.string.menu_open_sph_warning_title)
                    .setMessage(R.string.menu_open_sph_warning_description)
                    .setPositiveButton(R.string.menu_open_sph_warning_yes) { _, _ -> startActivity(autoLoginIntent); prefs.edit().putBoolean("open_sph_accepted_auto", true).apply() }
                    .setNegativeButton(R.string.menu_open_sph_warning_no) { _, _ -> startActivity(manualIntent) }
                    .show()
        }
    }
}