package com.hisaab.diary.core.phone

/** Phone number helpers. The app is made for Pakistan, so local numbers get +92. */
object PhoneNumbers {
    private const val COUNTRY_CODE = "92"
    private const val MIN_DIGITS = 7

    /**
     * Digits in international form for WhatsApp links (no "+"), e.g.
     * "0300-1234567" → "923001234567", "+92 300 1234567" → "923001234567".
     * Returns null when there are too few digits.
     */
    fun toInternational(phone: String): String? {
        val digits = phone.filter { it.isDigit() }
        val international = when {
            phone.trim().startsWith("+") -> digits
            digits.startsWith("00") -> digits.drop(2)
            digits.startsWith("0") -> COUNTRY_CODE + digits.drop(1)
            else -> digits
        }
        return international.takeIf { it.length >= MIN_DIGITS }
    }

    /** Number for SMS apps: keeps "+" and digits only. */
    fun forSms(phone: String): String = phone.filter { it.isDigit() || it == '+' }
}
