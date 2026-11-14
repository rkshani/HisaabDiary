package com.hisaab.diary.data.backup

import android.content.Context
import android.net.Uri
import androidx.sqlite.db.SimpleSQLiteQuery
import com.hisaab.diary.BuildConfig
import com.hisaab.diary.core.coroutines.IoDispatcher
import com.hisaab.diary.core.date.Clock
import com.hisaab.diary.core.security.BackupCrypto
import com.hisaab.diary.data.local.HisaabDatabase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Info saved inside every backup, checked before a restore. */
@Serializable
data class BackupMeta(
    val formatVersion: Int,
    val databaseVersion: Int,
    val appVersion: String,
    val createdAt: Long,
    val billCount: Int,
)

/** Backup and restore, behind an interface so ViewModels can be tested. */
interface BackupService {
    suspend fun create(target: Uri, password: String?): Boolean
}

/**
 * Makes a `.hisaab` backup: a zip with the Room database, bill photos and [BackupMeta],
 * optionally encrypted with a password (see [BackupCrypto]). Fully offline.
 */
@Singleton
class BackupManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val io: CoroutineDispatcher,
    private val database: HisaabDatabase,
    private val clock: Clock,
) : BackupService {
    private val json = Json { ignoreUnknownKeys = true }

    /** Writes the backup to [target]. @return true when it worked. */
    override suspend fun create(target: Uri, password: String?): Boolean = withContext(io) {
        runCatching {
            // Move everything from the WAL file into the main database file first.
            database.query(SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)")).use { it.moveToFirst() }
            val dbFile = context.getDatabasePath(HisaabDatabase.NAME)
            val bills = billsDir().listFiles()?.filter { it.isFile }.orEmpty()
            val meta = BackupMeta(
                formatVersion = FORMAT_VERSION,
                databaseVersion = HisaabDatabase.VERSION,
                appVersion = BuildConfig.VERSION_NAME,
                createdAt = clock.nowMillis(),
                billCount = bills.size,
            )
            val output = context.contentResolver.openOutputStream(target, "wt") ?: error("Cannot open file")
            output.use { raw ->
                ZipOutputStream(BackupCrypto.encryptingStream(raw, password)).use { zip ->
                    zip.putEntry(META_ENTRY) { write(json.encodeToString(BackupMeta.serializer(), meta).toByteArray()) }
                    zip.putEntry(DB_ENTRY) { dbFile.inputStream().use { it.copyTo(this) } }
                    bills.forEach { bill ->
                        zip.putEntry("$BILLS_PREFIX${bill.name}") { bill.inputStream().use { it.copyTo(this) } }
                    }
                }
            }
            true
        }.getOrDefault(false)
    }

    private fun billsDir() = File(context.filesDir, BILLS_DIR)

    private inline fun ZipOutputStream.putEntry(name: String, write: ZipOutputStream.() -> Unit) {
        putNextEntry(ZipEntry(name))
        write()
        closeEntry()
    }

    companion object {
        const val FORMAT_VERSION = 1
        const val META_ENTRY = "meta.json"
        const val DB_ENTRY = "hisaab.db"
        const val BILLS_PREFIX = "bills/"
        const val BILLS_DIR = "bills"

        /** Suggested file name, e.g. "hisaab-backup-2026-11-14.hisaab". */
        fun fileName(date: String) = "hisaab-backup-$date.hisaab"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {
    @Binds
    abstract fun bindBackupService(impl: BackupManager): BackupService
}
