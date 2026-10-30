package org.dx340.remote.protocol

import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class ConnectionAssistTest {
    @Test fun discoveryNeverSubstitutesAnotherPlayerForTheRememberedIdentity() {
        val cases = JSONArray(javaClass.getResource("/player-discovery.json")!!.readText())
        for (index in 0 until cases.length()) {
            val case = cases.getJSONObject(index)
            val found = case.getJSONArray("found")
            val result = PlayerDiscovery.finish((0 until found.length()).map { found.getString(it) }, if (case.isNull("remembered")) null else case.getString("remembered"))
            assertEquals(case.getString("name"), if (case.isNull("target")) null else case.getString("target"), result.target)
            assertEquals(case.getString("name"), case.getBoolean("choose"), result.choose)
        }
    }
    @Test fun approvalObservationStopsAtAuthorizationDeadlineCancelAndNewAttempt() {
        val steps = JSONArray(javaClass.getResource("/approval-retry.json")!!.readText())
        val retry = ApprovalRetry(); val tickets = mutableMapOf<String, Long>()
        for (index in 0 until steps.length()) {
            val step = steps.getJSONObject(index)
            when (step.getString("action")) {
                "begin" -> tickets[step.getString("ticket")] = retry.begin()
                "clear" -> retry.clear()
                "check" -> assertEquals("step$index", step.getBoolean("allowed"), retry.mayCheck(tickets.getValue(step.getString("ticket")), step.getBoolean("setup"), step.getBoolean("authorized"), step.getBoolean("pending")))
                else -> error("Unknown fixture action")
            }
        }
        val current = retry.begin(); assertEquals(current, retry.begin())
        retry.clear(); assertFalse(retry.current(current))
    }
}
