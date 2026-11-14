package com.hisaab.diary.di

import android.content.Context
import androidx.room.Room
import com.hisaab.diary.data.local.HisaabDatabase
import com.hisaab.diary.data.local.dao.BookDao
import com.hisaab.diary.data.local.dao.PartyDao
import com.hisaab.diary.data.local.dao.ReminderDao
import com.hisaab.diary.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HisaabDatabase =
        Room.databaseBuilder(context, HisaabDatabase::class.java, HisaabDatabase.NAME)
            .build()
}

/** DAOs come from the database. Separate, so tests can replace only the database. */
@Module
@InstallIn(SingletonComponent::class)
object DaoModule {
    @Provides
    fun provideBookDao(db: HisaabDatabase): BookDao = db.bookDao()

    @Provides
    fun providePartyDao(db: HisaabDatabase): PartyDao = db.partyDao()

    @Provides
    fun provideTransactionDao(db: HisaabDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideReminderDao(db: HisaabDatabase): ReminderDao = db.reminderDao()
}
