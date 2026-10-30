package org.dx340.remote.protocol

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ConnectionSetupDeadlineTest {
    @Test fun sharedDeadlineCasesRejectLateCallbacksAndSuspendedSetup() {
        val fixture = JSONObject(javaClass.getResource("/connection-setup-deadline.json")!!.readText())
        assertEquals(fixture.getLong("timeoutMs"), ConnectionSetupDeadline.TIMEOUT_MS)
        assertEquals(fixture.getString("message"), ConnectionSetupDeadline.MESSAGE)
        val cases = fixture.getJSONArray("cases")
        for (index in 0 until cases.length()) {
            val case = cases.getJSONObject(index); val steps = case.getJSONArray("steps")
            val deadline = ConnectionSetupDeadline(); val tickets = mutableMapOf<String, Long>()
            for (stepIndex in 0 until steps.length()) {
                val step = steps.getJSONObject(stepIndex)
                when (step.getString("action")) {
                    "begin" -> tickets[step.getString("ticket")] = deadline.begin(step.getLong("at"))
                    "clear" -> deadline.clear()
                    "check" -> assertEquals("${case.getString("name")}:$stepIndex", step.getBoolean("expired"),
                        deadline.expired(step.getLong("at"), if (step.has("ticket")) tickets.getValue(step.getString("ticket")) else null))
                    else -> error("Unknown fixture action")
                }
            }
        }
    }
    @Test fun nearClockLimitDoesNotOverflowOrResurrectCancelledSetup() {
        val deadline = ConnectionSetupDeadline()
        val token = deadline.begin(Long.MAX_VALUE - ConnectionSetupDeadline.TIMEOUT_MS)
        assertFalse(deadline.expired(Long.MAX_VALUE - 1, token))
        assertTrue(deadline.expired(Long.MAX_VALUE, token))
        deadline.clear(); assertFalse(deadline.expired(Long.MAX_VALUE, token)); assertFalse(deadline.active)
    }
}
