package org.dx340.remote.sources

/** Mango 4.1.37/240 publishes one-based row ordinals but consumes zero-based indices.
 * Only the final dispatch ID changes. Browser IDs/proofs/revisions stay untouched.
 * See the actual mismatch and installed public callback analysis in the test report.
 */
object MangoNativePlaybackId {
    const val SCHEME = "mango-4.1.37-240-zero-based-v1"
    fun resolve(versionName: String?, versionCode: Long, parent: String, id: String, title: String): String {
        require(versionName == "4.1.37" && versionCode == 240L) { "UNSUPPORTED_CAPABILITY" }
        val match = Regex("^(__ALL_MUSIC__|__NOW_PLAY_MUSIC__)([1-9][0-9]*)$").matchEntire(id)
        val prefix: String
        val ordinal: Int
        if (match != null) {
            prefix = match.groupValues[1]
            require(parent == prefix) { "EXPIRED_ITEM" }
            ordinal = match.groupValues[2].toIntOrNull() ?: error("EXPIRED_ITEM")
        } else {
            val album = Regex("^__MUSIC__((?:0|[1-9][0-9]*)_(?:0|[1-9][0-9]*)_[1-9][0-9]*)_([1-9][0-9]*)$").matchEntire(id)
                ?: error("UNSUPPORTED_CAPABILITY")
            val context = album.groupValues[1]
            require(parent == "__ARTIST_GENRE_ALBUM__$context") { "EXPIRED_ITEM" }
            require(context.split('_').all { it.toIntOrNull() != null }) { "EXPIRED_ITEM" }
            prefix = "__MUSIC__${context}_"
            ordinal = album.groupValues[2].toIntOrNull() ?: error("EXPIRED_ITEM")
        }
        // The public browser appends this exact ordinal to its row label as well.
        require(title.endsWith(" $ordinal")) { "EXPIRED_ITEM" }
        return prefix + (ordinal - 1)
    }
}
