package org.dx340.remote.protocol

/** Bound nesting before handing untrusted UTF-8 JSON to a recursive platform parser. */
object JsonEnvelopeGuard {
    fun validate(text: String) {
        require(text.length <= Wire.MAX_MESSAGE && text.trimStart().startsWith('{')) { "MALFORMED_MESSAGE" }
        val stack = ArrayDeque<Char>()
        var quoted = false; var escaped = false
        for (c in text) {
            if (quoted) {
                if (escaped) escaped = false else if (c == '\\') escaped = true else if (c == '"') quoted = false
            } else when (c) {
                '"' -> quoted = true
                '{', '[' -> { require(stack.size < 32) { "MALFORMED_MESSAGE" }; stack.addLast(c) }
                '}', ']' -> require(stack.isNotEmpty() && stack.removeLast() == if (c == '}') '{' else '[') { "MALFORMED_MESSAGE" }
            }
        }
        require(!quoted && stack.isEmpty()) { "MALFORMED_MESSAGE" }
    }
}
