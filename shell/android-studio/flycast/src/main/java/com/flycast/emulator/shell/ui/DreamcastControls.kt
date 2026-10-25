package com.flycast.emulator.shell.ui

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.view.KeyEvent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.flycast.emulator.emu.JNIdc
import com.flycast.emulator.emu.VGamepad
import com.flycast.emulator.shell.GbcastGameUi
import gb.frame.ControlButton
import gb.frame.ControlsHost
import gb.frame.HotkeyAction
import gb.frame.Hotkeys
import gb.frame.PadButton
import gb.frame.PadLayout
import gb.frame.padRect

/** Dreamcast adapter for GBFrame's shared Controls page. */
object DreamcastControls : ControlsHost {
    private const val PREF_KEYS = "gbcast.controls.keyMap"
    private const val PREF_HOTKEYS = "gbcast.controls.hotkeys"
    private const val PREF_FF_TOGGLE = "gbcast.controls.fastForwardToggle"
    private const val PREF_TOUCH = "gbcast.controls.showTouch"

    override val console = "Dreamcast"
    override val pad = DreamcastPad

    override val buttons: List<ControlButton> = listOf(
        ControlButton("Up", "D-pad up", icon = Icons.Default.KeyboardArrowUp, color = Color(0xFF343B50)),
        ControlButton("Down", "D-pad down", icon = Icons.Default.KeyboardArrowDown, color = Color(0xFF343B50)),
        ControlButton("Left", "D-pad left", icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft, color = Color(0xFF343B50)),
        ControlButton("Right", "D-pad right", icon = Icons.AutoMirrored.Filled.KeyboardArrowRight, color = Color(0xFF343B50)),
        ControlButton("A", "A button", badge = "A", color = Color(0xFF3C8D83)),
        ControlButton("B", "B button", badge = "B", color = Color(0xFFBD625A)),
        ControlButton("X", "X button", badge = "X", color = Color(0xFF6385C4)),
        ControlButton("Y", "Y button", badge = "Y", color = Color(0xFFCEB45E)),
        ControlButton("L", "Left trigger", badge = "L", color = Color(0xFF47435F)),
        ControlButton("R", "Right trigger", badge = "R", color = Color(0xFF47435F)),
        ControlButton("Start", "Start button", badge = "START", color = Color(0xFF6E58D8)),
    )

    private val defaultMap = linkedMapOf(
        KeyEvent.KEYCODE_DPAD_UP to "Up",
        KeyEvent.KEYCODE_DPAD_DOWN to "Down",
        KeyEvent.KEYCODE_DPAD_LEFT to "Left",
        KeyEvent.KEYCODE_DPAD_RIGHT to "Right",
        KeyEvent.KEYCODE_BUTTON_A to "A",
        KeyEvent.KEYCODE_BUTTON_B to "B",
        KeyEvent.KEYCODE_BUTTON_X to "X",
        KeyEvent.KEYCODE_BUTTON_Y to "Y",
        KeyEvent.KEYCODE_BUTTON_L1 to "L",
        KeyEvent.KEYCODE_BUTTON_R1 to "R",
        KeyEvent.KEYCODE_BUTTON_START to "Start",
    )

    private lateinit var prefs: SharedPreferences
    private var keyMap by androidx.compose.runtime.mutableStateOf<Map<Int, String>>(defaultMap)
    private var storedHotkeys by androidx.compose.runtime.mutableStateOf<Map<HotkeyAction, Set<Int>>>(emptyMap())
    override var remapTarget by androidx.compose.runtime.mutableStateOf<String?>(null)
    override var hotkeyCapture by androidx.compose.runtime.mutableStateOf<HotkeyAction?>(null)
    override var hotkeyCaptureKeys by androidx.compose.runtime.mutableStateOf<Set<Int>>(emptySet())
    private var ffActive = false
    private var ffToggleState by androidx.compose.runtime.mutableStateOf(false)
    private var touchState by androidx.compose.runtime.mutableStateOf(true)

    override val hotkeys: Map<HotkeyAction, Set<Int>> get() = storedHotkeys

    override var fastForwardToggle: Boolean
        get() = ffToggleState
        set(value) {
            ffToggleState = value
            preferences().edit().putBoolean(PREF_FF_TOGGLE, value).apply()
        }

    override var showTouchControls: Boolean
        get() = touchState
        set(value) {
            touchState = value
            preferences().edit().putBoolean(PREF_TOUCH, value).apply()
            if (!value) VGamepad.hide()
        }

    private fun preferences(): SharedPreferences =
        if (::prefs.isInitialized) prefs else error("DreamcastControls has not been initialized")

    fun initialize(context: Context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        keyMap = readKeyMap()
        storedHotkeys = readHotkeys()
        ffToggleState = prefs.getBoolean(PREF_FF_TOGGLE, false)
        touchState = prefs.getBoolean(PREF_TOUCH, true)
    }

    private fun readKeyMap(): Map<Int, String> {
        val saved = preferences().getString(PREF_KEYS, null) ?: return defaultMap
        val validIds = buttons.map { it.id }.toSet()
        val parsed = saved.split(";").mapNotNull { entry ->
            val p = entry.split("=", limit = 2)
            val code = p.getOrNull(0)?.toIntOrNull()
            val id = p.getOrNull(1)
            if (code != null && id != null && id in validIds) code to id else null
        }.toMap()
        return if (parsed.isEmpty()) defaultMap else parsed
    }

    private fun saveKeyMap() {
        preferences().edit().putString(PREF_KEYS, keyMap.entries.joinToString(";") { "${it.key}=${it.value}" }).apply()
    }

    private fun readHotkeys(): Map<HotkeyAction, Set<Int>> {
        val saved = preferences().getString(PREF_HOTKEYS, null) ?: return emptyMap()
        return saved.split(";").mapNotNull { entry ->
            val p = entry.split("=", limit = 2)
            val action = p.getOrNull(0)?.let { name -> HotkeyAction.entries.firstOrNull { it.name == name } }
            val keys = p.getOrNull(1)?.split("+")?.mapNotNull { it.toIntOrNull() }?.toSet()
            if (action != null && !keys.isNullOrEmpty()) action to keys else null
        }.toMap()
    }

    private fun saveHotkeys() {
        preferences().edit().putString(PREF_HOTKEYS, storedHotkeys.entries.joinToString(";") {
            "${it.key.name}=${it.value.joinToString("+")}"
        }).apply()
    }

    override fun keysFor(id: String): List<Int> = keyMap.filterValues { it == id }.keys.toList()

    override fun bindKey(id: String, keyCode: Int) {
        if (buttons.none { it.id == id }) return
        keyMap = keyMap.toMutableMap().also { map ->
            map.entries.removeAll { it.value == id || it.key == keyCode }
            map[keyCode] = id
        }
        saveKeyMap()
    }

    override fun resetKeyMap() {
        keyMap = defaultMap
        saveKeyMap()
    }

    override fun bindHotkey(action: HotkeyAction, keys: Set<Int>) {
        storedHotkeys = storedHotkeys.toMutableMap().also { map ->
            if (keys.isEmpty()) map.remove(action) else map[action] = keys
        }
        saveHotkeys()
    }

    @JvmStatic
    fun mapKeyCode(keyCode: Int): Int {
        val id = keyMap[keyCode] ?: return keyCode
        return when (id) {
            "Up" -> KeyEvent.KEYCODE_DPAD_UP
            "Down" -> KeyEvent.KEYCODE_DPAD_DOWN
            "Left" -> KeyEvent.KEYCODE_DPAD_LEFT
            "Right" -> KeyEvent.KEYCODE_DPAD_RIGHT
            "A" -> KeyEvent.KEYCODE_BUTTON_A
            "B" -> KeyEvent.KEYCODE_BUTTON_B
            "X" -> KeyEvent.KEYCODE_BUTTON_X
            "Y" -> KeyEvent.KEYCODE_BUTTON_Y
            "L" -> KeyEvent.KEYCODE_BUTTON_L2
            "R" -> KeyEvent.KEYCODE_BUTTON_R2
            "Start" -> KeyEvent.KEYCODE_BUTTON_START
            else -> keyCode
        }
    }

    @JvmStatic
    fun configureHotkeys(activity: Activity) {
        Hotkeys.configure(bindings = { hotkeys }, run = { action, down ->
            when (action) {
                HotkeyAction.QuickSave -> if (down) JNIdc.saveCurrentSlot()
                HotkeyAction.QuickLoad -> if (down) JNIdc.loadCurrentSlot()
                HotkeyAction.Menu -> if (down) GbcastGameUi.onAndroidBack(activity)
                HotkeyAction.FastForward -> {
                    val enabled = if (fastForwardToggle) {
                        if (down) ffActive = !ffActive
                        ffActive
                    } else down
                    ffActive = enabled
                    JNIdc.setFastForwardMode(enabled)
                }
            }
        })
    }
}

private val DreamcastPad = PadLayout(listOf(
    PadButton("L", "L", x = .25f, y = .14f, w = .20f, h = .13f, wOfWidth = true, shape = padRect(10), color = Color(0xFF47435F)),
    PadButton("R", "R", x = .75f, y = .14f, w = .20f, h = .13f, wOfWidth = true, shape = padRect(10), color = Color(0xFF47435F)),
    PadButton("Up", icon = Icons.Default.KeyboardArrowUp, x = .23f, y = .33f, w = .17f, h = .17f, shape = padRect(), color = Color(0xFF343B50)),
    PadButton("Down", icon = Icons.Default.KeyboardArrowDown, x = .23f, y = .67f, w = .17f, h = .17f, shape = padRect(), color = Color(0xFF343B50)),
    PadButton("Left", icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft, x = .15f, y = .50f, w = .17f, h = .17f, shape = padRect(), color = Color(0xFF343B50)),
    PadButton("Right", icon = Icons.AutoMirrored.Filled.KeyboardArrowRight, x = .31f, y = .50f, w = .17f, h = .17f, shape = padRect(), color = Color(0xFF343B50)),
    PadButton("Start", "START", x = .50f, y = .62f, w = .11f, h = .10f, wOfWidth = true, shape = RoundedCornerShape(50), color = Color(0xFF6E58D8)),
    PadButton("X", "X", x = .66f, y = .38f, w = .18f, h = .18f, color = Color(0xFF6385C4)),
    PadButton("Y", "Y", x = .75f, y = .33f, w = .18f, h = .18f, color = Color(0xFFCEB45E)),
    PadButton("A", "A", x = .64f, y = .62f, w = .18f, h = .18f, color = Color(0xFF3C8D83)),
    PadButton("B", "B", x = .74f, y = .58f, w = .18f, h = .18f, color = Color(0xFFBD625A)),

), body = {
    // Dreamcast's single central grip and VMU bay, rather than the generic dual-grip pad.
    val cw = size.width
    val ch = size.height
    val body = androidx.compose.ui.graphics.Path().apply {
        moveTo(cw * .16f, ch * .19f)
        cubicTo(cw * .25f, ch * .09f, cw * .75f, ch * .09f, cw * .84f, ch * .19f)
        lineTo(cw * .96f, ch * .43f)
        cubicTo(cw * .99f, ch * .53f, cw * .92f, ch * .63f, cw * .83f, ch * .62f)
        lineTo(cw * .66f, ch * .57f)
        cubicTo(cw * .61f, ch * .56f, cw * .60f, ch * .67f, cw * .57f, ch * .82f)
        cubicTo(cw * .55f, ch * .93f, cw * .45f, ch * .93f, cw * .43f, ch * .82f)
        cubicTo(cw * .40f, ch * .67f, cw * .39f, ch * .56f, cw * .34f, ch * .57f)
        lineTo(cw * .17f, ch * .62f)
        cubicTo(cw * .08f, ch * .63f, cw * .01f, ch * .53f, cw * .04f, ch * .43f)
        close()
    }
    drawPath(body, Color(0xFF343B50))
    drawPath(body, Color.White.copy(alpha = .28f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
    // Raised central VMU/display bay, characteristic of the Dreamcast controller.
    drawRoundRect(Color(0xFF1A2230), androidx.compose.ui.geometry.Offset(cw * .405f, ch * .22f),
        androidx.compose.ui.geometry.Size(cw * .19f, ch * .24f), androidx.compose.ui.geometry.CornerRadius(10f))
    drawRoundRect(Color(0xFF7C8999), androidx.compose.ui.geometry.Offset(cw * .435f, ch * .255f),
        androidx.compose.ui.geometry.Size(cw * .13f, ch * .13f), androidx.compose.ui.geometry.CornerRadius(4f))
})
