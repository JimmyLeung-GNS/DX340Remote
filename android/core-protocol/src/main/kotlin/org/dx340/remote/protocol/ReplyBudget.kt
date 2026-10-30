package org.dx340.remote.protocol

/** Pending-reply observation only; never extends a host lease or retries a command. */
object ReplyBudget {
    fun milliseconds(operation: String, item: String? = null): Long = when {
        operation == "GET_ARTWORK" -> 75000
        operation == "PLAY_ITEM" && item?.startsWith("mango-ui:") == true -> 30000
        else -> 12000
    }
}
