# GBCast

Android shell for the Flycast core (Dreamcast, Naomi, Naomi 2 and Atomiswave) using the shared [GBFrame](https://github.com/mindspot7/GBFrame) library. The launcher is the GBCast game library. `NativeGLActivity` still renders with Flycast's existing GLES/Vulkan path.

GBFrame is the git submodule `libs/gbframe` (commit `1f869337e6dbf67e039ee2752aa99ca459bc23b0` on `main`). The application id stays `com.flycast.emulator`. `minSdk` is **28** because that is GBFrame's minimum; the previous Flycast Android app allowed API 21.

## Android library

- Showcase, Grid and All in One come from GBFrame. Theme in the main menu switches them. The last selected game is restored by `LibraryView`.
- The games folder is a persisted Storage Access Framework tree. Indexed extensions: gdi, cdi, chd, cue, lst, zip, 7z, dat, bin. BIN/RAW/ISO tracks next to a GDI or CUE are not listed as separate games. GDI and CUE are staged with their sibling tracks; CDI, CHD and archives are copied to a real cache file because Flycast opens them by path.
- Covers use libretro-thumbnails `Sega - Dreamcast`.
- Settings in the library write Flycast `config::Option` values and call `SaveSettings`. Per-game overrides, cheats, RetroAchievements sign-in and the full button map stay in Flycast's own UI, opened from the in-game quick menu.
- Android Back during a game calls `gui_togglePause` and opens the quick menu. The system bars stay hidden (`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`). A controller B press that is also reported as Back is not treated as the menu while gameplay is in front.

Build the APK from `shell/android-studio` (`assembleRelease` or `assembleDebug`). The existing Android, desktop, Switch and UWP workflows are unchanged apart from the Android build now compiling this UI. `git` must be on `PATH` because the version name is `git describe`.

# Flycast

[![Android CI](https://github.com/flyinghead/flycast/actions/workflows/android.yml/badge.svg)](https://github.com/flyinghead/flycast/actions/workflows/android.yml)
[![C/C++ CI](https://github.com/flyinghead/flycast/actions/workflows/c-cpp.yml/badge.svg)](https://github.com/flyinghead/flycast/actions/workflows/c-cpp.yml)
[![Nintendo Switch CI](https://github.com/flyinghead/flycast/actions/workflows/switch.yml/badge.svg)](https://github.com/flyinghead/flycast/actions/workflows/switch.yml)
[![Windows UWP CI](https://github.com/flyinghead/flycast/actions/workflows/uwp.yml/badge.svg)](https://github.com/flyinghead/flycast/actions/workflows/uwp.yml)
[![BSD CI](https://github.com/flyinghead/flycast/actions/workflows/bsd.yml/badge.svg)](https://github.com/flyinghead/flycast/actions/workflows/bsd.yml)

<img src="shell/linux/flycast.png" alt="flycast logo" width="150"/>

**Flycast** is a multi-platform Sega Dreamcast, Naomi, Naomi 2, and Atomiswave emulator derived from [**reicast**](https://github.com/skmp/reicast-emulator).

Information about configuration and supported features can be found on [**TheArcadeStriker's flycast wiki**](https://github.com/TheArcadeStriker/flycast-wiki/wiki).

Join us on our [**Discord server**](https://discord.gg/X8YWP8w) for a chat.

## Downloads ![android](https://flyinghead.github.io/flycast-builds/android.jpg) ![windows](https://flyinghead.github.io/flycast-builds/windows.png) ![linux](https://flyinghead.github.io/flycast-builds/ubuntu.png) ![apple](https://flyinghead.github.io/flycast-builds/apple.png) ![switch](https://flyinghead.github.io/flycast-builds/switch.png) ![xbox](https://flyinghead.github.io/flycast-builds/xbox.png)

Get builds for your system from the [**builds page**](https://flyinghead.github.io/flycast-builds/) or [**GitHub Releases**](https://github.com/flyinghead/flycast/releases).

- **Latest master builds:** regular builds from the `master` branch with recent fixes and updates.
- **Nightly dev builds:** experimental builds with the latest features and changes.
- **Stable tagged releases:** versioned release builds published on GitHub Releases.

Automated test results are available from the builds page as well.

## Install

### Android ![android](https://flyinghead.github.io/flycast-builds/android.jpg)

&emsp;Install Flycast from [**Google Play**](https://play.google.com/store/apps/details?id=com.flycast.emulator).

### Flatpak (Linux ![ubuntu logo](https://flyinghead.github.io/flycast-builds/ubuntu.png))

1. [Set up Flatpak](https://www.flatpak.org/setup/).

2. Install Flycast from [Flathub](https://flathub.org/apps/details/org.flycast.Flycast):

&emsp;`flatpak install -y org.flycast.Flycast`

3. Run Flycast:

&emsp;`flatpak run org.flycast.Flycast`

### Homebrew (macOS ![apple logo](https://flyinghead.github.io/flycast-builds/apple.png))

1. [Set up Homebrew](https://brew.sh) or run `brew update` if already installed.

2. Choose one channel:

| Channel              | Install command                                         |
| -------------------- | ------------------------------------------------------- |
| Master (recommended) | `brew install --cask flyinghead/flycast/flycast@master` |
| Stable               | `brew install --cask flyinghead/flycast/flycast`        |
| Nightly dev          | `brew install --cask flyinghead/flycast/flycast@dev`    |

3. Run Flycast from your Application folder

&emsp;See the <a href="https://github.com/flyinghead/homebrew-flycast#readme">Flycast tap</a> for updating, uninstalling, and switching channels.

### iOS

&emsp;Due to persistent harassment from an iOS user, support for this platform has been dropped.

### Xbox One/Series ![xbox logo](https://flyinghead.github.io/flycast-builds/xbox.png)

&emsp;Grab the latest build from [**the builds page**](https://flyinghead.github.io/flycast-builds/), or the [**GitHub Actions**](https://github.com/flyinghead/flycast/actions/workflows/uwp.yml). Then install it using the **Xbox Device Portal**.

## Build from source

### macOS

&emsp;Right-click the bootstrap script and choose **Open**:

&emsp;`shell/apple/generate_xcode_project.command`

### Windows

&emsp;Double-click the bootstrap script:

&emsp;`shell\windows\generate_vs_project.bat`

### Linux

#### Dependencies

- **C/C++ compiler toolchain** (e.g. `gcc`/`g++`)
- **CMake**
- **make**
- **libcurl** (development headers)
- **libudev** (development headers)
- **SDL2** (development headers)
- **Graphics API**: Vulkan, OpenGL

#### Build

```
$ git clone --recursive https://github.com/flyinghead/flycast.git
$ cd flycast
$ mkdir build && cd build
$ cmake ..
$ make
```

## Packaging status

[![Packaging status](https://repology.org/badge/vertical-allrepos/flycast.svg)](https://repology.org/project/flycast/versions)
