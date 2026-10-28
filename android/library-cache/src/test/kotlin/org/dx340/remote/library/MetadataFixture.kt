package org.dx340.remote.library

import org.dx340.remote.protocol.Reassembler
import org.dx340.remote.protocol.Wire
import org.json.JSONArray
import org.json.JSONObject

/** Generated metadata only. No files, Android source service or playback engine is opened. */
internal object MetadataFixture {
    const val TRACKS = 10_000
    const val PAGE_SIZE = 25
    const val SOURCE = "synthetic:mango"
    const val REVISION = "fixture-r1"
    data class Track(val id: String, val title: String, val artist: String, val album: String, val format: String) {
        fun fingerprint() = listOf(id, title, artist, album, format).joinToString("\u0000")
        fun json() = JSONObject().put("id", id).put("title", title).put("artist", artist)
            .put("album", album).put("format", format)
    }
    fun collection(): List<Track> = List(TRACKS) { n ->
        Track("fixture-track-$n", when (n % 5) {
            0 -> "03. 同じファイル名.wav" // Deliberate duplicate titles in different albums.
            1 -> "第${n}首 · 星と海 🌊"
            2 -> "Café \"Live\" \\ Take $n"
            3 -> "Long title $n " + "樂曲".repeat(90)
            else -> "Track $n\nSecond line"
        }, if (n % 5 == 0) "" else "Artist ${n % 80} · 音楽", "Album ${n / 10}", if (n % 5 == 0) "WAV" else "FLAC")
    }
    fun key(page: Int, revision: String = REVISION) = LibraryIndex.Key(SOURCE, revision, "songs", page)
    fun page(tracks: List<Track>, page: Int): List<Track> {
        require(page >= 0)
        val start = minOf(page.toLong() * PAGE_SIZE, tracks.size.toLong()).toInt()
        return tracks.subList(start, minOf(start + PAGE_SIZE, tracks.size))
    }
    // Same documented limits/fields as native browse, without emulating a source service.
    fun encode(page: Int, tracks: List<Track>): ByteArray {
        val rows = JSONArray()
        tracks.forEach { rows.put(JSONObject().put("id", it.id).put("title", it.title.take(128))
            .put("subtitle", it.artist.take(64)).put("browsable", false).put("playable", true)) }
        val data = JSONObject().put("parent", "songs").put("revision", REVISION).put("page", page)
            .put("nextPage", if ((page + 1) * PAGE_SIZE < TRACKS) page + 1 else JSONObject.NULL).put("items", rows)
        return JSONObject().put("v", 1).put("type", "RESULT").put("id", "fixture-$page")
            .put("hostEpoch", "synthetic-host-epoch")
            .put("data", data).toString().toByteArray(Charsets.UTF_8)
    }
    fun decode(bytes: ByteArray) = JSONObject(String(bytes, Charsets.UTF_8)).getJSONObject("data")
    fun frameRoundTrip(bytes: ByteArray, capacity: Int): ByteArray {
        val reassembler = Reassembler()
        var result: ByteArray? = null
        Wire.frames(bytes, capacity, 7).forEach { frame -> reassembler.accept(frame)?.let { result = it } }
        return checkNotNull(result)
    }
}
