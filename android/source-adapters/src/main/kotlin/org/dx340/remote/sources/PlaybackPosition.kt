package org.dx340.remote.sources

/** A source estimate at host snapshot time, never an observation of audible output. */
data class PlaybackPosition(val position: Long, val speed: Double) {
    companion object {
        // Integers also have to remain exact in JSON/Dart/JavaScript clients.
        private const val MAX_TIME = 9_007_199_254_740_991L

        fun snapshot(position: Long, duration: Long, speed: Double, playing: Boolean,
                     updatedAt: Long, now: Long): PlaybackPosition {
            if (position < 0 || position > MAX_TIME) return PlaybackPosition(-1, 0.0)
            val end = duration.takeIf { it in 1..MAX_TIME }
            val base = end?.let { position.coerceAtMost(it) } ?: position
            val validClock = updatedAt > 0 && now >= updatedAt
            val rate = if (playing && end != null && validClock && speed.isFinite() && speed > 0 && speed <= 16) speed else 0.0
            if (rate == 0.0) return PlaybackPosition(base, rate)
            val projected = base.toDouble() + (now - updatedAt).toDouble() * rate
            return PlaybackPosition(projected.coerceAtMost(end!!.toDouble()).toLong(), rate)
        }
    }
}
