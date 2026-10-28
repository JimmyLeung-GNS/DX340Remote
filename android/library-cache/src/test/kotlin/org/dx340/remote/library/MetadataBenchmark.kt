package org.dx340.remote.library

import java.io.File
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

/** Explicitly scoped microbenchmark. Never use its timing as physical acceptance evidence. */
object MetadataBenchmark {
    @JvmStatic fun main(args: Array<String>) {
        val output = File(args.single()).absoluteFile
        output.parentFile.mkdirs()
        val generationStart = System.nanoTime()
        val tracks = MetadataFixture.collection()
        val generationMs = (System.nanoTime() - generationStart) / 1_000_000.0
        val fixture = File(output.parentFile, "synthetic-tracks.json")
        fixture.writeText(JSONArray().also { rows -> tracks.forEach { rows.put(it.json()) } }.toString(), Charsets.UTF_8)
        val guard = RevisionGuard()
        fun uncached(): ByteArray {
            val cache = LibraryIndex<MetadataFixture.Track>(200)
            val key = MetadataFixture.key(0)
            check(cache.get(key) == null)
            val page = MetadataFixture.page(tracks, 0)
            check(!guard.update("uncached:songs:0", page.associate { it.id to it.fingerprint() }))
            cache.put(key, page)
            return MetadataFixture.encode(0, page).also { check(MetadataFixture.decode(it).getJSONArray("items").length() == 25) }
        }
        val cache = LibraryIndex<MetadataFixture.Track>(200)
        cache.put(MetadataFixture.key(0), MetadataFixture.page(tracks, 0))
        fun cached(): ByteArray = MetadataFixture.encode(0, checkNotNull(cache.get(MetadataFixture.key(0)))).also {
            check(MetadataFixture.decode(it).getJSONArray("items").length() == 25)
        }
        fun time(action: () -> Any): Double {
            val start = System.nanoTime(); action(); return (System.nanoTime() - start) / 1_000_000.0
        }
        // Record first invocation before JIT warmup, then warmed distributions separately.
        val coldUncached = time { uncached() }
        val coldCached = time { cached() }
        val bytes = cached()
        repeat(10) { uncached(); cached(); MetadataFixture.frameRoundTrip(bytes, 20) }
        fun samples(action: () -> Any): JSONObject {
            val values = List(50) { time(action) }
            val sorted = values.sorted()
            return JSONObject().put("samplesMs", JSONArray(values)).put("medianMs", sorted[24])
                .put("p95Ms", sorted[47]).put("maxMs", sorted.last())
        }
        val cachedSamples = samples { cached() }
        val uncachedSamples = samples { uncached() }
        var maxPageBytes = 0
        val allPagesMs = time {
            val ids = mutableSetOf<String>()
            for (page in 0 until 400) {
                val rows = MetadataFixture.page(tracks, page)
                cache.put(MetadataFixture.key(page), rows)
                val encoded = MetadataFixture.encode(page, rows)
                maxPageBytes = maxOf(maxPageBytes, encoded.size)
                check(encoded.size <= 65536)
                val decoded = MetadataFixture.decode(encoded).getJSONArray("items")
                for (row in 0 until decoded.length()) check(ids.add(decoded.getJSONObject(row).getString("id")))
            }
            check(ids.size == 10_000)
            check(cache.get(MetadataFixture.key(0)) == null)
            check(cache.get(MetadataFixture.key(399))?.last()?.id == tracks.last().id)
        }
        val result = JSONObject().put("kind", "synthetic-metadata-local-jvm-components")
            .put("tracks", tracks.size).put("pageSize", 25).put("pages", 400).put("cacheCapacityPages", 200)
            .put("javaVersion", System.getProperty("java.version")).put("os", System.getProperty("os.name"))
            .put("fixtureFile", fixture.name).put("fixtureSha256", MessageDigest.getInstance("SHA-256").digest(fixture.readBytes()).joinToString("") { "%02x".format(it.toInt() and 255) })
            .put("fixtureGenerationMs", generationMs).put("firstUncachedComponentMs", coldUncached).put("firstCachedComponentMs", coldCached)
            .put("uncachedComponents", uncachedSamples).put("cachedComponents", cachedSamples)
            .put("framingCapacity20", samples { MetadataFixture.frameRoundTrip(bytes, 20) })
            .put("framingCapacity185", samples { MetadataFixture.frameRoundTrip(bytes, 185) })
            .put("walkAllPagesMs", allPagesMs).put("maxEncodedPageBytes", maxPageBytes)
            .put("componentTimingTargetMet", coldCached < 500 && coldUncached < 3000 && cachedSamples.getDouble("maxMs") < 500 && uncachedSamples.getDouble("maxMs") < 3000)
            .put("limitations", JSONArray(listOf("LibraryIndex is a standalone cache component, not wired into SourceRegistry.", "SourceRegistry uses RevisionGuard; fixture JSON rendering is a test harness using documented fields/limits.", "Metadata source is already in memory; fixture generation is measured separately.", "No disk-cache restore, Android MediaBrowser, device CPU, Flutter render, BLE radio, artwork, playback or audio timing is measured.")))
        output.writeText(result.toString(2), Charsets.UTF_8)
        check(result.getBoolean("componentTimingTargetMet")) { "Local component timing exceeded proposed targets; inspect ${output.path}" }
        println("10,000 synthetic metadata records / 400 pages verified; report ${output.path}")
    }
}
