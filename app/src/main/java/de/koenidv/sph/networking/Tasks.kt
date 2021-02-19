package de.koenidv.sph.networking

import android.content.Context
import android.content.Intent
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.StringRequestListener
import com.google.android.material.snackbar.Snackbar
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.database.TasksDb
import de.koenidv.sph.debugging.DebugLog
import de.koenidv.sph.debugging.Debugger
import de.koenidv.sph.ui.InfoSheet

//  Created by koenidv on 31.01.2021.
class Tasks {

    class TaskData(
            val id: String,
            val description: String,
            val isDone: Boolean,
            val color: Int = 0,
            val date: Long? = null,
            val dueDate: Long? = null,
            val isPinned: Boolean? = null,
    )

    /**
     * Get a lambda to handle task checked changes in posts
     * Will mark task as done in db and sph and show an error if that failed
     */
    fun onCheckedChanged(
            activity: FragmentActivity,
            callback: ((TaskData, Boolean) -> Unit)? = null):
            (TaskData, Boolean) -> Unit = { task, isDone ->

        // Get the corresponding course and post
        val course = TasksDb.getInstance().getCourseByTaskId(task.id)
        val post = TasksDb.getInstance().getPostByTaskId(task.id)

        // Log checking task
        if (Debugger.DEBUGGING_ENABLED)
            DebugLog("Tasks", "Updating task checked status",
                    bundleOf("taskId" to task.id,
                            "courseId" to course.courseId,
                            "numberId" to course.number_id,
                            "isDone" to isDone)).log()


        // Mark the task as done
        complete(course.number_id, task.id, post.postId, isDone) {
            if (it == NetworkManager.SUCCESS) {
                // Call back if function was specified
                if (callback != null) callback(task, isDone)
            } else {
                Snackbar.make(activity.findViewById(R.id.nav_host_fragment),
                        SphPlanner.applicationContext().getString(R.string.task_not_synchronized)
                                + " ($it)", Snackbar.LENGTH_SHORT)
                        .setAnchorView(R.id.nav_view).show()
            }
        }

        // If no callback was specified, send a local broadcast to update the ui
        if (callback == null) {
            // Send broadcast to update ui (remove from tasks list on home)
            val uiBroadcast = Intent("uichange")
            uiBroadcast.putExtra("content", "taskDone")
            uiBroadcast.putExtra("taskId", task.id)
            uiBroadcast.putExtra("postId", post.postId)
            uiBroadcast.putExtra("isDone", isDone)
            LocalBroadcastManager.getInstance(SphPlanner.applicationContext()).sendBroadcast(uiBroadcast)
        }

        // If this is the first time the user marked a task as done, show an info
        val prefs = activity.getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        if (isDone && !prefs.getBoolean("intro_tasks_sync", false)) {
            InfoSheet(R.drawable.img_taskssync, R.string.tasks_sync_info)
                    .show(activity.supportFragmentManager, "info-tasks")
            prefs.edit().putBoolean("intro_tasks_sync", true).apply()
        }

    }

    /**
     * Mark a task as done in the db and send a post to sph to mark it as read there, too
     * @param numberId NumberId of the course the task belongs to
     * @param taskId Task id that should be marked as done
     * @param postId Id of the according post
     * @param isDone Whether the task is now done or not
     */
    // todo retry later on error
    private fun complete(numberId: String?, taskId: String, postId: String, isDone: Boolean, callback: (success: Int) -> Unit) {
        // Mark as (un)done in the db
        TasksDb.getInstance().setDone(taskId, isDone)
        // Mark as done on sph
        // Cancel potential pending requests for this same task, just to be sure
        AndroidNetworking.cancel(taskId)
        // Only post to sph if we know the number id
        // May be null if it's a custom task, or for some weird reason that we don't know yet
        if (numberId != null) {
            // Log checking task
            if (Debugger.DEBUGGING_ENABLED)
                DebugLog("Tasks", "Posting task update to sph",
                        bundleOf("taskId" to taskId,
                                "numberId" to numberId,
                                "isDone" to isDone)).log()

            // We need an access token first
            TokenManager.getToken { success: Int, token: String? ->
                if (success == NetworkManager.SUCCESS) {

                    // Send a post request to let sph know the task is done
                    AndroidNetworking.post(
                            SphPlanner.applicationContext().getString(R.string.url_mycourses))
                            .addBodyParameter("a", "sus_homeworkDone")
                            .addBodyParameter("id", numberId)
                            .addBodyParameter("entry", postId.substringAfterLast("_"))
                            .addBodyParameter("b", if (isDone) "done" else "undone")
                            .setTag(taskId)
                            .build()
                            .getAsString(object : StringRequestListener {
                                override fun onResponse(response: String) {
                                    if (response == "1")
                                        callback(NetworkManager.SUCCESS)
                                    else
                                        callback(NetworkManager.FAILED_UNKNOWN)
                                    // Log response
                                    if (Debugger.DEBUGGING_ENABLED)
                                        DebugLog("Tasks", "Posted task update to sph",
                                                bundleOf("response" to response)).log()
                                }

                                override fun onError(error: ANError) {
                                    // Log error
                                    if (Debugger.DEBUGGING_ENABLED)
                                        DebugLog("Tasks", "Error updating tasks",
                                                error).log()

                                    when (error.errorDetail) {
                                        "connectionError" -> callback(NetworkManager.FAILED_NO_NETWORK)
                                        "requestCancelledError" -> callback(NetworkManager.FAILED_CANCELLED)
                                        else -> callback(NetworkManager.FAILED_UNKNOWN)
                                    }
                                }
                            })

                } else callback(success)
            }
        }
    }

}