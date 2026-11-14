package com.hisaab.diary.core.money

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Tiny calculator for the amount field: numbers with + − × ÷, normal precedence.
 * Uses BigDecimal (never Float/Double) and rounds the result to whole paisa.
 * Example: "1200+350×2" → Rs 1,900.
 */
object AmountCalculator {
    const val PLUS = '+'
    const val MINUS = '−'
    const val TIMES = '×'
    const val DIVIDE = '÷'
    val operators = setOf(PLUS, MINUS, TIMES, DIVIDE)

    private const val MAX_DECIMALS = 2
    private val mathContext = MathContext.DECIMAL64

    /** True when the text has an operator, so the UI should show the result preview. */
    fun hasOperator(expression: String): Boolean = expression.any { it in operators }

    /**
     * Result in paisa, or null when the expression is incomplete or invalid
     * (empty, ends with an operator, divide by zero, negative result, or too big).
     */
    fun evaluate(expression: String): Money? {
        val tokens = tokenize(expression) ?: return null
        val result = runCatching { calculate(tokens) }.getOrNull() ?: return null
        val paisa = result.setScale(MAX_DECIMALS, RoundingMode.HALF_UP).movePointRight(MAX_DECIMALS)
        return paisa
            .takeIf { it.signum() >= 0 && it <= BigDecimal.valueOf(MoneyParser.MAX.paisa) }
            ?.let { Money(it.longValueExact()) }
    }

    /**
     * Adds one keypad key to [expression] and returns the new text.
     * Keeps the text valid: one dot per number, max 2 decimals, no double operators.
     */
    fun append(expression: String, key: Char): String {
        val lastNumber = expression.takeLastWhile { it !in operators }
        return when {
            key.isDigit() -> appendDigit(expression, lastNumber, key)

            key == '.' -> when {
                '.' in lastNumber -> expression
                lastNumber.isEmpty() -> expression + "0."
                else -> "$expression."
            }

            key in operators -> when {
                expression.isEmpty() -> expression
                expression.last() in operators -> expression.dropLast(1) + key
                else -> expression + key
            }

            else -> expression
        }
    }

    private fun appendDigit(expression: String, lastNumber: String, digit: Char): String {
        val decimals = lastNumber.substringAfter('.', "")
        return when {
            '.' in lastNumber && decimals.length >= MAX_DECIMALS -> expression
            lastNumber == "0" -> expression.dropLast(1) + digit
            lastNumber.length >= MAX_NUMBER_LENGTH -> expression
            else -> expression + digit
        }
    }

    private const val MAX_NUMBER_LENGTH = 13

    private sealed interface Token {
        data class Num(val value: BigDecimal) : Token

        data class Op(val symbol: Char) : Token
    }

    private fun tokenize(expression: String): List<Token>? {
        if (expression.isBlank()) return null
        val numbers = expression.split(PLUS, MINUS, TIMES, DIVIDE).mapNotNull { it.toBigDecimalOrNull() }
        val ops = expression.filter { it in operators }
        if (numbers.size != ops.length + 1) return null
        return buildList {
            numbers.forEachIndexed { i, number ->
                add(Token.Num(number))
                if (i < ops.length) add(Token.Op(ops[i]))
            }
        }
    }

    /** Two passes: first × and ÷, then + and −. */
    private fun calculate(tokens: List<Token>): BigDecimal {
        val terms = mutableListOf<Token>()
        var index = 0
        while (index < tokens.size) {
            val token = tokens[index]
            if (token is Token.Op && (token.symbol == TIMES || token.symbol == DIVIDE)) {
                val left = (terms.removeAt(terms.lastIndex) as Token.Num).value
                val right = (tokens[index + 1] as Token.Num).value
                val value = if (token.symbol == TIMES) {
                    left.multiply(right, mathContext)
                } else {
                    left.divide(right, mathContext)
                }
                terms += Token.Num(value)
                index += 2
            } else {
                terms += token
                index++
            }
        }
        var total = (terms.first() as Token.Num).value
        for (i in 1 until terms.size step 2) {
            val op = (terms[i] as Token.Op).symbol
            val value = (terms[i + 1] as Token.Num).value
            total = if (op == PLUS) total.add(value) else total.subtract(value)
        }
        return total
    }
}
