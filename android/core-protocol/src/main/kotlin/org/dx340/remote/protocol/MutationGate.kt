package org.dx340.remote.protocol

/** Holds routing ownership while an asynchronous source-item validation is outstanding. */
class MutationGate {
    class Lease internal constructor(val id: String)
    private var owner: Lease? = null
    // Request duplicates are handled by the ledger before entering this gate. Another
    // controller/session must not bypass ownership by choosing the same request ID.
    fun enter(id: String): Lease { check(owner == null) { "SOURCE_BUSY" }; return Lease(id).also { owner = it } }
    fun leave(lease: Lease?) { if (owner === lease) owner = null }
}
