package com.hisaab.diary.ui.navigation

import kotlinx.serialization.Serializable

/** Type-safe navigation routes. */
@Serializable
data object HomeRoute

@Serializable
data object ReportsRoute

@Serializable
data object SettingsRoute

@Serializable
data class PartyDetailRoute(val partyId: Long)

/**
 * Add a new entry for [partyId], or edit [entryId] when it is not null.
 * [isGave] picks the starting type: true = "You gave", false = "You got".
 */
@Serializable
data class AddEntryRoute(val partyId: Long, val isGave: Boolean = true, val entryId: Long? = null)
