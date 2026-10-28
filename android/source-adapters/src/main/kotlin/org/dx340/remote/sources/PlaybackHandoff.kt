package org.dx340.remote.sources

enum class HandoffPlayback { ACTIVE, PAUSED, STOPPED, UNKNOWN }
data class HandoffSource(val id: String, val epoch: String, val playback: HandoffPlayback, val canPause: Boolean)

/** Single-command preparation only. Never retries pause, starts a player, or rolls playback back. */
class PlaybackHandoff(
    private val now: () -> Long,
    private val schedule: (Long, () -> Unit) -> Unit,
    private val sources: () -> List<HandoffSource>,
    private val pause: (HandoffSource) -> Unit
) {
    fun prepare(target: String, guard: () -> String?, done: (String?) -> Unit) {
        var finished = false
        fun finish(error: String?) { if (!finished) { finished = true; done(error) } }
        fun checkGuard(): Boolean {
            val error = guard()
            if (error != null) finish(error)
            return error == null
        }
        try {
            if (!checkGuard()) return
            val others = sources().filter { it.id != target }
            require(others.none { it.playback == HandoffPlayback.UNKNOWN }) { "SOURCE_HANDOFF_FAILED" }
            val active = others.filter { it.playback == HandoffPlayback.ACTIVE }
            if (active.isEmpty()) { finish(null); return }
            require(active.size == 1 && active.single().canPause) { "OTHER_SOURCE_PLAYING" }
            val previous = active.single()
            if (!checkGuard()) return
            pause(previous)
            val deadline = now() + 3500
            fun observe() {
                if (finished) return
                try {
                    if (!checkGuard()) return
                    val current = sources().filter { it.id != target }
                    val observed = current.singleOrNull { it.id == previous.id }
                    require(observed?.epoch == previous.epoch) { "SOURCE_HANDOFF_FAILED" }
                    require(current.none { it.id != previous.id && it.playback !in setOf(HandoffPlayback.PAUSED, HandoffPlayback.STOPPED) }) { "SOURCE_HANDOFF_FAILED" }
                    if (observed?.playback == HandoffPlayback.PAUSED) finish(null)
                    else if (now() >= deadline) finish("SOURCE_HANDOFF_FAILED")
                    else schedule(100, ::observe)
                } catch (e: Exception) { finish(e.message ?: "SOURCE_HANDOFF_FAILED") }
            }
            schedule(100, ::observe)
        } catch (e: Exception) { finish(e.message ?: "SOURCE_HANDOFF_FAILED") }
    }
}
