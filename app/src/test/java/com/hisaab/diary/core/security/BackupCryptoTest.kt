package com.hisaab.diary.core.security

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupCryptoTest {
    private val payload = "zip bytes here".repeat(50).toByteArray()

    private fun write(password: String?): ByteArray {
        val out = ByteArrayOutputStream()
        BackupCrypto.encryptingStream(out, password).use { it.write(payload) }
        return out.toByteArray()
    }

    @Test
    fun plainRoundTrip() {
        val file = write(null)
        assertFalse(BackupCrypto.isEncrypted(ByteArrayInputStream(file)))
        val back = BackupCrypto.decryptingStream(ByteArrayInputStream(file), null).readBytes()
        assertArrayEquals(payload, back)
    }

    @Test
    fun encryptedRoundTrip() {
        val file = write("secret")
        assertTrue(BackupCrypto.isEncrypted(ByteArrayInputStream(file)))
        val back = BackupCrypto.decryptingStream(ByteArrayInputStream(file), "secret").readBytes()
        assertArrayEquals(payload, back)
    }

    @Test
    fun wrongPasswordFails() {
        val file = write("secret")
        try {
            BackupCrypto.decryptingStream(ByteArrayInputStream(file), "nope").readBytes()
            fail("should fail")
        } catch (expected: IOException) {
            // GCM tag check fails: wrong password or damaged file.
        }
    }

    @Test(expected = BackupCrypto.NotABackupException::class)
    fun otherFilesAreRejected() {
        BackupCrypto.isEncrypted(ByteArrayInputStream("PK\u0003\u0004 not ours".toByteArray()))
    }
}
