package org.dx340.remote.sources

import org.junit.Assert.*
import org.junit.Test

class SourceSessionAccessTest {
    @Test fun startupDenialThenGrantRetriesRegistrationWithoutRestart() {
        var granted = false; var registrations = 0; var invalidations = 0
        val access = SourceSessionAccess({ registrations++ }, {}) { invalidations++ }
        fun read(): List<String> { if (!granted) throw SecurityException(); return listOf("mango") }
        assertNull(access.refresh(::read)); assertNull(access.refresh(::read))
        assertEquals("NOTIFICATION_ACCESS_REQUIRED", access.error)
        assertEquals(0, registrations); assertEquals(1, invalidations)
        granted = true
        assertEquals(listOf("mango"), access.refresh(::read))
        assertNull(access.error); assertEquals(1, registrations)
        access.refresh(::read)
        assertEquals(1, registrations)
    }

    @Test fun grantRevokedBetweenReadAndRegistrationStillInvalidatesAndCanRecover() {
        var granted = false; var attempts = 0; var invalidations = 0
        val access = SourceSessionAccess({ attempts++; if (!granted) throw SecurityException() }, {}) { invalidations++ }
        assertNull(access.refresh { "sessions read before revoke" })
        assertEquals(1, invalidations)
        granted = true
        assertEquals("fresh sessions", access.refresh { "fresh sessions" })
        assertEquals(2, attempts); assertNull(access.error)
    }

    @Test fun lossCancelsOldRequestsAndLeasesEvenIfPermissionReturnsBeforeCallback() {
        var granted = true; var registrations = 0; var removals = 0; var mutations = 0
        val callbacks = SourceCallbackGuard()
        val oldLease = callbacks.replace("mango")
        val replies = mutableListOf<String?>()
        val request = SourceRequest<String>({ null }, {}) { _, error -> replies += error }
        val access = SourceSessionAccess({ registrations++ }, { removals++ }) {
            callbacks.invalidateAll(); request.cancel("PERMISSION_REVOKED")
        }
        fun read(): String { if (!granted) throw SecurityException(); return "sessions" }
        access.refresh(::read)
        granted = false
        access.refresh(::read); access.refresh(::read)
        assertEquals(1, removals)
        granted = true
        access.refresh(::read)
        callbacks.dispatch(oldLease) { mutations++ }
        if (request.check()) mutations++
        assertEquals(0, mutations)
        assertEquals(listOf("PERMISSION_REVOKED"), replies)
        assertEquals(2, registrations)
        val freshLease = callbacks.replace("mango")
        callbacks.dispatch(freshLease) { mutations++ }
        assertEquals(1, mutations)
    }

    @Test fun shutdownUnregistersOnceAndNeverReregistersEvenAfterCleanupFailure() {
        var registrations = 0; var removals = 0; var reads = 0
        val access = SourceSessionAccess({ registrations++ }, { removals++; throw SecurityException() }, {})
        access.refresh { reads++; true }
        access.close(); access.close()
        assertNull(access.refresh { reads++; true })
        assertEquals(1, registrations); assertEquals(1, removals); assertEquals(1, reads)
    }

    @Test fun revocationDuringSessionAttachmentAlsoDiscardsThatGeneration() {
        var registrations = 0; var invalidations = 0
        val access = SourceSessionAccess({ registrations++ }, {}, { invalidations++ })
        access.refresh { "sessions" }
        // A later Android registerCallback can fail after discovery succeeded.
        access.permissionDenied(); access.permissionDenied()
        assertEquals(1, invalidations)
        assertEquals("NOTIFICATION_ACCESS_REQUIRED", access.error)
        assertEquals("new sessions", access.refresh { "new sessions" })
        assertEquals(2, registrations)
        assertNull(access.error)
    }
}
