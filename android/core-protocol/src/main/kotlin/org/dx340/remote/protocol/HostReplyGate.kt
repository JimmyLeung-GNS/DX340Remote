package org.dx340.remote.protocol

enum class HostReplyDecision { IGNORE, HELLO, CURRENT, RECONNECT }

/** Classify before dispatching ERROR/ACCEPTED or publishing any reply data. */
object HostReplyGate {
    const val CHANGED_MESSAGE = "Player connection changed. Connect again to refresh."
    fun classify(type: String, operation: String?, expectedHost: String, replyHost: String, authorized: Boolean): HostReplyDecision {
        if (type == "STATE_CHANGED") {
            if (!authorized) return HostReplyDecision.IGNORE
        } else if (operation == null) return HostReplyDecision.IGNORE
        if (type == "HELLO") return if (operation == "HELLO" && replyHost.isNotBlank()) HostReplyDecision.HELLO else HostReplyDecision.RECONNECT
        if (expectedHost.isEmpty()) return if (operation == "HELLO" && type == "ERROR") HostReplyDecision.CURRENT else HostReplyDecision.RECONNECT
        return if (replyHost == expectedHost) HostReplyDecision.CURRENT else HostReplyDecision.RECONNECT
    }
}
