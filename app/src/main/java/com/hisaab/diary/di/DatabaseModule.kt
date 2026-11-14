package com.hisaab.diary.di

import android.content.Context
import androidx.room.Room
import com.hisaab.diary.data.local.HisaabDatabase
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
