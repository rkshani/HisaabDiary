package com.hisaab.diary.data.export

import org.junit.Assert.assertEquals
import org.junit.Test

class ShareFilesTest {
    @Test
    fun `safe file names`() {
        assertEquals("Ali_Raza_statement_2026-11-14.pdf", ShareFiles.safeName("Ali Raza statement 2026-11-14.pdf"))
        assertEquals("file", ShareFiles.safeName("  /// "))
        assertEquals("Shop_1.csv", ShareFiles.safeName("Shop #1.csv"))
    }
}
