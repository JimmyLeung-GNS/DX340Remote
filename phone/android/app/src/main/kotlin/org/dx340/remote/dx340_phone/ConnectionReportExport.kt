package org.dx340.remote.dx340_phone

import android.content.ContentResolver
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import java.util.UUID
import java.io.File
import java.security.MessageDigest
import org.json.JSONObject

/** Creates a new app-owned report only after the user chooses Save connection report. */
internal object ConnectionReportExport {
    fun save(resolver: ContentResolver, report: String, ownApk: String): String {
        // Hash only our installed base APK; this is build identity, never a music/storage scan.
        val digest = MessageDigest.getInstance("SHA-256")
        File(ownApk).inputStream().use { input ->
            val buffer = ByteArray(65536)
            while (true) { val count = input.read(buffer); if (count < 0) break; if (count > 0) digest.update(buffer, 0, count) }
        }
        val value = JSONObject(report)
        value.getJSONObject("app").put("baseApkSha256", digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) })
        val bytes = value.toString(2).toByteArray(Charsets.UTF_8)
        require(bytes.size <= 262144) { "Report exceeds its bounded export size" }
        val name = "connection-${UUID.randomUUID()}.json"
        val folder = Environment.DIRECTORY_DOWNLOADS + "/DX340Remote"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, folder)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("Cannot create report")
        try {
            val stream = resolver.openOutputStream(uri, "w") ?: error("Cannot write report")
            stream.use { it.write(bytes) }
            val published = resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
            check(published == 1) { "Cannot publish report" }
            return "$folder/$name"
        } catch (failure: Exception) {
            // Only the exact URI created by this operation is eligible for cleanup.
            runCatching { resolver.delete(uri, null, null) }
            throw failure
        }
    }
}
