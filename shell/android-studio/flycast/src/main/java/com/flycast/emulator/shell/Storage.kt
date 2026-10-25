package com.flycast.emulator.shell

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import gb.frame.OpenDisc
import gb.frame.SafGames
import java.io.File
import java.nio.file.Files

/**
 * Games folder through the Storage Access Framework. Discs are copied to real cache files because Flycast reopens
 * tracks by path, and some handhelds refuse a symlink to /proc/self/fd.
 */
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
        val opened = open(path, stageCopy = ext == "chd") ?: return null
        return materialize(opened)
    }

    /**
     * Flycast reopens every track by path. A symlink to /proc/self/fd is refused on some handhelds, so the staged
     * descriptor and its sibling tracks are copied to ordinary cache files before the core sees them.
     */
    private fun materialize(opened: OpenDisc): OpenDisc? {
        val src = File(opened.path)
        val parent = src.parentFile
        if (!src.exists() || parent == null) return opened
        val files = parent.listFiles()?.filter { it.isFile || Files.isSymbolicLink(it.toPath()) }.orEmpty()
        if (files.none { Files.isSymbolicLink(it.toPath()) }) return opened
        val destDir = File(app.cacheDir, "disc-real").apply { deleteRecursively(); mkdirs() }
        val copied = runCatching {
            files.forEach { file ->
                val dest = File(destDir, file.name)
                file.inputStream().use { input -> dest.outputStream().use { output -> input.copyTo(output) } }
                if (!dest.isFile || dest.length() <= 0L) error("empty copy ${file.name}")
            }
            true
        }.getOrDefault(false)
        if (!copied) {
            opened.close()
            return null
        }
        val main = File(destDir, src.name)
        opened.close()
        return OpenDisc(main.absolutePath, emptyList())
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
