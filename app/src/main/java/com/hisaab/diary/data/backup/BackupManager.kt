package com.hisaab.diary.data.backup

import android.content.Context
import android.net.Uri
import androidx.sqlite.db.SimpleSQLiteQuery
import com.hisaab.diary.BuildConfig
import com.hisaab.diary.core.coroutines.IoDispatcher
import com.hisaab.diary.core.date.Clock
import com.hisaab.diary.core.security.BackupCrypto
import com.hisaab.diary.data.local.HisaabDatabase
import com.hisaab.diary.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
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

    /** Reads only the header. @return null when the file is not a Hisaab Diary backup. */
    suspend fun needsPassword(source: Uri): Boolean?

    /** Replaces all current data with the backup. The app must restart after [RestoreResult.OK]. */
    suspend fun restore(source: Uri, password: String?): RestoreResult
}

enum class RestoreResult { OK, WRONG_PASSWORD, NOT_A_BACKUP, NEWER_VERSION, FAILED }

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
    private val settings: SettingsRepository,
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
            settings.setLastBackupAt(clock.nowMillis())
            true
        }.getOrDefault(false)
    }

    override suspend fun needsPassword(source: Uri): Boolean? = withContext(io) {
        runCatching {
            context.contentResolver.openInputStream(source)?.use { BackupCrypto.isEncrypted(it) }
        }.getOrNull()
    }

    override suspend fun restore(source: Uri, password: String?): RestoreResult = withContext(io) {
        val work = File(context.cacheDir, RESTORE_DIR).apply {
            deleteRecursively()
            mkdirs()
        }
        try {
            val unpacked = unpack(source, password, work)
            if (unpacked != RestoreResult.OK) return@withContext unpacked
            val meta = json.decodeFromString(BackupMeta.serializer(), File(work, META_ENTRY).readText())
            if (meta.formatVersion > FORMAT_VERSION || meta.databaseVersion > HisaabDatabase.VERSION) {
                return@withContext RestoreResult.NEWER_VERSION
            }
            replaceData(work)
            RestoreResult.OK
        } catch (ignored: BackupCrypto.WrongPasswordException) {
            RestoreResult.WRONG_PASSWORD
        } catch (ignored: BackupCrypto.NotABackupException) {
            RestoreResult.NOT_A_BACKUP
        } catch (ignored: IOException) {
            // A wrong password breaks the AES-GCM check while reading.
            if (password.isNullOrEmpty()) RestoreResult.FAILED else RestoreResult.WRONG_PASSWORD
        } catch (ignored: SerializationException) {
            RestoreResult.NOT_A_BACKUP
        } finally {
            work.deleteRecursively()
        }
    }

    /** Unzips into [work]; only known entry names are written (no paths outside [work]). */
    private fun unpack(source: Uri, password: String?, work: File): RestoreResult {
        val input = context.contentResolver.openInputStream(source) ?: return RestoreResult.FAILED
        input.use { raw ->
            ZipInputStream(BackupCrypto.decryptingStream(raw, password)).use { zip -> zip.extractTo(work) }
        }
        val complete = File(work, META_ENTRY).exists() && File(work, DB_ENTRY).exists()
        return if (complete) RestoreResult.OK else RestoreResult.NOT_A_BACKUP
    }

    private fun ZipInputStream.extractTo(work: File) {
        generateSequence { nextEntry }.forEach { entry ->
            val target = safeTarget(work, entry.name)
            if (target != null && !entry.isDirectory) {
                target.parentFile?.mkdirs()
                target.outputStream().use { copyTo(it) }
            }
        }
    }

    private fun safeTarget(work: File, name: String): File? {
        val allowed = name == META_ENTRY || name == DB_ENTRY ||
            (name.startsWith(BILLS_PREFIX) && name.count { it == '/' } == 1 && !name.contains(".."))
        return if (allowed) File(work, name) else null
    }

    /** Closes the database and swaps in the restored files. */
    private fun replaceData(work: File) {
        database.close()
        val dbFile = context.getDatabasePath(HisaabDatabase.NAME)
        listOf("", "-wal", "-shm", "-journal").forEach { File(dbFile.path + it).delete() }
        File(work, DB_ENTRY).copyTo(dbFile, overwrite = true)
        val bills = billsDir()
        bills.deleteRecursively()
        bills.mkdirs()
        File(work, BILLS_DIR).listFiles()?.forEach { it.copyTo(File(bills, it.name), overwrite = true) }
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
        private const val RESTORE_DIR = "restore"

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
