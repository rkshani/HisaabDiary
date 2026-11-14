package com.hisaab.diary.core.money

/** How the currency is written before the amount. */
enum class CurrencySymbol(val text: String) {
    RS("Rs"),
    RUPEE_SIGN("₨"),
    NONE(""),
}

/**
 * Formats [Money] for display, e.g. `Rs 1,250` or `Rs 1,250.50`.
 * Uses fixed ASCII digits and comma grouping, so output does not depend on the phone locale.
 */
class MoneyFormatter(
    private val symbol: CurrencySymbol = CurrencySymbol.RS,
    /** Privacy mode: amounts show as "Rs ****". */
    private val hidden: Boolean = false,
) {
    /**
     * @param showSign add "-" for negative amounts. Lists usually show the absolute
     *   value with a color instead, so the default is false.
     * @param alwaysShowPaisa show ".00" even for whole rupees.
     */
    fun format(money: Money, showSign: Boolean = false, alwaysShowPaisa: Boolean = false): String {
        if (hidden) return if (symbol == CurrencySymbol.NONE) HIDDEN else "${symbol.text} $HIDDEN"
        val number = buildString {
            if (showSign && money.isNegative) append('-')
            append(groupThousands(money.abs().rupees))
            val paisa = money.paisaPart
            if (paisa != 0 || alwaysShowPaisa) {
                append('.')
                append(paisa.toString().padStart(2, '0'))
            }
        }
        return if (symbol == CurrencySymbol.NONE) number else "${symbol.text} $number"
    }

    /** Amount for an input field: no symbol, no grouping, e.g. `1250.5` → `1250.50`. */
    fun formatForInput(money: Money): String {
        val abs = money.abs()
        return if (abs.paisaPart == 0) {
            abs.rupees.toString()
        } else {
            "${abs.rupees}.${abs.paisaPart.toString().padStart(2, '0')}"
        }
    }

    private fun groupThousands(value: Long): String {
        val digits = value.toString()
        val firstGroup = digits.length % GROUP_SIZE
        return buildString {
            digits.forEachIndexed { index, char ->
                if (index > 0 && (index - firstGroup) % GROUP_SIZE == 0) append(',')
                append(char)
            }
        }
    }

    private companion object {
        const val GROUP_SIZE = 3
        const val HIDDEN = "****"
    }
}
