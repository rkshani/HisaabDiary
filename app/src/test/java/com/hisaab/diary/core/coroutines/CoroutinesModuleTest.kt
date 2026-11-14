package com.hisaab.diary.core.coroutines

import com.hisaab.diary.di.CoroutinesModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoroutinesModuleTest {
    @Test
    fun `io dispatcher is Dispatchers IO`() {
        assertEquals(Dispatchers.IO, CoroutinesModule.provideIoDispatcher())
    }

    @Test
    fun `application scope stays active when a child is cancelled`() {
        val scope = CoroutinesModule.provideApplicationScope(Dispatchers.Unconfined)
        val child = scope.launch { awaitCancellation() }

        child.cancel()

        assertTrue(child.isCancelled)
        assertTrue(scope.coroutineContext[Job]!!.isActive)
        scope.cancel()
    }
}
