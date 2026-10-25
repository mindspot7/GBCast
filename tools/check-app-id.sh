#!/usr/bin/env bash
# Keep GBCast's Android application ID, Gradle namespace, launcher activity and device-test script aligned.
set -eu
cd "$(dirname "$0")/.."

gradle="shell/android-studio/flycast/build.gradle.kts"
manifest="shell/android-studio/flycast/src/main/AndroidManifest.xml"
device_tools="tools/gbcast.ps1"
fail=0

id=$(sed -n 's/^[[:space:]]*applicationId = "\([^"]*\)".*/\1/p' "$gradle")
ns=$(sed -n 's/^[[:space:]]*namespace = "\([^"]*\)".*/\1/p' "$gradle")
ps_app=$(awk -F"'" '/^\$App[[:space:]]*=/ {print $2}' "$device_tools")
ps_activity=$(awk -F"'" '/^\$Activity[[:space:]]*=/ {print $2}' "$device_tools")
manifest_activity=$(awk '
  /<activity([[:space:]>])/ { inside=1; name=""; main=0; launcher=0 }
  inside && match($0, /android:name="[^"]+"/) {
    name=substr($0, RSTART+14, RLENGTH-15)
  }
  inside && /android.intent.action.MAIN/ { main=1 }
  inside && /android.intent.category.LAUNCHER/ { launcher=1 }
  inside && /<\/activity>/ {
    if (main && launcher) { print name; exit }
    inside=0
  }
' "$manifest")

check() {
  if [ "$2" != "$3" ]; then
    printf "ERROR: %s is '%s', expected '%s'\n" "$1" "$2" "$3"
    fail=1
  fi
}

[ -n "$id" ] || { echo "ERROR: applicationId not found in $gradle"; exit 1; }
check "Gradle namespace" "$ns" "$id"
check "PowerShell \$App" "$ps_app" "$id"
check "PowerShell \$Activity / manifest launcher" "$ps_activity" "$manifest_activity"

# If GBCast later adds an app-specific APP_ID to CMake, keep it aligned too.
cmake="CMakeLists.txt"
cmake_app_id=$(sed -n 's/.*APP_ID[[:space:]]*"\([^"]*\)".*/\1/p' "$cmake" | head -1)
if [ -n "$cmake_app_id" ]; then
  check "CMake APP_ID" "$cmake_app_id" "$id"
fi

if [ "$fail" -eq 0 ]; then
  printf "OK: applicationId/namespace %s; launcher %s\n" "$id" "$manifest_activity"
fi
exit "$fail"
