package org.dx340.remote.protocol

/** Dispatch only after a required ATT response has been submitted successfully.
 * A successful submission is not proof that the remote client received its ACK.
 */
object InputWriteReply {
    fun deliver(responseNeeded: Boolean, valid: Boolean, complete: ByteArray?,
                respond: () -> Boolean, failed: () -> Unit, dispatch: (ByteArray) -> Unit) {
        if (!responseNeeded) return
        val submitted = try { respond() } catch (_: Exception) { false }
        if (!submitted) { failed(); return }
        if (valid && complete != null) dispatch(complete)
    }
}
