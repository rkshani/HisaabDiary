package com.hisaab.diary.data.mapper

import com.hisaab.diary.core.money.Money
import com.hisaab.diary.data.local.entity.BookEntity
import com.hisaab.diary.data.local.entity.PartyEntity
import com.hisaab.diary.data.local.entity.TransactionEntity
import com.hisaab.diary.data.local.entity.TransactionType
import com.hisaab.diary.data.local.model.BookTotals as BookTotalsRow
import com.hisaab.diary.data.local.model.PartyWithBalance
import com.hisaab.diary.domain.model.Book
import com.hisaab.diary.domain.model.BookTotals
import com.hisaab.diary.domain.model.Entry
import com.hisaab.diary.domain.model.EntryType
import com.hisaab.diary.domain.model.Party
import com.hisaab.diary.domain.model.PartySummary

fun BookEntity.toDomain() = Book(id = id, name = name, createdAt = createdAt)

fun PartyEntity.toDomain() = Party(
    id = id,
    bookId = bookId,
    name = name,
    phone = phone,
    note = note,
    createdAt = createdAt,
    isArchived = isArchived,
)

fun Party.toEntity() = PartyEntity(
    id = id,
    bookId = bookId,
    name = name,
    phone = phone,
    note = note,
    createdAt = createdAt,
    isArchived = isArchived,
)

fun PartyWithBalance.toDomain() = PartySummary(
    party = party.toDomain(),
    totalGave = Money(totalGavePaisa),
    totalGot = Money(totalGotPaisa),
    lastEntryDate = lastEntryDate,
    entryCount = entryCount,
)

fun BookTotalsRow.toDomain() = BookTotals(willGet = Money(willGetPaisa), willGive = Money(willGivePaisa))

fun TransactionType.toDomain() = when (this) {
    TransactionType.GAVE -> EntryType.GAVE
    TransactionType.GOT -> EntryType.GOT
}

fun EntryType.toEntity() = when (this) {
    EntryType.GAVE -> TransactionType.GAVE
    EntryType.GOT -> TransactionType.GOT
}

fun TransactionEntity.toDomain() = Entry(
    id = id,
    partyId = partyId,
    type = type.toDomain(),
    amount = Money(amountPaisa),
    note = note,
    date = date,
    dueDate = dueDate,
    attachmentPath = attachmentPath,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Entry.toEntity() = TransactionEntity(
    id = id,
    partyId = partyId,
    type = type.toEntity(),
    amountPaisa = amount.paisa,
    note = note,
    date = date,
    dueDate = dueDate,
    attachmentPath = attachmentPath,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
