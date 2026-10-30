package org.dx340.remote.sources

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

/** Identity proof only, not a client-visible ID or a filesystem identity guarantee. */
object NativeItemFingerprint {
    fun of(id: String?, flags: Int, title: String?, subtitle: String?, description: String?, mediaUri: String?): String {
        val encoded = ByteArrayOutputStream()
        DataOutputStream(encoded).use { output ->
            output.writeUTF("DX340-native-item-v2")
            output.writeInt(flags)
            for (field in listOf(id, title, subtitle, description, mediaUri)) {
                if (field == null) output.writeInt(-1)
                else {
                    val bytes = field.toByteArray(Charsets.UTF_8)
                    output.writeInt(bytes.size)
                    output.write(bytes)
                }
            }
        }
        return java.security.MessageDigest.getInstance("SHA-256").digest(encoded.toByteArray())
            .joinToString("") { "%02x".format(it.toInt() and 255) }
    }
}
