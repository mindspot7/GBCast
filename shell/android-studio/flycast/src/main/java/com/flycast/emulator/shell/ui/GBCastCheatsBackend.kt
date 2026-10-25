package com.flycast.emulator.shell.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.flycast.emulator.emu.JNIdc
import gb.frame.CheatActionResult
import gb.frame.CheatUiItem
import gb.frame.CheatsBackend

/**
 * Flycast adapter for GBFrame's common Cheats screen.
 *
 * Flycast parses GameShark codes natively and owns the active game's cheat list and persistence.
 * The shared screen owns all presentation and interaction; this adapter only translates operations.
 */
object GBCastCheatsBackend : CheatsBackend {
    private var revision by mutableIntStateOf(0)

    override val cheats: List<CheatUiItem>
        get() = revision.let {
            // Observe mutations so GBFrame recomposes its list after enable/add/delete/import.
            (0 until JNIdc.cheatCount()).mapNotNull { index ->
                if (JNIdc.cheatBuiltIn(index)) return@mapNotNull null
                CheatUiItem(
                    id = index.toString(),
                    name = JNIdc.cheatDescription(index).orEmpty().ifBlank { "Cheat ${index + 1}" },
                    code = "GameShark",
                    enabled = JNIdc.cheatEnabled(index),
                    sourceLabel = "Flycast",
                )
            }
        }

    override val databaseStatus: String
        get() = "Flycast GameShark codes for the current game. No external cheat database is configured."

    override val codeHint: String
        get() = "GameShark code (hex words separated by spaces)"

    override val blockedMessage: String?
        get() = if (JNIdc.configGetBool("achievements", "HardcoreMode")) {
            "Cheats are disabled while RetroAchievements hardcore mode is enabled."
        } else {
            null
        }

    override fun add(name: String, code: String): CheatActionResult {
        blockedMessage?.let { return CheatActionResult.failure(it) }
        if (code.isBlank()) return CheatActionResult.failure("Enter a GameShark code.")
        val error = runCatching {
            JNIdc.addGameSharkCheat(name.trim().ifBlank { "Cheat" }, code.trim())
        }.getOrElse { it.message ?: "Flycast could not parse that code." }
        if (error != null) return CheatActionResult.failure(error)
        revision++
        return CheatActionResult.success("Cheat added to Flycast.")
    }

    override fun setEnabled(id: String, enabled: Boolean) {
        if (blockedMessage != null) return
        val index = id.toIntOrNull() ?: return
        if (JNIdc.setCheatEnabled(index, enabled)) revision++
    }

    override fun delete(id: String) {
        if (blockedMessage != null) return
        val index = id.toIntOrNull() ?: return
        if (index !in 0 until JNIdc.cheatCount() || JNIdc.cheatBuiltIn(index)) return
        JNIdc.removeCheat(index)
        revision++
    }

    override fun importFile(contents: String): CheatActionResult {
        blockedMessage?.let { return CheatActionResult.failure(it) }
        val sourceLines = contents.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith(";") && !it.startsWith("[") }
            .toList()
        if (sourceLines.isEmpty()) return CheatActionResult.failure("No GameShark codes were found in that file.")
        if (sourceLines.any { it.startsWith("cheat", ignoreCase = true) && "=" in it }) {
            return CheatActionResult.failure("Flycast .cht configuration files are not text-code lists. Import a text file with one GameShark code per line; use Name=code to label entries.")
        }

        var imported = 0
        for ((lineIndex, line) in sourceLines.withIndex()) {
            val equals = line.indexOf('=')
            val named = equals > 0
            val name = if (named) line.substringBefore('=').trim() else "Imported cheat ${lineIndex + 1}"
            val code = if (named) line.substringAfter('=').trim() else line
            val result = add(name, code)
            if (!result.success) {
                return if (imported == 0) result else CheatActionResult.success(
                    "Imported ${imported} code(s); stopped at line ${lineIndex + 1}: ${result.message.orEmpty()}",
                )
            }
            imported++
        }
        return CheatActionResult.success("Imported ${imported} GameShark code(s).")
    }
}
