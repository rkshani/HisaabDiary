package com.hisaab.diary.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Hashes the app-lock PIN with PBKDF2 and a random salt. The PIN itself is never saved. */
object PinHasher {
    const val PIN_LENGTH = 4
    private const val ITERATIONS = 20_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16

    // PBKDF2WithHmacSHA1 works on every Android version we support (minSdk 24).
    private const val ALGORITHM = "PBKDF2WithHmacSHA1"

    fun isValidPin(pin: String): Boolean = pin.length == PIN_LENGTH && pin.all { it.isDigit() }

    fun newSalt(): String = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }.toHex()

    fun hash(pin: String, salt: String): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt.hexToBytes(), ITERATIONS, KEY_BITS)
        return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded.toHex()
    }

    /** Constant-time compare, so timing does not leak how many digits were right. */
    fun matches(pin: String, salt: String, expectedHash: String): Boolean =
        MessageDigest.isEqual(hash(pin, salt).toByteArray(), expectedHash.toByteArray())

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray = chunked(2).map { it.toInt(HEX).toByte() }.toByteArray()

    private const val HEX = 16
}

/** When to lock: the app was in the background longer than the timeout. */
object LockPolicy {
    private const val MILLIS_PER_SECOND = 1000L

    fun shouldLock(lockEnabled: Boolean, backgroundAt: Long?, now: Long, timeoutSeconds: Int): Boolean =
        lockEnabled && backgroundAt != null && now - backgroundAt >= timeoutSeconds * MILLIS_PER_SECOND
}
