package com.example.shiftly

import com.example.shiftly.data.local.ReceiptStorageHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class ReceiptStorageHelperTest {

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    @Test
    fun formatWatermarkTimestamp_matchesExpectedPattern() {
        val originalDefault = TimeZone.getDefault()
        try {
            val utc = TimeZone.getTimeZone("UTC")
            TimeZone.setDefault(utc)
            val calendar = Calendar.getInstance(utc, Locale.US).apply {
                set(2025, Calendar.MAY, 20, 8, 0, 15)
                set(Calendar.MILLISECOND, 0)
            }
            val formatted = ReceiptStorageHelper.formatWatermarkTimestamp(
                timestampMillis = calendar.timeInMillis,
                locale = Locale.US
            )
            assertEquals("2025-05-20 08:00:15 AM", formatted)

            calendar.set(Calendar.HOUR_OF_DAY, 20)
            calendar.set(Calendar.MINUTE, 5)
            calendar.set(Calendar.SECOND, 9)
            val formattedPm = ReceiptStorageHelper.formatWatermarkTimestamp(
                timestampMillis = calendar.timeInMillis,
                locale = Locale.US
            )
            assertEquals("2025-05-20 08:05:09 PM", formattedPm)
        } finally {
            TimeZone.setDefault(originalDefault)
        }
    }

    @Test
    fun pruneOrphanedReceipts_keepsReferencedAndRecentDrafts_deletesAgedOrphans() {
        val receiptsDir = tempFolder.newFolder("receipts")
        val nowMillis = 1_750_000_000_000L
        val graceMillis = 60 * 60 * 1000L

        val referencedOldFile = File(receiptsDir, "receipt_active.jpg").apply {
            writeText("active")
            setLastModified(nowMillis - graceMillis * 10)
        }
        val unreferencedRecentFile = File(receiptsDir, "receipt_recent_draft.jpg").apply {
            writeText("draft")
            setLastModified(nowMillis - graceMillis / 2)
        }
        val unreferencedOldFile = File(receiptsDir, "receipt_orphan.jpg").apply {
            writeText("orphan")
            setLastModified(nowMillis - graceMillis * 2)
        }
        val nestedDir = File(receiptsDir, "nested").apply { mkdirs() }

        val deleted = ReceiptStorageHelper.pruneOrphanedReceipts(
            receiptsDir = receiptsDir,
            referencedPaths = setOf(referencedOldFile.absolutePath),
            nowMillis = nowMillis,
            maxUnreferencedAgeMillis = graceMillis
        )

        assertEquals(1, deleted)
        assertTrue(referencedOldFile.exists())
        assertTrue(unreferencedRecentFile.exists())
        assertFalse(unreferencedOldFile.exists())
        assertTrue(nestedDir.exists())
    }
}
