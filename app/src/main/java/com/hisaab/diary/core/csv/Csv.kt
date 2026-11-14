package com.hisaab.diary.core.csv

/** Tiny CSV writer (RFC 4180): quotes fields with comma, quote or new line; lines end with CRLF. */
object Csv {
    private const val QUOTE = "\""
    private val specialChars = setOf(',', '"', '\n', '\r')
    private val formulaStarts = setOf('=', '+', '-', '@')

    fun line(fields: List<String?>): String = fields.joinToString(",") { escape(it.orEmpty()) } + "\r\n"

    fun escape(field: String): String {
        val needsQuotes = field.any { it in specialChars } || field.startsWith(' ') || field.endsWith(' ')
        // Stop spreadsheet apps from running text as a formula (CSV injection).
        val safe = if (field.firstOrNull() in formulaStarts) "'$field" else field
        return if (needsQuotes) QUOTE + safe.replace(QUOTE, QUOTE + QUOTE) + QUOTE else safe
    }
}
