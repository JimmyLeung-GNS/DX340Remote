package org.dx340.remote.host

import android.content.Context
import org.dx340.remote.sources.UiLibraryAdapter
import org.json.JSONObject

class MangoUiAdapterProvider(private val context: Context) : UiLibraryAdapter {
    override fun invalidateLibrary() { service?.invalidateLibrary() }
    override val enabled get() = context.getSharedPreferences("mango-ui", Context.MODE_PRIVATE).getBoolean("enabled", false)
    private val service get() = MangoAccessibilityService.instance
    override val ready get() = enabled && service != null && service?.readiness() == null
    override val explanation get() = when {
        !enabled -> "Optional Mango browsing is off."
        service == null -> "Enable DX340 Mango browsing in Accessibility settings on DX340."
        else -> "Folders and existing playlists require DX340 awake/unlocked and Mango4.1.37/code240. App updates require revalidation. Native browsing and playback remain separate."
    }
    override fun browse(parent: String?, page: Int, revision: String?, stillAuthorized: () -> Boolean, done: (JSONObject?, String?) -> Unit) {
        if (!enabled) done(null, "UI_MODE_DISABLED") else if (service == null) done(null, "UI_PERMISSION_REQUIRED") else service!!.browse(parent, page, revision, stillAuthorized, done)
    }
    override fun validatePlayable(item: String, revision: String) = if (!enabled) "UI_MODE_DISABLED" else if (service == null) "UI_PERMISSION_REQUIRED" else service!!.validatePlayable(item, revision)
    override fun play(item: String, revision: String, stillAuthorized: () -> Boolean, done: (String?) -> Unit) {
        if (!enabled) done("UI_MODE_DISABLED") else if (service == null) done("UI_PERMISSION_REQUIRED") else service!!.play(item, revision, stillAuthorized, done)
    }
}
