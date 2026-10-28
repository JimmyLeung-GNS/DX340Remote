package org.dx340.remote.sources

import org.json.JSONObject

interface UiLibraryAdapter {
    val enabled: Boolean
    val ready: Boolean
    val explanation: String
    fun browse(parent: String?, page: Int, revision: String?, stillAuthorized: () -> Boolean, done: (JSONObject?, String?) -> Unit)
    fun validatePlayable(item: String, revision: String): String?
    fun play(item: String, revision: String, stillAuthorized: () -> Boolean, done: (String?) -> Unit)
}
