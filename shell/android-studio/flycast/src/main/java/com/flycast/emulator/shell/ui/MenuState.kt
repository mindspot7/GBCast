package com.flycast.emulator.shell.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Survives leaving the library for a settings page. The open menu keeps the tile that was chosen. */
internal object MenuState {
    val open = mutableStateOf(false)
    val quitOpen = mutableStateOf(false)
    var onQuit: (() -> Unit)? = null
    var selected by mutableStateOf<SettingsPage?>(null)
    @Volatile var libraryShown = false
    @Volatile var hasFocus = false
    val refocus = mutableIntStateOf(0)
    @Volatile var menuHasFocus = false
    val menuRefocus = mutableIntStateOf(0)
}
