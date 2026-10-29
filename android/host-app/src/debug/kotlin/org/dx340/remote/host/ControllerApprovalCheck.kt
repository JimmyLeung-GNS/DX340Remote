package org.dx340.remote.host

import android.content.Context
import android.os.Looper
import android.util.Xml
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.util.UUID

/** Real Android preference operations with synthetic IDs in an isolated file, never live approvals. */
class ControllerApprovalCheck(private val context: Context) {
    fun run(): JSONObject {
        check(Looper.myLooper() != Looper.getMainLooper()) { "CHECK_REQUIRES_WORKER_THREAD" }
        val original = context.getSharedPreferences("controllers", Context.MODE_PRIVATE).all.toMap()
        val name = "controller-approval-probe-${UUID.randomUUID()}"
        val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val store = ControllerApprovalStore(context, name)
        val ids = (0..8).map { "AA:00:00:00:00:%02X".format(it) }
        fun diskIdentities(): Set<String> {
            // An empty commit waits for the store's existing async apply writes to finish.
            check(preferences.edit().commit()) { "FIXTURE_PERSIST_FAILED" }
            val result = mutableSetOf<String>()
            File(context.dataDir, "shared_prefs/$name.xml").inputStream().use { input ->
                val parser = Xml.newPullParser().apply { setInput(input, "UTF-8") }
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType == XmlPullParser.START_TAG && parser.name == "boolean" && parser.getAttributeValue(null, "value") == "true") {
                        result += parser.getAttributeValue(null, "name")
                    }
                }
            }
            return result
        }
        val report = JSONObject().put("kind", "isolated-Android-approval-preferences-with-synthetic-identities-no-bonds-no-BLE")
        try {
            check(preferences.edit().putString("fixture-note", "preserve unrelated values").commit())
            ids.take(8).forEach(store::approve)
            store.approve(ids.first())
            check(store.identities() == ids.take(8).toSet() && diskIdentities() == ids.take(8).toSet())
            check(runCatching { store.approve(ids.last()) }.exceptionOrNull()?.message == "CONTROLLER_LIMIT")
            check(runCatching { store.approve("invalid-identity") }.exceptionOrNull()?.message == "INVALID_CONTROLLER")
            val removed = ids[3]
            check(store.revoke(removed))
            val remaining = ids.take(8).toSet() - removed
            check(store.identities() == remaining && diskIdentities() == remaining) { "UNRELATED_APPROVAL_CHANGED" }
            check(!store.revoke("AA:00:00:00:00:FF") && store.identities() == remaining)
            store.approve(ids.last())
            val beforeUnknown = store.identities()
            store.retainBonded(null)
            check(store.identities() == beforeUnknown && diskIdentities() == beforeUnknown) { "UNKNOWN_BOND_STATE_REMOVED_APPROVALS" }
            val retained = setOf(ids[0], ids[1], ids.last())
            store.retainBonded(retained) // Explicit fixture set, not a real OS bond lookup.
            check(store.identities() == retained && diskIdentities() == retained)
            store.revokeAll()
            check(store.identities().isEmpty() && diskIdentities().isEmpty())
            check(preferences.getString("fixture-note", "") == "preserve unrelated values")
            check(context.getSharedPreferences("controllers", Context.MODE_PRIVATE).all == original) { "LIVE_APPROVAL_PREFERENCES_CHANGED" }
            report.put("passed", true).put("otherApprovalsPreserved", 7).put("diskAfterTargetRevoke", remaining.size)
                .put("capacityAndInvalidIdentityRejected", true).put("unknownIdentityNoOp", true)
                .put("unknownBondSnapshotPreservedApprovals", true)
                .put("fixtureRetainAndRevokeAllPreservedUnrelatedValue", true).put("productionPreferencesUnchanged", true)
        } finally {
            check(context.deleteSharedPreferences(name)) { "FIXTURE_CLEANUP_FAILED" }
        }
        return report.put("temporaryPreferencesDeleted", true)
    }
}
