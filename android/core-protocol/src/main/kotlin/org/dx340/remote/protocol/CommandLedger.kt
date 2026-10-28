package org.dx340.remote.protocol

/** Do not evict IDs within a host epoch: an old retransmission must never execute twice. */
class CommandLedger(private val limit: Int = 2048) {
    private val entries = linkedMapOf<String, Pair<String, String?>>()
    @Synchronized fun begin(id: String, fingerprint: String): String? {
        require(id.matches(Regex("[A-Za-z0-9-]{1,64}"))) { "MALFORMED_MESSAGE" }
        entries[id]?.let {
            require(it.first == fingerprint) { "REQUEST_ID_REUSED" }
            return it.second ?: "PENDING"
        }
        check(entries.size < limit) { "SESSION_LIMIT" }
        entries[id] = fingerprint to null
        return null
    }
    @Synchronized fun finish(id: String, result: String) { entries[id] = entries.getValue(id).first to result }
}

object RoutingPolicy {
    fun validate(expectedHost: String, host: String, expectedSource: String, source: String, expectedEpoch: String, epoch: String, capability: Boolean) {
        require(host == expectedHost) { "STALE_HOST" }
        require(source == expectedSource) { "WRONG_SOURCE" }
        require(epoch == expectedEpoch) { "STALE_SOURCE" }
        require(capability) { "UNSUPPORTED_CAPABILITY" }
    }
}
