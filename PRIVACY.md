# GBCast — Privacy Policy

_Last updated: October 26, 2026_

GBCast is an emulator for Android. GBCast has no ads, analytics or user tracking, and does not sell personal data.

## Data kept on your device

GBCast keeps app settings, save data and save states, cheats, play time, downloaded cover images, the BIOS files you
select, and the location of your games folder on the device. Game images are read from the folder you choose and are
not uploaded by the library. If you use RetroAchievements, the app also keeps the login token supplied by that service
and locally queues casual-mode achievement unlocks while offline.

Android may include eligible app-private files in its platform backup when device backup is enabled. Clearing the app's
data or uninstalling it removes app-private data, except for files that you exported or otherwise placed in a folder of
your choice.

## Optional network connections

- **RetroAchievements** (`retroachievements.org` and its media service): used only when you enable the feature and sign
  in. Login credentials are sent to RetroAchievements; the password is not stored by GBCast. Later requests can include
  the issued token, a game-disc hash, profile information and achievement unlocks.
- **Cover art** (`thumbnails.libretro.com`): requested when a game has no local cover; the normalized game title is part
  of the image address.
- **Online and network-emulation features**: broadband-adapter emulation, Naomi networking, battle cable and GGPO can
  communicate with hosts selected or configured by the user when enabled.
- **Crash reports**: if **Upload crash logs** is enabled, a native crash dump and GBCast/Flycast log can be sent to the
  crash-report endpoint compiled into that build. The setting can be disabled in the app.

Those remote services can receive standard connection information such as the device's IP address and have their own
privacy practices.

## Permissions

- **Internet**: for the optional services and network features listed above.
- **Folder/file access**: Android's system picker grants access only to game, BIOS, cheat or other files and folders the
  user selects.

## Children

GBCast is not directed at children and does not knowingly collect personal data from them.

## Changes and contact

Policy changes are published in this source repository. Questions can be opened at
<https://github.com/mindspot7/GBCast/issues>.
