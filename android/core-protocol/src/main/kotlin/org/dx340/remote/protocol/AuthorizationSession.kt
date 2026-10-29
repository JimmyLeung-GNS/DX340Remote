package org.dx340.remote.protocol

import java.util.UUID

/** An address is an approved identity, not the lifetime of an authorized connection. */
class AuthorizationSession {
    class Lease internal constructor(val controller: String)
    private var active: Lease? = null
    private var revision = UUID.randomUUID().toString()
    @Synchronized fun ownershipRevision(): String = revision
    @Synchronized fun authorize(controller: String): Lease {
        require(controller.isNotBlank()) { "AUTHORIZATION_DENIED" }
        check(active == null || active?.controller == controller) { "CONTROLLER_BUSY" }
        return active ?: Lease(controller).also { active = it; revision = UUID.randomUUID().toString() }
    }
    /** Explicit approved-controller action. No playback/source/volume operation occurs here. */
    @Synchronized fun takeControl(controller: String, expectedRevision: String): Lease {
        require(controller.isNotBlank()) { "AUTHORIZATION_DENIED" }
        require(expectedRevision == revision) { "STALE_CONTROL" }
        if (active?.controller == controller) return active!!
        return Lease(controller).also { active = it; revision = UUID.randomUUID().toString() }
    }
    @Synchronized fun current(controller: String): Lease? = active?.takeIf { it.controller == controller }
    @Synchronized fun isCurrent(lease: Lease?): Boolean = lease != null && active === lease
    @Synchronized fun revoke(controller: String) { if (active?.controller == controller) { active = null; revision = UUID.randomUUID().toString() } }
    @Synchronized fun clear() { active = null; revision = UUID.randomUUID().toString() }
}
