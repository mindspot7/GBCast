package com.flycast.emulator.shell

import android.os.Bundle
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.flycast.emulator.shell.ui.GBCastRoot
import com.flycast.emulator.shell.ui.DreamcastControls
import gb.frame.ControlsCapture
import gb.frame.HomeKeys
import com.flycast.emulator.shell.ui.MenuState
import gb.frame.HomeTheme
import gb.frame.PlayTime
import gb.frame.setControllerContent

class LibraryActivity : ComponentActivity() {
    private var lastGamepadBAt = 0L
    private var lastGamepadBDevice = -1
    private var pairedBackDevice = -1
    private var pairedBackDownTime = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val error = runCatching {
            DreamcastControls.initialize(this)
            FlycastBridge.start(this)
        }.exceptionOrNull()
        if (error != null) {
            Log.e("GBCastStartup", "Failed to initialize GBCast launcher", error)
        }
        setControllerContent {
            if (error != null) {
                androidx.compose.material3.Text(
                    "GBCast failed to initialize. Please send this message to support:\n" +
                        (error.message ?: error.javaClass.simpleName)
                )
            } else {
                GBCastRoot(onQuit = ::quit)
            }
        }
        hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        PlayTime.update(null, false)
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun quit() {
        finishAndRemoveTask()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (ControlsCapture.dispatch(event, DreamcastControls)) return true
        // HomeKeys owns GBFrame's menu state, while GBCast renders its own MenuState.
        // Handle X here so the library shortcut toggles the menu that is actually visible.
        if (event.keyCode == KeyEvent.KEYCODE_BUTTON_X &&
            MenuState.libraryShown &&
            !ControlsCapture.active(DreamcastControls)
        ) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                if (!MenuState.open.value) MenuState.selected = null
                MenuState.open.value = !MenuState.open.value
            }
            return true
        }
        if (event.keyCode == KeyEvent.KEYCODE_BUTTON_B && event.action == KeyEvent.ACTION_DOWN) {
            lastGamepadBAt = event.eventTime
            lastGamepadBDevice = event.deviceId
        }
        if (HomeKeys.dispatch(event, enabled = MenuState.libraryShown && !ControlsCapture.active(DreamcastControls))) return true
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            val fromController = event.source and (InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_DPAD) != 0
            if (fromController && event.action == KeyEvent.ACTION_DOWN &&
                event.deviceId == lastGamepadBDevice &&
                event.eventTime >= lastGamepadBAt &&
                event.eventTime - lastGamepadBAt <= 150L
            ) {
                pairedBackDevice = event.deviceId
                pairedBackDownTime = event.downTime
                return true
            }
            if (event.action == KeyEvent.ACTION_UP && event.deviceId == pairedBackDevice && event.downTime == pairedBackDownTime) {
                pairedBackDevice = -1
                pairedBackDownTime = -1L
                return true
            }
            return backKey(event)
        }
        if (event.keyCode == KeyEvent.KEYCODE_BUTTON_B) return backKey(event)
        val mapped = when (event.keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> KeyEvent.KEYCODE_DPAD_CENTER
            else -> null
        }
        if (mapped != null) {
            return super.dispatchKeyEvent(
                KeyEvent(event.downTime, event.eventTime, event.action, mapped, event.repeatCount, event.metaState, event.deviceId, event.scanCode, event.flags, event.source),
            )
        }
        return super.dispatchKeyEvent(event)
    }

    private fun backKey(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) onBackPressedDispatcher.onBackPressed()
        return true
    }

    private val dpad = setOf(
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_CENTER,
    )
}
