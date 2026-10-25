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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import com.flycast.emulator.shell.FlycastBridge
import com.flycast.emulator.shell.Storage
import gb.frame.AboutCredit
import gb.frame.AboutPage
import gb.frame.ActionRow
import gb.frame.ChoiceRow
import gb.frame.ControlsPage
import gb.frame.ControlsCapturePrompts
import gb.frame.InfoRow
import gb.frame.SettingsDivider
import gb.frame.SettingsGroup
import gb.frame.ToggleRow
import kotlinx.coroutines.delay
import java.io.File

enum class SettingsPage(val label: String, val icon: ImageVector) {
    System("System", Icons.Default.Tune),
    Video("Video", Icons.Default.Tv),
    Audio("Audio", Icons.AutoMirrored.Filled.VolumeUp),
    Performance("Performance", Icons.Default.Speed),
    Controls("Controls", Icons.Default.SportsEsports),
    Extras("Network", Icons.Default.Wifi),
    About("About", Icons.Default.Info),
}

private class Cfg {
    var tick by mutableIntStateOf(0)
    fun bool(section: String, name: String) = FlycastBridge.bool(section, name)
    fun toggle(section: String, name: String) {
        FlycastBridge.setBool(section, name, !bool(section, name))
        tick++
    }
    fun int(section: String, name: String) = FlycastBridge.int(section, name)
    fun setInt(section: String, name: String, value: Int) {
        FlycastBridge.setInt(section, name, value)
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
            FlycastBridge.setGamesDir(ctx, uri.toString())
            toast("Games folder saved")
        }
    }
    val biosPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val saved = Storage.importFile(uri, FlycastBridge.dataDir())
            toast(if (saved != null) "Copied ${File(saved).name} into the Flycast data folder" else "Could not copy that file")
        }
    }

    Box(Modifier.fillMaxSize()) {
      Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(current.icon, null, tint = Color(0xFFFFB15A))
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
                    SettingsPage.System -> SystemPage(tick, cfg, { folderPicker.launch(null) }, { biosPicker.launch(arrayOf("*/*")) })
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
                        icon = painterResource(R.drawable.ic_gbcast),
                        credits = listOf(
                            AboutCredit("Flycast", "Emulation core for Dreamcast, Naomi, Naomi 2 and Atomiswave."),
                            AboutCredit("GBFrame", "Shared Android interface, settings, controls and menu components."),
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
private fun SystemPage(tick: Int, cfg: Cfg, onFolder: () -> Unit, onBios: () -> Unit) {
    val dir = FlycastBridge.gamesDir()
    SettingsGroup("Paths", Icons.Default.Folder) {
        ActionRow(
            Icons.Default.Folder,
            "Games folder",
            if (dir.isBlank()) "Not set" else Storage.folderLabel(dir),
            "Choose",
            onFolder,
            warn = dir.isBlank(),
        )
        SettingsDivider()
        ActionRow(
            Icons.Default.Memory,
            "BIOS and data files",
            if (FlycastBridge.biosPresent()) "dc_boot.bin is in the data folder" else "Copy dc_boot.bin and dc_flash.bin into the data folder",
            "Copy file",
            onBios,
            warn = !FlycastBridge.biosPresent(),
        )
        SettingsDivider()
        InfoRow("Flycast data folder", FlycastBridge.dataDir().absolutePath)
    }
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
        SettingsDivider()
        ToggleRow(Icons.Default.Tune, "HLE BIOS", "Use reios instead of dc_boot.bin when a game allows it.", cfg.bool("config", "UseReios")) {
            cfg.toggle("config", "UseReios")
        }
    }
}

@Composable
private fun VideoPage(tick: Int, cfg: Cfg) {
    val rendererValues = intArrayOf(0, 3, 4, 5)
    val renderer = indexOf(tick, rendererValues, cfg.int("config", "pvr.rend"))
    val resolutionValues = intArrayOf(480, 960, 1440, 1920)
    val resolution = indexOf(tick, resolutionValues, cfg.int("config", "rend.Resolution"))
    SettingsGroup("Rendering", Icons.Default.Tv) {
        ChoiceRow("Renderer", "OpenGL ES or Vulkan. Per-pixel is Flycast order-independent transparency.", listOf("GLES", "GLES OIT", "Vulkan", "Vulkan OIT"), renderer) {
            cfg.setInt("config", "pvr.rend", rendererValues[it])
        }
        SettingsDivider()
        ChoiceRow("Internal resolution", "480 is native. Applied by the Flycast renderer.", listOf("Native", "x2", "x3", "x4"), resolution) {
            cfg.setInt("config", "rend.Resolution", resolutionValues[it])
        }
        SettingsDivider()
        ToggleRow(Icons.Default.Tv, "Widescreen", "rend.WideScreen", cfg.bool("config", "rend.WideScreen")) { cfg.toggle("config", "rend.WideScreen") }
        SettingsDivider()
        ToggleRow(Icons.Default.Tv, "Super widescreen", "rend.SuperWideScreen", cfg.bool("config", "rend.SuperWideScreen")) { cfg.toggle("config", "rend.SuperWideScreen") }
        SettingsDivider()
        ToggleRow(Icons.Default.Tune, "Widescreen game hacks", "rend.WidescreenGameHacks", cfg.bool("config", "rend.WidescreenGameHacks")) { cfg.toggle("config", "rend.WidescreenGameHacks") }
        SettingsDivider()
        ToggleRow(Icons.Default.Tv, "VSync", "rend.vsync", cfg.bool("config", "rend.vsync")) { cfg.toggle("config", "rend.vsync") }
        SettingsDivider()
        ToggleRow(Icons.Default.Tv, "Integer scale", "rend.IntegerScale", cfg.bool("config", "rend.IntegerScale")) { cfg.toggle("config", "rend.IntegerScale") }
        SettingsDivider()
        ToggleRow(Icons.Default.Info, "Show FPS", "rend.ShowFPS", cfg.bool("config", "rend.ShowFPS")) { cfg.toggle("config", "rend.ShowFPS") }
        SettingsDivider()
        ChoiceRow("Texture filtering", null, listOf("Default", "Nearest", "Linear"), cfg.int("config", "rend.TextureFiltering").coerceIn(0, 2)) {
            cfg.setInt("config", "rend.TextureFiltering", it)
        }
        SettingsDivider()
        ChoiceRow("Anisotropic", null, listOf("Off", "2x", "4x", "8x", "16x"), indexOf(tick, intArrayOf(1, 2, 4, 8, 16), cfg.int("config", "rend.AnisotropicFiltering"))) {
            cfg.setInt("config", "rend.AnisotropicFiltering", intArrayOf(1, 2, 4, 8, 16)[it])
        }
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
    SettingsGroup("Audio", Icons.AutoMirrored.Filled.VolumeUp) {
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
        ToggleRow(Icons.Default.Speed, "Dynarec", "Dynarec.Enabled", cfg.bool("config", "Dynarec.Enabled")) { cfg.toggle("config", "Dynarec.Enabled") }
        SettingsDivider()
        ToggleRow(Icons.Default.Speed, "Threaded rendering", null, cfg.bool("config", "rend.ThreadedRendering")) { cfg.toggle("config", "rend.ThreadedRendering") }
        SettingsDivider()
        ToggleRow(Icons.Default.Speed, "Frame pacing", null, cfg.bool("config", "rend.FramePacing")) { cfg.toggle("config", "rend.FramePacing") }
        SettingsDivider()
        ToggleRow(Icons.Default.Speed, "Fast GD-ROM load", null, cfg.bool("config", "FastGDRomLoad")) { cfg.toggle("config", "FastGDRomLoad") }
        SettingsDivider()
        ChoiceRow("Auto frame skip", "pvr.AutoSkipFrame", listOf("Off", "Normal", "Max"), cfg.int("config", "pvr.AutoSkipFrame").coerceIn(0, 2)) {
            cfg.setInt("config", "pvr.AutoSkipFrame", it)
        }
    }
}


@Composable
private fun ExtrasPage(tick: Int, cfg: Cfg) {
    SettingsGroup("Saves", null) {
        ToggleRow(Icons.Default.Memory, "Auto load state", "Dreamcast.AutoLoadState", cfg.bool("config", "Dreamcast.AutoLoadState")) { cfg.toggle("config", "Dreamcast.AutoLoadState") }
        SettingsDivider()
        ToggleRow(Icons.Default.Memory, "Auto save state", "Dreamcast.AutoSaveState", cfg.bool("config", "Dreamcast.AutoSaveState")) { cfg.toggle("config", "Dreamcast.AutoSaveState") }
        SettingsDivider()
        ToggleRow(Icons.Default.Tune, "Force free play", "Naomi/Atomiswave", cfg.bool("config", "ForceFreePlay")) { cfg.toggle("config", "ForceFreePlay") }
    }
    SettingsGroup("Network and achievements", Icons.Default.Wifi) {
        ToggleRow(Icons.Default.Wifi, "Broadband adapter", "EmulateBBA", cfg.bool("network", "EmulateBBA")) { cfg.toggle("network", "EmulateBBA") }
        SettingsDivider()
        ToggleRow(Icons.Default.Wifi, "Naomi network", "network.Enable", cfg.bool("network", "Enable")) { cfg.toggle("network", "Enable") }
        SettingsDivider()
        ToggleRow(Icons.Default.Wifi, "Battle cable", null, cfg.bool("network", "BattleCable")) { cfg.toggle("network", "BattleCable") }
        SettingsDivider()
        ToggleRow(Icons.Default.Wifi, "GGPO", "Rollback netplay. The host, delay and chat options stay in Flycast's network screen.", cfg.bool("network", "GGPO")) { cfg.toggle("network", "GGPO") }
        SettingsDivider()
        ToggleRow(Icons.Default.Info, "Upload crash logs", null, cfg.bool("config", "UploadCrashLogs")) { cfg.toggle("config", "UploadCrashLogs") }
    }
    gb.frame.RetroAchievementsSettings("GBCast")
}
