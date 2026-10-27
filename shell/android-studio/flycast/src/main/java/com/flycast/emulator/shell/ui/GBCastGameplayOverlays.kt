package com.flycast.emulator.shell.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flycast.emulator.emu.JNIdc
import gb.frame.PerfOptions
import gb.frame.PerfOverlay
import gb.frame.RaNotifications
import kotlinx.coroutines.delay

object GBCastOverlayOptions {
    private lateinit var preferences: android.content.SharedPreferences
    var options by mutableStateOf(PerfOptions())
        private set
    fun initialize(context: Context) {
        preferences = context.getSharedPreferences("gbcast-overlays", Context.MODE_PRIVATE)
        options = PerfOptions(JNIdc.configGetBool("config", "rend.ShowFPS"),
            preferences.getBoolean("speed", false), preferences.getBoolean("cpu", false), preferences.getBoolean("gpu", false),
            preferences.getBoolean("renderer", false), preferences.getBoolean("resolution", false), preferences.getBoolean("frameTime", false))
    }
    fun set(key: String, enabled: Boolean) {
        when (key) {
            "fps" -> { JNIdc.configSetBool("config", "rend.ShowFPS", enabled); options = options.copy(fps = enabled) }
            "speed" -> options = options.copy(speed = enabled)
            "cpu" -> options = options.copy(cpu = enabled)
            "gpu" -> options = options.copy(gpu = enabled)
            "renderer" -> options = options.copy(renderer = enabled)
            "resolution" -> options = options.copy(resolution = enabled)
            "frameTime" -> options = options.copy(frameTime = enabled)
        }
        preferences.edit().putBoolean(key, enabled).apply()
    }

    fun items() = options.let { p -> listOf(
        Triple("fps", "FPS", p.fps), Triple("speed", "Speed", p.speed),
        Triple("cpu", "CPU", p.cpu), Triple("gpu", "GPU", p.gpu),
        Triple("renderer", "Renderer", p.renderer), Triple("resolution", "Resolution", p.resolution),
        Triple("frameTime", "Frame time", p.frameTime)) }
}

@Composable
fun GBCastGameplayOverlays(menuOpen: Boolean, onMenu: () -> Unit) {
    var stats by remember { mutableStateOf(intArrayOf(0, 640, 480, 60, 0, 0)) }
    var fps by remember { mutableFloatStateOf(0f) }
    var notice by remember { mutableStateOf<String?>(null) }
    var noticeAt by remember { mutableLongStateOf(0) }
    var lastCount by remember { mutableIntStateOf(-1) }
    var lastTime by remember { mutableLongStateOf(System.nanoTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = System.nanoTime()
            val next = JNIdc.overlayStats()
            if (next.size >= 6 && now - lastTime >= 900_000_000) {
                val count = next[0]
                fps = if (lastCount >= 0 && count >= lastCount && JNIdc.guiState() == 0)
                    (count - lastCount) * 1_000_000_000f / (now - lastTime) else 0f
                lastCount = count; lastTime = now; stats = next
            }
            JNIdc.pollOverlayMessage()?.let { notice = it; noticeAt = now }
            if (notice != null && now - noticeAt > 3_000_000_000) notice = null
            delay(250)
        }
    }
    Box(Modifier.fillMaxSize()) {
        if (!menuOpen) {
            FilledTonalIconButton(onClick = onMenu, modifier = Modifier.align(Alignment.TopStart).safeDrawingPadding().padding(8.dp)) {
                Icon(Icons.Default.Menu, "Menu")
            }
            PerfOverlay(GBCastOverlayOptions.options, "GBCast", fps, stats.getOrElse(3) { 60 }.toFloat().coerceAtLeast(1f),
                when (stats.getOrElse(4) { 0 }) { 4 -> "Vulkan"; 5 -> "Vulkan OIT"; 3 -> "OpenGL OIT"; else -> "OpenGL ES" },
                stats.getOrElse(1) { 640 }, stats.getOrElse(2) { 480 }, if (fps > 0f) 1000f / fps else 0f,
                gpuName = { JNIdc.overlayGpuName().orEmpty() },
                modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(10.dp))
            if (stats.getOrElse(5) { 0 } != 0) Text("Fast-forward", color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.BottomEnd).safeDrawingPadding().padding(12.dp))
        }
        RaNotifications(Modifier.align(Alignment.TopCenter).safeDrawingPadding().padding(top = 56.dp))
        notice?.let { message ->
            Surface(color = Color.Black.copy(alpha = .8f), shape = MaterialTheme.shapes.medium,
                modifier = Modifier.align(Alignment.TopCenter).safeDrawingPadding().padding(top = 12.dp)) {
                Text(message, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(12.dp))
            }
        }
    }
}
