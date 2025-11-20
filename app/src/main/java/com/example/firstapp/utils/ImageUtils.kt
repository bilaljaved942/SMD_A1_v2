package com.example.firstapp.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.util.UUID

object ImageUtils {
    
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 80): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
    
    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }
    
    fun generateUniqueId(): String {
        return UUID.randomUUID().toString()
    }
    
    fun isBase64Valid(base64String: String?): Boolean {
        if (base64String.isNullOrBlank()) return false
        return try {
            Base64.decode(base64String, Base64.NO_WRAP)
            true
        } catch (e: Exception) {
            false
        }
    }
}
