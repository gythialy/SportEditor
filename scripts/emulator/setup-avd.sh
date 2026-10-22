#!/usr/bin/env bash
# Create the workspace AVD, inject Magisk into its ramdisk and boot it.
# Idempotent: re-running reuses the AVD, cached artifacts and the patched ramdisk.
set -eo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
# shellcheck source=common.sh
. scripts/emulator/common.sh

ensure_avd
download_artifacts

if emulator_running && [ -f "$PATCHED_RAMDISK" ] && [ "$FORCE_PATCH" != "1" ]; then
  log "emulator already running with the patched ramdisk"
  exit 0
fi

if [ -f "$PATCHED_RAMDISK" ] && [ "$FORCE_PATCH" != "1" ]; then
  log "patched ramdisk already exists (set FORCE_PATCH=1 to rebuild)"
else
  log "booting the unpatched AVD to generate the Magisk ramdisk"
  stop_emulator
  start_emulator ""
  wait_for_boot 600
  patch_ramdisk
  stop_emulator
fi

start_emulator "$PATCHED_RAMDISK"
wait_for_boot 600

adb_cmd install -r "$MAGISK_APK" >/dev/null 2>&1 || true
adb_cmd shell am start -n com.topjohnwu.magisk/.ui.MainActivity >/dev/null 2>&1 || true

cat <<'DONE'

Magisk is installed but Shell still needs superuser access:

  1. In the emulator, open Magisk and complete "Additional Setup" (it reboots).
  2. Open Magisk -> Superuser and enable "[SharedUID] Shell" (or run
     adb shell "su -c id" and tap GRANT -> Forever).
  3. Verify: adb shell "su -c id" prints uid=0.
  4. Run: scripts/emulator/install-vector.sh

DONE
