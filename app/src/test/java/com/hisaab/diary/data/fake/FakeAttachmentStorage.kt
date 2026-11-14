package com.hisaab.diary.data.fake

import android.net.Uri
import com.hisaab.diary.data.files.AttachmentStorage

/** Remembers "saved" and deleted paths instead of touching files. */
class FakeAttachmentStorage : AttachmentStorage {
    val saved = mutableListOf<String>()
    val deleted = mutableListOf<String>()
    private var counter = 0

    override suspend fun save(source: Uri): String = "/bills/photo${++counter}.jpg".also { saved += it }

    override fun newCameraUri(): Uri = Uri.parse("content://test/camera.jpg")

    override fun delete(path: String) {
        deleted += path
    }
}
