package org.dx340.remote.protocol

/** Reconnection returns to observation, never to queued playback mutations. */
class ControllerSession {
    var hostEpoch = ""; private set
    var authorized = false; private set
    val pending = mutableMapOf<String, String>()
    fun lost() { authorized = false; hostEpoch = ""; pending.clear() }
    fun hello(epoch: String) { require(epoch.isNotBlank()); lost(); hostEpoch = epoch }
    fun approve(epoch: String) { require(hostEpoch.isNotEmpty() && epoch == hostEpoch) { "STALE_HOST" }; authorized = true }
    fun register(id: String, operation: String) {
        require(authorized || operation == "HELLO" || operation == "AUTHORIZE") { "AUTHORIZATION_DENIED" }
        require(pending.size < 8) { "PENDING_LIMIT" }
        pending[id] = operation
    }
}
