package com.flycast.emulator.shell

import org.junit.Assert.*
import org.junit.Test

class DisplayModeTest {
    @Test fun switchingEveryPairClearsIncompatibleFlagsAndRoundTrips() {
        val values = mutableMapOf<String, Boolean>()
        val setter: (String, String, Boolean) -> Unit = { _, key, value -> values[key] = value }
        val getter: (String, String) -> Boolean = { _, key -> values[key] == true }
        for (from in DisplayMode.entries) for (to in DisplayMode.entries) {
            from.apply(setter)
            to.apply(setter)
            assertEquals(to, DisplayMode.current(getter))
            assertEquals(to.wide, values["rend.WideScreen"])
            assertEquals(to.superWide, values["rend.SuperWideScreen"])
            assertEquals(to.fullscreen, values["rend.StretchToWindow"])
        }
    }

    @Test fun legacyOrphanSuperWideFlagKeepsOriginalAspect() {
        assertEquals(DisplayMode.Original, DisplayMode.current { _, key -> key == "rend.SuperWideScreen" })
    }
}
