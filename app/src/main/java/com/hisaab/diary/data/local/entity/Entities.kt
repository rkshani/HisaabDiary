package com.hisaab.diary.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A khata book, e.g. "Personal" or "Shop". */
@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/** A person or business you give money to or get money from. Deleting a book deletes its parties. */
@Entity(
    tableName = "parties",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("book_id"), Index("name")],
)
data class PartyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "book_id") val bookId: Long,
    val name: String,
    val phone: String? = null,
    val note: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "is_archived", defaultValue = "0") val isArchived: Boolean = false,
)

/** Direction of one entry. GAVE = you gave money (they owe you). GOT = you got money. */
enum class TransactionType { GAVE, GOT }

/**
 * One khata entry. Money is in paisa, dates are epoch millis.
 * Deleting a party deletes its entries.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = PartyEntity::class,
            parentColumns = ["id"],
            childColumns = ["party_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("party_id"), Index("date"), Index("due_date")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "party_id") val partyId: Long,
    val type: TransactionType,
    @ColumnInfo(name = "amount_paisa") val amountPaisa: Long,
    val note: String? = null,
    val date: Long,
    @ColumnInfo(name = "due_date") val dueDate: Long? = null,
    @ColumnInfo(name = "attachment_path") val attachmentPath: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** A reminder to collect or pay money. Deleting a party deletes its reminders. */
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = PartyEntity::class,
            parentColumns = ["id"],
            childColumns = ["party_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("party_id"), Index("remind_at")],
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "party_id") val partyId: Long,
    @ColumnInfo(name = "remind_at") val remindAt: Long,
    @ColumnInfo(name = "is_done", defaultValue = "0") val isDone: Boolean = false,
)
