package org.dx340.remote.sources

import org.junit.Assert.*
import org.junit.Test
import org.dx340.remote.protocol.AuthorizationSession

class SourceRequestTest {
    @Test fun reapprovedSameIdentityCannotMakeAnOldNativeCallbackValidAgain() {
        val authorization = AuthorizationSession()
        val oldLease = authorization.authorize("phone-A")
        var cacheWrites = 0
        val replies = mutableListOf<Pair<String?, String?>>()
        val request = SourceRequest<String>({ if (authorization.isCurrent(oldLease)) null else "AUTHORIZATION_DENIED" }, {}) { value, error -> replies += value to error }
        assertTrue(request.check())
        authorization.revoke("phone-A"); authorization.authorize("phone-A")
        if (request.check()) { cacheWrites++; request.finish("old page", null) }
        request.finish("late page", null)
        assertEquals(0, cacheWrites)
        assertEquals(listOf(null to "AUTHORIZATION_DENIED"), replies)
    }
    private class Fixture {
        var invalid: String? = null
        var cleanups = 0
        var cacheWrites = 0
        val replies = mutableListOf<Pair<String?, String?>>()
        val request = SourceRequest<String>({ invalid }, { cleanups++ }) { value, error -> replies += value to error }
        fun loaded() {
            if (!request.check()) return
            cacheWrites++
            request.finish("page", null)
        }
    }

    @Test fun timeoutDropsLatePagesBeforeCacheMutationAndCompletesOnlyOnce() {
        val f = Fixture()
        f.request.finish(null, "SOURCE_TIMEOUT")
        f.loaded(); f.loaded(); f.request.finish(null, "BROWSE_DENIED")
        assertEquals(0, f.cacheWrites)
        assertEquals(listOf(null to "SOURCE_TIMEOUT"), f.replies)
        assertEquals(1, f.cleanups)
    }

    @Test fun revokedAuthorizationPermissionOrStaleSourceCannotPopulateCache() {
        for (reason in listOf("AUTHORIZATION_DENIED", "PERMISSION_REVOKED", "STALE_LIBRARY", "BROWSE_UNAVAILABLE")) {
            val f = Fixture(); f.invalid = reason
            f.loaded(); f.loaded()
            assertEquals(0, f.cacheWrites)
            assertEquals(listOf(null to reason), f.replies)
            assertEquals(1, f.cleanups)
        }
    }

    @Test fun contextIsRecheckedAtCompletionEvenAfterInitialValidation() {
        val f = Fixture()
        assertTrue(f.request.check())
        f.invalid = "STALE_LIBRARY"
        f.request.finish("old page", null)
        assertEquals(listOf(null to "STALE_LIBRARY"), f.replies)
    }

    @Test fun successDropsDuplicateCallbacksAndTimeoutWithoutTouchingCacheAgain() {
        val f = Fixture(); f.loaded(); f.loaded()
        f.request.finish(null, "SOURCE_TIMEOUT")
        assertEquals(1, f.cacheWrites)
        assertEquals(listOf("page" to null), f.replies)
        assertEquals(1, f.cleanups)
    }

    @Test fun shutdownCancelsOnceAndCannotResurrectWithNewPermissionOrEpoch() {
        val f = Fixture()
        f.request.cancel("SOURCE_NOT_READY")
        f.invalid = null
        f.loaded(); f.request.cancel("SOURCE_NOT_READY")
        assertEquals(0, f.cacheWrites)
        assertEquals(listOf(null to "SOURCE_NOT_READY"), f.replies)
        assertEquals(1, f.cleanups)
    }

    @Test fun cleanupReentrancyCannotPublishTwiceOrWriteThroughOldCallback() {
        var writes = 0
        val replies = mutableListOf<String?>()
        lateinit var request: SourceRequest<String>
        request = SourceRequest({ null }, {
            if (request.check()) writes++
            request.finish("nested", null)
        }) { value, _ -> replies += value }
        request.finish("page", null)
        assertEquals(0, writes)
        assertEquals(listOf("page"), replies)
    }
}
