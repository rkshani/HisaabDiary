package com.hisaab.diary.data.export

import android.content.Context
import android.net.Uri
import com.hisaab.diary.R
import com.hisaab.diary.core.coroutines.IoDispatcher
import com.hisaab.diary.core.csv.Csv
import com.hisaab.diary.core.date.DateFormatter
import com.hisaab.diary.core.money.MoneyFormatter
import com.hisaab.diary.domain.model.EntryType
import com.hisaab.diary.domain.model.ExportRow
import com.hisaab.diary.domain.repository.EntryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Writes entries as CSV to a file the user picked (Storage Access Framework). */
interface CsvExporter {
    /** @param bookId one book, or null for all books. @return number of rows, or null on failure. */
    suspend fun export(bookId: Long?, target: Uri): Int?
}

class AndroidCsvExporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val io: CoroutineDispatcher,
    private val entries: EntryRepository,
) : CsvExporter {
    override suspend fun export(bookId: Long?, target: Uri): Int? = withContext(io) {
        runCatching {
            val rows = entries.getForExport(bookId)
            val stream = context.contentResolver.openOutputStream(target, "wt") ?: error("No output stream")
            stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                // BOM so Excel opens UTF-8 names correctly.
                writer.write(BOM)
                writer.write(Csv.line(header()))
                rows.forEach { writer.write(Csv.line(toFields(it))) }
            }
            rows.size
        }.getOrNull()
    }

    private fun header(): List<String> = listOf(
        R.string.csv_date,
        R.string.csv_book,
        R.string.csv_person,
        R.string.csv_phone,
        R.string.csv_type,
        R.string.csv_amount,
        R.string.csv_note,
        R.string.csv_due_date,
    ).map(context::getString)

    private fun toFields(row: ExportRow): List<String?> = csvFields(
        row = row,
        typeLabel = context.getString(
            if (row.entry.type == EntryType.GAVE) R.string.action_you_gave else R.string.action_you_got,
        ),
    )

    private companion object {
        const val BOM = "﻿"
    }
}

/** One CSV line for [row]. Amount is a plain number (e.g. 1250.50) so spreadsheets can add it up. */
fun csvFields(row: ExportRow, typeLabel: String): List<String?> {
    val dates = DateFormatter()
    val entry = row.entry
    return listOf(
        dates.fileDate(entry.date),
        row.bookName,
        row.partyName,
        row.partyPhone,
        typeLabel,
        MoneyFormatter().formatForInput(entry.amount),
        entry.note,
        entry.dueDate?.let(dates::fileDate),
    )
}

@Module
@InstallIn(SingletonComponent::class)
abstract class CsvModule {
    @Binds
    abstract fun bindCsvExporter(impl: AndroidCsvExporter): CsvExporter
}
