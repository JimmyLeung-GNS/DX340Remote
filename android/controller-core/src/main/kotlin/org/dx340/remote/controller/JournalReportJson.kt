package org.dx340.remote.controller

import org.dx340.remote.protocol.RequestJournal
import org.json.JSONArray
import org.json.JSONObject

/** Accepts the allowlisted journal, never a playback snapshot or command arguments. */
internal object JournalReportJson {
    fun encode(journal: RequestJournal): JSONObject = value(journal.report()) as JSONObject
    private fun value(item: Any?): Any = when (item) {
        null -> JSONObject.NULL
        is Map<*, *> -> JSONObject().also { out -> item.forEach { (key, child) -> out.put(key as String, value(child)) } }
        is List<*> -> JSONArray().also { out -> item.forEach { out.put(value(it)) } }
        else -> item
    }
}
