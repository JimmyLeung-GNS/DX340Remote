package org.dx340.remote.controller

import org.dx340.remote.protocol.RequestJournal
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class JournalReportJsonTest {
    @Test fun unknownTimingsRemainExplicitNullAcrossRealJsonSerialization() {
        var clock = 100L; val journal = RequestJournal { clock }
        clock = 105; journal.begin("read", "GET_STATE", "mango")
        val parsed = JSONObject(JournalReportJson.encode(journal).toString())
        val row = parsed.getJSONArray("requests").getJSONObject(0)
        assertEquals(5, row.getInt("startedMs"))
        for (field in listOf("acceptedAfterMs", "finishedAfterMs", "error")) { assertTrue(row.has(field)); assertTrue(row.isNull(field)) }
        assertFalse(row.getBoolean("accepted")); assertFalse(row.getBoolean("terminalApplied"))
    }
    @Test fun privacyCategoriesCannotTurnUnknownLabelsIntoExportedMetadata() {
        val journal = RequestJournal { 1L }
        journal.begin("safe-id", "PRIVATE_TRACK.wav", "PRIVATE_ARTIST")
        journal.reply("safe-id", "ERROR", error = "PRIVATE_CREDENTIAL")
        val text = JournalReportJson.encode(journal).toString()
        assertFalse(text.contains("PRIVATE"))
        val row = JSONObject(text).getJSONArray("requests").getJSONObject(0)
        assertEquals("OTHER", row.getString("operation")); assertEquals("other", row.getString("source")); assertEquals("OTHER", row.getString("error"))
    }
    @Test fun acceptedAndAppliedTerminalUseDifferentFieldsWithoutChangingTheirTimings() {
        var clock = 0L; val journal = RequestJournal { clock }
        journal.begin("pause", "SET_PLAYING", "apple"); clock = 10; journal.reply("pause", "ACCEPTED")
        val accepted = JSONObject(JournalReportJson.encode(journal).toString()).getJSONArray("requests").getJSONObject(0)
        assertTrue(accepted.getBoolean("accepted")); assertFalse(accepted.getBoolean("terminalApplied")); assertTrue(accepted.isNull("finishedAfterMs"))
        clock = 30; journal.reply("pause", "RESULT", outcome = "observed"); journal.markTerminalApplied("pause")
        val final = JSONObject(JournalReportJson.encode(journal).toString()).getJSONArray("requests").getJSONObject(0)
        assertEquals(10, final.getInt("acceptedAfterMs")); assertEquals(30, final.getInt("finishedAfterMs")); assertTrue(final.getBoolean("terminalApplied")); assertEquals("observed", final.getString("hostOutcome"))
    }
}
