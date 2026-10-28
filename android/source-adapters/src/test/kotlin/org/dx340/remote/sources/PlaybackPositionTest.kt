package org.dx340.remote.sources

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackPositionTest {
    private fun sample(position: Long = 10_000, duration: Long = 60_000,
                       speed: Double = 1.0, playing: Boolean = true,
                       updatedAt: Long = 1_000, now: Long = 6_000) =
        PlaybackPosition.snapshot(position, duration, speed, playing, updatedAt, now)

    @Test fun accountsForAgeAndPlaybackSpeedAtSnapshotTime() {
        assertEquals(PlaybackPosition(15_000, 1.0), sample())
        assertEquals(PlaybackPosition(20_000, 2.0), sample(speed = 2.0))
    }

    @Test fun pausedOrNonAdvancingSourcesKeepTheirReportedPosition() {
        assertEquals(PlaybackPosition(10_000, 0.0), sample(playing = false))
        assertEquals(PlaybackPosition(10_000, 0.0), sample(speed = 0.0))
    }

    @Test fun unknownPositionOrClockCannotInventElapsedTime() {
        assertEquals(PlaybackPosition(-1, 0.0), sample(position = -1))
        assertEquals(PlaybackPosition(10_000, 0.0), sample(updatedAt = 0))
        assertEquals(PlaybackPosition(10_000, 0.0), sample(updatedAt = 7_000))
        assertEquals(PlaybackPosition(10_000, 0.0), sample(duration = 0))
    }

    @Test fun invalidRatesFailClosed() {
        for (rate in listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0, 17.0)) {
            assertEquals(PlaybackPosition(10_000, 0.0), sample(speed = rate))
        }
    }

    @Test fun neverRunsPastTrackEndOrOverflowsForAnOldSample() {
        assertEquals(PlaybackPosition(60_000, 1.0), sample(position = 59_000))
        assertEquals(PlaybackPosition(60_000, 1.0), sample(now = Long.MAX_VALUE))
        assertEquals(PlaybackPosition(60_000, 0.0), sample(position = 70_000, playing = false))
        assertEquals(PlaybackPosition(-1, 0.0), sample(position = Long.MAX_VALUE))
    }
}
