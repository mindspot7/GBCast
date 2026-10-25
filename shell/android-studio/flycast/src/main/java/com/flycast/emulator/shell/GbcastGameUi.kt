package com.flycast.emulator.shell

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import gb.frame.QuickMenuHost
import gb.frame.QuickMenuItemRow
import gb.frame.QuickMenuPage
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
        val discPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            val copied = Storage.importFile(uri, File(activity.cacheDir, "disc-swap"))
            message = if (copied == null) "Could not read that disc" else JNIdc.insertDisk(copied) ?: "Disc inserted. Resume to continue."
        }
        val rootPage = gb.frame.QuickMenuPage(id = "gbcast-root", title = "Quick Menu") {
            val wide = tick.let { JNIdc.configGetBool("config", "rend.WideScreen") }
            val vsync = tick.let { JNIdc.configGetBool("config", "rend.vsync") }
            val perGame = tick.let { JNIdc.hasPerGameConfig() }
            gb.frame.QuickMenuItemRow(
                id = "resume",
                icon = Icons.Default.PlayArrow,
                label = "Resume",
            ) { dismiss() }
            gb.frame.QuickMenuItemRow(
                id = "save",
                icon = Icons.Default.Save,
                label = "Save state",
                value = "Slot ${slot + 1}",
            ) {
                JNIdc.configSetInt("config", "Dreamcast.SavestateSlot", slot)
                JNIdc.saveCurrentSlot()
                message = "Saved slot ${slot + 1}"
                tick++
            }
            gb.frame.QuickMenuItemRow(
                id = "load",
                icon = Icons.Default.Restore,
                label = "Load state",
                value = "Slot ${slot + 1}",
            ) {
                JNIdc.configSetInt("config", "Dreamcast.SavestateSlot", slot)
                message = if (JNIdc.loadCurrentSlot()) "Loaded slot ${slot + 1}" else "No state in slot ${slot + 1}"
                tick++
            }
            gb.frame.QuickMenuItemRow(
                id = "previous-slot",
                icon = Icons.Default.KeyboardArrowLeft,
                label = "Previous slot",
                value = "Slot ${(slot + 9) % 10 + 1}",
            ) { slot = (slot + 9) % 10 }
            gb.frame.QuickMenuItemRow(
                id = "next-slot",
                icon = Icons.Default.KeyboardArrowRight,
                label = "Next slot",
                value = "Slot ${(slot + 1) % 10 + 1}",
            ) { slot = (slot + 1) % 10 }
            gb.frame.QuickMenuItemRow(
                id = "restart",
                icon = Icons.Default.Refresh,
                label = "Restart game",
            ) {
                JNIdc.restartGame()
                visible = false
                menuOwnsKeys = false
                compose?.visibility = View.GONE
                glView?.requestFocus()
            }
            gb.frame.QuickMenuItemRow(
                id = "swap-disc",
                icon = Icons.Default.Album,
                label = "Swap disc",
            ) { discPicker.launch(arrayOf("*/*")) }
            ToggleRow(Icons.Default.Tv, "Widescreen", null, wide) {
                JNIdc.configSetBool("config", "rend.WideScreen", it)
                tick++
            }
            ToggleRow(Icons.Default.Tv, "VSync", null, vsync) {
                JNIdc.configSetBool("config", "rend.vsync", it)
                tick++
            }
            ToggleRow(Icons.Default.Info, "Per-game overrides", "Stores later changes in this game's Flycast section.", perGame) {
                JNIdc.setPerGameConfig(it)
                tick++
            }
            gb.frame.QuickMenuItemRow(
                id = "settings",
                icon = Icons.Default.Settings,
                label = "Emulator settings",
            ) { hideForNative(SETTINGS) }
            gb.frame.QuickMenuItemRow(
                id = "cheats",
                icon = Icons.Default.Build,
                label = "Cheats",
            ) { hideForNative(CHEATS) }
            gb.frame.QuickMenuItemRow(
                id = "exit",
                icon = Icons.Default.ExitToApp,
                label = "Exit game",
            ) { activity.finish() }
            message?.let { Text(it, color = Color(0xFFFFB15A), modifier = Modifier.padding(12.dp)) }
        }
        GBCastTheme {
            gb.frame.QuickMenuHost(
                rootPage = rootPage,
                gameTitle = null,
                onDismiss = ::resume,
                modifier = Modifier.fillMaxSize(),
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
