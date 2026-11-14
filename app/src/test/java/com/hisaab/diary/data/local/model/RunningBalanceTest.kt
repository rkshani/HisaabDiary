package com.hisaab.diary.data.local.model

import com.hisaab.diary.data.local.entity.TransactionEntity
import com.hisaab.diary.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunningBalanceTest {
    @Test
    fun `running balance adds GOT and subtracts GAVE`() {
        val rows = withRunningBalance(
            listOf(
                entry(TransactionType.GAVE, 1_000),
                entry(TransactionType.GOT, 300),
                entry(TransactionType.GOT, 900),
            ),
        )
        assertEquals(listOf(-1_000L, -700L, 200L), rows.map { it.runningBalancePaisa })
    }

    @Test
    fun `empty list gives empty ledger`() {
        assertTrue(withRunningBalance(emptyList()).isEmpty())
    }

    private fun entry(type: TransactionType, paisa: Long) = TransactionEntity(
        partyId = 1,
        type = type,
        amountPaisa = paisa,
        date = 0,
        createdAt = 0,
        updatedAt = 0,
    )
}
