# GBCast

Android shell for the Flycast core (Dreamcast, Naomi, Naomi 2 and Atomiswave) using the shared [GBFrame](https://github.com/mindspot7/GBFrame) library. The launcher is the GBCast game library. `NativeGLActivity` still renders with Flycast's existing GLES/Vulkan path.

GBFrame is the git submodule `libs/gbframe` (commit `117ea1cef29c25efc44438ca96eb078b51f4ece8` on `main`). The Android `applicationId` and Gradle `namespace` are both `app.gbcast.emulator`. `minSdk` is **28** because that is GBFrame's minimum; the previous Flycast Android app allowed API 21.

**Migration note:** changing the application ID from `com.flycast.emulator` to `app.gbcast.emulator` makes Android treat GBCast as a different app. Existing installations will not be updated in place, and app-private data will not automatically transfer. Back up any important data before switching.

## Android library

- Showcase, Grid and All in One come from GBFrame. Theme in the main menu switches them. The last selected game is restored by `LibraryView`.
- The games folder is a persisted Storage Access Framework tree. Indexed extensions: gdi, cdi, chd, cue, lst, zip, 7z, dat, bin. BIN/RAW/ISO tracks next to a GDI or CUE are not listed as separate games. GDI and CUE are staged with their sibling tracks; CDI, CHD and archives are copied to a real cache file because Flycast opens them by path.
- Covers use libretro-thumbnails `Sega - Dreamcast`.
- GBFrame supplies the shared library layouts, main-menu icons and navigation, footer help bar, About page, and the standard Controls page (including remapping/hotkey capture with a Dreamcast-specific pad diagram). The in-game pause panel uses GBFrame's `QuickMenuHost`, with Flycast-backed save/load slots, video/audio/performance options, disc swap, restart, and per-game settings. The in-game Cheats entry uses GBFrame's shared `CheatsScreen` with a Flycast GameShark adapter. The shared GBFrame RetroAchievements settings and running-game achievements screen use the Flycast rcheevos client through a `RaBackend` adapter. Hash lookup for library games that are not currently running is not exposed by the adapter yet.
- Android Back during a game calls `gui_togglePause` and opens the quick menu. The system bars stay hidden (`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`). A controller B press that is also reported as Back is not treated as the menu while gameplay is in front.

Build the APK from `shell/android-studio` (`assembleRelease` or `assembleDebug`). The `Android` GitHub Actions workflow builds the APK; the separate `App ID consistency` workflow checks the application ID, namespace, launcher activity and device-tool settings. Run `bash tools/check-app-id.sh` locally for the same check. Device commands are available through `tools/gbcast.ps1` (requires `adb`; `Install` also requires authenticated `gh`). `git` must be on `PATH` because the version name is `git describe`.

The in-app brand mark is `ic_gbcast.xml` in the Android app's resources. Pass this same artwork to GBFrame's `HomeTopBar`, `HomeMenuPanel`, and `AboutPage`; the launcher icon is a separate controller image. Development builds are named `GBCast-v<versionName>-build<runNumber>.apk`, and the Actions artifact uses the same stem. Numbered user releases should be named `GBCast-v<releaseVersion>.apk`, following the [GBFrame app integration standard](https://github.com/mindspot7/GBFrame/blob/main/docs/APP_INTEGRATION_STANDARD.md).

# Flycast

The emulation core comes from [Flycast](https://github.com/flyinghead/flycast), a Sega Dreamcast, Naomi, Naomi 2, and Atomiswave emulator derived from [reicast](https://github.com/skmp/reicast-emulator). This repository keeps the Android shell and the core code that APK builds. Desktop, Apple, Switch, UWP, and libretro projects are not part of GBCast.

Configuration notes for the core are on [TheArcadeStriker's flycast wiki](https://github.com/TheArcadeStriker/flycast-wiki/wiki).

