package de.koenidv.sph

import android.content.Intent
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
        if (item.itemId == R.id.optionsItem) {
            // Show a bottom sheet with information and options
            val optionsSheet = OptionsSheet()
            optionsSheet.show(supportFragmentManager, "optionsSheet")
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}