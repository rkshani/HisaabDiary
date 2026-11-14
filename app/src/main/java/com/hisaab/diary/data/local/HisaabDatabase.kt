package com.hisaab.diary.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.hisaab.diary.data.local.dao.BookDao
import com.hisaab.diary.data.local.dao.PartyDao
import com.hisaab.diary.data.local.dao.ReminderDao
import com.hisaab.diary.data.local.dao.TransactionDao
import com.hisaab.diary.data.local.entity.BookEntity
import com.hisaab.diary.data.local.entity.PartyEntity
import com.hisaab.diary.data.local.entity.ReminderEntity
import com.hisaab.diary.data.local.entity.TransactionEntity
import com.hisaab.diary.data.local.entity.TransactionType

/**
 * The app database. The schema JSON is exported to `app/schemas` and committed,
 * so every future change needs a tested migration.
 */
@Database(
    entities = [BookEntity::class, PartyEntity::class, TransactionEntity::class, ReminderEntity::class],
    version = HisaabDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class HisaabDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    abstract fun partyDao(): PartyDao

    abstract fun transactionDao(): TransactionDao

    abstract fun reminderDao(): ReminderDao

    companion object {
        const val VERSION = 1
        const val NAME = "hisaab.db"
    }
}

/** Stores [TransactionType] as text ("GAVE"/"GOT") so the order of enum values never matters. */
class Converters {
    @TypeConverter
    fun fromType(type: TransactionType): String = type.name

    @TypeConverter
    fun toType(value: String): TransactionType = TransactionType.valueOf(value)
}
