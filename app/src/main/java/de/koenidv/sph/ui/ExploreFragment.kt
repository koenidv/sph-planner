package de.koenidv.sph.ui

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.gridlayout.widget.GridLayout
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.adapters.ExploreLinksAdapter
import de.koenidv.sph.database.FunctionTilesDb
import de.koenidv.sph.objects.FunctionTile


class ExploreFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_explore, container, false)
        val nav = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        val features = FunctionTilesDb.getInstance()

        /*
         * Collections
         */

        val gridLayout: GridLayout = view.findViewById(R.id.exploreGrid)
        val timetableText = view.findViewById<TextView>(R.id.timetableTextView)
        val postsText = view.findViewById<TextView>(R.id.postsTextView)
        val attachmentsText = view.findViewById<TextView>(R.id.attachmentsTextView)
        val tasksText = view.findViewById<TextView>(R.id.tasksTextView)
        val changesText = view.findViewById<TextView>(R.id.changesTextView)
        val usersText = view.findViewById<TextView>(R.id.usersTextView)

        // Set on click listeners, open respective fragment

        // Timetable
        if (features.supports(FunctionTile.FEATURE_TIMETABLE)) {
            timetableText.setOnClickListener {
                // Open timetable
                nav.navigate(R.id.timetableFromExploreAction)
            }
        } else gridLayout.removeView(timetableText)

        // Posts
        if (features.supports(FunctionTile.FEATURE_COURSES)) {
            postsText.setOnClickListener {
                // Open all posts
                nav.navigate(R.id.allPostsFromExploreAction)
            }
        } else gridLayout.removeView(postsText)

        // Attachments WORK IN PROGRESS, therefore hidden
        @Suppress("SimplifyBooleanWithConstants")
        if (false && features.supports(FunctionTile.FEATURE_COURSES)) {
            attachmentsText.setOnClickListener {
                // Open attachments
                //nav.navigate(R.id.attachmentsFromExploreAction)
                nav.navigate(R.id.frag_placeholder)
            }
        } else gridLayout.removeView(attachmentsText)

        // Tasks
        if (features.supports(FunctionTile.FEATURE_COURSES)) {
            tasksText.setOnClickListener {
                nav.navigate(R.id.tasksFromExploreAction)
            }
        } else gridLayout.removeView(tasksText)

        // Changes
        if (features.supports(FunctionTile.FEATURE_CHANGES)) {
            changesText.setOnClickListener {
                nav.navigate(R.id.changesFromExploreAction)
            }
        } else gridLayout.removeView(changesText)

        // Users WORK IN PROGRESS, therefore hidden
        @Suppress("SimplifyBooleanWithConstants")
        if (false && features.supports(FunctionTile.FEATURE_MESSAGES)) {
            usersText.setOnClickListener {
                nav.navigate(R.id.usersFromExploreAction)
            }
        } else gridLayout.removeView(usersText)


        /*
         * Links
         */

        val linksRecycler = view.findViewById<RecyclerView>(R.id.linksRecycler)

        // Get all tiles that are not displayed within the app itself
        val tiles = FunctionTilesDb.getInstance().getFunctionsByType("other").sortedBy { it.name }.toMutableList()
        // Add start page item at the end of the list. Might remove this later
        val colorValue = TypedValue()
        requireContext().theme.resolveAttribute(R.attr.colorPrimary, colorValue, true)
        tiles.add(FunctionTile(
                getString(R.string.tile_startpage),
                "https://start.schulportal.hessen.de/index.php",
                "start",
                "home",
                colorValue.data
        ))
        // Set up links recycler
        val linksAdapter = ExploreLinksAdapter(tiles) {
            // If it's SchoolMoodle and the respective app is installes, open it
            // This should be handled differently, but there aren't really any now
            val moodleIntent = requireContext().packageManager.getLaunchIntentForPackage("com.moodle.moodlemobile")
            if (it.name == "SchulMoodle" && moodleIntent != null) {
                startActivity(moodleIntent)
            } else {
                // Open WebViewFragment with respective url on click
                nav.navigate(R.id.webviewFromExploreAction, bundleOf("url" to it.location))
                // Set action bar title
                (activity as AppCompatActivity).supportActionBar?.title = it.name
                SphPlanner.openInBrowserUrl = it.location
            }
        }
        linksRecycler.layoutManager = LinearLayoutManager(requireContext())
        linksRecycler.adapter = linksAdapter

        return view
    }

}