package org.dx340.remote.sources

import org.dx340.remote.library.LibraryIndex
import org.dx340.remote.library.RevisionGuard
import org.junit.Assert.*
import org.junit.Test

class NativeItemFingerprintTest {
    private fun fingerprint(album: String?) = NativeItemFingerprint.of("__MUSIC__0_0_1_1", 2, "Intro 1", "tayori", album, null)

    @Test fun reusedIdTitleAndArtistWithChangedAlbumInvalidateSelectionAndCacheProof() {
        val original = fingerprint("1st Live “春を待つ” 追加公演  [BD]")
        val replacement = fingerprint("2nd Anniversary Live ”Filament“  [BD]")
        assertNotEquals(original, replacement)
        assertThrows(IllegalArgumentException::class.java) { RevisionGuard().validateSelection(original, replacement) }
        val key = LibraryIndex.Key("mango", "same-source-epoch", "same-reused-parent", 0)
        val cache = NativeBrowsePageCache({ 0L })
        cache.remember(key, listOf(NativeBrowsePageCache.Row("__MUSIC__0_0_1_1", "Intro 1", "tayori", false, true, original)), null)
        assertNull(cache.read(key, true) { it.fingerprint == replacement })
    }
    @Test fun everyPublicIdentityFieldContributesIndependently() {
        val original = NativeItemFingerprint.of("id", 2, "title", "artist", "album", "content://item/1")
        val variants = listOf(
            NativeItemFingerprint.of("other-id", 2, "title", "artist", "album", "content://item/1"),
            NativeItemFingerprint.of("id", 1, "title", "artist", "album", "content://item/1"),
            NativeItemFingerprint.of("id", 2, "new-title", "artist", "album", "content://item/1"),
            NativeItemFingerprint.of("id", 2, "title", "new-artist", "album", "content://item/1"),
            NativeItemFingerprint.of("id", 2, "title", "artist", "new-album", "content://item/1"),
            NativeItemFingerprint.of("id", 2, "title", "artist", "album", "content://item/2"))
        assertTrue(variants.all { it != original })
    }
    @Test fun nullAndLiteralNullHaveDifferentIdentities() {
        assertNotEquals(NativeItemFingerprint.of("id", 2, null, "artist", null, null),
            NativeItemFingerprint.of("id", 2, "null", "artist", null, null))
    }
    @Test fun embeddedFieldSeparatorsCannotBorrowAnotherFieldBoundary() {
        assertNotEquals(NativeItemFingerprint.of("id", 2, "a\u0000b", "c", null, null),
            NativeItemFingerprint.of("id", 2, "a", "b\u0000c", null, null))
    }
    @Test fun unchangedUnicodeAndNullableMetadataIsDeterministic() {
        val first = NativeItemFingerprint.of(null, 0, "雪・白日夢", null, "春を待つ", null)
        assertEquals(first, NativeItemFingerprint.of(null, 0, "雪・白日夢", null, "春を待つ", null))
        assertTrue(first.matches(Regex("[a-f0-9]{64}")))
    }
}
