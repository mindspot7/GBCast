# Local validation: shared data folder, monitoring and Android Back

- GBFrame is the source for `AppDataFolder`, `DataFolderSync`, the wizard/System
  controls and `MonitoringSettings` (main Video and quick-menu Overlays).
- GBCast mounts native `data` and library `covers`. Optional custom textures use
  `data/textures/<game ID>`; the Video toggle controls the native replacement flag.
  Games remain a separate read-only SAF tree. Preferences/account tokens are not
  copied. Native files use the local working copy and synchronize after writes.
- Tested on RG Rotate, ADB `ROTATE05716608`: selected `Documents/GBCastData` through
  the system picker, retained read/write permission after process restart, restored
  a fixture and synchronized a completed local write back to the selected folder.
  Renaming the selected folder produced a visible warning; restoring it and using
  Sync now completed the pending write. Local data was retained throughout.
- During Crazy Taxi: Android Back opened and retained the paused shared quick
  menu. Back from Overlays returned to the root. Enabling Show fast-forward button
  removed the root fast-forward action. The common monitoring toggles were present.
- Local app unit tests (18), shared synchronization/monitoring tests (10) and app-ID
  consistency passed. The adaptive icon removed the white backing on this launcher
  and used its black compatibility background.
- A real texture replacement pack was not provided; pack rendering is not claimed
  as visually validated. Cloud APK builds remain manual/explicit-trigger only.


## Direct authoritative data storage - build 43

- Shared GBFrame SAF Kotlin/native adapters replace the background mirror. The chosen tree is the actual data store. Ordinary startup does not hash/synchronize file contents.
- One-time migration verified destination data and retired the obsolete local native data and covers directories. Verified on RG Rotate that both physical local directories are absent after migration and after a native save.
- Existing saves/covers remain accessible. Crazy Taxi launched; saved to previously empty manual Slot 5, displayed its screenshot, and loaded that slot from Documents/GBCastData. Native VMU/NVRAM also use the selected tree.
- Renamed the selected folder temporarily while the app was stopped: startup reported unavailable storage and offered the shared chooser without opening the core or falling back to local files. Restored the folder afterwards.
- On-device instrumentation: DirectDataFolderTest verifies direct Kotlin/native reads/writes, external edits, metadata, replacement, listing, mmap, traversal rejection, no local mirror, and no fallback to a deliberately stale local save (2 tests).
- Local unit tests: 18 app tests and 12 shared tests, including default-storage File adapter regression checks. App-ID check passed.
- Existing destination files are retained when changing folders; unavailable previous storage is reported explicitly. Migration diagnostics/backups are under ignored test-output/direct-data-before on the PC.
- No new storage permissions. Uses ACTION_OPEN_DOCUMENT_TREE, persistent URI grants and ContentResolver descriptors; no MANAGE_EXTERNAL_STORAGE or guessed shared-storage paths.
- Texture directory access uses the same native adapter; a real replacement texture pack was not supplied for visual testing.
- Folder-change device test: used the Android picker to move from GBCastData to a fresh GBCastDirectCheck tree, reopened with the new tree, ran Check access, then selected the original GBCastData again. Existing states/covers and direct access survived; the temporary test tree was removed after verifying the restored selection.


## Shared System layout - build 44

- Uses GBFrame SystemSettings: Data (Data folder, Games folder, Bios folder), BIOS emulation/HLE, automatic save states, Loading Times, then Dreamcast-specific settings.
- Folder rows show effective paths when configured; the BIOS row lists dc_boot.bin and dc_flash.bin separately as Loaded/Missing and supports the existing boot .bak alternative. Missing firmware is not labelled Loaded because HLE is available.
- Removed duplicate Paths/Dreamcast BIOS groups and wizard paragraphs from System. The shared BIOS selection dialog offers folder import or individual files.
- Fast GD-ROM load moved from the main Performance page into Loading Times. No unsupported Saturn Skip Engine controls were added to GBCast.
- RG Rotate: verified the three Data rows, wrapped BIOS path and both Missing states (no Dreamcast BIOS installed), shared BIOS dialog/folder picker, HLE placement, Save states/Loading Times/Dreamcast order, and touch scrolling on 720x720.
- Local app tests (18), shared tests (14), and app-ID checks passed. No storage permissions or installation IDs changed.
