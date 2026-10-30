package org.dx340.remote.sources

/** Event-time public volume metadata only; never paths or file contents. */
data class StorageVolumeSample(val primary: Boolean, val uuid: String?, val state: String)

/** Bounded deduplication, preserving an unmount/remount even if final state is unchanged. */
class StorageVolumeChanges(initial: List<StorageVolumeSample>) {
    companion object { const val MAX_VOLUMES = 64 }
    private var closed = false
    private var uncertain = false
    private val states = linkedMapOf<String, String>()
    val retainedCount get() = states.size

    init { replace(initial) }

    private fun key(sample: StorageVolumeSample): String? {
        val uuid = sample.uuid?.takeIf { it.isNotBlank() && it.length <= 128 }
        return if (sample.primary) "primary:${uuid ?: "none"}" else uuid?.let { "secondary:$it" }
    }
    private fun remember(key: String, state: String) {
        states.remove(key)
        states[key] = state
        if (states.size > MAX_VOLUMES) states.remove(states.keys.first())
    }
    private fun replace(snapshot: List<StorageVolumeSample>) {
        states.clear(); uncertain = snapshot.size > MAX_VOLUMES
        snapshot.take(MAX_VOLUMES).forEach { sample ->
            val key = key(sample)
            if (key == null) uncertain = true else remember(key, sample.state)
        }
    }
    fun observe(sample: StorageVolumeSample): Boolean {
        if (closed) return false
        // Two anonymous removable volumes cannot safely be treated as one identity.
        val key = key(sample) ?: return true
        val previous = states[key]
        remember(key, sample.state)
        return previous != sample.state
    }
    /** Startup reconciliation only; callbacks must use their own event-time sample. */
    fun reconcile(snapshot: List<StorageVolumeSample>): Boolean {
        if (closed) return false
        val previous = states.toMap()
        val wasUncertain = uncertain
        replace(snapshot)
        return wasUncertain || uncertain || previous != states
    }
    fun close() { closed = true; states.clear() }
}
