# GBCast

Android shell for the Flycast core (Dreamcast, Naomi, Naomi 2 and Atomiswave) using the shared [GBFrame](https://github.com/mindspot7/GBFrame) library. The launcher is the GBCast game library. `NativeGLActivity` still renders with Flycast's existing GLES/Vulkan path.

GBFrame is the git submodule `libs/gbframe` (commit `4678e91db423008ded3bcfc3bf8ce2c21de4c8fe` on `main`). The Android `applicationId` and Gradle `namespace` are both `app.gbcast.emulator`. `minSdk` is **28** because that is GBFrame's minimum; the previous Flycast Android app allowed API 21.

**Migration note:** changing the application ID from `com.flycast.emulator` to `app.gbcast.emulator` makes Android treat GBCast as a different app. Existing installations will not be updated in place, and app-private data will not automatically transfer. Back up any important data before switching.

## Android library

- Showcase, Grid and All in One come from GBFrame. Theme in the main menu switches them. The last selected game is restored by `LibraryView`.
- The games folder is a persisted Storage Access Framework tree. Indexed extensions: gdi, cdi, chd, cue, lst, zip, 7z, dat, bin. BIN/RAW/ISO tracks next to a GDI or CUE are not listed as separate games. GBCast first gives the core direct descriptor-backed paths; only CHD/ISO images are copied to a real cache file when a device cannot reopen that path. The library shows a blocking loading indicator while this fallback copy is prepared.
- Covers use libretro-thumbnails `Sega - Dreamcast` and are normalized by GBFrame to the system's majority box-art ratio; Dreamcast covers use a square `1.0` frame.
- Game selection uses GBFrame's shared `HomeActions`: A/tap follows normal auto-load, Y/long press opens per-game options, Open starts without auto-loading a state, and Continue loads the selected state. The shared manual slot limit is five; quick/auto states are separate.
- GBFrame supplies the shared library layouts, main-menu icons and navigation, footer help bar, About page, and the standard Controls page (including remapping/hotkey capture with a Dreamcast-specific pad diagram). The in-game pause panel uses GBFrame's `QuickMenuHost`, with Flycast-backed save/load slots, video/audio/performance options, disc swap, restart, and per-game settings. The in-game Cheats entry uses GBFrame's shared `CheatsScreen` with a Flycast GameShark adapter. The shared GBFrame RetroAchievements settings and running-game achievements screen use the Flycast rcheevos client through a `RaBackend` adapter; HTTP is routed through GBFrame so casual-mode sessions can use its in-app offline cache, unlock queue, and reconnect synchronization. Hash lookup for library games that are not currently running is not exposed by the adapter yet.
- Android Back during a game calls `gui_togglePause` and opens the quick menu. The system bars stay hidden (`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`). A controller B press that is also reported as Back is not treated as the menu while gameplay is in front.

Build the APK from `shell/android-studio` (`assembleRelease` or `assembleDebug`). The `Android` GitHub Actions workflow builds the APK; the separate `App ID consistency` workflow checks the application ID, namespace, launcher activity and device-tool settings. Run `bash tools/check-app-id.sh` locally for the same check. Device commands are available through `tools/gbcast.ps1` (requires `adb`; `Install` also requires authenticated `gh`). `git` must be on `PATH` because the version name is `git describe`.

The launcher and GBFrame's `HomeTopBar`, `HomeMenuPanel`, and `AboutPage` use the same GBCast foreground artwork (`R.drawable.ic_launcher_foreground` is the inline-compatible form), so the installed app, library, menu, and About page keep one identity. Development builds are named `GBCast-v<versionName>-build<runNumber>.apk`, and the Actions artifact uses the same stem. Numbered user releases should be named `GBCast-v<releaseVersion>.apk`, following the [GBFrame app integration standard](https://github.com/mindspot7/GBFrame/blob/main/docs/APP_INTEGRATION_STANDARD.md).

GBCast has no ads or analytics. Its optional network features and on-device data are described in the [privacy policy](PRIVACY.md).

# Flycast

The emulation core comes from [Flycast](https://github.com/flyinghead/flycast), a Sega Dreamcast, Naomi, Naomi 2, and Atomiswave emulator derived from [reicast](https://github.com/skmp/reicast-emulator). This repository keeps the Android shell and the core code that APK builds. Desktop, Apple, Switch, UWP, and libretro projects are not part of GBCast.

Configuration notes for the core are on [TheArcadeStriker's flycast wiki](https://github.com/TheArcadeStriker/flycast-wiki/wiki).

