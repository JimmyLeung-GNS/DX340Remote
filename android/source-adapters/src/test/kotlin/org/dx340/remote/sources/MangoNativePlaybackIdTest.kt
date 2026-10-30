package org.dx340.remote.sources

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MangoNativePlaybackIdTest {
    private fun resolve(parent: String, id: String, title: String) = MangoNativePlaybackId.resolve("4.1.37", 240, parent, id, title)

    @Test fun actualWrongSecondTrackMapsToZeroBasedSecondInsteadOfThird() {
        assertEquals("__MUSIC__0_0_1_1", resolve("__ARTIST_GENRE_ALBUM__0_0_1", "__MUSIC__0_0_1_2", "白日夢 2"))
    }
    @Test fun firstLastAndLaterPageOrdinalsPreserveAlbumContext() {
        for (ordinal in listOf(1, 23, 26, 76)) {
            assertEquals("__MUSIC__2_0_8_${ordinal - 1}", resolve("__ARTIST_GENRE_ALBUM__2_0_8", "__MUSIC__2_0_8_$ordinal", "Track $ordinal"))
        }
        assertEquals("__MUSIC__0_3_4_0", resolve("__ARTIST_GENRE_ALBUM__0_3_4", "__MUSIC__0_3_4_1", "Intro 1"))
    }
    @Test fun allMusicAndNowPlayingUseTheirOwnValidatedParent() {
        assertEquals("__ALL_MUSIC__4", resolve("__ALL_MUSIC__", "__ALL_MUSIC__5", "777 5"))
        assertEquals("__NOW_PLAY_MUSIC__0", resolve("__NOW_PLAY_MUSIC__", "__NOW_PLAY_MUSIC__1", "Track 1"))
        assertThrows(IllegalArgumentException::class.java) { resolve("__ALL_MUSIC__", "__NOW_PLAY_MUSIC__1", "Track 1") }
    }
    @Test fun changedVersionCannotBorrowThisMapping() {
        for ((name, code) in listOf("4.1.38" to 240L, "4.1.37" to 241L, null to 240L)) {
            assertThrows(IllegalArgumentException::class.java) { MangoNativePlaybackId.resolve(name, code, "__ALL_MUSIC__", "__ALL_MUSIC__1", "Track 1") }
        }
    }
    @Test fun malformedIdsAndOverflowCannotBecomePlaybackIndices() {
        for (id in listOf("__MUSIC__0_0_1_0", "__MUSIC__0_0_1_-1", "__MUSIC__0_0_1_01", "__MUSIC__0_0_0_1", "__MUSIC__0_0_1_2147483648", "__MUSIC__0_0_2147483648_1", "__ALL_MUSIC__0", "mango-ui:track", "__MUSIC__0_0_1_2_suffix")) {
            assertThrows(RuntimeException::class.java) { resolve("__ARTIST_GENRE_ALBUM__0_0_1", id, "Track 2") }
        }
    }
    @Test fun staleParentAndDisplayOrdinalMismatchFailClosed() {
        assertThrows(IllegalArgumentException::class.java) { resolve("__ARTIST_GENRE_ALBUM__0_0_2", "__MUSIC__0_0_1_2", "白日夢 2") }
        for (title in listOf("白日夢", "白日夢 3", "白日夢 02")) {
            assertThrows(IllegalArgumentException::class.java) { resolve("__ARTIST_GENRE_ALBUM__0_0_1", "__MUSIC__0_0_1_2", title) }
        }
    }
}
