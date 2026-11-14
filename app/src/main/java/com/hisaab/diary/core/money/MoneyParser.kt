package com.hisaab.diary.core.money

/**
 * Reads an amount typed by the user, e.g. `1250`, `1,250.5`, `Rs 99.99`.
 * Returns null when the text is not a valid positive-or-zero amount
 * (letters, a minus sign, more than 2 decimals, or too big).
 */
object MoneyParser {
    /** Biggest amount we accept: Rs 999,999,999,999.99 is far above any real khata entry. */
    val MAX = Money(999_999_999_999_99L)

    private const val MAX_DECIMALS = 2
    private val pattern = Regex("""^(\d+)(?:\.(\d{0,2}))?$""")

    fun parse(input: String): Money? {
        val cleaned = input.trim()
            .removePrefix(CurrencySymbol.RS.text)
            .removePrefix(CurrencySymbol.RUPEE_SIGN.text)
            .replace(",", "")
            .replace(" ", "")
            .let { if (it.startsWith(".") && it.length > 1) "0$it" else it }
        val match = pattern.matchEntire(cleaned)
            ?.takeIf { it.groupValues[1].length <= MAX_RUPEE_DIGITS }
            ?: return null
        val rupees = match.groupValues[1].toLong()
        val paisa = match.groupValues[2].padEnd(MAX_DECIMALS, '0').toLong()
        return Money(rupees * Money.PAISA_PER_RUPEE + paisa).takeIf { it <= MAX }
    }

    private const val MAX_RUPEE_DIGITS = 13
}
