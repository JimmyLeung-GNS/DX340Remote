package org.dx340.remote.protocol

/** Holds routing ownership while an asynchronous source-item validation is outstanding. */
class MutationGate {
    private var owner: String? = null
    fun enter(id: String) { check(owner == null || owner == id) { "SOURCE_BUSY" }; owner = id }
    fun leave(id: String) { if (owner == id) owner = null }
}
