package com.flycast.emulator.shell

import android.content.Context
import android.content.Intent
import com.flycast.emulator.Emulator
import com.flycast.emulator.NativeGLActivity
import com.flycast.emulator.emu.HttpClient
import com.flycast.emulator.emu.JNIdc
import gb.frame.Covers
import gb.frame.HomeTheme
import gb.frame.LibraryView
import gb.frame.OpenDisc
import gb.frame.PlayTime
import gb.frame.RetroAchievements
import gb.frame.SaveData
import java.io.File

/** Coordinates the Compose shell with the Flycast core. One staged disc stays open until the next launch. */
object FlycastBridge {
    const val EXTRA_PATH = "gbcast_game_path"
    const val EXTRA_SLOT = "gbcast_state_slot"
    const val EXTRA_LOAD = "gbcast_load_state"
    const val EXTRA_RESTORE_AUTO_LOAD = "gbcast_restore_auto_load"
    const val EXTRA_AUTO_LOAD_VALUE = "gbcast_auto_load_value"

    private const val PREF_GAMES = "gbcast.gamesDir"
    private const val PREF_SETUP = "gbcast.setupDone"
    private var sharedRaInitialized = false

    var disc: OpenDisc? = null
        private set

    fun start(context: Context) {
        val app = context.applicationContext as Emulator
        val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(app)
        Storage.gamesDir = prefs.getString(PREF_GAMES, "") ?: ""
        Storage.start(app)
        HomeTheme.init(prefs)
        LibraryView.init(prefs)
        PlayTime.init(prefs)
        Covers.init(File(app.filesDir, "gbcast"), "Sega - Dreamcast", frameAspectRatio = 1.0f)
        app.ensureCore()?.let { error -> throw IllegalStateException(error) }
        val savedSlot = int("config", "Dreamcast.SavestateSlot")
        if (savedSlot !in 0 until SaveData.MANUAL_SAVE_STATE_SLOTS) {
            setInt("config", "Dreamcast.SavestateSlot", 0)
        }
        HttpClient.initialize()
        if (!sharedRaInitialized) {
            RetroAchievements.init(app, "GBCast", appVersion(app), com.flycast.emulator.shell.ui.GBCastRaBackend)
            sharedRaInitialized = true
        }
    }

    @Suppress("DEPRECATION")
    private fun appVersion(context: Context): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "?"

    fun gamesDir(): String = Storage.gamesDir

    fun setGamesDir(context: Context, uri: String) {
        Storage.gamesDir = uri
        android.preference.PreferenceManager.getDefaultSharedPreferences(context)
            .edit().putString(PREF_GAMES, uri).apply()
    }

    fun setupDone(context: Context) =
        android.preference.PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_SETUP, false)

    fun finishSetup(context: Context) {
        android.preference.PreferenceManager.getDefaultSharedPreferences(context)
            .edit().putBoolean(PREF_SETUP, true).apply()
    }

    fun bool(section: String, name: String) = JNIdc.configGetBool(section, name)

    fun setBool(section: String, name: String, value: Boolean): Boolean = JNIdc.configSetBool(section, name, value)

    fun int(section: String, name: String) = JNIdc.configGetInt(section, name)

    fun setInt(section: String, name: String, value: Int): Boolean = JNIdc.configSetInt(section, name, value)

    fun dataDir(): File = File(JNIdc.dataDirectory() ?: "")

    fun biosPresent(): Boolean = File(dataDir(), "dc_boot.bin").isFile || File(dataDir(), "dc_boot.bin.bak").isFile

    /** Opens [safPath] and starts [NativeGLActivity]. [slot] loads that Flycast state after boot when auto-load is off. */
    fun launch(context: Context, safPath: String, slot: Int = -1, fresh: Boolean = false): String? {
        if (slot != -1 && slot !in 0 until SaveData.MANUAL_SAVE_STATE_SLOTS) {
            return "Save slot must be between 1 and ${SaveData.MANUAL_SAVE_STATE_SLOTS}."
        }
        val opened = Storage.openForCore(safPath) ?: return "Could not open this game. Check the folder permission and the disc files."
        disc?.close()
        disc = opened
        PlayTime.update(safPath, true)
        if (slot >= 0) setInt("config", "Dreamcast.SavestateSlot", slot)
        val auto = bool("config", "Dreamcast.AutoLoadState")
        val restoreAutoLoad = fresh && auto
        if (restoreAutoLoad) setBool("config", "Dreamcast.AutoLoadState", false)
        val intent = Intent(context, NativeGLActivity::class.java)
            .putExtra(EXTRA_PATH, opened.path)
            .putExtra(EXTRA_SLOT, slot)
            .putExtra(EXTRA_LOAD, slot >= 0 && !auto)
            .putExtra(EXTRA_RESTORE_AUTO_LOAD, restoreAutoLoad)
            .putExtra(EXTRA_AUTO_LOAD_VALUE, auto)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            if (restoreAutoLoad) setBool("config", "Dreamcast.AutoLoadState", auto)
            opened.close()
            disc = null
            return "Could not start this game."
        }
        return null
    }

    fun states(games: List<gb.frame.Game>): List<SaveSlot> {
        val dir = dataDir()
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".state") } ?: return emptyList()
        val byName = games.associateBy { Storage.fileName(it.path).substringBeforeLast('.') }
        return files.mapNotNull { file ->
            val match = Regex("^(.*?)(?:_(\\d+))?\\.state$").matchEntire(file.name) ?: return@mapNotNull null
            val base = match.groupValues[1]
            val slot = match.groupValues[2].toIntOrNull() ?: 0
            if (slot !in 0 until SaveData.MANUAL_SAVE_STATE_SLOTS) return@mapNotNull null
            val game = byName[base] ?: return@mapNotNull null
            SaveSlot(game.path, game.title, slot, file.lastModified())
        }.sortedByDescending { it.time }
    }

    data class SaveSlot(val gamePath: String, val title: String, val slot: Int, val time: Long)
}

