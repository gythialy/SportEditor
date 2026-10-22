#!/usr/bin/env bash
# Install Vector (LSPosed fork, API 102), its manager, the module APK and the
# fake com.tencent.mm test target. Requires an already rooted AVD.
set -eo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
# shellcheck source=common.sh
. scripts/emulator/common.sh

APK="$1"
[ -n "$APK" ] || APK="app/build/outputs/apk/debug/app-debug.apk"
[ -f "$APK" ] || die "module APK not found: $APK"
download_artifacts
emulator_running || die "start the AVD first: scripts/emulator/run-avd.sh"

if ! adb_cmd shell 'su -c id' 2>/dev/null | grep -q 'uid=0'; then
  die "Shell does not have superuser yet; grant it in Magisk -> Superuser"
fi

log "installing Vector as a Magisk module"
adb_cmd push "$VECTOR_ZIP" /data/local/tmp/Vector.zip
adb_cmd shell 'su -c "magisk --install-module /data/local/tmp/Vector.zip"'

# First boot lets Magisk finish the module upgrade and Vector install its
# sepolicy rules; the second boot then scans manager + module with a clean state.
adb_cmd reboot
sleep 15
wait_for_boot 600

log "installing Vector manager and module APK"
adb_cmd install -r "$VECTOR_MANAGER" >/dev/null
adb_cmd install -r "$APK" >/dev/null

adb_cmd reboot
sleep 15
wait_for_boot 600

log "installing fake target"
if [ -f "$CACHE/fake-wechat.apk" ]; then
  # The test APK is self-signed; an older copy may use another debug key.
  adb_cmd uninstall com.tencent.mm >/dev/null 2>&1 || true
  adb_cmd install "$CACHE/fake-wechat.apk" >/dev/null
fi

adb_cmd shell am start -n org.matrix.vector.manager/.ui.MainActivity >/dev/null 2>&1 || true
cat <<'DONE'

Next (one-time, in the emulator):
  1. Vector manager -> Modules -> Sport Editor -> enable the toggle.
  2. In the scope list, check com.tencent.mm ("Fixed by the module" still needs
     a checkbox) and tap Apply.
  3. Verify: scripts/emulator/install-module.sh --verify

DONE
