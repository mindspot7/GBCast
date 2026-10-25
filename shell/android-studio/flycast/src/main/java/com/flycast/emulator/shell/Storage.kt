package com.flycast.emulator.shell

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import gb.frame.OpenDisc
import gb.frame.SafGames
import java.io.File
import java.nio.file.Files

/**
 * Games folder through the Storage Access Framework. GDI and CUE stay as staged symlinks so relative track names resolve.
 * Single-file images are copied to a real cache file because Flycast reopens them by path.
 */
private object GamesFolder {
    var path: String = ""
}

object Storage : SafGames(GameIndex.extensions, { GamesFolder.path }) {
    var gamesDir: String
        get() = GamesFolder.path
        set(value) { GamesFolder.path = value }
    private lateinit var app: Context
    private val copyExt = setOf("cdi", "chd", "zip", "7z", "dat", "lst", "bin")

    fun start(context: Context) {
        app = context.applicationContext
        super.init(app)
    }

    fun openForCore(path: String): OpenDisc? {
        val ext = fileName(path).substringAfterLast('.', "").lowercase()
        if (ext == "chd") return open(path, stageCopy = true)
        val opened = open(path) ?: return null
        if (ext !in copyExt) return opened
        val src = File(opened.path)
        if (src.isFile && !Files.isSymbolicLink(src.toPath())) return opened
        val destDir = File(app.cacheDir, "disc-real").apply { mkdirs() }
        val dest = File(destDir, src.name)
        val copied = runCatching {
            src.inputStream().use { input -> dest.outputStream().use { output -> input.copyTo(output) } }
            dest.isFile && dest.length() > 0L
        }.getOrDefault(false)
        if (!copied) return opened
        opened.close()
        return OpenDisc(dest.absolutePath, emptyList())
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
