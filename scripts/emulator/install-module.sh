#!/usr/bin/env bash
# Install (or reinstall) the module APK, restart target processes and show logs.
set -eo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
# shellcheck source=common.sh
. scripts/emulator/common.sh

APK="app/build/outputs/apk/debug/app-debug.apk"
VERIFY=0
for arg in "$@"; do
  case "$arg" in
    --verify) VERIFY=1 ;;
    *.apk) APK="$arg" ;;
    *) die "unknown argument: $arg" ;;
  esac
done
[ -f "$APK" ] || die "APK not found: $APK"
emulator_running || die "start the AVD first: scripts/emulator/run-avd.sh"

log "installing $APK"
adb_cmd install -r "$APK" >/dev/null

for pkg in com.tencent.mm com.eg.android.AlipayGphone com.tencent.mobileqq com.sina.weibo; do
  adb_cmd shell am force-stop "$pkg" >/dev/null 2>&1 || true
done
adb_cmd shell am start -n io.github.sporteditor/.ui.MainActivity >/dev/null 2>&1 || true

if [ "$VERIFY" = "1" ]; then
  log "launching fake target and filtering logs"
  adb_cmd logcat -c
  adb_cmd shell am force-stop com.tencent.mm >/dev/null 2>&1 || true
  adb_cmd shell monkey -p com.tencent.mm -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
  sleep 5
  adb_cmd logcat -d | grep -E 'SportEditor|VectorModuleManager|FakeWeChat' | tail -n 30
fi
