package com.hisaab.diary.core.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumbersTest {
    @Test
    fun `local and international numbers`() {
        assertEquals("923001234567", PhoneNumbers.toInternational("0300-1234567"))
        assertEquals("923001234567", PhoneNumbers.toInternational("+92 300 1234567"))
        assertEquals("923001234567", PhoneNumbers.toInternational("0092 300 1234567"))
        assertEquals("447700900123", PhoneNumbers.toInternational("+44 7700 900123"))
        assertEquals("3001234567", PhoneNumbers.toInternational("3001234567"))
    }

    @Test
    fun `too short is null`() {
        assertNull(PhoneNumbers.toInternational("123"))
        assertNull(PhoneNumbers.toInternational(""))
    }

    @Test
    fun `sms keeps plus and digits`() {
        assertEquals("+923001234567", PhoneNumbers.forSms("+92 300-1234567"))
    }
}
