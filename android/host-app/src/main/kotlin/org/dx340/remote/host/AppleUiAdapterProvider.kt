package org.dx340.remote.host

import android.content.Context
import org.dx340.remote.sources.UiLibraryAdapter
import org.json.JSONObject

class AppleUiAdapterProvider(private val context: Context) : UiLibraryAdapter {
    override val enabled get() = context.getSharedPreferences("apple-ui", Context.MODE_PRIVATE).getBoolean("enabled", false)
    private val service get() = AppleMusicAccessibilityService.instance
    override val ready get() = enabled && service?.readiness() == null && service != null
    override val explanation get() = when {
        !enabled -> "Optional Apple Music assisted browsing is off."
        service == null -> "Enable DX340 Apple Music browsing in Accessibility settings on DX340."
        service?.readiness() != null -> "Apple Music assisted browsing requires DX340 screen on, unlocked, and the verified app version."
        else -> "Apple Music assisted browsing: DX340 must stay screen on and unlocked. App updates require revalidation."
    }
    override fun browse(parent: String?, page: Int, revision: String?, done: (JSONObject?, String?) -> Unit) {
        if (!enabled || service == null) done(null, "UI_PERMISSION_REQUIRED") else service!!.browse(parent, page, revision, done)
    }
    override fun validatePlayable(item: String, revision: String) = if (!enabled || service == null) "UI_PERMISSION_REQUIRED" else service!!.validatePlayable(item, revision)
    override fun play(item: String, revision: String, stillAuthorized: () -> Boolean, done: (String?) -> Unit) {
        if (!enabled || service == null) done("UI_PERMISSION_REQUIRED") else service!!.play(item, revision, stillAuthorized, done)
    }
}
