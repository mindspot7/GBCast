package com.flycast.emulator.shell

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import gb.frame.OpenDisc
import gb.frame.SafGames
import java.io.File
import java.nio.file.Files

/** Games folder through the Storage Access Framework, preferring direct descriptor-backed access. */
private object GamesFolder {
    var path: String = ""
}

object Storage : SafGames(GameIndex.extensions, { GamesFolder.path }) {
    var gamesDir: String
        get() = GamesFolder.path
        set(value) { GamesFolder.path = value }
    private lateinit var app: Context

    fun start(context: Context) {
        app = context.applicationContext
        super.init(app)
    }

    fun openForCore(path: String): OpenDisc? {
        val ext = fileName(path).substringAfterLast('.', "").lowercase()
        val opened = open(path) ?: return null
        val src = File(opened.path)
        val parent = src.parentFile
        if (parent == null) return opened
        val links = parent.listFiles()?.filter { Files.isSymbolicLink(it.toPath()) }.orEmpty()
        if (links.isEmpty()) return opened

        // Flycast reopens descriptor links by path. Most devices allow that directly; probe every referenced track
        // before deciding this device needs the GBFrame cached-copy fallback.
        val directAccess = links.all { link -> runCatching { link.inputStream().use { it.read() } }.isSuccess }
        if (directAccess) {
            clearDiscCopies()
            return opened
        }
        opened.close()
        return if (ext == "chd" || ext == "iso") open(path, stageCopy = true) else null
    }

    fun importFile(uri: Uri, destDir: File): String? {
        destDir.mkdirs()
        val name = app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: uri.lastPathSegment?.substringAfterLast('/') ?: return null
        val safe = name.replace(Regex("[\\\\/]+"), "_")
        val dest = File(destDir, safe)
        app.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        return dest.absolutePath
    }
}
