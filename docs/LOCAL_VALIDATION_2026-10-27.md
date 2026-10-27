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
