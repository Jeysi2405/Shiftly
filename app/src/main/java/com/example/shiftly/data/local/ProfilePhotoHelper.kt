package com.example.shiftly.data.local

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ProfilePhotoHelper {

    private fun getProfileDir(context: Context): File {
        val dir = File(context.filesDir, "profile")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun saveProfilePhotoFromUri(context: Context, sourceUri: Uri): String? {
        return try {
            val dir = getProfileDir(context)
            val destFile = File(dir, "profile_avatar_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (destFile.exists() && destFile.length() > 0L) {
                dir.listFiles()?.forEach { existing ->
                    if (existing.absolutePath != destFile.absolutePath) {
                        existing.delete()
                    }
                }
                destFile.absolutePath
            } else {
                destFile.delete()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteProfilePhoto(filePath: String?) {
        if (filePath.isNullOrBlank()) return
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
        }
    }
}
