package com.example.firstapp.utils

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast

/**
 * Lightweight screenshot detector using a MediaStore ContentObserver.
 * Note: Querying metadata may require READ permissions on newer Android versions.
 * This implementation is best-effort and falls back to generic notification.
 */
class ScreenshotMonitor(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver
    private var observer: ContentObserver? = null

    fun start() {
        if (observer != null) return

        observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                handleMediaChange(uri)
            }
        }

        try {
            resolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer as ContentObserver
            )
        } catch (e: Exception) {
            Log.e("ScreenshotMonitor", "Failed to register observer: ${e.message}", e)
        }
    }

    fun stop() {
        observer?.let {
            try {
                resolver.unregisterContentObserver(it)
            } catch (_: Exception) { }
        }
        observer = null
    }

    private fun handleMediaChange(uri: Uri?) {
        // Best effort: try to detect "Screenshots" bucket/path; fallback to generic toast
        try {
            val projection = arrayOf(
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.RELATIVE_PATH,
                MediaStore.Images.Media.DISPLAY_NAME
            )
            val queryUri = uri ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            context.contentResolver.query(queryUri, projection, null, null, "${MediaStore.MediaColumns.DATE_ADDED} DESC")
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val bucket = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                        val relative = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                        val display = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)

                        val bucketName = if (bucket >= 0) cursor.getString(bucket) else null
                        val relativePath = if (relative >= 0) cursor.getString(relative) else null
                        val displayName = if (display >= 0) cursor.getString(display) else null

                        val looksLikeScreenshot = listOf(bucketName, relativePath, displayName)
                            .filterNotNull()
                            .any { it.contains("Screenshot", ignoreCase = true) || it.contains("Screenshots", ignoreCase = true) }

                        if (looksLikeScreenshot) {
                            Toast.makeText(context, "Screenshot detected", Toast.LENGTH_SHORT).show()
                            return
                        }
                    }
                }
        } catch (e: SecurityException) {
            // No permissions to read metadata; fallback generic
            Log.w("ScreenshotMonitor", "No permission to query media: ${e.message}")
        } catch (e: Exception) {
            Log.w("ScreenshotMonitor", "Query failed: ${e.message}")
        }

        // Generic fallback when we cannot confirm
        Toast.makeText(context, "New image captured (possible screenshot)", Toast.LENGTH_SHORT).show()
    }
}
