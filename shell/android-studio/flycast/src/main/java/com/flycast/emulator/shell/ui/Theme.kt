package com.flycast.emulator.shell.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import gb.frame.EmuPalette
import gb.frame.EmuTheme

@Composable
fun GBCastTheme(content: @Composable () -> Unit) =
    EmuTheme(EmuPalette.fromAccent(Color(0xFF3EC6FF), Color(0xFFFFB15A)), content)
