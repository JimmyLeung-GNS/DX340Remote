package org.dx340.remote.protocol

/** Bounded observations only. Never authorizes, sends or retries a command. */
class RequestJournal(private val capacity: Int = 256, private val nowMs: () -> Long) {
    init { require(capacity in 1..512) }
    private val origin = nowMs()
    private data class Entry(val id: String, val operation: String, val source: String, val start: Long,
        var accepted: Boolean = false, var acceptedAfter: Long? = null, var finishedAfter: Long? = null, var writeAcknowledgedAfter: Long? = null,
        var result: String = "pending", var terminalApplied: Boolean = false, var outcome: String = "none", var error: String? = null)
    private val entries = linkedMapOf<String, Entry>()
    private val connections = ArrayDeque<Map<String, Any?>>()
    private var evictedRequests = 0L
    private var evictedConnections = 0L
    private fun elapsed(end: Long, start: Long): Long? = if (end >= start && start >= 0) end - start else null
    fun begin(id: String, operation: String, source: String) {
        if (!id.matches(Regex("[A-Za-z0-9_-]{1,64}")) || entries.containsKey(id)) return
        if (entries.size == capacity) { entries.remove(entries.keys.first()); evictedRequests++ }
        entries[id] = Entry(id, operation.takeIf { it in operations } ?: "OTHER", sourceCategory(source), nowMs())
    }
    fun phase(value: String) {
        val phase = value.takeIf { it in phases } ?: "other"
        if (connections.lastOrNull()?.get("phase") == phase) return
        if (connections.size == capacity) { connections.removeFirst(); evictedConnections++ }
        connections.addLast(mapOf("atMs" to elapsed(nowMs(), origin), "phase" to phase))
    }
    fun reply(id: String, type: String, outcome: String = "", error: String = ""): Map<String, Any?>? {
        val entry = entries[id]?.takeIf { it.result == "pending" } ?: return null
        if (type == "ACCEPTED") {
            if (entry.accepted) return null
            entry.accepted = true; entry.acceptedAfter = elapsed(nowMs(), entry.start)
        } else {
            entry.finishedAfter = elapsed(nowMs(), entry.start)
            entry.result = when (type) { "HELLO", "RESULT" -> "result"; "ERROR" -> "error"; else -> "other_reply" }
            entry.outcome = outcome.takeIf { it in setOf("observed", "dispatched") } ?: "none"
            entry.error = if (type == "ERROR") error.takeIf { it in errors } ?: "OTHER" else null
        }
        return snapshot(entry)
    }
    fun markTerminalApplied(id: String) {
        entries[id]?.takeIf { it.result in setOf("result", "error", "other_reply") }?.terminalApplied = true
    }
    fun writeAcknowledged(id: String) {
        val entry = entries[id]?.takeIf { it.writeAcknowledgedAfter == null && it.result in setOf("pending", "result", "error", "other_reply") } ?: return
        entry.writeAcknowledgedAfter = elapsed(nowMs(), entry.start)
    }
    fun finish(id: String, reason: String): Map<String, Any?>? {
        val entry = entries[id]?.takeIf { it.result == "pending" } ?: return null
        entry.result = reason.takeIf { it in endings } ?: "other"
        entry.finishedAfter = elapsed(nowMs(), entry.start)
        return snapshot(entry)
    }
    fun abandon(reason: String = "connection_lost"): List<Map<String, Any?>> = entries.keys.toList().mapNotNull { finish(it, reason) }
    private fun snapshot(e: Entry): Map<String, Any?> = linkedMapOf("id" to e.id, "operation" to e.operation, "source" to e.source,
        "startedMs" to elapsed(e.start, origin), "writeAcknowledgedAfterMs" to e.writeAcknowledgedAfter,
        "accepted" to e.accepted, "acceptedAfterMs" to e.acceptedAfter,
        "finishedAfterMs" to e.finishedAfter, "result" to e.result, "terminalApplied" to e.terminalApplied, "hostOutcome" to e.outcome, "error" to e.error)
    fun report(): Map<String, Any?> = linkedMapOf("formatVersion" to 1, "clock" to "monotonic_ms_relative_to_model", "capacity" to capacity,
        "evictedRequests" to evictedRequests, "evictedConnections" to evictedConnections,
        "requests" to entries.values.map(::snapshot), "connections" to connections.map { it.toMap() })
    companion object {
        private val operations = setOf("HELLO", "AUTHORIZE", "REQUEST_CONTROL", "GET_STATE", "GET_QUEUE", "GET_ARTWORK", "GET_CAPABILITIES", "LIST_SOURCES", "SUBSCRIBE_STATE", "SET_PLAYING", "NEXT", "PREVIOUS", "SEEK", "SELECT_SOURCE", "PLAY_ITEM", "PLAY_QUEUE_ITEM", "ADJUST_VOLUME", "BROWSE", "SEARCH", "SET_SHUFFLE", "SET_REPEAT")
        // Wire source IDs are package names. Export only these fixed categories.
        private fun sourceCategory(source: String): String = when (source) {
            "com.ibasso.music", "mango" -> "mango"
            "com.apple.android.music", "apple" -> "apple"
            "none", "" -> source
            else -> "other"
        }
        private val errors = setOf("AUTHORIZATION_DENIED", "LOCAL_APPROVAL_REQUIRED", "CONTROLLER_BUSY", "STALE_HOST", "STALE_CONTROL", "CONTROL_REQUEST_USED", "STALE_SOURCE", "SOURCE_UNAVAILABLE", "SOURCE_NOT_READY", "SOURCE_BUSY", "UNSUPPORTED", "STALE_LIBRARY", "EXPIRED_ITEM", "STALE_QUEUE", "STALE_ARTWORK", "INVALID_REQUEST", "BAD_REQUEST", "MALFORMED_REQUEST", "PROTOCOL_MISMATCH", "STALE_VOLUME", "VOLUME_AT_BOUNDARY", "VOLUME_PROFILE_CHANGED", "VOLUME_VERIFICATION_REQUIRED", "VOLUME_SOURCE_NOT_READY", "UI_PERMISSION_REQUIRED", "UI_SCREEN_UNLOCK_REQUIRED", "UI_FOREGROUND_REQUIRED", "UI_SETUP_REQUIRED", "UI_UNEXPECTED_SCREEN", "UI_STORAGE_CONTEXT_REQUIRED", "UI_VERSION_UNVERIFIED", "UI_ACTIVE_PAGE_UNVERIFIED", "UI_TREE_CHANGED", "UI_ACTION_TIMEOUT", "UI_ACTION_INTERRUPTED", "UI_MODE_DISABLED", "UI_BUSY", "RESPONSE_TOO_LARGE")
        private val phases = setOf("idle", "preparing", "searching", "choosing", "connecting", "approval", "busy", "connected", "permission", "bluetoothOff", "failed")
        private val endings = setOf("timeout", "send_failed", "connection_lost", "session_reset")
    }
}
