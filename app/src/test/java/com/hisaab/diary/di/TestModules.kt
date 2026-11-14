package com.hisaab.diary.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.hisaab.diary.core.coroutines.ApplicationScope
import com.hisaab.diary.data.local.HisaabDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.io.File
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

/** In-memory database for Hilt tests. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object TestDatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HisaabDatabase =
        Room.inMemoryDatabaseBuilder(context, HisaabDatabase::class.java).allowMainThreadQueries().build()
}

/** A new DataStore file per test, so tests do not share settings. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DataStoreModule::class])
object TestDataStoreModule {
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationScope scope: CoroutineScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope) {
            File.createTempFile("settings", ".preferences_pb").also { it.delete() }
        }
}
