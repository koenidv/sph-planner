package de.koenidv.sph.networking

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.database.*
import de.koenidv.sph.objects.*
import de.koenidv.sph.parsing.RawParser
import de.koenidv.sph.parsing.Utility
import org.jsoup.Jsoup
import java.util.*

//  Created by koenidv on 31.01.2021.
class Posts(private val networkManager: NetworkManager = NetworkManager()) {

    /**
     * Load all posts from all courses or
     * update courses where the latest post changed
     */
    fun fetch(callback: (Int) -> Unit, createIndex: Boolean = false) {
        if (createIndex) load(markAsRead = true, callback = callback)
        else update(callback)
    }

    /**
     * Checks for new posts on the overview page
     * and only loads them for courses with a new entry
     * This will not update anything if a post was added with an older date
     * Therefore we also load courses that haven't been loaded within the last 48 hours
     */
    private fun update(callback: (success: Int) -> Unit) {
        // Get my courses page
        networkManager.loadSiteWithToken(SphPlanner.applicationContext()
                .getString(R.string.url_mycourses)) { success, response ->
            if (success == NetworkManager.SUCCESS) {
                // Check which courses should be updated
                // Any course that is not in this list does not have any posts.
                val prefs = SphPlanner.applicationContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
                val courses = mutableListOf<Course>()
                val courseids = mutableListOf<String>()
                val rows = Jsoup.parse(response!!).select("#aktuell tbody tr")
                val postsdb = PostsDb.getInstance()
                val coursesdb = CoursesDb.getInstance()
                val timenow = Date().time

                var numberId: String
                var postIndex: String
                var courseId: String?
                var latestPost: Post?

                for (row in rows) {
                    numberId = row.attr("data-book")
                    postIndex = row.attr("data-entry")
                    courseId = coursesdb.getCourseIdByNumberId(numberId)

                    if (courseId == null) {
                        // Course has not already been added, at least not with a number id
                        // Try to find a matching course or create one and save it to the db
                        val courseName = row.select("span.name").text()
                        val teacherId = row.select("span.teacher button")
                                .first().ownText().toLowerCase(Locale.ROOT)
                        val isLK = courseName.contains("LK")

                        val courseToAdd = RawParser().getCourseFromPostsoverviewData(
                                courseName, teacherId, isLK, numberId,
                                Utility.parseStringArray(R.array.course_colors))
                        // If the found course is null, this will not crash
                        // (won't try adding null to courses list)
                        // But it also won't update the course
                        if (courseToAdd != null) {
                            coursesdb.save(courseToAdd)
                            courseId = courseToAdd.courseId
                        }
                    }

                    latestPost = postsdb.getByCourseId(courseId, 1).getOrNull(0)

                    // Check if the index of the last post in this course matches the newest index on sph
                    if (latestPost?.postId?.substring(latestPost.postId.lastIndexOf("_") + 1)
                            != postIndex
                            || timenow - prefs.getLong("updated_posts_${courseId}", 0) > 48 * 60 * 60 * 1000
                    ) {
                        val courseToAdd = coursesdb.getByInternalId(courseId)
                        if (courseToAdd != null) {
                            courses.add(courseToAdd)
                            courseids.add(courseId!!)
                        }
                    }
                }

                if (courses.isNotEmpty()) {
                    // Now that we got all the courses that should be updated, update them
                    load(courses) {
                        prefs.edit().putLong("updated_posts", timenow).apply()
                        // Send broadcast to update posts
                        val uiBroadcast = Intent("uichange")
                        uiBroadcast.putExtra("content", "posts")
                        uiBroadcast.putExtra("courses", courseids.toTypedArray())
                        LocalBroadcastManager.getInstance(SphPlanner.applicationContext()).sendBroadcast(uiBroadcast)
                        callback(it)
                    }
                } else callback(NetworkManager.SUCCESS)
            } else callback(success)
        }
    }

    /**
     * Load and save Posts, PostAtachments and PostTasks for a list of courses
     * @param coursesToLoad Courses that should be loaded, all courses with NumberIds when null
     */
    fun load(coursesToLoad: List<Course>? = null,
             markAsRead: Boolean = false,
             callback: (success: Int) -> Unit) {
        // Use all courses with number_id if nothing was specified
        val courses = coursesToLoad ?: CoursesDb.getInstance().withNumberId
        // Save all errors in a list, only return one later
        val errors = mutableListOf<Int>()
        // Get all current posts for comparison
        val allPosts = PostsDb.getInstance().all
        // Counter for done courses
        var counter = 0

        val prefs = SphPlanner.applicationContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val time = Date().time

        // Load every course
        for (course in courses) {
            networkManager.loadSiteWithToken(SphPlanner.applicationContext()
                    .getString(R.string.url_course_overview)
                    .replace("%numberid", course.number_id.toString())) { success: Int, result: String? ->
                if (success == NetworkManager.SUCCESS) {
                    // Parse data
                    RawParser().parsePosts(course.courseId,
                            result!!,
                            allPosts,
                            markAsRead,
                            onParsed = { posts: List<Post>,
                                         files: List<FileAttachment>,
                                         tasks: List<Task>,
                                         links: List<LinkAttachment> ->
                                // Write all that parsed stuff to the database
                                PostsDb.getInstance().save(posts)
                                TasksDb.getInstance().save(tasks)
                                FileAttachmentsDb.getInstance().save(files)
                                LinkAttachmentsDb.getInstance().save(links)
                            })
                    // Remember when we last refreshed this course
                    prefs.edit().putLong("updated_posts_${course.courseId}", time).apply()
                } else {
                    Log.d(SphPlanner.TAG, "Couldn't load " +
                            SphPlanner.applicationContext().getString(R.string.url_course_overview)
                                    .replace("%numberid", course.number_id
                                            .toString()))
                    errors.add(success)
                }
                counter++
                if (counter == courses.size) {
                    // Return success or the highest error code
                    if (errors.isEmpty()) {
                        callback(NetworkManager.SUCCESS)
                        prefs.edit().putLong("updated_posts", time).apply()
                    } else callback(errors.maxOf { it })
                }
            }
        }
    }
}