package org.dx340.remote.sources

/** Time observed with the same semantic snapshot in the same active window. */
class UiSnapshotStability {
    private var window: Int? = null
    private var signature: String? = null
    private var since = 0L
    private var lastObserved = 0L
    fun observe(window: Int, signature: String, now: Long): Long {
        if (this.window != window || this.signature != signature || now < lastObserved) {
            this.window = window; this.signature = signature; since = now
        }
        lastObserved = now
        return now - since
    }
}
