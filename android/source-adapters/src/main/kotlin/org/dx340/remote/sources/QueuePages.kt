package org.dx340.remote.sources

import org.json.JSONArray
import org.json.JSONObject

/** A bounded window of the current public source queue, never a playback operation. */
object QueuePages {
    data class Entry(val id: String, val title: String)
    const val MAX_ITEMS = 10000
    const val MAX_PAGE_BYTES = 49152

    fun render(queue: List<Entry>, revision: String, args: JSONObject): JSONObject {
        val rawOffset = if (args.has("offset")) args.get("offset") else 0
        require(rawOffset is Int || rawOffset is Long) { "INVALID_ARGUMENT" }
        val offsetLong = (rawOffset as Number).toLong()
        require(offsetLong in 0..MAX_ITEMS.toLong()) { "INVALID_ARGUMENT" }
        val offset = offsetLong.toInt()
        require(queue.size <= MAX_ITEMS) { "QUEUE_LIMIT" }
        val expected = if (args.isNull("revision")) null else args.get("revision").also {
            require(it is String) { "INVALID_ARGUMENT" }
        } as String
        require((offset == 0 && expected == null) || expected == revision) { "STALE_LIBRARY" }
        require(offset <= queue.size) { "INVALID_ARGUMENT" }
        val items = JSONArray()
        val result = JSONObject().put("revision", revision).put("offset", offset).put("total", queue.size).put("items", items)
        fun continuation() { result.put("nextOffset", (offset + items.length()).takeIf { it < queue.size } ?: JSONObject.NULL) }
        for (index in offset until minOf(offset + 50, queue.size)) {
            val entry = queue[index]
            items.put(JSONObject().put("id", entry.id).put("title", entry.title))
            continuation()
            if (result.toString().toByteArray(Charsets.UTF_8).size > MAX_PAGE_BYTES) {
                items.remove(items.length() - 1)
                require(items.length() > 0) { "MESSAGE_TOO_LARGE" }
                break
            }
        }
        continuation()
        return result
    }
}
