package com.example.shiftly.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptStorageHelper {

    private const val WATERMARK_DATE_PATTERN = "yyyy-MM-dd hh:mm:ss a"
    private const val MAX_CAMERA_DIMENSION_PX = 2048

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
     * Formats [timestampMillis] as `yyyy-MM-dd hh:mm:ss a` (e.g. `2025-05-20 08:00:15 AM`).
     */
    fun formatWatermarkTimestamp(
        timestampMillis: Long = System.currentTimeMillis(),
        locale: Locale = Locale.US
    ): String {
        val formatter = SimpleDateFormat(WATERMARK_DATE_PATTERN, locale)
        return formatter.format(Date(timestampMillis))
    }

    /**
     * Draws a high-contrast white date/time watermark onto the bottom-right corner of [bitmap].
     */
    fun drawTimestampWatermark(
        bitmap: Bitmap,
        timestampMillis: Long = System.currentTimeMillis()
    ): Bitmap {
        val target = if (bitmap.isMutable) {
            bitmap
        } else {
            bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        val timestampText = formatWatermarkTimestamp(timestampMillis)
        val canvas = Canvas(target)
        val minSide = minOf(target.width, target.height).toFloat()
        val textSizePx = (minSide * 0.032f).coerceIn(20f, 64f)
        val marginPx = (minSide * 0.025f).coerceIn(12f, 48f)
        val padX = textSizePx * 0.45f
        val padY = textSizePx * 0.30f
        val cornerRadius = textSizePx * 0.28f

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = textSizePx
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            setShadowLayer(
                (textSizePx * 0.14f).coerceAtLeast(2f),
                0f,
                (textSizePx * 0.06f).coerceAtLeast(1f),
                Color.argb(215, 0, 0, 0)
            )
        }

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(150, 0, 0, 0)
            style = Paint.Style.FILL
        }

        val textWidth = textPaint.measureText(timestampText)
        val fm = textPaint.fontMetrics
        val textHeight = fm.descent - fm.ascent

        val rightEdge = target.width - marginPx
        val bottomEdge = target.height - marginPx
        val pillRect = RectF(
            (rightEdge - textWidth - padX * 2f).coerceAtLeast(marginPx),
            (bottomEdge - textHeight - padY * 2f).coerceAtLeast(marginPx),
            rightEdge,
            bottomEdge
        )

        canvas.drawRoundRect(pillRect, cornerRadius, cornerRadius, bgPaint)

        val baselineY = pillRect.centerY() - (fm.ascent + fm.descent) / 2f
        val textX = pillRect.right - padX
        canvas.drawText(timestampText, textX, baselineY, textPaint)

        return target
    }

    /**
     * Decodes the captured camera JPEG [file], normalizes EXIF orientation so the bottom-right
     * corner matches the visual orientation, draws the timestamp watermark, and re-saves to [file].
     */
    fun applyTimestampWatermark(
        file: File,
        timestampMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (!file.exists() || file.length() <= 0L) return false

        var decoded: Bitmap? = null
        var oriented: Bitmap? = null
        var watermarked: Bitmap? = null
        val tempFile = File(file.parentFile ?: return false, "${file.nameWithoutExtension}_wm.tmp")

        return try {
            val orientation = try {
                ExifInterface(file.absolutePath).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } catch (_: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }

            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
                return false
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(
                    boundsOptions.outWidth,
                    boundsOptions.outHeight
                )
                inMutable = true
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            decoded = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return false
            oriented = applyExifOrientation(decoded, orientation)
            if (oriented !== decoded) {
                decoded.recycle()
                decoded = null
            }

            watermarked = drawTimestampWatermark(oriented, timestampMillis)
            if (watermarked !== oriented) {
                oriented.recycle()
                oriented = null
            }

            FileOutputStream(tempFile).use { out ->
                if (!watermarked.compress(Bitmap.CompressFormat.JPEG, 90, out)) {
                    return false
                }
                out.flush()
            }

            if (tempFile.exists() && tempFile.length() > 0L) {
                if (file.exists() && !file.delete()) {
                    tempFile.copyTo(file, overwrite = true)
                    tempFile.delete()
                } else if (!tempFile.renameTo(file)) {
                    tempFile.copyTo(file, overwrite = true)
                    tempFile.delete()
                }
                true
            } else {
                tempFile.delete()
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            tempFile.delete()
            false
        } catch (oom: OutOfMemoryError) {
            oom.printStackTrace()
            tempFile.delete()
            false
        } finally {
            decoded?.recycle()
            oriented?.recycle()
            watermarked?.recycle()
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int): Int {
        var inSampleSize = 1
        val longestSide = maxOf(width, height)
        while (longestSide / inSampleSize > MAX_CAMERA_DIMENSION_PX) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
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
