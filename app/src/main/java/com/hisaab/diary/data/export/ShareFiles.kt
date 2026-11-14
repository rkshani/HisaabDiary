package com.hisaab.diary.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Files we make for sharing (PDF, CSV) live in cache/share and are shared with FileProvider. */
object ShareFiles {
    private const val SHARE_DIR = "share"

    /** A fresh file in the share folder. Old shared files are removed first. */
    fun newFile(context: Context, name: String): File {
        val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        return File(dir, safeName(name))
    }

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    /** Share sheet for [uri] with read permission for the receiving app. */
    fun shareIntent(uri: Uri, mimeType: String, title: String): Intent = Intent.createChooser(
        Intent(Intent.ACTION_SEND)
            .setType(mimeType)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
        title,
    )

    /** Keeps letters, digits, dot, dash and underscore, e.g. "Ali Raza.pdf" → "Ali_Raza.pdf". */
    fun safeName(name: String): String = name.trim()
        .replace(Regex("[^A-Za-z0-9._-]+"), "_")
        .trim('_')
        .ifEmpty { "file" }
}
