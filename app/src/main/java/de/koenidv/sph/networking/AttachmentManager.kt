package de.koenidv.sph.networking

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.DownloadListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.TAG
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.database.FileAttachmentsDb
import de.koenidv.sph.database.LinkAttachmentsDb
import de.koenidv.sph.objects.Attachment
import de.koenidv.sph.objects.FileAttachment
import de.koenidv.sph.objects.Task
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.io.File
import java.util.*


//  Created by koenidv on 26.12.2020.
class AttachmentManager {
    companion object {
        const val ATTACHMENT_USED = 0
        const val ATTACHMENT_USED_PIN = 1
        const val ATTACHMENT_PINNED = 2
        const val ATTACHMENT_UNPINNED = 3
        const val ATTACHMENT_RENAMED = 4
        const val ATTACHMENT_RENAMED_PIN = 5
    }

    // Movement method to open links in-app
    fun movementMethod(activity: Activity, webViewAction: Int): BetterLinkMovementMethod = BetterLinkMovementMethod.newInstance()
            .setOnLinkClickListener { _, url ->
                val prefs: SharedPreferences = applicationContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
                if (prefs.getBoolean("open_links_inapp", true)) {
                    // Open WebViewFragment with respective url on click
                    Navigation.findNavController(activity, R.id.nav_host_fragment)
                            .navigate(webViewAction, bundleOf("url" to url))
                    true
                } else false
            }

    /**
     * Returns a lambda to handle clicks on an attachment item
     */
    fun onAttachmentClick(activity: Activity, onAction: (action: Int, attachment: Attachment) -> Unit):
            (Attachment, View) -> Unit = { attachment, view ->
        handleAttachment(activity, attachment, view)
        onAction(ATTACHMENT_USED, attachment)
    }

    /**
     * Returns a lambda to handle long clicks on an attachment item
     */
    @SuppressLint("SetTextI18n")
    fun onAttachmentLongClick(activity: Activity, onAction: (action: Int, attachment: Attachment) -> Unit): (Attachment, View) -> Unit =
            { attachment, view ->
                val sheet = BottomSheetDialog(activity)
                sheet.setContentView(R.layout.sheet_manage_attachment)

                val type = attachment.type()
                @Suppress("SimplifyBooleanWithConstants") val isPinned = (type == "file" && FileAttachmentsDb.getInstance().isPinned(attachment.attachId())
                        || type == "link" && LinkAttachmentsDb.getInstance().isPinned(attachment.attachId()))

                val open = sheet.findViewById<TextView>(R.id.openTextView)
                val download = sheet.findViewById<TextView>(R.id.downloadTextView)
                val delete = sheet.findViewById<TextView>(R.id.deleteTextView)
                val pin = sheet.findViewById<TextView>(R.id.pinTextView)
                val unpin = sheet.findViewById<TextView>(R.id.unpinTextView)
                val rename = sheet.findViewById<TextView>(R.id.renameTextView)
                val share = sheet.findViewById<TextView>(R.id.shareTextView)
                val icon = view.findViewById<TextView>(R.id.iconTextView)

                val loading = view.findViewById<ProgressBar>(R.id.downloadingProgressBar)

                val doneSnackbar = Snackbar.make(activity.findViewById(R.id.nav_host_fragment),
                        "", Snackbar.LENGTH_SHORT)
                        .setAnchorView(R.id.nav_view)

                // Hide unusable options

                // Hide if attachment is not a file
                if (type != "file") {
                    download?.visibility = GONE
                    delete?.visibility = GONE
                } else {
                    // Check if file exists
                    if (File(applicationContext().filesDir.toString() + "/" + attachment.file().localPath()).exists())
                        download?.visibility = GONE
                    else delete?.visibility = GONE
                }

                // Check if the attachment is pinned in the database
                // Might have changed since it was loaded into the recyclerview
                if (isPinned)
                    pin?.visibility = GONE
                else unpin?.visibility = GONE


                // Set option logic

                // Open file or link optin
                open?.setOnClickListener {
                    sheet.dismiss()
                    handleAttachment(activity, attachment, view)
                    // Notify the calling activity
                    if (isPinned) onAction(ATTACHMENT_USED_PIN, attachment)
                    else onAction(ATTACHMENT_USED, attachment)
                }

                // Download option
                download?.setOnClickListener {
                    // Prepare downloading snackbar
                    @SuppressLint("CutPasteId")
                    val snackbar = Snackbar.make(activity.findViewById(R.id.nav_host_fragment),
                            activity.getString(R.string.attachments_downloading_size, attachment.file().fileSize),
                            Snackbar.LENGTH_INDEFINITE)
                            .setAnchorView(R.id.nav_view)
                    // Add option to cancel the download
                    snackbar.setAction(R.string.cancel) {
                        AndroidNetworking.cancel(attachment.attachId())
                        loading.visibility = GONE
                    }
                    // Show the snackbar
                    snackbar.show()
                    // Show a downloading progress bar
                    loading.visibility = View.VISIBLE
                    // Download the file
                    downloadFile(attachment.file()) {
                        if (it == NetworkManager.SUCCESS) {
                            // If downloading & opening was successful
                            // Add check icon to show file has been downloaded
                            @SuppressLint("SetTextI18n")
                            icon.text = "check-circle ${icon.text}"
                            // Dismiss snackbar and progress bar
                            snackbar.dismiss()
                            loading.visibility = GONE
                            // Show done snackbar
                            doneSnackbar.setText(R.string.attachments_options_download_complete).show()
                            // Update last use
                            LinkAttachmentsDb.getInstance().used(attachment.attachId())
                            // Notify the calling activity
                            if (isPinned) onAction(ATTACHMENT_USED_PIN, attachment)
                            else onAction(ATTACHMENT_USED, attachment)
                        } else {
                            // An error occurred
                            snackbar.dismiss()
                            doneSnackbar.setText(R.string.error).show()
                            loading.visibility = GONE
                        }
                    }
                    sheet.dismiss()
                }

                // Remove from device option
                delete?.setOnClickListener {
                    // File to remove
                    val file = File(applicationContext().filesDir.toString() + "/" + attachment.file().localPath())
                    // Try to delete file
                    if (file.delete())
                        doneSnackbar.setText(R.string.attachments_options_delete_complete)
                    else
                        doneSnackbar.setText(R.string.error)
                    // Remove downloaded icon from icon textview
                    icon.text = icon.text.toString().replace("check-circle ", "")
                    sheet.dismiss()
                    // Show result
                    doneSnackbar.show()
                }

                // Pin
                pin?.setOnClickListener {
                    // Mark attachment as pinned
                    if (type == "file")
                        FileAttachmentsDb.getInstance().setPinned(attachment.attachId(), true)
                    else
                        LinkAttachmentsDb.getInstance().setPinned(attachment.attachId(), true)
                    // Update the icon textview accordingly
                    icon.text = "thumbtack ${icon.text}"
                    // Hide the bottom sheet dialog
                    sheet.dismiss()
                    // Show success
                    doneSnackbar.setText(R.string.attachments_options_pin_complete).show()
                    // Notify the calling activity
                    onAction(ATTACHMENT_PINNED, attachment)
                }

                // Unpin
                unpin?.setOnClickListener {
                    // Mark attachment as not pinned
                    if (type == "file")
                        FileAttachmentsDb.getInstance().setPinned(attachment.attachId(), false)
                    else
                        LinkAttachmentsDb.getInstance().setPinned(attachment.attachId(), false)
                    // Update the icon textview accordingly
                    icon.text = icon.text.toString().replace("thumbtack ", "")
                    // Hide the bottom sheet dialog
                    sheet.dismiss()
                    // Show success
                    doneSnackbar.setText(R.string.attachments_options_unpin_complete).show()
                    // Notify the calling activity
                    onAction(ATTACHMENT_UNPINNED, attachment)
                }

                // Change display name
                rename?.setOnClickListener {
                    sheet.dismiss()
                    // Show a dialog to rename the attachment
                    MaterialDialog(activity).show {
                        input(prefill = attachment.name()) { _, text ->
                            if (type == "file")
                                FileAttachmentsDb.getInstance().rename(attachment.attachId(), text.toString())
                            else
                                LinkAttachmentsDb.getInstance().rename(attachment.attachId(), text.toString())

                            // Update ui
                            attachment.setName(text.toString())
                            if (isPinned) onAction(ATTACHMENT_RENAMED_PIN, attachment)
                            else onAction(ATTACHMENT_RENAMED, attachment)
                        }
                        positiveButton(R.string.save)
                        negativeButton(R.string.cancel)
                        title(R.string.attachments_options_rename)
                    }
                }

                // Share attachment
                share?.setOnClickListener {
                    if (type == "file") {
                        // Share file
                        // If file exists, share, else download and share
                        if (File(applicationContext().filesDir.toString() + "/" + attachment.file().localPath()).exists()) {
                            shareAttachmentFile(attachment.file(), activity)
                            sheet.dismiss()
                        } else {
                            // First, download the file
                            // Prepare downloading snackbar
                            @SuppressLint("CutPasteId")
                            val snackbar = Snackbar.make(activity.findViewById(R.id.nav_host_fragment),
                                    activity.getString(R.string.attachments_downloading_size, attachment.file().fileSize),
                                    Snackbar.LENGTH_INDEFINITE)
                                    .setAnchorView(R.id.nav_view)
                            // Show loading progress bar
                            loading.visibility = View.VISIBLE
                            // Add option to cancel the download
                            snackbar.setAction(R.string.cancel) {
                                AndroidNetworking.cancel(attachment.attachId())
                                loading.visibility = GONE
                            }
                            // Show the snackbar
                            snackbar.show()
                            // Download the file
                            downloadFile(attachment.file()) {
                                if (it == NetworkManager.SUCCESS) {
                                    // If downloading & opening was successful
                                    // Add check icon to show file has been downloaded
                                    icon.text = "check-circle ${icon.text}"
                                    // Dismiss snackbar and progress bar
                                    snackbar.dismiss()
                                    loading.visibility = GONE
                                    // Now share the downloaded file
                                    shareAttachmentFile(attachment.file(), activity)
                                    // Update file last use
                                    FileAttachmentsDb.getInstance().used(attachment.attachId())
                                    // Notify the calling activity
                                    if (isPinned) onAction(ATTACHMENT_USED_PIN, attachment)
                                    else onAction(ATTACHMENT_USED, attachment)
                                } else {
                                    // An error occurred
                                    snackbar.dismiss()
                                    doneSnackbar.setText(R.string.error).show()
                                    loading.visibility = GONE
                                }
                            }
                        }
                    } else {
                        // Share link url as plaintext
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, attachment.url())
                            this.type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        activity.startActivity(shareIntent)
                        // Update link last use
                        LinkAttachmentsDb.getInstance().used(attachment.attachId())
                        // Notify the calling activity
                        if (isPinned) onAction(ATTACHMENT_USED_PIN, attachment)
                        else onAction(ATTACHMENT_USED, attachment)

                    }
                    sheet.dismiss()
                }

                sheet.show()
            }

    /**
     * Returns a lambda to handle task checked changes
     */
    fun onTaskCheckedChanged(activity: Activity, courseNumberId: String? = null, onComplete: ((Task, Boolean) -> Unit)? = null):
            (task: Task, isDone: Boolean) -> Unit = { task, isDone ->
        val numberId = courseNumberId ?: CoursesDb.getInstance().getNumberId(task.id_course)
        NetworkManager().markTaskAsDone(numberId, task, isDone) {
            if (it == NetworkManager.SUCCESS) {
                if (onComplete != null) onComplete(task, isDone)
            } else {
                Snackbar.make(activity.findViewById(R.id.nav_host_fragment),
                        applicationContext().getString(R.string.task_not_synchronized)
                                + " ($it)", Snackbar.LENGTH_SHORT)
                        .setAnchorView(R.id.nav_view).show()
            }
        }

    }

    /**
     * Download if needed and open a file or link
     */
    private fun handleAttachment(activity: Activity, attachment: Attachment, view: View) {
        if (attachment.type() == "file") {
            val loading = view.findViewById<ProgressBar>(R.id.downloadingProgressBar)

            // Open file if existing, otherwise download and open
            // Prepare downloading snackbar
            val snackbar = Snackbar.make(activity.findViewById(R.id.nav_host_fragment),
                    activity.getString(R.string.attachments_downloading_size, attachment.file().fileSize),
                    Snackbar.LENGTH_INDEFINITE)
                    .setAnchorView(R.id.nav_view)
            // Add option to cancel the download
            snackbar.setAction(R.string.cancel) {
                AndroidNetworking.cancel(attachment.attachId())
                loading.visibility = GONE
            }
            // Show the snackbar
            snackbar.show()
            loading.visibility = View.VISIBLE
            // Let AttachmentManager handle downloading and opening the file
            AttachmentManager().handleFileAttachment(attachment.file()) { opened ->
                // Hide snackbar when the file has been opened
                if (opened == NetworkManager.SUCCESS) {
                    val icon = view.findViewById<TextView>(R.id.iconTextView)
                    // If downloading & opening was successful
                    // Update file last use
                    FileAttachmentsDb.getInstance().used(attachment.attachId())
                    // Add check icon if there wasn't a donwloaded check before
                    loading.visibility = GONE
                    if (!icon.text.contains("check-circle"))
                        @SuppressLint("SetTextI18n")
                        icon.text = "check-circle ${icon.text}"
                    // Hide snackbar
                    snackbar.dismiss()
                } else {
                    // An error occurred
                    snackbar.dismiss()
                    loading.visibility = GONE
                    Snackbar.make(activity.findViewById(R.id.nav_host_fragment),
                            R.string.error, Snackbar.LENGTH_SHORT).setAnchorView(R.id.nav_view)
                            .setAnchorView(R.id.nav_view).show()
                }
            }
        } else {
            // Update link last use
            LinkAttachmentsDb.getInstance().used(attachment.attachId())
            // Open url
            if (activity.getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE).getBoolean("open_links_inapp", true)) {
                // Open in in-app
                activity.findNavController(R.id.nav_host_fragment)
                        .navigate(R.id.webviewAction, bundleOf("url" to attachment.url()))
            } else {
                // Open in browser
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(attachment.url()))
                activity.startActivity(browserIntent)
            }
        }
    }

    /**
     * Handles downloading and opening attachment items
     */
    private fun handleFileAttachment(attachment: FileAttachment, onComplete: (success: Int) -> Unit) {
        // Check if file already exists
        val file = File(applicationContext().filesDir.toString() + "/" + attachment.localPath())

        if (!file.exists()) {
            // Download file, then open it
            // todo handle errors
            downloadFile(attachment) {
                if (it == NetworkManager.SUCCESS) {
                    openAttachmentFile(attachment)
                }
                onComplete(it)
            }
        } else {
            // File already exists, open it
            openAttachmentFile(attachment)
            onComplete(NetworkManager.SUCCESS)
        }
    }

    /**
     * Download a file attachment
     * @param file FileAttachment to download
     */
    private fun downloadFile(file: FileAttachment, onComplete: (success: Int) -> Unit) {
        // Get an access token
        TokenManager().generateAccessToken { success: Int, token: String? ->
            if (success == NetworkManager.SUCCESS) {
                // Set sid cookie
                CookieStore.saveFromResponse(
                        HttpUrl.parse("https://schulportal.hessen.de")!!,
                        listOf(Cookie.Builder().domain("schulportal.hessen.de").name("sid").value(token!!).build()))
                // Apply cookie jar
                val okHttpClient = OkHttpClient.Builder()
                        .cookieJar(CookieStore)
                        .build()
                AndroidNetworking.initialize(applicationContext(), okHttpClient)

                // Download attachment
                AndroidNetworking.download(file.url, applicationContext().filesDir.toString(), file.localPath())
                        .setPriority(Priority.HIGH)
                        .setTag(file.attachmentId)
                        // There's no real need to cache the downloaded file
                        // as we're saving it to local storage, just a waste of space
                        .doNotCacheResponse()
                        .build()
                        .setDownloadProgressListener { bytesDownloaded, totalBytes ->
                            val progress = (10000 / totalBytes * bytesDownloaded) / 100
                            Log.d(TAG, "Downloading: $progress")
                        }
                        .startDownload(object : DownloadListener {
                            override fun onDownloadComplete() {
                                Log.d(TAG, "File download Completed")
                                Log.d(TAG, "onDownloadComplete isMainThread : " + (Looper.myLooper() == Looper.getMainLooper()).toString())

                                onComplete(NetworkManager.SUCCESS)
                            }

                            override fun onError(error: ANError) {
                                if (error.errorCode != 0) {
                                    // received ANError from server
                                    // error.getErrorCode() - the ANError code from server
                                    // error.getErrorBody() - the ANError body from server
                                    // error.getErrorDetail() - just an ANError detail
                                    Log.d(TAG, "onError errorCode : " + error.errorCode)
                                    Log.d(TAG, "onError errorBody : " + error.errorBody)
                                    Log.d(TAG, "onError errorDetail : " + error.errorDetail)
                                } else {
                                    // error.getErrorDetail() : connectionError, parseError, requestCancelledError
                                    Log.d(TAG, "onError errorDetail : " + error.errorDetail)
                                }
                            }
                        })
            } else {
                onComplete(success)
            }
        }
    }

    /**
     * Open a file included in a postAttachment
     */
    private fun openAttachmentFile(attachment: FileAttachment) {
        val fileToOpen = File(applicationContext().filesDir.toString() + "/" + attachment.localPath())
        val path = FileProvider.getUriForFile(applicationContext(), applicationContext().packageName + ".provider", fileToOpen)

        val fileIntent = Intent(Intent.ACTION_VIEW)
        fileIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION

        fileIntent.setDataAndType(path, getMimeType(attachment.fileType))

        // Try opening the file with the correct application
        // If no application can be found, let the user decide
        try {
            applicationContext().startActivity(fileIntent)
        } catch (exception: ActivityNotFoundException) {
            fileIntent.setDataAndType(path, "*/*")
            applicationContext().startActivity(fileIntent)
        }
        // Update last use date
        FileAttachmentsDb.getInstance().used(attachment.attachmentId)
    }

    /**
     * Share an attached file
     */
    private fun shareAttachmentFile(attachment: FileAttachment, activity: Activity) {
        // Get a uri to the file
        val fileToShare = File(applicationContext().filesDir.toString() + "/" + attachment.localPath())
        val uri = FileProvider.getUriForFile(applicationContext(), applicationContext().packageName + ".provider", fileToShare)
        // Check if the file actually exists
        if (fileToShare.exists()) {
            // Create a share intent
            val intent = ShareCompat.IntentBuilder.from(activity)
                    .setStream(uri) // uri from FileProvider
                    .setType(getMimeType(attachment.fileType))
                    .intent
                    .setAction(Intent.ACTION_SEND)
                    .setDataAndType(uri, getMimeType(attachment.fileType))
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)

            // Create chooser
            val chooser = Intent.createChooser(intent,
                    applicationContext().getString(R.string.attachments_options_share))
            chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            // Start intent chooser
            try {
                applicationContext().startActivity(chooser)
                // Update last use date
                FileAttachmentsDb.getInstance().used(attachment.attachmentId)
            } catch (ane: ActivityNotFoundException) {
            }
        }
    }

    /**
     * Get a file's content type, using it's extension
     */
    private fun getMimeType(fileType: String): String {
        return when (fileType.toLowerCase(Locale.ROOT)) {
            "pdf" -> "application/pdf"
            "doc", "docx", "odt" -> "application/msword"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "zip" -> "application/zip"
            "txt" -> "text/plain"
            "jpg", "jpeg", "png" -> "image/jpeg"
            "gif" -> "image/gif"
            "mp4", "mpg", "mpeg", "avi" -> "video/*"
            else -> "*/*"
        }
    }

    /**
     * Get an font awesome icon for each filetype
     */
    fun getIconForFiletype(filetype: String): String {
        return when (filetype) {
            "link" -> "link"
            "pdf" -> "file-pdf"
            "doc", "docx" -> "file-word"
            "ppt", "pptx" -> "file-powerpoint"
            "xls", "xlsx" -> "file-excel"
            "jpg", "jpeg", "png", "gif" -> "file-image"
            "zip", "rar" -> "file-archive"
            "txt", "odt" -> "file-alt"
            else -> "file ($filetype)"
        }
    }
}