package org.dx340.remote.host

/** Transport ownership only. Source playback and the durable local choice are separate. */
internal class HostRadioRecovery {
    enum class State { OFF, TURNING_ON, ON, TURNING_OFF, UNKNOWN }
    enum class Action { NONE, START, STOP }
    private var running = false

    fun update(state: State, enabled: Boolean): Action {
        if (state == State.UNKNOWN) return Action.NONE
        val wanted = enabled && state == State.ON
        if (wanted == running) return Action.NONE
        running = wanted
        return if (wanted) Action.START else Action.STOP
    }

    fun startFailed() { running = false }
}
