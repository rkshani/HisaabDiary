package com.hisaab.diary.domain.usecase

import com.hisaab.diary.domain.model.Entry
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the last deleted entry for a short time, so the ledger screen can offer "Undo"
 * after the edit screen has closed. In memory only.
 */
@Singleton
class RecentlyDeletedEntry @Inject constructor() {
    private val _entry = MutableStateFlow<Entry?>(null)
    val entry: StateFlow<Entry?> = _entry.asStateFlow()

    fun remember(entry: Entry) {
        _entry.value = entry
    }

    /** Clears it, but only if it is still [entry] (a newer delete wins). */
    fun clear(entry: Entry) {
        _entry.compareAndSet(entry, null)
    }
}
