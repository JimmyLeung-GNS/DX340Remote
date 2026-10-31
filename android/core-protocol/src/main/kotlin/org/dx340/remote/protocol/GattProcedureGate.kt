package org.dx340.remote.protocol

/** Serialize public GATT procedures and coalesce database invalidation until the old callback drains. */
class GattProcedureGate {
    enum class Operation { DISCOVER, MTU, SUBSCRIBE, WRITE }
    enum class Completion { IGNORED, CONTINUE, REDISCOVER }
    private var current: Operation? = null
    private var invalidated = false
    val busy get() = current != null
    fun begin(operation: Operation): Boolean {
        if (busy) return false
        current = operation
        return true
    }
    fun changed(): Boolean {
        invalidated = true
        if (busy) return false
        invalidated = false
        return true
    }
    fun complete(operation: Operation): Completion {
        if (current != operation) return Completion.IGNORED
        current = null
        val refresh = invalidated; invalidated = false
        return if (refresh) Completion.REDISCOVER else Completion.CONTINUE
    }
    fun clear() { current = null; invalidated = false }
}
