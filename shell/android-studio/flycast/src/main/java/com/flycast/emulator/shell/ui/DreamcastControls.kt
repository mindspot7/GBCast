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
    var fastForwardActive by androidx.compose.runtime.mutableStateOf(false)
        private set
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
                    fastForwardActive = enabled
                    JNIdc.setFastForwardMode(enabled)
                }
            }
        })
    }

    fun setFastForward(enabled: Boolean) {
        ffActive = enabled
        fastForwardActive = enabled
        JNIdc.setFastForwardMode(enabled)
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
    // GBFrame's PadLayout owns all common hit testing, buttons and remapping; only the console shell
    // is app-specific. The VMU bay and single central handle distinguish Dreamcast from Saturn.
    val cw = size.width
    val ch = size.height
    val shell = androidx.compose.ui.graphics.Path().apply {
        moveTo(cw * .19f, ch * .18f)
        cubicTo(cw * .28f, ch * .10f, cw * .72f, ch * .10f, cw * .81f, ch * .18f)
        lineTo(cw * .94f, ch * .48f)
        cubicTo(cw * .98f, ch * .59f, cw * .91f, ch * .70f, cw * .82f, ch * .68f)
        lineTo(cw * .66f, ch * .62f)
        cubicTo(cw * .61f, ch * .60f, cw * .60f, ch * .70f, cw * .57f, ch * .86f)
        cubicTo(cw * .55f, ch * .96f, cw * .45f, ch * .96f, cw * .43f, ch * .86f)
        cubicTo(cw * .40f, ch * .70f, cw * .39f, ch * .60f, cw * .34f, ch * .62f)
        lineTo(cw * .18f, ch * .68f)
        cubicTo(cw * .09f, ch * .70f, cw * .02f, ch * .59f, cw * .06f, ch * .48f)
        close()
    }
    drawPath(shell, Color(0xFF343B50))
    drawPath(shell, Color.White.copy(alpha = .24f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))

    // Recess around the D-pad and action-button cluster.
    drawCircle(Color(0xFF262C3D), radius = ch * .18f, center = androidx.compose.ui.geometry.Offset(cw * .24f, ch * .50f))
    drawCircle(Color(0xFF262C3D), radius = ch * .18f, center = androidx.compose.ui.geometry.Offset(cw * .75f, ch * .50f))

    // VMU / LCD cartridge bay at the top centre of a Dreamcast controller.
    drawRoundRect(
        Color(0xFF1B2230),
        androidx.compose.ui.geometry.Offset(cw * .425f, ch * .205f),
        androidx.compose.ui.geometry.Size(cw * .15f, ch * .255f),
        androidx.compose.ui.geometry.CornerRadius(9f),
    )
    drawRoundRect(
        Color(0xFF97A5B7),
        androidx.compose.ui.geometry.Offset(cw * .448f, ch * .235f),
        androidx.compose.ui.geometry.Size(cw * .104f, ch * .145f),
        androidx.compose.ui.geometry.CornerRadius(3f),
    )
    drawRect(
        Color(0xFF526577),
        topLeft = androidx.compose.ui.geometry.Offset(cw * .463f, ch * .258f),
        size = androidx.compose.ui.geometry.Size(cw * .074f, ch * .018f),
    )
    drawCircle(
        Color(0xFFCFD5DC),
        radius = ch * .008f,
        center = androidx.compose.ui.geometry.Offset(cw * .50f, ch * .414f),
    )
})
