# GBCast

Android shell for the Flycast core (Dreamcast, Naomi, Naomi 2 and Atomiswave) using the shared [GBFrame](https://github.com/mindspot7/GBFrame) library. The launcher is the GBCast game library. `NativeGLActivity` still renders with Flycast's existing GLES/Vulkan path.

GBFrame is the git submodule `libs/gbframe` (commit `52399a255c1c8edc1cc3614fc1215000334fe12b` on `main`). The Android `applicationId` and Gradle `namespace` are both `app.gbcast.emulator`. `minSdk` is **28** because that is GBFrame's minimum; the previous Flycast Android app allowed API 21.

**Migration note:** changing the application ID from `com.flycast.emulator` to `app.gbcast.emulator` makes Android treat GBCast as a different app. Existing installations will not be updated in place, and app-private data will not automatically transfer. Back up any important data before switching.

## Android library

- Showcase, Grid and All in One come from GBFrame. Theme in the main menu switches them. The last selected game is restored by `LibraryView`.
- The games folder is a persisted Storage Access Framework tree. Indexed extensions: gdi, cdi, chd, cue, lst, zip, 7z, dat, bin. BIN/RAW/ISO tracks next to a GDI or CUE are not listed as separate games. GDI and CUE are staged with their sibling tracks; CDI, CHD and archives are copied to a real cache file because Flycast opens them by path.
- Covers use libretro-thumbnails `Sega - Dreamcast`.
- GBFrame supplies the shared library layouts, main-menu icons and navigation, footer help bar, About page, and the standard Controls page (including remapping/hotkey capture with a Dreamcast-specific pad diagram). The in-game pause panel uses GBFrame's `QuickMenuHost`, with Flycast-backed save/load slots, video/audio/performance options, disc swap, restart, and per-game settings. Flycast's native Cheats and RetroAchievements screens remain available as fallback entries until app-specific adapters can use GBFrame's shared Cheats/RetroAchievements screens.
- Android Back during a game calls `gui_togglePause` and opens the quick menu. The system bars stay hidden (`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`). A controller B press that is also reported as Back is not treated as the menu while gameplay is in front.

Build the APK from `shell/android-studio` (`assembleRelease` or `assembleDebug`). The only GitHub Actions workflow is Android. `git` must be on `PATH` because the version name is `git describe`.

# Flycast

The emulation core comes from [Flycast](https://github.com/flyinghead/flycast), a Sega Dreamcast, Naomi, Naomi 2, and Atomiswave emulator derived from [reicast](https://github.com/skmp/reicast-emulator). This repository keeps the Android shell and the core code that APK builds. Desktop, Apple, Switch, UWP, and libretro projects are not part of GBCast.

Configuration notes for the core are on [TheArcadeStriker's flycast wiki](https://github.com/TheArcadeStriker/flycast-wiki/wiki).
