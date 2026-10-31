package org.dx340.remote.protocol

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Test

class AssistedPageUseTest {
    @Test fun onlyMatchingSourceAssistedSelectionsConsumeThePage() {
        val cases = JSONArray(javaClass.getResource("/assisted-selection-eligibility.json")!!.readText())
        for (index in 0 until cases.length()) {
            val case = cases.getJSONObject(index)
            assertEquals(case.getString("name"), case.getBoolean("tracks"), AssistedPageUse.tracks(case.getString("source"),
                case.getString("item"), case.getBoolean("assisted"), case.getString("requestedRevision"), case.getString("pageRevision")))
        }
    }
    @Test fun sharedSelectionCompletionRetiresOnlyItsCurrentAssistedPage() {
        val cases = JSONArray(javaClass.getResource("/assisted-page-use.json")!!.readText())
        for (index in 0 until cases.length()) {
            val case = cases.getJSONObject(index)
            val use = AssistedPageUse()
            val steps = case.getJSONArray("steps")
            for (stepIndex in 0 until steps.length()) {
                val step = steps.getJSONObject(stepIndex)
                when (step.getString("action")) {
                    "begin" -> use.begin(step.getString("id"), step.getString("source"), step.getString("epoch"), step.getString("revision"), step.getBoolean("assisted"))
                    "clear" -> use.clear()
                    "abandon" -> assertEquals(case.getString("name"), step.getBoolean("retire"),
                        use.abandon(step.getString("source"), step.getString("epoch"), step.getString("revision")))
                    "finish" -> assertEquals("${case.getString("name")}:$stepIndex", step.getBoolean("retire"),
                        use.finish(step.getString("id"), step.getString("source"), step.getString("epoch"), step.getString("revision")))
                    else -> error("Unknown fixture action")
                }
            }
        }
    }
}
