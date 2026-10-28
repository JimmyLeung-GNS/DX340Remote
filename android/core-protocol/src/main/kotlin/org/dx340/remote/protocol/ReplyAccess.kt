package org.dx340.remote.protocol

/** Late async results must be checked against current authorization, not request-time approval. */
object ReplyAccess {
    fun maySend(type: String, bonded: Boolean, approved: Boolean, authenticated: Boolean): Boolean =
        bonded && ((type == "HELLO" || type == "ERROR") || (approved && authenticated))
}
