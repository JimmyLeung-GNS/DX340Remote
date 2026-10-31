package org.dx340.remote.sources

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class MangoStorageMenuTest {
    private val p = "com.ibasso.music:id/"
    private val fixture = JSONObject(javaClass.getResource("/mango-navigation/storage-drawer.json")!!.readText())
    private fun observed(): List<UiNode> = fixture.getJSONArray("nodes").let { rows ->
        (0 until rows.length()).map { i -> rows.getJSONObject(i).let { n ->
            UiNode(n.getString("path"), n.optString("parent").takeIf { !n.isNull("parent") },
                n.getString("id"), n.getString("text"), n.getString("description"), n.getString("class"),
                clickable = n.getBoolean("clickable"), forward = n.getBoolean("forward"), drawingOrder = n.getInt("drawingOrder"))
        } }
    }
    private fun without(nodes: List<UiNode>, id: String): List<UiNode> {
        val paths = nodes.filter { it.id == p + id }.map { it.path }
        return nodes.filterNot { n -> paths.any { n.path == it || n.path.startsWith("$it.") } }
    }
    @Test fun capturedDrawerChoosesOnlyTheRequestedStorageOption() {
        val nodes = observed()
        assertTrue(MangoUiModel.menuVisible(nodes))
        assertEquals(fixture.getString("expectedInternalPath"), MangoUiModel.storageAction(nodes, "internal").path)
        assertEquals(p + "sdcardLayout", MangoUiModel.storageAction(nodes, "sd").id)
    }
    @Test fun sdlessDrawerStillAllowsInternalAndRejectsTheAbsentSdOption() {
        val nodes = without(observed(), "sdcardLayout")
        assertTrue(MangoUiModel.menuVisible(nodes))
        assertEquals(fixture.getString("expectedInternalPath"), MangoUiModel.storageAction(nodes, "internal").path)
        assertEquals("UI_STORAGE_CONTEXT_REQUIRED", assertThrows(RuntimeException::class.java) { MangoUiModel.storageAction(nodes, "sd") }.message)
    }
    @Test fun eitherVisibleMenuOptionBlocksCachedFolderRowsUnderTheDrawer() {
        val nodes = observed()
        val cachedPage = without(without(nodes, "sdcardLayout"), "internalStorgeLayout")
        assertEquals("sd", MangoUiModel.parse(cachedPage, "sd").storage)
        for (hidden in listOf("sdcardLayout", "internalStorgeLayout")) {
            assertThrows(RuntimeException::class.java) { MangoUiModel.parse(without(nodes, hidden), "sd") }
        }
    }
    @Test fun ambiguousDisabledNonclickableOrMislabeledStorageCannotBeSelected() {
        val nodes = without(observed(), "sdcardLayout")
        val option = nodes.single { it.id == p + "internalStorgeLayout" }
        val label = nodes.single { it.path.startsWith(option.path + ".") && it.text == "Internal Storage" }
        for (bad in listOf(
            nodes + option.copy(path = "9.0", parent = "9"),
            nodes.map { if (it.path == option.path) it.copy(enabled = false) else it },
            nodes.map { if (it.path == option.path) it.copy(clickable = false) else it },
            nodes.map { if (it.path == label.path) it.copy(text = "Local Network") else it },
            nodes + label.copy(path = option.path + ".99", parent = option.path))) {
            assertThrows(RuntimeException::class.java) { MangoUiModel.storageAction(bad, "internal") }
        }
    }
    @Test fun anOpenDrawerDoesNotAuthorizeUnknownOrNetworkStorageActions() {
        for (storage in listOf("network", "net", "", "../sd")) {
            assertEquals("UI_STORAGE_CONTEXT_REQUIRED", assertThrows(RuntimeException::class.java) { MangoUiModel.storageAction(observed(), storage) }.message)
        }
    }
}
