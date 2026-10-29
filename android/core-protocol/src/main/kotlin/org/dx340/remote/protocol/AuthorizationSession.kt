package org.dx340.remote.protocol

/** An address is an approved identity, not the lifetime of an authorized connection. */
class AuthorizationSession {
    class Lease internal constructor(val controller: String)
    private var active: Lease? = null
    @Synchronized fun authorize(controller: String): Lease {
        require(controller.isNotBlank()) { "AUTHORIZATION_DENIED" }
        check(active == null || active?.controller == controller) { "CONTROLLER_BUSY" }
        return active ?: Lease(controller).also { active = it }
    }
    @Synchronized fun current(controller: String): Lease? = active?.takeIf { it.controller == controller }
    @Synchronized fun isCurrent(lease: Lease?): Boolean = lease != null && active === lease
    @Synchronized fun revoke(controller: String) { if (active?.controller == controller) active = null }
    @Synchronized fun clear() { active = null }
}
