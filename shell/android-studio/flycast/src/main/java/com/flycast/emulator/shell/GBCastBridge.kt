package com.flycast.emulator.shell

import android.content.Context
import android.content.Intent
import com.flycast.emulator.Emulator
import com.flycast.emulator.NativeGLActivity
import com.flycast.emulator.emu.HttpClient
import com.flycast.emulator.emu.JNIdc
import gb.frame.Covers
import gb.frame.DeviceTier
import gb.frame.HomeTheme
import gb.frame.LibraryView
import gb.frame.OpenDisc
import gb.frame.PlayTime
import gb.frame.RetroAchievements
import gb.frame.SaveData
import gb.frame.DataFile as File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Coordinates the Compose shell with the Flycast core. One staged disc stays open until the next launch. */
object GBCastBridge {
    const val AUTO_SAVE_SLOT = SaveData.MANUAL_SAVE_STATE_SLOTS
    const val EXTRA_PATH = "gbcast_game_path"
    const val EXTRA_SLOT = "gbcast_state_slot"
    const val EXTRA_LOAD = "gbcast_load_state"
    const val EXTRA_RESTORE_AUTO_LOAD = "gbcast_restore_auto_load"
    const val EXTRA_AUTO_LOAD_VALUE = "gbcast_auto_load_value"

    private const val PREF_GAMES = "gbcast.gamesDir"
    private const val PREF_SETUP = "gbcast.setupDone"
    private var sharedRaInitialized = false
    private lateinit var statePreferences: android.content.SharedPreferences
    lateinit var dataFolder: gb.frame.AppDataFolder
        private set

    @JvmStatic
    fun prepareDataFolder(context: Context, home: String) {
        if (::dataFolder.isInitialized) return
        statePreferences = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
        dataFolder = gb.frame.AppDataFolder(context, "GBCast", mapOf(
            "bios" to File(home, "data/bios"),
            "saves" to File(home, "data/saves"),
            "savestates" to File(home, "data/savestates"),
            "textures" to File(home, "data/textures"),
            "data" to File(home, "data"),
            "covers" to File(context.filesDir, "gbcast/covers"),
        ), texturesRelativePath = "textures")
        System.loadLibrary("flycast")
        dataFolder.openAtStartup()
        File(home, "data/textures").mkdirs()
        val old = File(home, "data")
        for (file in old.listFiles().orEmpty().filter { it.isFile }) {
            val name = file.name
            val destination = when {
                name.endsWith("_nvmem.bin") -> File(old, "saves/$name")
                name in DreamcastBios.sizes || name == "dc_boot.bin.bak" || name.startsWith("naomi") && (name.endsWith(".zip") || name.endsWith(".bin")) ||
                    name.endsWith("bios.zip") -> File(old, "bios/$name")
                name.startsWith("vmu_save_") || name.contains("_vmu_save_") -> File(old, "saves/$name")
                else -> null
            }
            if (destination != null) gb.frame.DataLayout.move(file, destination)
        }
    }


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
        Thread({ DeviceTier.detect() }, "gbcast-device-tier").start()
        Covers.init(File(app.filesDir, "gbcast"), "Sega - Dreamcast", frameAspectRatio = 1.0f)
        app.ensureCore()?.let { error -> throw IllegalStateException(error) }
        JNIdc.initAutoSkip()
        com.flycast.emulator.shell.ui.GBCastOverlayOptions.initialize(app)
        com.flycast.emulator.shell.ui.GBCastCheatDatabase.initialize(app)
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

    suspend fun applyRecommendedPreset(): String {
        val device = withContext(Dispatchers.IO) { DeviceTier.detect() }
        val preset = when (device.tier) {
            DeviceTier.Tier.Entry, DeviceTier.Tier.Low -> "Compatible"
            DeviceTier.Tier.Mid -> "Balanced"
            DeviceTier.Tier.High -> "Quality"
        }
        when (preset) {
            "Compatible" -> {
                setInt("config", "pvr.rend", 0)
                setInt("config", "rend.Resolution", 480)
            }
            "Quality" -> {
                setInt("config", "pvr.rend", 4)
                setInt("config", "rend.Resolution", device.gpuMaxScale.coerceIn(2, 4) * 480)
            }
            else -> {
                setInt("config", "pvr.rend", 0)
                setInt("config", "rend.Resolution", 960)
                setBool("config", "rend.vsync", true)
                setBool("config", "rend.FramePacing", true)
            }
        }
        setBool("config", "Dynarec.Enabled", true)
        return "$preset · ${device.summary}"
    }

    fun deviceRecommendation(): String {
        val device = DeviceTier.detect()
        val preset = when (device.tier) {
            DeviceTier.Tier.Entry, DeviceTier.Tier.Low -> "Compatible"
            DeviceTier.Tier.Mid -> "Balanced"
            DeviceTier.Tier.High -> "Quality"
        }
        return "$preset · ${device.summary}"
    }

    fun bool(section: String, name: String) = JNIdc.configGetBool(section, name)

    fun setBool(section: String, name: String, value: Boolean): Boolean = JNIdc.configSetBool(section, name, value)

    fun int(section: String, name: String) = JNIdc.configGetInt(section, name)

    fun setInt(section: String, name: String, value: Int): Boolean = JNIdc.configSetInt(section, name, value)

    fun dataDir(): File = File(JNIdc.dataDirectory() ?: "")

    fun biosDir(): File = File(dataDir(), "bios")
    fun biosPresent(): Boolean = DreamcastBios.present(biosDir(), "dc_boot.bin")

    /** Opens [safPath] and starts [NativeGLActivity]. [slot] loads that Flycast state after boot when auto-load is off. */
    suspend fun launch(context: Context, safPath: String, slot: Int = -1, fresh: Boolean = false): String? {
        if (slot != -1 && slot !in 0..AUTO_SAVE_SLOT) {
            return "Save slot must be between 1 and ${SaveData.MANUAL_SAVE_STATE_SLOTS}."
        }
        val opened = withContext(Dispatchers.IO) { Storage.openForCore(safPath) }
            ?: return "Could not open this game. Check the folder permission and the disc files."
        migrateStates(safPath)
        disc?.close()
        disc = opened
        PlayTime.update(safPath, true)
        val auto = bool("config", "Dreamcast.AutoLoadState")
        val launchSlot = when {
            slot >= 0 -> slot
            auto && !fresh -> latestStateSlot(safPath) ?: -1
            else -> -1
        }
        // The native boot reloads per-game slot settings. Load the chosen latest slot explicitly
        // after that reload, rather than letting boot load a stale numbered slot.
        val restoreAutoLoad = auto
        if (restoreAutoLoad) setBool("config", "Dreamcast.AutoLoadState", false)
        val intent = Intent(context, NativeGLActivity::class.java)
            .putExtra(EXTRA_PATH, opened.path)
            .putExtra(EXTRA_SLOT, launchSlot)
            .putExtra(EXTRA_LOAD, launchSlot >= 0)
            .putExtra(EXTRA_RESTORE_AUTO_LOAD, restoreAutoLoad)
            .putExtra(EXTRA_AUTO_LOAD_VALUE, auto)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
            LibraryView.gameOpened(safPath)
        } catch (_: Exception) {
            if (restoreAutoLoad) setBool("config", "Dreamcast.AutoLoadState", auto)
            opened.close()
            disc = null
            return "Could not start this game."
        }
        return null
    }

    private fun stateLabel(slot: Int) = if (slot == AUTO_SAVE_SLOT) "auto" else (slot + 1).toString()
    private fun stateFile(game: String, slot: Int) = gb.frame.DataLayout.stateFile(dataDir(), game, stateLabel(slot))
    private fun migrateStates(game: String) {
        val base = Storage.fileName(game).substringBeforeLast('.')
        val mover = gb.frame.SaveStateFiles(dataDir(), statePreferences)
        for (slot in 0..AUTO_SAVE_SLOT) {
            val suffix = if (slot == 0) "" else "_$slot"
            mover.moveFamily(File(dataDir(), "$base$suffix.state"), stateFile(game, slot))
        }
    }

    fun states(games: List<gb.frame.Game>): List<SaveSlot> {
        val legacy = dataDir().listFiles().orEmpty().filter { it.isFile && it.name.endsWith(".state") }.map { it.name }.toSet()
        for (game in games) {
            val base = Storage.fileName(game.path).substringBeforeLast('.')
            if ((0..AUTO_SAVE_SLOT).any { "$base${if (it == 0) "" else "_$it"}.state" in legacy }) migrateStates(game.path)
        }
        val byName = games.associateBy { SaveData.gameFolderName(it.path) }
        val out = mutableListOf<SaveSlot>()
        for (dir in File(dataDir(), "savestates").listFiles().orEmpty().filter { it.isDirectory }) {
            val game = byName[dir.name] ?: continue
            for (file in dir.listFiles().orEmpty().filter { it.isFile && it.name.endsWith(".state") }) {
                val label = file.name.removePrefix("${dir.name}_").removeSuffix(".state")
                val slot = if (label == "auto") AUTO_SAVE_SLOT else (label.toIntOrNull() ?: continue) - 1
                if (slot in 0..AUTO_SAVE_SLOT) out += SaveSlot(game.path, game.title, slot, file.lastModified())
            }
        }
        return out.sortedByDescending { it.time }
    }

    data class SaveSlot(val gamePath: String, val title: String, val slot: Int, val time: Long)

    private fun latestStateSlot(gamePath: String): Int? = (0..AUTO_SAVE_SLOT).mapNotNull { slot ->
        stateFile(gamePath, slot).takeIf { it.isFile }?.let { slot to it.lastModified() }
    }.maxByOrNull { it.second }?.first

}



