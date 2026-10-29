package com.flycast.emulator.shell.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.gbcast.emulator.R
import com.flycast.emulator.shell.GBCastBridge
import com.flycast.emulator.shell.DreamcastBios
import com.flycast.emulator.shell.DisplayMode
import com.flycast.emulator.shell.Storage
import gb.frame.AboutCredit
import gb.frame.AboutPage
import gb.frame.rememberAppIconPainter
import gb.frame.ActionRow
import gb.frame.ChoiceRow
import gb.frame.ControlsPage
import gb.frame.ControlsCapturePrompts
import gb.frame.InfoRow
import gb.frame.SettingsDivider
import gb.frame.SettingsGroup
import gb.frame.ToggleRow
import gb.frame.SystemSettings
import gb.frame.SystemFolderSetting
import gb.frame.SystemBiosFolderSetting
import gb.frame.SystemBiosFile
import gb.frame.SystemStartupOptions
import gb.frame.SystemToggleSetting
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

enum class SettingsPage(val label: String, val icon: ImageVector) {
    System("System", Icons.Default.Tune),
    Video("Video", Icons.Default.Tv),
    Audio("Audio", Icons.AutoMirrored.Filled.VolumeUp),
    Performance("Performance", Icons.Default.Speed),
    Controls("Controls", Icons.Default.SportsEsports),
    Extras("Extras", Icons.Default.Info),
    About("About", Icons.Default.Info),
}

private class Cfg {
    var tick by mutableIntStateOf(0)
    fun bool(section: String, name: String) = GBCastBridge.bool(section, name)
    fun toggle(section: String, name: String) {
        GBCastBridge.setBool(section, name, !bool(section, name))
        tick++
    }
    fun setBool(section: String, name: String, value: Boolean) {
        check(GBCastBridge.setBool(section, name, value)) { "Unsupported setting: $name" }
        tick++
    }
    fun int(section: String, name: String) = GBCastBridge.int(section, name)
    fun setInt(section: String, name: String, value: Int) {
        GBCastBridge.setInt(section, name, value)
        tick++
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, toast: (String) -> Unit, page: SettingsPage?) {
    val ctx = LocalContext.current
    val current = page ?: SettingsPage.System
    val cfg = remember { Cfg() }
    val tick = cfg.tick
    val scope = rememberCoroutineScope()
    var importingBios by remember { mutableStateOf(false) }
    val backFocus = remember { FocusRequester() }
    val view = LocalView.current
    LaunchedEffect(current) {
        repeat(8) {
            view.requestFocusFromTouch()
            if (runCatching { backFocus.requestFocus() }.isSuccess) return@LaunchedEffect
            delay(50)
        }
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            Storage.takeFolder(uri)
            GBCastBridge.setGamesDir(ctx, uri.toString())
            toast("Games folder saved")
        }
    }
    val biosPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null && !importingBios) scope.launch {
            importingBios = true
            try {
                val count = withContext(Dispatchers.IO) { Storage.importBiosFolder(uri, GBCastBridge.biosDir()) }
                cfg.tick++
                toast(if (count == 0) "No files found in that folder" else "Copied $count files into the GBCast data folder")
            } catch (error: Exception) {
                cfg.tick++
                toast("Import stopped: ${error.message}. Files already copied were kept.")
            } finally { importingBios = false }
        }
    }
    val dreamcastBiosPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty() && !importingBios) scope.launch {
            importingBios = true
            try {
                val results = withContext(Dispatchers.IO) {
                    uris.map { uri ->
                        runCatching {
                            val name = ctx.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                                if (it.moveToFirst()) it.getString(0) else null
                            } ?: error("Could not read filename")
                            requireNotNull(ctx.contentResolver.openInputStream(uri)).use { input ->
                                DreamcastBios.install(GBCastBridge.biosDir(), name, input)
                            }
                        }.fold({ "Imported $it" }, { it.message ?: "Could not import BIOS" })
                    }
                }
                cfg.tick++
                toast(results.joinToString("\n"))
            } finally { importingBios = false }
        }
    }

    Box(Modifier.fillMaxSize()) {
      Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(current.icon, null, tint = gb.frame.palette.accent)
                    Spacer(Modifier.width(12.dp))
                    Text(current.label, fontWeight = FontWeight.Bold)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.focusRequester(backFocus)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 760.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (current) {
                    SettingsPage.System -> SystemPage(tick, cfg, { folderPicker.launch(null) }, { if (!importingBios) biosPicker.launch(null) }, importingBios) {
                        if (!importingBios) dreamcastBiosPicker.launch(arrayOf("*/*"))
                    }
                    SettingsPage.Video -> VideoPage(tick, cfg)
                    SettingsPage.Audio -> AudioPage(tick, cfg)
                    SettingsPage.Performance -> PerformancePage(tick, cfg)
                    SettingsPage.Controls -> ControlsPage(DreamcastControls) {
                        val devices = intArrayOf(0, 18, 5, 6, 7, 8, 10)
                        val selected = indexOf(tick, devices, cfg.int("input", "device1"))
                        val vibeValues = intArrayOf(0, 20, 50, 80)
                        val vibe = indexOf(tick, vibeValues, cfg.int("input", "VirtualGamepadVibration"))
                        ChoiceRow("Port A", "Controller type for the first Dreamcast port", listOf("Pad", "XL", "Key", "Mouse", "Gun", "Twin", "None"), selected) {
                            cfg.setInt("input", "device1", devices[it])
                        }
                        SettingsDivider()
                        ChoiceRow("Touch vibration", "Vibration strength for the on-screen controls", listOf("Off", "Low", "Mid", "High"), vibe) {
                            cfg.setInt("input", "VirtualGamepadVibration", vibeValues[it])
                        }
                    }
                    SettingsPage.Extras -> ExtrasPage(tick, cfg)
                    SettingsPage.About -> AboutPage(
                        appName = "GBCast",
                        manufacturer = "Sega",
                        console = "Dreamcast",
                        icon = rememberAppIconPainter(R.drawable.ic_launcher_foreground),
                        credits = listOf(
                            AboutCredit("Flycast", "Emulation core for Dreamcast, Naomi, Naomi 2 and Atomiswave."),
                            AboutCredit("GBFrame", "Shared Android interface, settings, controls and menu components."),
                            AboutCredit("libretro-database", "Offline Dreamcast cheat database by its contributors (CC BY-SA 4.0). License included in the APK assets."),
                            AboutCredit("GBCast", "Android frontend and integration with Flycast."),
                        ),
                        license = "See the GBCast repository LICENSE and Flycast's upstream license notices for the full terms that apply to each component.",
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
      }
      ControlsCapturePrompts(DreamcastControls)
    }
}

@Composable
private fun SystemPage(tick: Int, cfg: Cfg, onFolder: () -> Unit, onBios: () -> Unit, importingBios: Boolean, onDreamcastBios: () -> Unit) {
    val biosFiles = remember(tick) {
        DreamcastBios.sizes.keys.map { name ->
            val present = DreamcastBios.present(GBCastBridge.biosDir(), name)
            val actualName = if (name == "dc_boot.bin" && present && !gb.frame.DataFile(GBCastBridge.biosDir(), name).isFile) "$name.bak" else name
            SystemBiosFile(actualName, present)
        }
    }
    SystemSettings(
        dataFolder = GBCastBridge.dataFolder,
        gamesFolder = SystemFolderSetting(GBCastBridge.gamesDir().takeIf { it.isNotBlank() }?.let(Storage::folderLabel),
            "Choose the folder containing your Dreamcast and arcade disc images.", onFolder),
        biosFolder = SystemBiosFolderSetting(
            path = if (GBCastBridge.dataFolder.folder != null || biosFiles.any { it.loaded }) GBCastBridge.dataFolder.displayPath(GBCastBridge.biosDir()) else null,
            description = "Choose a folder containing dc_boot.bin, dc_flash.bin and any other required BIOS files.",
            files = biosFiles, onChooseFolder = onBios, onChooseFiles = onDreamcastBios, busy = importingBios,
        ),
        autoSave = cfg.bool("config", "Dreamcast.AutoSaveState"),
        autoLoad = cfg.bool("config", "Dreamcast.AutoLoadState"),
        onAutoSave = { cfg.setBool("config", "Dreamcast.AutoSaveState", it) },
        onAutoLoad = { cfg.setBool("config", "Dreamcast.AutoLoadState", it) },
        hleBios = {
            ToggleRow(Icons.Default.Tune, "HLE BIOS", "Use reios instead of dc_boot.bin when a game allows it.", cfg.bool("config", "UseReios")) {
                cfg.toggle("config", "UseReios")
            }
        },
        startup = SystemStartupOptions(
            fastBoot = SystemToggleSetting(cfg.bool("config", "GBFrame.FastBoot"), { cfg.setBool("config", "GBFrame.FastBoot", it) }, "Skip the BIOS intro and its clock and language screens"),
            skipLoadingScreens = SystemToggleSetting(cfg.bool("config", "GBFrame.SkipLoadingScreens"), { cfg.setBool("config", "GBFrame.SkipLoadingScreens", it) }),
            skipTransitions = SystemToggleSetting(cfg.bool("config", "GBFrame.SkipTransitions"), { cfg.setBool("config", "GBFrame.SkipTransitions", it) }),
            skipSaveWaits = SystemToggleSetting(cfg.bool("config", "GBFrame.SkipSaveWaits"), { cfg.setBool("config", "GBFrame.SkipSaveWaits", it) }),
        ),
        startupExtras = {
            ToggleRow(Icons.Default.Speed, "Fast GD-ROM load", "Reduce emulated GD-ROM transfer delays. Independent of the automatic skip options; some games require original timing.", cfg.bool("config", "FastGDRomLoad")) {
                cfg.toggle("config", "FastGDRomLoad")
            }
        },
        extraSettings = {
            val region = indexOf(tick, intArrayOf(0, 1, 2, 3), cfg.int("config", "Dreamcast.Region"))
            val cable = indexOf(tick, intArrayOf(0, 2, 3), cfg.int("config", "Dreamcast.Cable"))
            val broadcast = indexOf(tick, intArrayOf(0, 1, 2, 3, 4), cfg.int("config", "Dreamcast.Broadcast"))
            SettingsGroup("Dreamcast", Icons.Default.Tune) {
                ChoiceRow("Region", "Used the next time a game boots", listOf("Japan", "USA", "Europe", "Default"), region) {
                    cfg.setInt("config", "Dreamcast.Region", intArrayOf(0, 1, 2, 3)[it])
                }
                SettingsDivider()
                ChoiceRow("Cable", "VGA, RGB or composite", listOf("VGA", "RGB", "TV"), cable) {
                    cfg.setInt("config", "Dreamcast.Cable", intArrayOf(0, 2, 3)[it])
                }
                SettingsDivider()
                ChoiceRow("Broadcast", null, listOf("NTSC", "PAL", "PAL-M", "PAL-N", "Default"), broadcast) {
                    cfg.setInt("config", "Dreamcast.Broadcast", it)
                }
                SettingsDivider()
                ToggleRow(Icons.Default.Memory, "32 MB RAM", "Dreamcast RAM mod. Arcade boards ignore this.", cfg.bool("config", "Dreamcast.RamMod32MB")) {
                    cfg.toggle("config", "Dreamcast.RamMod32MB")
                }
            }
        },
    )
}

@Composable
private fun VideoPage(tick: Int, cfg: Cfg) {
    val rendererValues = intArrayOf(0, 3, 4, 5)
    val renderer = indexOf(tick, rendererValues, cfg.int("config", "pvr.rend"))
    val resolutionValues = intArrayOf(480, 960, 1440, 1920)
    val resolution = indexOf(tick, resolutionValues, cfg.int("config", "rend.Resolution"))
    SettingsGroup("Rendering", Icons.Default.Tv) {
        ChoiceRow("Renderer", "OpenGL ES or Vulkan. Per-pixel uses order-independent transparency.", listOf("GLES", "GLES OIT", "Vulkan", "Vulkan OIT"), renderer) {
            cfg.setInt("config", "pvr.rend", rendererValues[it])
        }
        SettingsDivider()
        ChoiceRow("Internal resolution", "480 is native. Applied by the GBCast renderer.", listOf("Native", "x2", "x3", "x4"), resolution) {
            cfg.setInt("config", "rend.Resolution", resolutionValues[it])
        }
        SettingsDivider()
        ChoiceRow("Display mode", "Fullscreen stretches the picture; Super widescreen expands the view to the screen ratio.",
            DisplayMode.entries.map { it.label }, GBCastBridge.globalDisplay().ordinal) { selected ->
            DisplayMode.entries[selected].apply(cfg::setBool)
            GBCastBridge.setGlobalDisplay(DisplayMode.entries[selected])
            cfg.setInt("config", "rend.ScreenStretching", 100)
        }
        SettingsDivider()
        gb.frame.WidescreenGameHacksRow(cfg.bool("config", "rend.WidescreenGameHacks")) { cfg.toggle("config", "rend.WidescreenGameHacks") }
        SettingsDivider()
        ToggleRow(Icons.Default.Tv, "VSync", "rend.vsync", cfg.bool("config", "rend.vsync")) { cfg.toggle("config", "rend.vsync") }
        SettingsDivider()
        ToggleRow(Icons.Default.Tv, "Integer scale", "rend.IntegerScale", cfg.bool("config", "rend.IntegerScale")) { cfg.toggle("config", "rend.IntegerScale") }
        SettingsDivider()
        ChoiceRow("Texture filtering", null, listOf("Default", "Nearest", "Linear"), cfg.int("config", "rend.TextureFiltering").coerceIn(0, 2)) {
            cfg.setInt("config", "rend.TextureFiltering", it)
        }
        SettingsDivider()
        ChoiceRow("Anisotropic", null, listOf("Off", "2x", "4x", "8x", "16x"), indexOf(tick, intArrayOf(1, 2, 4, 8, 16), cfg.int("config", "rend.AnisotropicFiltering"))) {
            cfg.setInt("config", "rend.AnisotropicFiltering", intArrayOf(1, 2, 4, 8, 16)[it])
        }
    }
    SettingsGroup("Overlays", Icons.Default.Info) {
        gb.frame.MonitoringSettingsRows(GBCastOverlayOptions)
    }
    SettingsGroup("Textures", Icons.Default.Tv) {
        ToggleRow(Icons.Default.Tv, "Custom textures", "Load texture packs from data/textures/<game ID> in the selected data folder after reopening the app.",
            cfg.bool("config", "rend.CustomTextures")) { cfg.setBool("config", "rend.CustomTextures", it) }
        SettingsDivider()
        gb.frame.TexturePackImportRow(GBCastBridge.texturesFolder())
    }
    SettingsGroup("Accuracy", null) {
        ToggleRow(Icons.Default.Tune, "Per-strip sorting", "Off uses per-triangle sorting.", cfg.bool("config", "rend.PerStripSorting")) { cfg.toggle("config", "rend.PerStripSorting") }
        SettingsDivider()
        ToggleRow(Icons.Default.Tune, "Fog", null, cfg.bool("config", "rend.Fog")) { cfg.toggle("config", "rend.Fog") }
        SettingsDivider()
        ToggleRow(Icons.Default.Tune, "Mipmaps", null, cfg.bool("config", "rend.UseMipmaps")) { cfg.toggle("config", "rend.UseMipmaps") }
        SettingsDivider()
        ToggleRow(Icons.Default.Tune, "Delay frame swapping", null, cfg.bool("config", "rend.DelayFrameSwapping")) { cfg.toggle("config", "rend.DelayFrameSwapping") }
        SettingsDivider()
        ToggleRow(Icons.Default.Tune, "Emulate framebuffer", null, cfg.bool("config", "rend.EmulateFramebuffer")) { cfg.toggle("config", "rend.EmulateFramebuffer") }
        SettingsDivider()
        ToggleRow(Icons.Default.Tv, "Rotate 90°", "rend.Rotate90", cfg.bool("config", "rend.Rotate90")) { cfg.toggle("config", "rend.Rotate90") }
    }
}

@Composable
private fun AudioPage(tick: Int, cfg: Cfg) {
    val samples = cfg.int("config", "aica.BufferSize")
    val latency = if (samples == Int.MIN_VALUE) 2 else listOf(64, 96, 128).indices.minByOrNull { kotlin.math.abs(listOf(64, 96, 128)[it] - samples * 1000 / 44100) } ?: 2
    // The kit's standard options first, then Flycast's own
    gb.frame.AudioSettingsGroups(GBCastAudio)
    SettingsGroup("Sound chip (AICA)", Icons.AutoMirrored.Filled.VolumeUp) {
        ToggleRow(Icons.AutoMirrored.Filled.VolumeUp, "DSP", "aica.DSPEnabled", cfg.bool("config", "aica.DSPEnabled")) { cfg.toggle("config", "aica.DSPEnabled") }
        SettingsDivider()
        ToggleRow(Icons.AutoMirrored.Filled.VolumeUp, "Auto latency", "aica.AutoLatency", cfg.bool("config", "aica.AutoLatency")) { cfg.toggle("config", "aica.AutoLatency") }
        SettingsDivider()
        ChoiceRow("Buffer", "Approximate latency. Stored as aica.BufferSize.", listOf("64 ms", "96 ms", "128 ms"), latency) {
            cfg.setInt("config", "aica.BufferSize", listOf(64, 96, 128)[it] * 44100 / 1000)
        }
        SettingsDivider()
        ToggleRow(Icons.AutoMirrored.Filled.VolumeUp, "VMU sound", null, cfg.bool("audio", "VmuSound")) { cfg.toggle("audio", "VmuSound") }
    }
}

@Composable
private fun PerformancePage(tick: Int, cfg: Cfg) {
    SettingsGroup("Performance", Icons.Default.Speed) {
        InfoRow("Recommended preset", GBCastBridge.deviceRecommendation())
        SettingsDivider()
        ToggleRow(Icons.Default.Speed, "Dynarec", "Dynarec.Enabled", cfg.bool("config", "Dynarec.Enabled")) { cfg.toggle("config", "Dynarec.Enabled") }
        SettingsDivider()
        ToggleRow(Icons.Default.Speed, "Threaded rendering", null, cfg.bool("config", "rend.ThreadedRendering")) { cfg.toggle("config", "rend.ThreadedRendering") }
        SettingsDivider()
        ToggleRow(Icons.Default.Speed, "Frame pacing", null, cfg.bool("config", "rend.FramePacing")) { cfg.toggle("config", "rend.FramePacing") }
        SettingsDivider()
        ChoiceRow("Auto frame skip", "pvr.AutoSkipFrame", listOf("Off", "Normal", "Max"), cfg.int("config", "pvr.AutoSkipFrame").coerceIn(0, 2)) {
            cfg.setInt("config", "pvr.AutoSkipFrame", it)
        }
    }
}


@Composable
private fun ExtrasPage(tick: Int, cfg: Cfg) {
    SettingsGroup("Arcade", null) {
        ToggleRow(Icons.Default.Tune, "Force free play", "Naomi/Atomiswave", cfg.bool("config", "ForceFreePlay")) { cfg.toggle("config", "ForceFreePlay") }
    }
    SettingsGroup("Network and achievements", Icons.Default.Wifi) {
        ToggleRow(Icons.Default.Wifi, "Broadband adapter", "EmulateBBA", cfg.bool("network", "EmulateBBA")) { cfg.toggle("network", "EmulateBBA") }
        SettingsDivider()
        ToggleRow(Icons.Default.Wifi, "Naomi network", "network.Enable", cfg.bool("network", "Enable")) { cfg.toggle("network", "Enable") }
        SettingsDivider()
        ToggleRow(Icons.Default.Wifi, "Battle cable", null, cfg.bool("network", "BattleCable")) { cfg.toggle("network", "BattleCable") }
        SettingsDivider()
        ToggleRow(Icons.Default.Wifi, "GGPO", "Rollback netplay. Host, delay and chat options are in the advanced network screen.", cfg.bool("network", "GGPO")) { cfg.toggle("network", "GGPO") }
        SettingsDivider()
        ToggleRow(Icons.Default.Info, "Upload crash logs", null, cfg.bool("config", "UploadCrashLogs")) { cfg.toggle("config", "UploadCrashLogs") }
    }
    gb.frame.RetroAchievementsSettings("GBCast")
}

/** Find a native enum value in the labels list, falling back to the first available option. */
private fun indexOf(@Suppress("UNUSED_PARAMETER") tick: Int, values: IntArray, value: Int): Int =
    values.indexOf(value).takeIf { it >= 0 } ?: 0

