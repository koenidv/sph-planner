package de.koenidv.sph

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import de.koenidv.sph.database.ChangesDb
import de.koenidv.sph.database.FunctionTilesDb
import de.koenidv.sph.debugging.Debugger
import de.koenidv.sph.networking.NetworkManager
import de.koenidv.sph.objects.FunctionTile
import de.koenidv.sph.ui.OnboardingActivity
import de.koenidv.sph.ui.OptionsSheet

//  Created by koenidv on 05.12.2020.

class MainActivity : AppCompatActivity() {

    lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onResume() {
        ChangesDb.instance!!.removeOld()
        super.onResume()


        // In-App updates via Play Core API
        val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(applicationContext)
        val appUpdateInfoTask: Task<AppUpdateInfo> = appUpdateManager.appUpdateInfo
        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                // Request the update
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.FLEXIBLE,
                            this,
                            100)

                    // Create a listener to track request state updates.
                    val listener = InstallStateUpdatedListener { state ->
                        // Show module progress, log state, or install the update.
                        if (state.installStatus() == InstallStatus.DOWNLOADED) {
                            Snackbar.make(
                                    findViewById(R.id.nav_host_fragment),
                                    R.string.update_downloaded,
                                    Snackbar.LENGTH_INDEFINITE)
                                    .setAnchorView(R.id.nav_view)
                                    .setAction(R.string.update_action) {
                                        appUpdateManager.completeUpdate()
                                    }.show()
                        }
                    }
                    appUpdateManager.registerListener(listener)
                } catch (mE: SendIntentException) {
                    mE.printStackTrace()
                }
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {

        val prefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE)
        var lastNavArguments: Bundle? = null

        // Apply custom accent color theme
        if (prefs.contains("themeRes")) setTheme(prefs.getInt("themeRes", R.style.Theme_SPH_Electric))
        // Apply custom dark / light theme
        AppCompatDelegate.setDefaultNightMode(when (prefs.getInt("forceDarkType", 1)) {
            -1 -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            0 -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_YES
        })

        // Navigate to OnboardingActivity if user hasn't completed setup yet
        if (!prefs.getBoolean("credsVerified", false)
                || !prefs.getBoolean("introComplete", false)) {
            // Enable debugging
            Debugger.setEnabled(true)
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
        navController.addOnDestinationChangedListener { _, _, args ->
            // Reset browser url on destination changed
            SphPlanner.openInBrowserUrl = null
            lastNavArguments = args
        }

        /*
         * Pull to refresh
         */
        swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            navController.currentDestination?.id?.let { destination ->
                // If destination is known, let network manager handle the refreshing
                NetworkManager().handlePullToRefresh(destination, lastNavArguments) { success ->
                    val errorSnackbar = Snackbar.make(findViewById<FragmentContainerView>(R.id.nav_host_fragment), "", Snackbar.LENGTH_LONG)
                    errorSnackbar.setAnchorView(R.id.nav_view)
                    // Show error message if needed
                    when (success) {
                        NetworkManager.FAILED_NO_NETWORK -> errorSnackbar.setText(R.string.error_offline).show()
                        NetworkManager.FAILED_MAINTENANCE -> errorSnackbar.setText(R.string.error_maintenance).show()
                        NetworkManager.FAILED_SERVER_ERROR -> errorSnackbar.setText(R.string.error_server).show()
                        NetworkManager.FAILED_UNKNOWN, NetworkManager.FAILED_CANCELLED ->
                            errorSnackbar.setText(R.string.error).show()
                        else -> if (success != NetworkManager.SUCCESS) errorSnackbar.setText(R.string.error).show()
                    }
                    // If this is due to a server error, display a link to sph's status page
                    if (success == NetworkManager.FAILED_MAINTENANCE
                            || success == NetworkManager.FAILED_SERVER_ERROR
                            || success == NetworkManager.FAILED_UNKNOWN) {
                        errorSnackbar.setAction(R.string.sph_status) {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_status))))
                        }
                    }
                    // Indicate no longer refreshing
                    swipeRefresh.isRefreshing = false
                    if (success == NetworkManager.FAILED_INVALID_CREDENTIALS) {
                        // Credentials seem to be invalid
                        // Show sign in screen
                        prefs.edit().remove("credsVerified").apply()
                    }
                }
            }
        }

        /*
         * Hide messages tab if messages are not supported
         */
        if (!FirebaseRemoteConfig.getInstance().getBoolean("messages_enabled") ||
                !FunctionTilesDb.getInstance().supports(FunctionTile.FEATURE_MESSAGES)) {
            navView.menu.findItem(R.id.nav_messages).isVisible = false
        }

        // Save theme color to use somewhere without application context
        // Get theme color
        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
        prefs.edit().putInt("themeColor", typedValue.data).apply()
        theme.resolveAttribute(R.attr.backgroundColor, typedValue, true)
        prefs.edit().putInt("backgroundColor", typedValue.data).apply()

    }

    // Create options menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.openInBrowserItem) {
            // Open in browser
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
                    .putLong("updated_posts", 0).apply()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    companion object {
        /**
         * Prompt user to use AutoSPH if he hasn't accepted this before
         * Otherwise just open SPH in browser for manual login
         */
        fun openSph(context: Context) {
            val prefs = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE)
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
                context.startActivity(autoLoginIntent)
            } else {
                AlertDialog.Builder(context)
                        .setTitle(R.string.menu_open_sph_warning_title)
                        .setMessage(R.string.menu_open_sph_warning_description)
                        .setPositiveButton(R.string.menu_open_sph_warning_yes) { _, _ -> context.startActivity(autoLoginIntent); prefs.edit().putBoolean("open_sph_accepted_auto", true).apply() }
                        .setNegativeButton(R.string.menu_open_sph_warning_no) { _, _ -> context.startActivity(manualIntent) }
                        .show()
            }
        }
    }
}