package org.dx340.remote.protocol

/** A second transport is for an already approved/bonded controller, never a new pairing. */
object PeerAdmission {
    fun allowed(existing: Int, approved: Boolean, bonded: Boolean, pairingOpen: Boolean): Boolean =
        existing in 0..1 && if (existing == 0) pairingOpen || approved && bonded else approved && bonded
}
