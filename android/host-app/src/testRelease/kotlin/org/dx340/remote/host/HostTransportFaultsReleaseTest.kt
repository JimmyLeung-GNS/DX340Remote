package org.dx340.remote.host

import org.junit.Assert.assertFalse
import org.junit.Test

class HostTransportFaultsReleaseTest {
    @Test fun releaseCannotArmOrIgnoreEvenWithDirectInvocation() {
        val faults = HostTransportFaults { error("Release must not evaluate diagnostic clock") }
        val link = Any()
        repeat(3) {
            assertFalse(faults.arm(link))
            assertFalse(faults.ignoreCompletion(link, true))
            assertFalse(faults.ignoreCompletion(link, false))
            faults.clear()
        }
    }
}
