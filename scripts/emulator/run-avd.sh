#!/usr/bin/env bash
# Boot the prepared root-able AVD. Requires setup-avd.sh to have run once.
set -eo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
# shellcheck source=common.sh
. scripts/emulator/common.sh

[ -f "$PATCHED_RAMDISK" ] || die "run scripts/emulator/setup-avd.sh first"
if emulator_running; then
  log "emulator already running"
  exit 0
fi

start_emulator "$PATCHED_RAMDISK"
if [ "$NO_WAIT" = "1" ]; then
  log "emulator started in background; tail $CACHE/emulator.log"
else
  wait_for_boot 600
fi
