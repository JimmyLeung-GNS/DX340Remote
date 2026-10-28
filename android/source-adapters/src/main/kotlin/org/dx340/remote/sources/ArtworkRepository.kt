package org.dx340.remote.sources

import android.graphics.Bitmap
import android.util.LruCache
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

/** Only public MediaMetadata artwork. No source-private files or network fetches. */
class ArtworkRepository {
    private val images = object : LruCache<String, ByteArray>(1024 * 1024) {
        override fun sizeOf(key: String, value: ByteArray) = value.size
    }
    private val latest = mutableMapOf<String, Pair<Int, String>>()
    fun remember(source: String, bitmap: Bitmap?): String? {
        if (bitmap == null || bitmap.isRecycled) return null
        latest[source]?.takeIf { it.first == bitmap.generationId && images.get(it.second) != null }?.let { return it.second }
        val scale = minOf(1.0, 256.0 / maxOf(bitmap.width, bitmap.height))
        val small = Bitmap.createScaledBitmap(bitmap, maxOf(1, (bitmap.width * scale).toInt()), maxOf(1, (bitmap.height * scale).toInt()), true)
        val out = ByteArrayOutputStream()
        var quality = 80
        do { out.reset(); small.compress(Bitmap.CompressFormat.JPEG, quality, out); quality -= 15 } while (out.size() > 44 * 1024 && quality >= 20)
        if (small !== bitmap) small.recycle()
        if (out.size() > 44 * 1024) return null
        val bytes = out.toByteArray()
        val key = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
        images.put(key, bytes); latest[source] = bitmap.generationId to key; return key
    }
    fun get(key: String): ByteArray? = images.get(key)
}
