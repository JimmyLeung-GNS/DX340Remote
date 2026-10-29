package org.dx340.remote.protocol

/** A visible action consumes a proposal once; reconnects and timeouts never replay it. */
class ControlProposal {
    private var revision: String? = null
    val available get() = revision != null
    fun observeBusy(replyHost: String, expectedHost: String, value: String) {
        revision = value.takeIf { expectedHost.isNotEmpty() && replyHost == expectedHost && it.matches(Regex("[A-Za-z0-9-]{1,64}")) }
    }
    fun consume(): String? = revision.also { revision = null }
    fun clear() { revision = null }
}
