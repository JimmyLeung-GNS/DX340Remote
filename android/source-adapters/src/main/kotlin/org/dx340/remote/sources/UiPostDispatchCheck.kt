package org.dx340.remote.sources

/** Observe immediate UI failure after one issued action. A null result means dispatched, never played. */
class UiPostDispatchCheck(
    private val now: () -> Long,
    private val schedule: (Long, () -> Unit) -> Unit,
    private val guard: () -> String?,
    private val sample: () -> String?,
    private val done: (String?) -> Unit
) {
    private var startedAt: Long? = null
    private var finished = false

    fun start() {
        check(startedAt == null && !finished)
        startedAt = now()
        schedule(150, ::poll)
    }

    fun cancel(error: String) = finish(error)

    private fun finish(error: String?) {
        if (finished) return
        finished = true
        done(error)
    }

    private fun poll() {
        if (finished) return
        try {
            guard()?.let { finish(it); return }
            val current = now()
            val started = startedAt ?: return
            if (current < started) { finish("UI_ACTION_INTERRUPTED"); return }
            // Keep the original deadline even when an Android callback is delayed.
            if (current - started > 1200) { finish(null); return }
            sample()?.let { finish(it); return }
            val after = now()
            if (after < started) finish("UI_ACTION_INTERRUPTED")
            else if (after - started >= 1200) finish(null)
            else schedule(minOf(150, 1200 - (after - started)), ::poll)
        } catch (e: Exception) { finish(e.message ?: "UI_UNEXPECTED_SCREEN") }
    }
}
