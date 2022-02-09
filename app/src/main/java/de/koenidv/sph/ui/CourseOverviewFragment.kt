package de.koenidv.sph.ui

import android.content.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.Navigation
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.adapters.AttachmentsAdapter
import de.koenidv.sph.adapters.PostsAdapter
import de.koenidv.sph.adapters.SchedulesAdapter
import de.koenidv.sph.database.*
import de.koenidv.sph.networking.AttachmentManager
import de.koenidv.sph.networking.Tasks
import de.koenidv.sph.objects.Attachment
import de.koenidv.sph.objects.Course
import de.koenidv.sph.objects.Lesson
import de.koenidv.sph.objects.Post
import me.saket.bettermovementmethod.BetterLinkMovementMethod


// Created by koenidv on 18.12.2020.
// Adapted JAN-22 StKl
class CourseOverviewFragment : Fragment() {

    lateinit var courseId: String
    var isExanded = false
    lateinit var postsRecycler: RecyclerView
    lateinit var posts: MutableList<Post>
    lateinit var postsToShow: MutableList<Post>

    // Refresh whenever the broadcast "uichange" is received
    private val uichangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Only do something posts were updated
            // This includes attachments, tasks
            if (intent.getStringExtra("content") == "posts") {
                val allPosts: List<Post> = if (!isExanded) {
                    // Not expanded, get 5 latest posts
                    PostsDb.getInstance().getByCourseId(courseId, 5)
                } else {
                    // Expanded, get all posts
                    PostsDb.getInstance().getByCourseId(courseId)
                }
                // If there wasn't any post before but now is,
                // it's easier to recreate the fragment
                if (allPosts.isNotEmpty() && postsRecycler.adapter == null) {
                    parentFragmentManager.beginTransaction()
                            .replace(R.id.nav_host_fragment,
                                    CourseOverviewFragment().also {
                                        it.arguments = bundleOf("courseId" to courseId)
                                    })
                            .setReorderingAllowed(true) //Optimizing state changes for better transitions
                            .commit()
                    return
                }
                // Add all new posts to the posts list
                // Removed posts will not be removed
                // That shouldn't happen all to often and if it does,
                // the changes will be reflected once the fragment is recreated
                var changed = 0
                // New posts will most probably be the latest ones
                // Even if not, it's good to have freshly fetched posts at the top of the list
                for (post in allPosts) {
                    if (posts.indexOfFirst { it.postId == post.postId } == -1) {
                        posts.add(0, post)
                        changed++
                    }
                }
                // If anything changed, update recyclerview
                // We can assume that posts are added to the top
                if (changed > 0) {
                    // Add all posts that should be displayed
                    postsToShow.clear()
                    if (!isExanded) {
                        postsToShow.addAll(posts.take(2))
                        // Mark displayed posts as read
                        PostsDb.getInstance().markAsRead(postsToShow[0].postId, postsToShow.getOrNull(1)?.postId)
                        // todo properly update recyclerview
                        if (changed == 1) {
                            postsRecycler.adapter?.notifyItemRangeRemoved(5 - changed, 1)
                            postsRecycler.adapter?.notifyItemRangeInserted(0, changed)
                        } else postsRecycler.adapter?.notifyDataSetChanged()
                    } else {
                        postsToShow.addAll(posts)
                        postsRecycler.adapter?.notifyDataSetChanged()
                        // Mark all posts as read
                        PostsDb.getInstance().markCourseAsRead(courseId)
                    }
                }

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Register to receive messages.
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(uichangeReceiver,
                IntentFilter("uichange"))
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_course_overview, container, false)
        val prefs: SharedPreferences = SphPlanner.appContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)

        val pinsSchedText = view.findViewById<TextView>(R.id.pinsScheduleTextView)
        val pinsSchedRecycler = view.findViewById<RecyclerView>(R.id.pinsScheduleRecycler)

        val pinsTitleText = view.findViewById<TextView>(R.id.pinsTitleTextView)
        val pinsRecycler = view.findViewById<RecyclerView>(R.id.pinsRecycler)
        val postsTitleText = view.findViewById<TextView>(R.id.postsTitleTextView)
        postsRecycler = view.findViewById(R.id.postsRecycler)
        val postsLoading = view.findViewById<ProgressBar>(R.id.postsLoading)
        val loadMorePostsButton = view.findViewById<MaterialButton>(R.id.loadMorePostsButton)

        // Get passed course id argument
        courseId = arguments?.getString("courseId") ?: ""
        val course: Course? = CoursesDb.getByInternalId(courseId)

        // Set action bar title
        val appendix = if (course?.isLK == true) getString(R.string.course_appendix_lk) else ""
        if(!course?.fullname.isNullOrEmpty()) {
            (activity as AppCompatActivity).supportActionBar?.title =
                course?.fullname + appendix
        }
        else {
            (activity as AppCompatActivity).supportActionBar?.title =
                course?.courseId + appendix
        }
        // Set open in browser url
        SphPlanner.openInBrowserUrl = getString(R.string.url_course_overview).replace("%numberid", course?.number_id.toString())

        // Get data
        posts = PostsDb.getInstance().getByCourseId(courseId).toMutableList()
        // Get file attachments..
        val pins = FileAttachmentsDb.getInstance().getPinsByCourseId(courseId).toMutableList()
        // ..and link attachments
        pins.addAll(LinkAttachmentsDb.getInstance().getPinsByCourseId(courseId))
        // We need to reorder pins as files and links should be mixed, according to their last use
        pins.sortByDescending { it.lastuse() }

        // Initialize inline function so we can use it in onAttachmentLongClick,
        // but also attach the adapter from there
        var attachPinsAdapter: () -> Unit = {}

        // Prepare listeners
        val onAttachmentClick = AttachmentManager().onAttachmentClick(requireActivity()) { _: Int, _: Attachment ->
            // Currently, it's not really worth updating the pinned list on click
        }
        val onAttachmentLongClick = AttachmentManager().onAttachmentLongClick(requireActivity()) { action: Int, attachment: Attachment ->
            // Update ui according to selected action
            when (action) {
                AttachmentManager.ATTACHMENT_USED_PIN -> {
                    // Attachment was used.
                    // Move it to the front
                    val index = pins.indexOf(attachment)
                    pins.removeAt(index)
                    pins.add(0, attachment)
                    // Notify the adapter
                    pinsRecycler.adapter?.notifyItemMoved(index, 0)
                }
                AttachmentManager.ATTACHMENT_PINNED -> {
                    // Make title and recycler visible if this is the first pin
                    if (pinsTitleText.visibility == View.GONE) {
                        pinsTitleText.visibility = View.VISIBLE
                        pinsRecycler.visibility = View.VISIBLE
                        attachPinsAdapter()
                    }
                    // New attachment pinned
                    // Add it to the front
                    pins.add(0, attachment)
                    // Notify the adapter
                    pinsRecycler.adapter?.notifyItemInserted(0)
                }
                AttachmentManager.ATTACHMENT_UNPINNED -> {
                    // Attachment no longer pinned
                    // Remove it from the list
                    val index = pins.indexOf(attachment)
                    pins.removeAt(index)
                    // Notify the adapter
                    pinsRecycler.adapter?.notifyItemRemoved(index)
                    // Hide title and recycler if this was the last pin
                    if (pins.isEmpty()) {
                        pinsTitleText.visibility = View.GONE
                        pinsRecycler.visibility = View.GONE
                    }
                }
                AttachmentManager.ATTACHMENT_RENAMED_PIN -> {
                    // Update item in pins recycler
                    val pinsIndex = pins.indexOfFirst { it.attachId() == attachment.attachId() }
                    pins[pinsIndex] = attachment
                    pinsRecycler.adapter?.notifyItemChanged(pinsIndex)
                    // Notify posts recycler about changed data
                    val postsIndex = posts.indexOfFirst { it.postId == attachment.postId() }
                    postsRecycler.adapter?.notifyItemChanged(postsIndex)
                }
                AttachmentManager.ATTACHMENT_RENAMED -> {
                    // Notify posts recycler about changed data
                    val postsIndex = posts.indexOfFirst { it.postId == attachment.postId() }
                    postsRecycler.adapter?.notifyItemChanged(postsIndex)
                }
            }

        }

        /*
         * Pinned attachments recycler
         */

        attachPinsAdapter = {
            pinsRecycler.adapter = AttachmentsAdapter(pins,
                    onAttachmentClick,
                    onAttachmentLongClick)
            PagerSnapHelper().attachToRecyclerView(pinsRecycler)
        }

        if (!pins.isNullOrEmpty()) {
            attachPinsAdapter()
        } else {
            // Hide recycler and title if there are no pinned attachments
            pinsTitleText.visibility = View.GONE
            pinsRecycler.visibility = View.GONE
        }

        /*
         * Pinned schedules recycler
         */

        //Primary loop for data
        var sched = SchedulesDb.getSchedForCourse(course?.named_id).toMutableList()//courseId

        //Extra loop fr data
        //course_id => stunde.. in timetable => Damit nochmal schedule abfragen
        //if title null dann course.id

        //timetable entries with course ID
        val tmTblEntries1 = TimetableDb.instance!!.getForCourse(course?.courseId)
        //search timetable entries with same day and hour to check alternative search string
        var tmTblEntries2: MutableList<Lesson> = mutableListOf()
        for (item in tmTblEntries1) {
            tmTblEntries2.addAll(TimetableDb.instance!!.getCoursesWithDayHour(item.day, item.hour))
        }
        tmTblEntries2 = tmTblEntries2.distinct().toMutableList()

        //now I can check in this list for all returned id_course (e.g. rel ka) and search with first letters in schedule db gain for add. stuff
        //attetion! NOT to do in case of one letter only - e.g. d_wz_1 for Deutsch
        for (item in tmTblEntries2) {
            var spprtStr = item.idCourse//rel ka OR e_wz_1 OR bio_tur
            spprtStr = spprtStr.replace(
                "[^a-zA-Z]".toRegex(), /*NOT a letter*/
                "#"
            )
            spprtStr = spprtStr.substring(0, spprtStr.indexOf("#"))
            if(spprtStr.length >= 2) sched.addAll(SchedulesDb.getSchedForCourse(spprtStr).toMutableList())
        }
        sched = sched.distinctBy { it.nme }.toMutableList()

        if (!sched.isNullOrEmpty()) {
            pinsSchedRecycler.adapter = SchedulesAdapter(sched, false, false)
        } else {
            // Hide recycler and title if there are no pinned schedules
            pinsSchedText.visibility = View.GONE
            pinsSchedRecycler.visibility = View.GONE
        }

        /*
         * Posts recycler
         * Set up with first 5 posts only
         * Then load more on button click
         */

        if (posts.isNotEmpty()) {
            postsToShow = posts.take(5).toMutableList()

            // Movement method to open links in-app
            val movementMethod = BetterLinkMovementMethod.newInstance()
            movementMethod.setOnLinkClickListener { _, url ->
                if (prefs.getBoolean("open_links_inapp", true)) {
                    // Open WebViewFragment with respective url on click
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                            .navigate(R.id.webviewFromPostsAction, bundleOf("url" to url))
                    true
                } else false
            }

            val postsAdapter = PostsAdapter(
                    postsToShow,
                    movementMethod,
                    onAttachmentClick,
                    onAttachmentLongClick,
                    Tasks().onCheckedChanged(requireActivity()))
            postsRecycler.adapter = postsAdapter

            loadMorePostsButton.setOnClickListener {
                // Load all posts
                // Replace posts adapter data with all posts
                postsToShow.clear()
                postsToShow.addAll(posts)
                // Show loading symbol and hide button
                postsLoading.visibility = View.VISIBLE
                loadMorePostsButton.visibility = View.GONE
                // Update RecyclerView and hide ProgessBar
                // todo only notify for updated items
                postsAdapter.notifyDataSetChanged()
                postsLoading.visibility = View.GONE
                // Mark all posts as read
                PostsDb.getInstance().markCourseAsRead(courseId)
                // Remember we're showing all posts, using this in the update function
                isExanded = true
            }

            // Mark displayed posts as read
            PostsDb.getInstance().markAsRead(postsToShow[0].postId, postsToShow.getOrNull(1)?.postId)

            // Hide load more button if there are no further posts
            if (posts.size <= 2) {
                loadMorePostsButton.visibility = View.GONE
            }
        } else {
            postsTitleText.text = getString(R.string.posts_no_data)
            postsRecycler.visibility = View.GONE
            loadMorePostsButton.visibility = View.GONE
        }

        return view
    }

    override fun onDestroy() {
        // Unregister broadcast receiver
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(uichangeReceiver)
        super.onDestroy()
    }

}