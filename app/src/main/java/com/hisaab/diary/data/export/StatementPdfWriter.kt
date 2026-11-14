package com.hisaab.diary.data.export

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.hisaab.diary.R
import com.hisaab.diary.core.coroutines.IoDispatcher
import com.hisaab.diary.core.date.Clock
import com.hisaab.diary.core.date.DateFormatter
import com.hisaab.diary.core.money.Money
import com.hisaab.diary.core.money.MoneyFormatter
import com.hisaab.diary.domain.model.EntryType
import com.hisaab.diary.domain.model.LedgerEntry
import com.hisaab.diary.domain.model.PartySummary
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Makes a party statement PDF and returns the file, or null on failure. */
interface StatementExporter {
    suspend fun export(summary: PartySummary, ledger: List<LedgerEntry>): File?
}

/**
 * Draws an A4 statement with Android [PdfDocument]: header, a table with running balance
 * (more pages when needed), totals and a "Made with Hisaab Diary" footer.
 */
class StatementPdfWriter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val io: CoroutineDispatcher,
    private val clock: Clock,
) : StatementExporter {
    private val money = MoneyFormatter()
    private val dates = DateFormatter()

    override suspend fun export(summary: PartySummary, ledger: List<LedgerEntry>): File? = withContext(io) {
        runCatching {
            val file = ShareFiles.newFile(
                context,
                "${summary.party.name} statement ${dates.fileDate(clock.nowMillis())}.pdf",
            )
            val document = PdfDocument()
            try {
                draw(document, summary, ledger)
                file.outputStream().use { document.writeTo(it) }
            } finally {
                document.close()
            }
            file
        }.getOrNull()
    }

    private fun draw(document: PdfDocument, summary: PartySummary, ledger: List<LedgerEntry>) {
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var y = drawHeader(page, summary)
        y = drawTableHeader(page, y)
        for (row in ledger) {
            if (y > PAGE_HEIGHT - BOTTOM_SPACE) {
                drawFooter(page, pageNumber)
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                y = drawTableHeader(page, MARGIN + ROW_HEIGHT)
            }
            y = drawRow(page, row, y)
        }
        drawTotals(page, summary, y + ROW_HEIGHT)
        drawFooter(page, pageNumber)
        document.finishPage(page)
    }

    private fun drawHeader(page: PdfDocument.Page, summary: PartySummary): Float {
        val canvas = page.canvas
        var y = MARGIN + TITLE_SIZE
        canvas.drawText(context.getString(R.string.app_name), MARGIN, y, paint(TITLE_SIZE, bold = true))
        y += LINE_GAP * 2
        canvas.drawText(
            context.getString(R.string.pdf_statement_of, summary.party.name),
            MARGIN,
            y,
            paint(HEADING_SIZE, bold = true),
        )
        summary.party.phone?.let {
            y += LINE_GAP
            canvas.drawText(it, MARGIN, y, paint(TEXT_SIZE))
        }
        y += LINE_GAP
        canvas.drawText(
            context.getString(R.string.pdf_generated_on, dates.dateTime(clock.nowMillis())),
            MARGIN,
            y,
            paint(TEXT_SIZE),
        )
        return y + LINE_GAP * 2
    }

    private fun drawTableHeader(page: PdfDocument.Page, top: Float): Float {
        val bold = paint(TEXT_SIZE, bold = true)
        val canvas = page.canvas
        canvas.drawText(context.getString(R.string.pdf_col_date), COL_DATE, top, bold)
        canvas.drawText(context.getString(R.string.pdf_col_note), COL_NOTE, top, bold)
        canvas.drawText(context.getString(R.string.action_you_gave), COL_GAVE, top, bold.alignRight())
        canvas.drawText(context.getString(R.string.action_you_got), COL_GOT, top, bold.alignRight())
        canvas.drawText(context.getString(R.string.pdf_col_balance), COL_BALANCE, top, bold.alignRight())
        canvas.drawLine(MARGIN, top + LINE_PAD, PAGE_WIDTH - MARGIN, top + LINE_PAD, paint(TEXT_SIZE))
        return top + ROW_HEIGHT
    }

    private fun drawRow(page: PdfDocument.Page, row: LedgerEntry, top: Float): Float {
        val canvas = page.canvas
        val text = paint(TEXT_SIZE)
        val entry = row.entry
        canvas.drawText(dates.date(entry.date), COL_DATE, top, text)
        canvas.drawText(ellipsize(entry.note.orEmpty(), text, COL_GAVE - COL_NOTE - NOTE_GAP), COL_NOTE, top, text)
        val amount = money.format(entry.amount)
        if (entry.type == EntryType.GAVE) {
            canvas.drawText(amount, COL_GAVE, top, text.alignRight())
        } else {
            canvas.drawText(amount, COL_GOT, top, text.alignRight())
        }
        canvas.drawText(balanceText(row.runningBalance), COL_BALANCE, top, text.alignRight())
        return top + ROW_HEIGHT
    }

    private fun drawTotals(page: PdfDocument.Page, summary: PartySummary, top: Float) {
        val canvas = page.canvas
        val bold = paint(TEXT_SIZE, bold = true)
        canvas.drawLine(MARGIN, top - ROW_HEIGHT + LINE_PAD, PAGE_WIDTH - MARGIN, top - ROW_HEIGHT + LINE_PAD, bold)
        canvas.drawText(context.getString(R.string.pdf_total), COL_DATE, top, bold)
        canvas.drawText(money.format(summary.totalGave), COL_GAVE, top, bold.alignRight())
        canvas.drawText(money.format(summary.totalGot), COL_GOT, top, bold.alignRight())
        canvas.drawText(balanceText(summary.balance), COL_BALANCE, top, bold.alignRight())
    }

    private fun drawFooter(page: PdfDocument.Page, pageNumber: Int) {
        val small = paint(SMALL_SIZE)
        val y = PAGE_HEIGHT - MARGIN
        page.canvas.drawText(context.getString(R.string.pdf_footer), MARGIN, y, small)
        page.canvas.drawText(
            context.getString(R.string.pdf_page, pageNumber),
            PAGE_WIDTH - MARGIN,
            y,
            small.alignRight(),
        )
    }

    /** "Rs 1,500 (will get)" style, so the sign is clear on paper without colors. */
    private fun balanceText(balance: Money): String {
        val label = when {
            balance.isNegative -> context.getString(R.string.pdf_bal_get)
            balance.isPositive -> context.getString(R.string.pdf_bal_give)
            else -> ""
        }
        return (money.format(balance) + " " + label).trim()
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 0 && paint.measureText(text.substring(0, end) + ELLIPSIS) > maxWidth) end--
        return text.substring(0, end) + ELLIPSIS
    }

    private fun paint(size: Float, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun Paint.alignRight() = Paint(this).apply { textAlign = Paint.Align.RIGHT }

    private companion object {
        // A4 in PostScript points.
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 40f
        const val TITLE_SIZE = 20f
        const val HEADING_SIZE = 14f
        const val TEXT_SIZE = 10f
        const val SMALL_SIZE = 8f
        const val LINE_GAP = 16f
        const val LINE_PAD = 5f
        const val ROW_HEIGHT = 18f
        const val BOTTOM_SPACE = 80f
        const val NOTE_GAP = 70f
        const val COL_DATE = MARGIN
        const val COL_NOTE = 120f
        const val COL_GAVE = 360f
        const val COL_GOT = 440f
        const val COL_BALANCE = 555f
        const val ELLIPSIS = "…"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ExportModule {
    @Binds
    abstract fun bindStatementExporter(impl: StatementPdfWriter): StatementExporter
}
