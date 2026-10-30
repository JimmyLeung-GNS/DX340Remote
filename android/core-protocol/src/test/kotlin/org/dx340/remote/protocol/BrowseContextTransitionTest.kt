package org.dx340.remote.protocol

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class BrowseContextTransitionTest {
    @Test fun sharedContextTransitionsRetireOnlyAffectedListsAndKeepAuthorizationStatus() {
        val cases = JSONArray(javaClass.getResource("/browse-invalidation.json")!!.readText())
        fun context(value: JSONObject) = BrowseContext(value.getString("source"), value.getString("epoch"),
            value.getString("mode"), value.getBoolean("assistedReady"))
        for (index in 0 until cases.length()) {
            val case = cases.getJSONObject(index)
            val result = BrowseContextTransition.evaluate(context(case.getJSONObject("old")), context(case.getJSONObject("new")),
                case.getBoolean("hasLibrary"), case.getBoolean("hasQueue"), case.getBoolean("assistedLibrary"), case.getBoolean("notify"))
            assertEquals(case.getString("name"), case.getString("reset"), result.reset.name)
            assertEquals(case.getString("name"), if (case.isNull("message")) null else case.getString("message"), result.message)
        }
    }
}
