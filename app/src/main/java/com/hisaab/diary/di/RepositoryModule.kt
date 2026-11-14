package com.hisaab.diary.di

import com.hisaab.diary.data.repository.BookRepositoryImpl
import com.hisaab.diary.data.repository.EntryRepositoryImpl
import com.hisaab.diary.data.repository.PartyRepositoryImpl
import com.hisaab.diary.data.repository.ReminderRepositoryImpl
import com.hisaab.diary.domain.repository.BookRepository
import com.hisaab.diary.domain.repository.EntryRepository
import com.hisaab.diary.domain.repository.PartyRepository
import com.hisaab.diary.domain.repository.ReminderRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindBookRepository(impl: BookRepositoryImpl): BookRepository

    @Binds
    @Singleton
    abstract fun bindPartyRepository(impl: PartyRepositoryImpl): PartyRepository

    @Binds
    @Singleton
    abstract fun bindEntryRepository(impl: EntryRepositoryImpl): EntryRepository

    @Binds
    @Singleton
    abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository
}
