package org.dx340.remote.host

/** Only explicit local enable actions change the durable choice. Recovery never opens pairing. */
internal class HostControlChoice(private val read: () -> Boolean, private val write: (Boolean) -> Boolean) {
    enum class Start { STOP, START, PAIR, PROBE }

    fun resolve(action: String?, permissionsGranted: Boolean): Start {
        if (action == "STOP") {
            write(false)
            return Start.STOP
        }
        if (!permissionsGranted) return Start.STOP
        return when (action) {
            "START", "PAIR", "PROBE" -> {
                if (!write(true)) Start.STOP else Start.valueOf(action)
            }
            null, "RESTORE" -> if (read()) Start.START else Start.STOP
            else -> Start.STOP
        }
    }
}
