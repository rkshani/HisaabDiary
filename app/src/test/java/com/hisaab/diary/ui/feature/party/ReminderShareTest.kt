package com.hisaab.diary.ui.feature.party

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderShareTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun messageUsesApprovedTemplate() {
        val message = reminderMessage(context, "Ali", "Rs 2,000")
        assertEquals(
            "Assalam o Alaikum Ali,\n\nThis is a gentle reminder that\nRs 2,000 is pending in my khata.\n" +
                "Please pay when you can.\n\nThank you!\n— sent from Hisaab Diary",
            message,
        )
    }

    @Test
    fun whatsappWithPhoneOpensTheChat() {
        val intent = reminderIntent(ReminderChannel.WHATSAPP, "Hi Ali", "0300-1234567")
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("com.whatsapp", intent.`package`)
        assertEquals("https://wa.me/923001234567?text=Hi%20Ali", intent.dataString)
    }

    @Test
    fun whatsappWithoutPhoneSharesText() {
        val intent = reminderIntent(ReminderChannel.WHATSAPP, "Hi", null)
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("Hi", intent.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun smsGoesToThePhone() {
        val intent = reminderIntent(ReminderChannel.SMS, "Hi", "+92 300 1234567")
        assertEquals(Intent.ACTION_SENDTO, intent.action)
        assertEquals("smsto:+923001234567", intent.dataString)
        assertEquals("Hi", intent.getStringExtra("sms_body"))
    }

    @Test
    fun otherAppsUsesChooser() {
        val intent = reminderIntent(ReminderChannel.OTHER, "Hi", "0300")
        assertTrue(intent.action == Intent.ACTION_CHOOSER)
    }
}
