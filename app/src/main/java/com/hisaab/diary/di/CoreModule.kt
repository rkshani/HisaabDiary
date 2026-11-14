package com.hisaab.diary.di

import com.hisaab.diary.core.date.Clock
import com.hisaab.diary.core.date.DateFormatter
import com.hisaab.diary.core.date.DateUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {
    @Provides
    fun provideClock(): Clock = Clock.System

    @Provides
    fun provideDateUtils(clock: Clock): DateUtils = DateUtils(clock)

    @Provides
    fun provideDateFormatter(): DateFormatter = DateFormatter()
}
