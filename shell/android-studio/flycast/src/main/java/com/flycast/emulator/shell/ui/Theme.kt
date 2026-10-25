package com.flycast.emulator.shell.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import gb.frame.EmuPalette
import gb.frame.EmuTheme

/**
 * Dreamcast Graphite identity:
 * - graphite base (#24272D)
 * - restrained orange accent (#E66D32)
 * - metallic gray secondary (#929BA8)
 * - warm white foreground (#F0E7D8)
 *
 * Shared GBFrame components consume this palette through EmuTheme.
 */
private val GraphiteOrange = Color(0xFFE66D32)
private val MetallicGray = Color(0xFF929BA8)

@Composable
fun GBCastTheme(content: @Composable () -> Unit) =
    EmuTheme(EmuPalette.fromAccent(GraphiteOrange, MetallicGray), content)
