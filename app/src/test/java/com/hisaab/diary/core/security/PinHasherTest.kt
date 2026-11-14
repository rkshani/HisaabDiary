package com.hisaab.diary.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {
    @Test
    fun `hash matches only the same pin`() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("1234", salt)
        assertTrue(PinHasher.matches("1234", salt, hash))
        assertFalse(PinHasher.matches("1235", salt, hash))
        assertNotEquals("1234", hash)
    }

    @Test
    fun `different salts give different hashes`() {
        assertNotEquals(PinHasher.hash("1234", PinHasher.newSalt()), PinHasher.hash("1234", PinHasher.newSalt()))
        assertEquals(32, PinHasher.newSalt().length)
    }

    @Test
    fun `pin must be four digits`() {
        assertTrue(PinHasher.isValidPin("0007"))
        assertFalse(PinHasher.isValidPin("123"))
        assertFalse(PinHasher.isValidPin("12a4"))
    }

    @Test
    fun `lock policy`() {
        assertTrue(LockPolicy.shouldLock(true, backgroundAt = 0, now = 30_000, timeoutSeconds = 30))
        assertFalse(LockPolicy.shouldLock(true, backgroundAt = 0, now = 29_999, timeoutSeconds = 30))
        assertFalse(LockPolicy.shouldLock(false, backgroundAt = 0, now = 99_999, timeoutSeconds = 30))
        assertFalse(LockPolicy.shouldLock(true, backgroundAt = null, now = 99_999, timeoutSeconds = 30))
        assertTrue(LockPolicy.shouldLock(true, backgroundAt = 10, now = 10, timeoutSeconds = 0))
    }
}
