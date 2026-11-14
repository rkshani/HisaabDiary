package com.hisaab.diary.core.coroutines

import javax.inject.Qualifier

/** Dispatcher for disk work (Room, files). Injected so tests can swap it. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** Dispatcher for CPU work (sorting, PDF/CSV building). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/** App-wide scope for work that must finish even if the screen closes. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
