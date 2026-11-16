package com.example.shiftly.data.local

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ReceiptStorageHelper {

    private fun getReceiptsDir(context: Context): File {
        val dir = File(context.filesDir, "receipts")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Copies an image chosen from the Android Photo Picker into private offline app storage.
     * Returns the saved absolute file path.
     */
    fun saveUriToLocalReceipts(context: Context, sourceUri: Uri): String? {
        return try {
            val destFile = File(getReceiptsDir(context), "receipt_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (destFile.exists() && destFile.length() > 0L) {
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

    /**
     * Creates a private file and content Uri for capturing a receipt photo with the camera.
     */
    fun createCameraReceiptTarget(context: Context): Pair<File, Uri> {
        val file = File(getReceiptsDir(context), "receipt_cam_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return file to uri
    }

    /**
     * Cleans up the local receipt file when an expense is deleted or an attachment is discarded.
     */
    fun deleteReceiptFile(filePath: String?) {
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