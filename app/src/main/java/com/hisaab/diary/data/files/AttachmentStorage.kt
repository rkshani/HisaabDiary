package com.hisaab.diary.data.files

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import com.hisaab.diary.core.coroutines.IoDispatcher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Bill photos, saved as small JPEG files inside the app (never in public storage). */
interface AttachmentStorage {
    /** Copies and compresses the picked/taken image. @return the saved file path, or null on failure. */
    suspend fun save(source: Uri): String?

    /** A temporary file the camera app can write to. */
    fun newCameraUri(): Uri

    /** Deletes one of our saved files. Quick, safe to call from anywhere. */
    fun delete(path: String)
}

class AndroidAttachmentStorage @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : AttachmentStorage {
    private val billsDir get() = File(context.filesDir, BILLS_DIR).apply { mkdirs() }
    private val cameraDir get() = File(context.cacheDir, CAMERA_DIR).apply { mkdirs() }

    override suspend fun save(source: Uri): String? = withContext(io) {
        runCatching {
            val bitmap = decodeScaled(source) ?: return@runCatching null
            val rotated = rotateFromExif(source, bitmap)
            val file = File(billsDir, "${UUID.randomUUID()}.jpg")
            file.outputStream().use { rotated.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            file.absolutePath
        }.getOrNull()
    }

    override fun newCameraUri(): Uri {
        val file = File(cameraDir, "camera.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    override fun delete(path: String) {
        val file = File(path)
        // Only delete our own files.
        if (file.parentFile?.canonicalPath == billsDir.canonicalPath) file.delete()
    }

    private fun decodeScaled(source: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val sample = sampleSize(bounds.outWidth, bounds.outHeight)
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    // Platform ExifInterface is fine here: we only read the orientation tag of our own picked image.
    @SuppressLint("ExifInterface")
    private fun rotateFromExif(source: Uri, bitmap: Bitmap): Bitmap {
        val orientation = context.contentResolver.openInputStream(source)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> QUARTER_TURN
            ExifInterface.ORIENTATION_ROTATE_180 -> 2 * QUARTER_TURN
            ExifInterface.ORIENTATION_ROTATE_270 -> -QUARTER_TURN
            else -> return bitmap
        }
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /** Power of two that brings the longest side down to about [MAX_SIDE]. */
    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= MAX_SIDE) sample *= 2
        return sample
    }

    private companion object {
        const val BILLS_DIR = "bills"
        const val CAMERA_DIR = "camera"
        const val JPEG_QUALITY = 80
        const val MAX_SIDE = 1600
        const val QUARTER_TURN = 90f
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class FilesModule {
    @Binds
    @Singleton
    abstract fun bindAttachmentStorage(impl: AndroidAttachmentStorage): AttachmentStorage
}
