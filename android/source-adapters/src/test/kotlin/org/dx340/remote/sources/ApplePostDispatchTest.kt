package org.dx340.remote.sources

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ApplePostDispatchTest {
    private fun dialog(): List<UiNode> {
        val rows = JSONObject(javaClass.getResource("/apple-offline-dialog.json")!!.readText()).getJSONArray("nodes")
        return (0 until rows.length()).map { rows.getJSONObject(it).let { n ->
            val path = n.getString("path")
            UiNode(path, path.substringBeforeLast('.', "").takeIf(String::isNotEmpty), n.getString("id"),
                n.getString("text"), n.getString("description"), n.getString("class"), n.getBoolean("clickable"), n.getBoolean("selected"))
        } }
    }
    @Test fun capturedSourceErrorIsReportedWithoutRequiringItsEnglishTextOrErrorNumber() {
        assertEquals("UI_UNEXPECTED_SCREEN", AppleUiModel.postDispatchFailure(dialog()))
        val localized = dialog().map { if (it.text.isNotEmpty()) it.copy(text = "Localized source message") else it }
        assertEquals("UI_UNEXPECTED_SCREEN", AppleUiModel.postDispatchFailure(localized))
    }
    @Test fun normalLibraryRowsOrUnrelatedMessagesAreNotSourceErrorDialogs() {
        val nodes = dialog()
        val tab = UiNode("0.9", "0", "com.apple.android.music:id/action_library", description = "Library", selected = true)
        assertNull(AppleUiModel.postDispatchFailure(nodes + tab))
        assertNull(AppleUiModel.postDispatchFailure(nodes.filterNot { it.id.endsWith("/message") }))
        assertNull(AppleUiModel.postDispatchFailure(nodes.map { if (it.className == "android.widget.Button") it.copy(parent = "9") else it }))
        assertNull(AppleUiModel.postDispatchFailure(emptyList()))
        assertThrows(IllegalArgumentException::class.java) { AppleUiModel.postDispatchFailure(nodes + nodes.first()) }
    }
    private class Harness {
        var clock = 0L
        var issue: String? = null
        var sampleIssue: String? = null
        var samples = 0
        val callbacks = mutableListOf<Pair<Long, () -> Unit>>()
        val results = mutableListOf<String?>()
        val check = UiPostDispatchCheck({ clock }, { delay, task -> callbacks.add(clock + delay to task) },
            { issue }, { samples++; sampleIssue }, { results.add(it) })
        fun tick(at: Long? = null): () -> Unit {
            val (due, task) = callbacks.removeAt(0)
            clock = at ?: due; task(); return task
        }
    }
    @Test fun immediateFailureCompletesOnceAndLateCallbacksCannotReadOrReplaceIt() {
        val h = Harness(); h.sampleIssue = "UI_UNEXPECTED_SCREEN"; h.check.start()
        val late = h.tick(); h.sampleIssue = null; late(); h.check.cancel("STALE_LIBRARY")
        assertEquals(listOf("UI_UNEXPECTED_SCREEN"), h.results); assertEquals(1, h.samples)
        assertTrue(h.callbacks.isEmpty())
    }
    @Test fun cleanWindowFinishesAtOriginalBoundWithDispatchOnlyResult() {
        val h = Harness(); h.check.start()
        repeat(8) { h.tick() }
        assertEquals(1200L, h.clock); assertEquals(8, h.samples)
        assertEquals(listOf<String?>(null), h.results); assertTrue(h.callbacks.isEmpty())
        val delayed = Harness(); delayed.check.start(); delayed.tick(5000)
        assertEquals(0, delayed.samples); assertEquals(listOf<String?>(null), delayed.results)
    }
    @Test fun lostAuthorizationAndLifecycleCancellationStopSamplingWithoutReplayingAnAction() {
        val denied = Harness(); denied.check.start(); denied.issue = "AUTHORIZATION_DENIED"; denied.tick()
        assertEquals(listOf("AUTHORIZATION_DENIED"), denied.results); assertEquals(0, denied.samples)
        for (issue in listOf("UI_PERMISSION_REQUIRED", "UI_ACTION_INTERRUPTED", "STALE_LIBRARY")) {
            val h = Harness(); h.check.start(); h.check.cancel(issue); h.tick()
            assertEquals(listOf(issue), h.results); assertEquals(0, h.samples); assertTrue(h.callbacks.isEmpty())
        }
    }
    @Test fun backwardsClockStopsInsteadOfRenewingObservation() {
        val h = Harness(); h.clock = 100; h.check.start(); h.tick(99)
        assertEquals(listOf("UI_ACTION_INTERRUPTED"), h.results); assertEquals(0, h.samples)
        assertTrue(h.callbacks.isEmpty())
    }
}
