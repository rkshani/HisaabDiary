package com.hisaab.diary.core.security

import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Backup file header + optional password encryption (AES-GCM, key from PBKDF2).
 * Layout: "HSBK" | version(1) | encrypted(1) | [salt(16) iv(12)] | zip bytes (encrypted or plain).
 */
object BackupCrypto {
    private val MAGIC = byteArrayOf('H'.code.toByte(), 'S'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte())
    private const val FORMAT_VERSION = 1
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private const val KEY_BITS = 256
    private const val ITERATIONS = 120_000
    private const val KDF = "PBKDF2WithHmacSHA1"
    private const val CIPHER = "AES/GCM/NoPadding"

    class WrongPasswordException : IOException("Wrong password or damaged file")

    class NotABackupException : IOException("This is not a Hisaab Diary backup")

    /** Writes the header and returns the stream to write the zip into (close it when done). */
    fun encryptingStream(output: OutputStream, password: String?): OutputStream {
        output.write(MAGIC)
        output.write(FORMAT_VERSION)
        if (password.isNullOrEmpty()) {
            output.write(0)
            return output
        }
        output.write(1)
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        output.write(salt)
        output.write(iv)
        val cipher = Cipher.getInstance(CIPHER).apply {
            init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(TAG_BITS, iv))
        }
        return CipherOutputStream(output, cipher)
    }

    /** True when the file needs a password. Reads only the header. */
    fun isEncrypted(input: InputStream): Boolean {
        val data = DataInputStream(input)
        readHeader(data)
        return data.readUnsignedByte() == 1
    }

    /**
     * Reads the header and returns the stream of zip bytes.
     * A wrong password shows up as [WrongPasswordException] while reading the zip.
     */
    fun decryptingStream(input: InputStream, password: String?): InputStream {
        val data = DataInputStream(input)
        readHeader(data)
        if (data.readUnsignedByte() == 0) return data
        if (password.isNullOrEmpty()) throw WrongPasswordException()
        val salt = ByteArray(SALT_BYTES).also(data::readFully)
        val iv = ByteArray(IV_BYTES).also(data::readFully)
        val cipher = Cipher.getInstance(CIPHER).apply {
            init(Cipher.DECRYPT_MODE, key(password, salt), GCMParameterSpec(TAG_BITS, iv))
        }
        return CipherInputStream(data, cipher)
    }

    private fun readHeader(data: DataInputStream) {
        val magic = ByteArray(MAGIC.size)
        val ok = runCatching { data.readFully(magic) }.isSuccess &&
            magic.contentEquals(MAGIC) &&
            data.readUnsignedByte() <= FORMAT_VERSION
        if (!ok) throw NotABackupException()
    }

    private fun key(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val bytes = SecretKeyFactory.getInstance(KDF).generateSecret(spec).encoded
        return SecretKeySpec(bytes, "AES")
    }
}
