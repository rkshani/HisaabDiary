package com.hisaab.diary.data.contacts

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import com.hisaab.diary.core.coroutines.IoDispatcher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

data class PickedContact(val name: String, val phone: String?)

/** Reads the contact the user picked in the system contact picker. */
interface ContactReader {
    suspend fun read(uri: Uri): PickedContact?
}

/**
 * Reads the one phone row returned by the picker (`ACTION_PICK` on Phone.CONTENT_URI).
 * The picker grants temporary access to that row only, so no READ_CONTACTS permission is needed.
 */
class AndroidContactReader @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : ContactReader {
    override suspend fun read(uri: Uri): PickedContact? = withContext(io) {
        runCatching {
            context.contentResolver.query(uri, arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER), null, null, null)
                ?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    val name = cursor.getString(0).orEmpty().trim()
                    val phone = cursor.getString(1)?.trim()?.ifEmpty { null }
                    PickedContact(name, phone).takeIf { name.isNotEmpty() || phone != null }
                }
        }.getOrNull()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ContactsModule {
    @Binds
    abstract fun bindContactReader(impl: AndroidContactReader): ContactReader
}
