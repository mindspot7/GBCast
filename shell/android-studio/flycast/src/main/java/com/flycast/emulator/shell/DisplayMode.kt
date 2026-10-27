package com.flycast.emulator.shell

/** One exclusive presentation choice shared by Settings and the in-game menu. */
enum class DisplayMode(val label: String, val wide: Boolean, val superWide: Boolean, val fullscreen: Boolean) {
    Original("4:3", false, false, false),
    Wide("16:9", true, false, false),
    SuperWide("Super widescreen", true, true, false),
    Fullscreen("Fullscreen", false, false, true);

    fun apply(setBool: (String, String, Boolean) -> Unit) {
        setBool("config", "rend.WideScreen", wide)
        setBool("config", "rend.SuperWideScreen", superWide)
        setBool("config", "rend.StretchToWindow", fullscreen)
    }

    companion object {
        fun current(getBool: (String, String) -> Boolean): DisplayMode = when {
            getBool("config", "rend.StretchToWindow") -> Fullscreen
            !getBool("config", "rend.WideScreen") -> Original
            getBool("config", "rend.SuperWideScreen") -> SuperWide
            else -> Wide
        }
    }
}
