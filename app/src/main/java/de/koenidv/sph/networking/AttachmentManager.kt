package de.koenidv.sph.networking

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.DownloadListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner.Companion.TAG
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import de.koenidv.sph.objects.PostAttachment
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.io.File
import java.util.*


//  Created by koenidv on 26.12.2020.
class AttachmentManager {

    /**
     * Returns a lambda to handle clicks on an attachment item
     */
    fun onAttachmentClick(activity: Activity): (PostAttachment, View) -> Unit =
            { attachment, view ->
                // Prepare downloading snackbar
                val snackbar = Snackbar.make(activity.findViewById(R.id.nav_host_fragment),
                        activity.getString(R.string.attachments_downloading_size, attachment.fileSize),
                        Snackbar.LENGTH_INDEFINITE)
                // Add option to cancel the download
                snackbar.setAction(R.string.cancel) {
                    AndroidNetworking.cancel(attachment.attachmentId)
                }
                // Show the snackbar
                snackbar.show()
                // Let AttachmentManager handle downloading and opening the file
                AttachmentManager().handleAttachment(attachment) { opened ->
                    // Hide snackbar when the file has been opened
                    if (opened == NetworkManager().SUCCESS) {
                        val icon = view.findViewById<TextView>(R.id.iconTextView)
                        // If downloading & opening was successful
                        // Add check icon if there wasn't a donwloaded check before
                        if (!icon.text.contains("check-circle"))
                            @SuppressLint("SetTextI18n")
                            icon.text = "check-circle ${icon.text}"
                        // Hide snackbar
                        snackbar.dismiss()
                    } else {
                        // An error occurred
                        snackbar.dismiss()
                        Snackbar.make(activity.findViewById(R.id.nav_host_fragment),
                                R.string.error, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }

    /**
     * Returns a lambda to handle long clicks on an attachment item
     */
    fun onAttachmentLongClick(activity: Activity): (PostAttachment, View) -> Unit =
            { attachment, view ->
                val sheet = BottomSheetDialog(activity)
                sheet.setContentView(R.layout.sheet_manage_attachment)

                val download = sheet.findViewById<TextView>(R.id.downloadTextView)
                val delete = sheet.findViewById<TextView>(R.id.deleteTextView)
                val pin = sheet.findViewById<TextView>(R.id.pinTextView)
                val unpin = sheet.findViewById<TextView>(R.id.unpinTextView)
                val share = sheet.findViewById<TextView>(R.id.shareTextView)
                val icon = view.findViewById<TextView>(R.id.iconTextView)

                val doneSnackbar = Snackbar.make(activity.findViewById(R.id.nav_host_fragment),
                        "", Snackbar.LENGTH_SHORT)

                // Hide unusable options

                // Check if file exists
                if (File(applicationContext().filesDir.toString() + "/" + attachment.localPath()).exists())
                    download?.visibility = View.GONE
                else delete?.visibility = View.GONE

                // todo Check if attachment is pinned
                if (false) pin?.visibility = View.GONE
                else unpin?.visibility = View.GONE

                // Set option logic

                // Download option
                download?.setOnClickListener {
                    // Prepare downloading snackbar
                    @SuppressLint("CutPasteId")
                    val snackbar = Snackbar.make(activity.findViewById(R.id.nav_host_fragment),
                            activity.getString(R.string.attachments_downloading_size, attachment.fileSize),
                            Snackbar.LENGTH_INDEFINITE)
                    // Add option to cancel the download
                    snackbar.setAction(R.string.cancel) {
                        AndroidNetworking.cancel(attachment.attachmentId)
                    }
                    // Show the snackbar
                    snackbar.show()
                    // Download the file
                    downloadFile(attachment) {
                        if (it == NetworkManager().SUCCESS) {
                            // If downloading & opening was successful
                            // Add check icon to show file has been downloaded
                            @SuppressLint("SetTextI18n")
                            icon.text = "check-circle ${icon.text}"
                            // Dismiss snackbar
                            snackbar.dismiss()
                            doneSnackbar.setText(R.string.attachments_options_download_complete).show()
                        } else {
                            // An error occurred
                            snackbar.dismiss()
                            doneSnackbar.setText(R.string.error).show()
                        }
                    }
                    sheet.dismiss()
                }

                // Remove from device option
                delete?.setOnClickListener {
                    // File to remove
                    val file = File(applicationContext().filesDir.toString() + "/" + attachment.localPath())
                    // Try to delete file
                    if (file.delete())
                        doneSnackbar.setText(R.string.attachments_options_delete_complete)
                    else
                        doneSnackbar.setText(R.string.error)
                    // Show result
                    doneSnackbar.show()
                    // Remove downloaded icon from icon textview
                    icon.text = icon.text.toString().replace("check-circle ", "")
                    sheet.dismiss()
                }

                // todo pin, unpin

                // Share a file
                share?.setOnClickListener {
                    // If file exists, share, else download and share
                    if (File(applicationContext().filesDir.toString() + "/" + attachment.localPath()).exists()) {
                        shareAttachmentFile(attachment, activity)
                        sheet.dismiss()
                    } else {
                        // First, download the file
                        // Prepare downloading snackbar
                        @SuppressLint("CutPasteId")
                        val snackbar = Snackbar.make(activity.findViewById(R.id.nav_host_fragment),
                                activity.getString(R.string.attachments_downloading_size, attachment.fileSize),
                                Snackbar.LENGTH_INDEFINITE)
                        // Add option to cancel the download
                        snackbar.setAction(R.string.cancel) {
                            AndroidNetworking.cancel(attachment.attachmentId)
                        }
                        // Show the snackbar
                        snackbar.show()
                        // Download the file
                        downloadFile(attachment) {
                            if (it == NetworkManager().SUCCESS) {
                                // If downloading & opening was successful
                                // Add check icon to show file has been downloaded
                                @SuppressLint("SetTextI18n")
                                icon.text = "check-circle ${icon.text}"
                                // Dismiss snackbar
                                snackbar.dismiss()
                                // Now share the downloaded file
                                shareAttachmentFile(attachment, activity)
                            } else {
                                // An error occurred
                                snackbar.dismiss()
                                doneSnackbar.setText(R.string.error).show()
                            }
                        }
                    }
                    sheet.dismiss()
                }

                sheet.show()
            }

    /**
     * Handles downloading and opening attachment items
     */
    private fun handleAttachment(attachment: PostAttachment, onComplete: (success: Int) -> Unit) {
        // Check if file already exists
        val file = File(applicationContext().filesDir.toString() + "/" + attachment.localPath())

        if (!file.exists()) {
            // Download file, then open it
            // todo handle errors
            downloadFile(attachment) {
                if (it == NetworkManager().SUCCESS) {
                    openAttachmentFile(attachment)
                    onComplete(NetworkManager().SUCCESS)
                }
            }
        } else {
            // File already exists, open it
            openAttachmentFile(attachment)
            onComplete(NetworkManager().SUCCESS)
        }
    }

    /**
     * Download a file attachment
     * @param file PostAttachment to download
     */
    private fun downloadFile(file: PostAttachment, onComplete: (success: Int) -> Unit) {
        // Get an access token
        TokenManager().generateAccessToken { success: Int, token: String? ->
            if (success == NetworkManager().SUCCESS) {
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
                        .build()
                        .setAnalyticsListener { timeTakenInMillis, bytesSent, bytesReceived, isFromCache ->
                            Log.d(TAG, " timeTakenInMillis : $timeTakenInMillis")
                            Log.d(TAG, " bytesSent : $bytesSent")
                            Log.d(TAG, " bytesReceived : $bytesReceived")
                            Log.d(TAG, " isFromCache : $isFromCache")
                        }
                        .setDownloadProgressListener { bytesDownloaded, totalBytes ->
                            val progress = (10000 / totalBytes * bytesDownloaded) / 100
                            Log.d(TAG, "Downloading: $progress")
                        }
                        .startDownload(object : DownloadListener {
                            override fun onDownloadComplete() {
                                Log.d(TAG, "File download Completed")
                                Log.d(TAG, "onDownloadComplete isMainThread : " + (Looper.myLooper() == Looper.getMainLooper()).toString())

                                onComplete(NetworkManager().SUCCESS)
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
    private fun openAttachmentFile(attachment: PostAttachment) {
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
    }

    /**
     * Share an attached file
     * todo doesn't work yet
     */
    private fun shareAttachmentFile(attachment: PostAttachment, activity: Activity) {
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
            "ppt", "pptx" -> "application/vnd-ms-powerpoint"
            "xls", "xlsx" -> "application/vnd-ms-excel"
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