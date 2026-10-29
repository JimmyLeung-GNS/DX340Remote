package org.dx340.remote.protocol

/** Holds routing ownership while an asynchronous source-item validation is outstanding. */
class MutationGate {
    private var owner: String? = null
    // Request duplicates are handled by the ledger before entering this gate. Another
    // controller/session must not bypass ownership by choosing the same request ID.
    fun enter(id: String) { check(owner == null) { "SOURCE_BUSY" }; owner = id }
    fun leave(id: String) { if (owner == id) owner = null }
}
