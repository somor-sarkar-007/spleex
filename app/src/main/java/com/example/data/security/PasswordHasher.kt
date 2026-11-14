package com.example.data.security

import java.security.MessageDigest
import java.security.SecureRandom

object PasswordHasher {
    private val random = SecureRandom()

    fun hashPassword(password: String): String {
        val salt = ByteArray(16)
        random.nextBytes(salt)
        val saltHex = salt.toHex()
        val digest = sha256("$saltHex:$password")
        return "$saltHex:$digest"
    }

    fun verifyPassword(password: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) return false
        val saltHex = parts[0]
        val expectedHash = parts[1]
        val actualHash = sha256("$saltHex:$password")
        return actualHash == expectedHash
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
