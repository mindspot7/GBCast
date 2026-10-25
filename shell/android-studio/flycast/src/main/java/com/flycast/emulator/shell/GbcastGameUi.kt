package com.flycast.emulator.shell

import android.app.Activity
import android.net.Uri
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.flycast.emulator.emu.JNIdc
import com.flycast.emulator.shell.ui.DreamcastControls
import com.flycast.emulator.shell.ui.GBCastTheme
import kotlinx.coroutines.delay
import java.io.File


import android.app.Activity
import android.net.Uri
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import com.flycast.emulator.emu.JNIdc
import com.flycast.emulator.shell.ui.GBCastTheme
import gb.frame.ToggleRow
import kotlinx.coroutines.delay
import java.io.File

/** In-game quick menu. Pausing uses Flycast's gui_togglePause, which stops the CPU and keeps the last frame. */
object GbcastGameUi {
    const val CLOSED = 0
    const val COMMANDS = 1
    const val PAUSE = 2
    const val SETTINGS = 3
    const val MAIN = 4
    const val VJOY = 6
    const val VJOY_CMD = 7
    const val DISK = 8
    const val LOADING = 9
    const val CHEATS = 11
    const val ACHIEVEMENTS = 12

    @JvmStatic var menuOwnsKeys: Boolean = false
    private var visible by mutableStateOf(false)
    private var compose: ComposeView? = null
    private var glView: View? = null
    private var message by mutableStateOf<String?>(null)
    private var slot by mutableIntStateOf(0)

    @JvmStatic
    fun attach(activity: Activity, layout: ViewGroup, gameView: View) {
        glView = gameView
        Storage.start(activity)
        val view = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { if (visible) QuickMenu(activity) }
            visibility = View.GONE
            isFocusable = true
            isFocusableInTouchMode = true
        }
        compose = view
        layout.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    @JvmStatic
    fun noteGamepadB(event: KeyEvent) {
        GbcastInput.note(event)
    }

    @JvmStatic
    fun onAndroidBack(activity: Activity) {
        when (JNIdc.guiState()) {
            LOADING, VJOY, VJOY_CMD -> JNIdc.guiOpenSettings()
            COMMANDS, SETTINGS, DISK, CHEATS, ACHIEVEMENTS -> {
                JNIdc.guiSetState(PAUSE)
                show()
            }
            PAUSE -> if (visible) resume() else show()
            MAIN -> activity.finish()
            else -> {
                JNIdc.guiTogglePause()
                show()
            }
        }
    }

    private fun show() {
        visible = true
        menuOwnsKeys = true
        glView?.isFocusable = false
        glView?.isFocusableInTouchMode = false
        compose?.visibility = View.VISIBLE
        compose?.requestFocusFromTouch()
        compose?.requestFocus()
        slot = JNIdc.configGetInt("config", "Dreamcast.SavestateSlot").coerceAtLeast(0)
    }

    private fun resume() {
        visible = false
        menuOwnsKeys = false
        compose?.visibility = View.GONE
        if (JNIdc.guiState() == PAUSE) JNIdc.guiTogglePause()
        glView?.isFocusable = true
        glView?.isFocusableInTouchMode = true
        glView?.requestFocus()
    }

    private fun hideForNative(state: Int) {
        JNIdc.guiSetState(state)
        visible = false
        menuOwnsKeys = false
        compose?.visibility = View.GONE
        glView?.requestFocus()
    }

    @Composable
    private fun QuickMenu(activity: Activity) {
        var tick by androidx.compose.runtime.remember { mutableIntStateOf(0) }
        val toast: (String) -> Unit = { text ->
            android.widget.Toast.makeText(activity, text, android.widget.Toast.LENGTH_SHORT).show()
        }

        fun currentBool(section: String, name: String): Boolean {
            val revision = tick
            return JNIdc.configGetBool(section, name)
        }
        fun currentInt(section: String, name: String): Int {
            val revision = tick
            return JNIdc.configGetInt(section, name)
        }
        fun setBool(section: String, name: String, value: Boolean) {
            JNIdc.configSetBool(section, name, value)
            tick++
        }
        fun setInt(section: String, name: String, value: Int) {
            JNIdc.configSetInt(section, name, value)
            tick++
        }
        fun hasState(index: Int): Boolean {
            val path = JNIdc.currentGamePath().orEmpty()
            if (path.isBlank()) return false
            val fileName = File(path).name
            val base = fileName.substringBeforeLast('.', fileName)
            val suffix = if (index == 0) "" else "_" + index
            return File(JNIdc.dataDirectory().orEmpty(), base + suffix + ".state").isFile
        }

        val discPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            val copied = Storage.importFile(uri, File(activity.cacheDir, "disc-swap"))
            message = if (copied == null) {
                "Could not read that disc"
            } else {
                JNIdc.insertDisk(copied) ?: "Disc inserted. Resume to continue."
            }
            if (copied != null && JNIdc.insertDisk(copied) == null) {
                // insertDisk was already called above; this branch is intentionally left empty.
            }
        }

        val savePage = androidx.compose.runtime.remember {
            gb.frame.QuickMenuPage("save-state", "Save state") {
                val revision = tick
                for (index in 0..9) {
                    val exists = hasState(index)
                    gb.frame.QuickMenuItemRow(
                        id = "save-slot-" + index,
                        icon = androidx.compose.material.icons.Icons.Default.Save,
                        label = "Slot " + (index + 1),
                        value = if (exists) "Overwrite" else "Empty",
                    ) {
                        JNIdc.configSetInt("config", "Dreamcast.SavestateSlot", index)
                        slot = index
                        JNIdc.saveCurrentSlot()
                        toast("Saved slot " + (index + 1))
                        dismiss()
                    }
                }
            }
        }
        val loadPage = androidx.compose.runtime.remember {
            gb.frame.QuickMenuPage("load-state", "Load state") {
                val revision = tick
                for (index in 0..9) {
                    val exists = hasState(index)
                    gb.frame.QuickMenuItemRow(
                        id = "load-slot-" + index,
                        icon = androidx.compose.material.icons.Icons.Default.Restore,
                        label = "Slot " + (index + 1),
                        value = if (exists) "Saved" else "Empty",
                        enabled = exists,
                    ) {
                        JNIdc.configSetInt("config", "Dreamcast.SavestateSlot", index)
                        slot = index
                        if (JNIdc.loadCurrentSlot()) {
                            toast("Loaded slot " + (index + 1))
                            dismiss()
                        } else {
                            toast("Could not load slot " + (index + 1))
                            tick++
                        }
                    }
                }
            }
        }

        val videoPage = androidx.compose.runtime.remember {
            gb.frame.QuickMenuPage("video", "Video") {
                val revision = tick
                val rendererValues = listOf(0 to "GLES", 3 to "GLES OIT", 4 to "Vulkan", 5 to "Vulkan OIT")
                val renderer = currentInt("config", "pvr.rend")
                val rendererIndex = rendererValues.indexOfFirst { it.first == renderer }.let { if (it < 0) 0 else it }
                gb.frame.QuickMenuItemRow(
                    id = "renderer", icon = androidx.compose.material.icons.Icons.Default.Tv,
                    label = "Renderer", value = rendererValues[rendererIndex].second,
                ) {
                    setInt("config", "pvr.rend", rendererValues[(rendererIndex + 1) % rendererValues.size].first)
                }

                val resolutionValues = listOf(480 to "Native", 960 to "2x", 1440 to "3x", 1920 to "4x")
                val resolution = currentInt("config", "rend.Resolution")
                val resolutionIndex = resolutionValues.indexOfFirst { it.first == resolution }.let { if (it < 0) 0 else it }
                gb.frame.QuickMenuItemRow(
                    id = "resolution", icon = androidx.compose.material.icons.Icons.Default.AspectRatio,
                    label = "Internal resolution", value = resolutionValues[resolutionIndex].second,
                ) {
                    setInt("config", "rend.Resolution", resolutionValues[(resolutionIndex + 1) % resolutionValues.size].first)
                }

                for ((key, label) in listOf(
                    "rend.WideScreen" to "Widescreen",
                    "rend.SuperWideScreen" to "Super widescreen",
                    "rend.WidescreenGameHacks" to "Widescreen game hacks",
                    "rend.vsync" to "VSync",
                    "rend.IntegerScale" to "Integer scale",
                    "rend.ShowFPS" to "Show FPS",
                )) {
                    gb.frame.QuickMenuItemRow(
                        id = key, icon = androidx.compose.material.icons.Icons.Default.Tv,
                        label = label, value = if (currentBool("config", key)) "On" else "Off",
                    ) { setBool("config", key, !currentBool("config", key)) }
                }

                val filters = listOf(0 to "Default", 1 to "Nearest", 2 to "Linear")
                val filter = currentInt("config", "rend.TextureFiltering")
                val filterIndex = filters.indexOfFirst { it.first == filter }.let { if (it < 0) 0 else it }
                gb.frame.QuickMenuItemRow(
                    id = "texture-filter", icon = androidx.compose.material.icons.Icons.Default.Tune,
                    label = "Texture filtering", value = filters[filterIndex].second,
                ) { setInt("config", "rend.TextureFiltering", filters[(filterIndex + 1) % filters.size].first) }

                val anisotropic = listOf(1, 2, 4, 8, 16)
                val anisoValue = currentInt("config", "rend.AnisotropicFiltering")
                val anisoIndex = anisotropic.indexOf(anisoValue).let { if (it < 0) 0 else it }
                gb.frame.QuickMenuItemRow(
                    id = "anisotropic", icon = androidx.compose.material.icons.Icons.Default.Tune,
                    label = "Anisotropic filtering", value = if (anisotropic[anisoIndex] == 1) "Off" else anisotropic[anisoIndex].toString() + "x",
                ) { setInt("config", "rend.AnisotropicFiltering", anisotropic[(anisoIndex + 1) % anisotropic.size]) }

                for ((key, label) in listOf(
                    "rend.PerStripSorting" to "Per-strip sorting",
                    "rend.Fog" to "Fog",
                    "rend.UseMipmaps" to "Mipmaps",
                    "rend.DelayFrameSwapping" to "Delay frame swapping",
                    "rend.EmulateFramebuffer" to "Emulate framebuffer",
                    "rend.Rotate90" to "Rotate 90°",
                )) {
                    gb.frame.QuickMenuItemRow(
                        id = key, icon = androidx.compose.material.icons.Icons.Default.Tune,
                        label = label, value = if (currentBool("config", key)) "On" else "Off",
                    ) { setBool("config", key, !currentBool("config", key)) }
                }
            }
        }

        val audioPage = androidx.compose.runtime.remember {
            gb.frame.QuickMenuPage("audio", "Audio") {
                val revision = tick
                for ((key, label) in listOf(
                    "aica.DSPEnabled" to "DSP",
                    "aica.AutoLatency" to "Automatic latency",
                )) {
                    gb.frame.QuickMenuItemRow(
                        id = key, icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.VolumeUp,
                        label = label, value = if (currentBool("config", key)) "On" else "Off",
                    ) { setBool("config", key, !currentBool("config", key)) }
                }

                val buffers = listOf(64, 96, 128)
                val actualBuffer = currentInt("config", "aica.BufferSize")
                val bufferIndex = buffers.indices.minByOrNull {
                    kotlin.math.abs((buffers[it] * 44100 / 1000) - actualBuffer)
                } ?: 1
                gb.frame.QuickMenuItemRow(
                    id = "audio-buffer", icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.VolumeUp,
                    label = "Buffer latency", value = buffers[bufferIndex].toString() + " ms",
                ) { setInt("config", "aica.BufferSize", buffers[(bufferIndex + 1) % buffers.size] * 44100 / 1000) }

                gb.frame.QuickMenuItemRow(
                    id = "vmu-sound", icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.VolumeUp,
                    label = "VMU sound", value = if (currentBool("audio", "VmuSound")) "On" else "Off",
                ) { setBool("audio", "VmuSound", !currentBool("audio", "VmuSound")) }
            }
        }

        val performancePage = androidx.compose.runtime.remember {
            gb.frame.QuickMenuPage("performance", "Performance") {
                val revision = tick
                for ((key, label) in listOf(
                    "Dynarec.Enabled" to "Dynarec",
                    "rend.ThreadedRendering" to "Threaded rendering",
                    "rend.FramePacing" to "Frame pacing",
                    "FastGDRomLoad" to "Fast GD-ROM load",
                )) {
                    gb.frame.QuickMenuItemRow(
                        id = key, icon = androidx.compose.material.icons.Icons.Default.Speed,
                        label = label, value = if (currentBool("config", key)) "On" else "Off",
                    ) { setBool("config", key, !currentBool("config", key)) }
                }
                val skips = listOf(0 to "Off", 1 to "Normal", 2 to "Max")
                val skip = currentInt("config", "pvr.AutoSkipFrame")
                val skipIndex = skips.indexOfFirst { it.first == skip }.let { if (it < 0) 0 else it }
                gb.frame.QuickMenuItemRow(
                    id = "auto-frame-skip", icon = androidx.compose.material.icons.Icons.Default.Speed,
                    label = "Auto frame skip", value = skips[skipIndex].second,
                ) { setInt("config", "pvr.AutoSkipFrame", skips[(skipIndex + 1) % skips.size].first) }
            }
        }

        val overlaysPage = androidx.compose.runtime.remember {
            gb.frame.QuickMenuPage("overlays", "Overlays") {
                val revision = tick
                gb.frame.QuickMenuItemRow(
                    id = "show-fps", icon = androidx.compose.material.icons.Icons.Default.Speed,
                    label = "Show FPS", value = if (currentBool("config", "rend.ShowFPS")) "On" else "Off",
                ) { setBool("config", "rend.ShowFPS", !currentBool("config", "rend.ShowFPS")) }
                gb.frame.QuickMenuItemRow(
                    id = "touch-controls", icon = androidx.compose.material.icons.Icons.Default.TouchApp,
                    label = "Touch controls", value = if (com.flycast.emulator.shell.ui.DreamcastControls.showTouchControls) "Shown" else "Hidden",
                ) {
                    com.flycast.emulator.shell.ui.DreamcastControls.showTouchControls =
                        !com.flycast.emulator.shell.ui.DreamcastControls.showTouchControls
                    tick++
                    if (com.flycast.emulator.shell.ui.DreamcastControls.showTouchControls) {
                        (activity as? com.flycast.emulator.NativeGLActivity)?.setTouchControlsVisible(true)
                    } else {
                        (activity as? com.flycast.emulator.NativeGLActivity)?.setTouchControlsVisible(false)
                    }
                }
            }
        }

        val gamePage = androidx.compose.runtime.remember {
            gb.frame.QuickMenuPage("game", "Game") {
                gb.frame.QuickMenuItemRow(
                    id = "restart", icon = androidx.compose.material.icons.Icons.Default.RestartAlt,
                    label = "Restart",
                ) {
                    JNIdc.restartGame()
                    resume()
                }
                gb.frame.QuickMenuItemRow(
                    id = "disc-swap", icon = androidx.compose.material.icons.Icons.Default.Eject,
                    label = "Swap disc", hasSubmenu = false,
                ) { discPicker.launch(arrayOf("*/*")) }

                gb.frame.QuickMenuItemRow(
                    id = "per-game", icon = androidx.compose.material.icons.Icons.Default.Tune,
                    label = "Per-game overrides", value = if (JNIdc.hasPerGameConfig()) "On" else "Off",
                ) {
                    JNIdc.setPerGameConfig(!JNIdc.hasPerGameConfig())
                    tick++
                }
                gb.frame.QuickMenuItemRow(
                    id = "auto-load", icon = androidx.compose.material.icons.Icons.Default.Restore,
                    label = "Auto load state", value = if (currentBool("config", "Dreamcast.AutoLoadState")) "On" else "Off",
                ) { setBool("config", "Dreamcast.AutoLoadState", !currentBool("config", "Dreamcast.AutoLoadState")) }
                gb.frame.QuickMenuItemRow(
                    id = "auto-save", icon = androidx.compose.material.icons.Icons.Default.Save,
                    label = "Auto save state", value = if (currentBool("config", "Dreamcast.AutoSaveState")) "On" else "Off",
                ) { setBool("config", "Dreamcast.AutoSaveState", !currentBool("config", "Dreamcast.AutoSaveState")) }

                gb.frame.QuickMenuItemRow(
                    id = "native-settings", icon = androidx.compose.material.icons.Icons.Default.Tune,
                    label = "Flycast settings",
                ) { hideForNative(SETTINGS) }
                gb.frame.QuickMenuItemRow(
                    id = "native-cheats", icon = androidx.compose.material.icons.Icons.Default.Memory,
                    label = "Flycast cheats",
                ) { hideForNative(CHEATS) }
                gb.frame.QuickMenuItemRow(
                    id = "native-achievements", icon = androidx.compose.material.icons.Icons.Default.SportsEsports,
                    label = "Flycast achievements",
                ) { hideForNative(ACHIEVEMENTS) }
                message?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
            }
        }

        val rootPage = androidx.compose.runtime.remember {
            gb.frame.QuickMenuPage("gbcast-root", "Paused") {
                val revision = tick
                val gameTitle = File(JNIdc.currentGamePath().orEmpty()).nameWithoutExtension
                gb.frame.QuickMenuItemRow(
                    id = "resume", icon = androidx.compose.material.icons.Icons.Default.PlayArrow,
                    label = "Resume",
                ) { dismiss() }
                gb.frame.QuickMenuItemRow(
                    id = "save", icon = androidx.compose.material.icons.Icons.Default.Save,
                    label = "Save state", hasSubmenu = true,
                ) { openPage(savePage, returnFocusItemId = "save") }
                gb.frame.QuickMenuItemRow(
                    id = "load", icon = androidx.compose.material.icons.Icons.Default.Restore,
                    label = "Load state", hasSubmenu = true,
                ) { openPage(loadPage, returnFocusItemId = "load") }
                gb.frame.QuickMenuItemRow(
                    id = "fast-forward", icon = androidx.compose.material.icons.Icons.Default.FastForward,
                    label = "Fast-forward",
                    value = if (com.flycast.emulator.shell.ui.DreamcastControls.fastForwardActive) "On" else "Off",
                ) { com.flycast.emulator.shell.ui.DreamcastControls.setFastForward(!com.flycast.emulator.shell.ui.DreamcastControls.fastForwardActive) }
                gb.frame.QuickMenuItemRow(
                    id = "video", icon = androidx.compose.material.icons.Icons.Default.Tv,
                    label = "Video", hasSubmenu = true,
                ) { openPage(videoPage, returnFocusItemId = "video") }
                gb.frame.QuickMenuItemRow(
                    id = "audio", icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.VolumeUp,
                    label = "Audio", hasSubmenu = true,
                ) { openPage(audioPage, returnFocusItemId = "audio") }
                gb.frame.QuickMenuItemRow(
                    id = "performance", icon = androidx.compose.material.icons.Icons.Default.Speed,
                    label = "Performance", hasSubmenu = true,
                ) { openPage(performancePage, returnFocusItemId = "performance") }
                gb.frame.QuickMenuItemRow(
                    id = "overlays", icon = androidx.compose.material.icons.Icons.Default.Layers,
                    label = "Overlays", hasSubmenu = true,
                ) { openPage(overlaysPage, returnFocusItemId = "overlays") }
                gb.frame.QuickMenuItemRow(
                    id = "game", icon = androidx.compose.material.icons.Icons.Default.SportsEsports,
                    label = "Game", hasSubmenu = true,
                ) { openPage(gamePage, returnFocusItemId = "game") }
                message?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
                gb.frame.QuickMenuItemRow(
                    id = "exit", icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ExitToApp,
                    label = "Exit",
                ) { activity.finish() }
            }
        }

        GBCastTheme {
            gb.frame.QuickMenuHost(
                rootPage = rootPage,
                gameTitle = File(JNIdc.currentGamePath().orEmpty()).nameWithoutExtension.ifBlank { null },
                onDismiss = ::resume,
            )
        }
    }
}

object GbcastInput {
    private var at = 0L
    private var device = -1

    @JvmStatic
    fun note(event: KeyEvent) {
        if (event.action == KeyEvent.ACTION_DOWN) {
            at = event.eventTime
            device = event.deviceId
        }
    }

    @JvmStatic
    fun consumePairedBack(event: KeyEvent): Boolean {
        val paired = event.deviceId == device && event.eventTime >= at && event.eventTime - at <= 150L
        if (paired) {
            at = 0L
            return true
        }
        return false
    }
}
