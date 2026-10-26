package com.flycast.emulator.shell.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.flycast.emulator.shell.GBCastBridge
import gb.frame.PresetOption
import gb.frame.PresetPicker
import gb.frame.SetupWizard
import gb.frame.WizardStep
import gb.frame.MenuState
import kotlinx.coroutines.launch

private enum class Screen { Library, Settings }

@Composable
fun GBCastRoot(onQuit: () -> Unit) {
    GBCastTheme {
        val ctx = LocalContext.current
        var setup by remember { mutableStateOf(GBCastBridge.setupDone(ctx)) }
        var screen by remember { mutableStateOf(Screen.Library) }
        var settingsPage by remember { mutableStateOf<SettingsPage?>(null) }
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        var preset by remember { mutableStateOf(1) }

        Surface(Modifier.fillMaxSize(), color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
            if (!setup) {
                SetupWizard(
                    steps = listOf(
                        WizardStep(
                            Icons.Default.Folder,
                            "Games",
                            listOf(Color(0xFF3EC6FF), Color(0xFF1B6F99)),
                            "Pick the folder with your Dreamcast, Naomi, Naomi 2 or Atomiswave images. GDI, CUE, CDI, CHD, ZIP and 7Z are indexed. Track BIN files next to a GDI or CUE are kept with that disc.",
                        ) {
                            val picker = androidx.activity.compose.rememberLauncherForActivityResult(
                                androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree(),
                            ) { uri ->
                                if (uri != null) {
                                    com.flycast.emulator.shell.Storage.takeFolder(uri)
                                    GBCastBridge.setGamesDir(ctx, uri.toString())
                                }
                            }
                            androidx.compose.material3.Button(onClick = { picker.launch(null) }) {
                                Text(if (GBCastBridge.gamesDir().isBlank()) "Choose games folder" else "Folder selected")
                            }
                        },
                        WizardStep(
                            Icons.Default.SportsEsports,
                            "Performance",
                            listOf(Color(0xFFFFB15A), Color(0xFF8A4B12)),
                            "These presets configure GBCast's renderer, resolution and dynamic recompiler. BIOS files (dc_boot.bin, dc_flash.bin) can be copied later from System.",
                        ) {
                            PresetPicker(
                                listOf(
                                    PresetOption("Compatible", "GLES, native resolution, dynarec on"),
                                    PresetOption("Balanced", "GLES, 2x resolution, vsync and frame pacing"),
                                    PresetOption("Quality", "Vulkan, 3x resolution"),
                                ),
                                preset,
                            ) { chosen ->
                                preset = chosen
                                when (chosen) {
                                    0 -> {
                                        GBCastBridge.setInt("config", "pvr.rend", 0)
                                        GBCastBridge.setInt("config", "rend.Resolution", 480)
                                    }
                                    2 -> {
                                        GBCastBridge.setInt("config", "pvr.rend", 4)
                                        GBCastBridge.setInt("config", "rend.Resolution", 1440)
                                    }
                                    else -> {
                                        GBCastBridge.setInt("config", "pvr.rend", 0)
                                        GBCastBridge.setInt("config", "rend.Resolution", 960)
                                        GBCastBridge.setBool("config", "rend.vsync", true)
                                        GBCastBridge.setBool("config", "rend.FramePacing", true)
                                    }
                                }
                                GBCastBridge.setBool("config", "Dynarec.Enabled", true)
                            }
                        },
                    ),
                    onFinish = {
                        GBCastBridge.finishSetup(ctx)
                        setup = true
                    },
                )
            } else {
                BackHandler(screen == Screen.Settings) {
                    screen = Screen.Library
                    if (MenuState.open.value) {
                        MenuState.menuHasFocus = false
                        MenuState.menuRefocus.intValue++
                    }
                }
                Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = androidx.compose.ui.graphics.Color.Transparent) { pad ->
                    Box(Modifier.padding(pad).fillMaxSize()) {
                        when (screen) {
                            Screen.Library -> LibraryScreen(
                                onOpenSettings = { settingsPage = it; screen = Screen.Settings },
                                onQuit = onQuit,
                                launch = { path, slot, fresh ->
                                    scope.launch {
                                        GBCastBridge.launch(ctx, path, slot, fresh)?.let { snackbar.showSnackbar(it) }
                                    }
                                },
                            )
                            Screen.Settings -> SettingsScreen(
                                onBack = {
                                    screen = Screen.Library
                                    if (MenuState.open.value) {
                                        MenuState.menuHasFocus = false
                                        MenuState.menuRefocus.intValue++
                                    }
                                },
                                toast = { msg -> scope.launch { snackbar.showSnackbar(msg) } },
                                page = settingsPage,
                            )
                        }
                    }
                }
            }
        }
    }
}

