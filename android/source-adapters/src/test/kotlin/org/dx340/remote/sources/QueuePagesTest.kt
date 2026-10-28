package org.dx340.remote.sources

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class QueuePagesTest {
    private val entries = (0..100).map { QueuePages.Entry(it.toString(), "Track $it") }
    @Test fun reachesEveryQueueItemWithoutRepeatingOrSkipping() {
        val seen = mutableListOf<String>(); var offset = 0
        do {
            val page = QueuePages.render(entries, "r1", JSONObject().put("offset", offset).put("revision", if (offset == 0) JSONObject.NULL else "r1"))
            assertEquals(101, page.getInt("total")); assertEquals(offset, page.getInt("offset"))
            val items = page.getJSONArray("items")
            repeat(items.length()) { seen.add(items.getJSONObject(it).getString("id")) }
            if (page.isNull("nextOffset")) break
            offset = page.getInt("nextOffset")
        } while (true)
        assertEquals(entries.map { it.id }, seen)
    }
    @Test fun oldClientsStillReceiveTheFirstWindowAndEmptyQueuesTerminate() {
        assertEquals(50, QueuePages.render(entries, "r", JSONObject()).getJSONArray("items").length())
        val empty = QueuePages.render(emptyList(), "r", JSONObject())
        assertEquals(0, empty.getInt("total")); assertTrue(empty.isNull("nextOffset"))
    }
    @Test fun queueChangesCannotBeSilentlyMixedAcrossWindows() {
        for (args in listOf(JSONObject().put("offset", 50), JSONObject().put("offset", 50).put("revision", "old"), JSONObject().put("revision", "old"))) {
            val error = assertThrows(IllegalArgumentException::class.java) { QueuePages.render(entries, "new", args) }
            assertEquals("STALE_LIBRARY", error.message)
        }
    }
    @Test fun rejectsInvalidOffsetsAndUnboundedQueues() {
        for (offset in listOf(-1, 102, 10001, 1.5, "50", JSONObject.NULL, Long.MAX_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) { QueuePages.render(entries, "r", JSONObject().put("offset", offset).put("revision", "r")) }
        }
        assertThrows(IllegalArgumentException::class.java) { QueuePages.render(List(10001) { entries[0] }, "r", JSONObject()) }
    }
    @Test fun reducesWindowByEncodedUtf8SizeAndKeepsWholeTitles() {
        val title = "音樂\"\\".repeat(800)
        val queue = (0..49).map { QueuePages.Entry("$it", title) }
        val first = QueuePages.render(queue, "r", JSONObject())
        assertTrue(first.toString().toByteArray(Charsets.UTF_8).size <= QueuePages.MAX_PAGE_BYTES)
        val count = first.getJSONArray("items").length()
        assertTrue(count in 1..49); assertEquals(count, first.getInt("nextOffset"))
        assertEquals(title, first.getJSONArray("items").getJSONObject(0).getString("title"))
        assertEquals(count, QueuePages.render(queue, "r", JSONObject().put("offset", count).put("revision", "r")).getInt("offset"))
        assertThrows(IllegalArgumentException::class.java) { QueuePages.render(listOf(QueuePages.Entry("large", title.repeat(20))), "r", JSONObject()) }
    }
}
