package com.example.shiftly.utils

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Cryptographic helper for 4-digit App Lock PINs.
 * Stores and verifies salted PBKDF2-HMAC-SHA256 hashes; never persists new plaintext PINs.
 */
object PinSecurity {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PREFIX = "pbkdf2"
    private const val ITERATIONS = 65_536
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    fun isValidPinFormat(pin: String): Boolean =
        pin.length == 4 && pin.all { it in '0'..'9' }

    fun isHashed(storedValue: String): Boolean =
        storedValue.startsWith("$PREFIX$")

    fun hashPin(pin: String): String {
        require(isValidPinFormat(pin)) { "PIN must be exactly 4 digits." }
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        val hash = pbkdf2(pin.toCharArray(), salt)
        val encoder = Base64.getEncoder()
        return "$PREFIX$${encoder.encodeToString(salt)}$${encoder.encodeToString(hash)}"
    }

    fun verifyPin(inputPin: String, storedValue: String): Boolean {
        if (!isValidPinFormat(inputPin) || storedValue.isBlank()) return false
        return if (isHashed(storedValue)) {
            val parts = storedValue.split("$")
            if (parts.size != 3) return false
            val decoder = Base64.getDecoder()
            val salt = runCatching { decoder.decode(parts[1]) }.getOrNull() ?: return false
            val expectedHash = runCatching { decoder.decode(parts[2]) }.getOrNull() ?: return false
            val actualHash = pbkdf2(inputPin.toCharArray(), salt)
            MessageDigest.isEqual(expectedHash, actualHash)
        } else {
            // Legacy pre-upgrade plaintext fallback for existing devices before first PIN update
            MessageDigest.isEqual(
                inputPin.toByteArray(Charsets.UTF_8),
                storedValue.toByteArray(Charsets.UTF_8)
            )
        }
    }

    private fun pbkdf2(chars: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(chars, salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
