package de.koenidv.sph.networking

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Looper
import android.util.Log
import androidx.core.content.FileProvider
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.DownloadListener
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

    fun handleAttachment(attachment: PostAttachment, onComplete: (success: Int) -> Unit) {
        // Check if file already exists
        val file = File(applicationContext().filesDir.toString() + "/" + attachment.localPath())

        if (!file.exists()) {
            // Download file, then open it
            // todo handle errors
            downloadFile(attachment) {
                if (it == NetworkManager().SUCCESS) {
                    openFile(attachment)
                    onComplete(NetworkManager().SUCCESS)
                }
            }
        } else {
            // File already exists, open it
            openFile(attachment)
            onComplete(NetworkManager().SUCCESS)
        }
    }

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
    private fun openFile(file: PostAttachment) {
        val fileToOpen = File(applicationContext().filesDir.toString() + "/" + file.localPath())
        val path = FileProvider.getUriForFile(applicationContext(), applicationContext().packageName + ".provider", fileToOpen)

        val fileIntent = Intent(Intent.ACTION_VIEW)
        fileIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION

        fileIntent.setDataAndType(path, getMimeType(file.fileType))

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
     * Get a file's content type, using it's extension
     */
    private fun getMimeType(fileType: String): String {
        return when (fileType.toLowerCase(Locale.ROOT)) {
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
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
            "txt" -> "file-alt"
            else -> "file ($filetype)"
        }
    }
}