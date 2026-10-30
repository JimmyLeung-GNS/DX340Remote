package org.dx340.remote.protocol

import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class ReplyBudgetTest {
    @Test fun platformContractRetainsSlowAssistedSelectionWithoutExtendingControls() {
        val cases = JSONArray(javaClass.getResource("/reply-budgets.json")!!.readText())
        for (index in 0 until cases.length()) {
            val case = cases.getJSONObject(index)
            val budget = ReplyBudget.milliseconds(case.getString("operation"), if (case.isNull("item")) null else case.getString("item"))
            assertEquals(case.getLong("milliseconds"), budget)
            if (case.has("observedPreparationMs")) {
                assertTrue(case.getLong("observedPreparationMs") > 12000)
                assertTrue(budget > case.getLong("observedPreparationMs"))
                assertTrue(budget > 20000 + 3500) // Bounded folder prep plus source handoff.
            }
        }
    }
}
