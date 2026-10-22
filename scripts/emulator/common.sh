#!/usr/bin/env bash
# Shared configuration and helpers for the root-able emulator workflow.
# Everything is kept inside the repository: .avd/, .emulator-cache/, .android-home/.
set -eo pipefail

ROOT="$(git -C "$(dirname "$0")" rev-parse --show-toplevel 2>/dev/null || true)"
if [ -z "$ROOT" ]; then
  ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
fi

SDK="$ANDROID_SDK_ROOT"
[ -n "$SDK" ] || SDK="$ANDROID_HOME"
if [ -z "$SDK" ] && [ -f "$ROOT/local.properties" ]; then
  SDK="$(sed -n 's/^sdk\.dir=//p' "$ROOT/local.properties" | head -n 1)"
fi
if [ -z "$SDK" ]; then
  SDK="$HOME/Developer/android-sdk"
fi

if [ -z "$AVD_NAME" ]; then AVD_NAME=SportEditorTest; fi
if [ -z "$AVD_HOME" ]; then AVD_HOME="$ROOT/.avd"; fi
if [ -z "$CACHE" ]; then CACHE="$ROOT/.emulator-cache"; fi
if [ -z "$ANDROID_USER_HOME" ]; then ANDROID_USER_HOME="$ROOT/.android-home"; fi
if [ -z "$HEADLESS" ]; then HEADLESS=1; fi
if [ -z "$GPU_MODE" ]; then GPU_MODE=swiftshader_indirect; fi
if [ -z "$AVD_RAM_MB" ]; then AVD_RAM_MB=4096; fi
if [ -z "$AVD_CORES" ]; then AVD_CORES=4; fi

export ANDROID_SDK_ROOT="$SDK"
export ANDROID_HOME="$SDK"
export ANDROID_AVD_HOME="$AVD_HOME"
export ANDROID_USER_HOME
export PATH="$SDK/platform-tools:$SDK/emulator:$PATH"

ADB="$SDK/platform-tools/adb"
EMULATOR="$SDK/emulator/emulator"
PATCHED_RAMDISK="$CACHE/ramdisk-patched-magisk27.img"

MAGISK_APK="$CACHE/Magisk-v27.0.apk"
MAGISK_URL="https://github.com/topjohnwu/Magisk/releases/download/v27.0/Magisk-v27.0.apk"
MAGISK_SHA256="f511bd33d3242911d05b0939f910a3133ef2ba0e0ff1e098128f9f3cd0c16610"

VECTOR_ZIP="$CACHE/Vector-v2.2-3080-Release.zip"
VECTOR_URL="https://github.com/JingMatrix/Vector/releases/download/v2.2/Vector-v2.2-3080-Release.zip"
VECTOR_SHA256="9ee8323575d615f7b3f1076ff60b2a63a49390ef11881b52632311a37f6f79cc"
VECTOR_DIR="$CACHE/vector"
# shellcheck disable=SC2034 # consumed by install-vector.sh
VECTOR_MANAGER="$VECTOR_DIR/manager.apk"

log() { printf '\n[%s] %s\n' "$(date +%H:%M:%S)" "$*"; }
die() { printf 'error: %s\n' "$*" >&2; exit 1; }

need() {
  command -v "$1" >/dev/null 2>&1 || die "missing required tool: $1"
}

configured_sdk() {
  [ -x "$ADB" ] || die "adb not found at $ADB (set ANDROID_SDK_ROOT)"
  [ -x "$EMULATOR" ] || die "emulator not found at $EMULATOR"
}

sdk_image_dir() {
  if [ -n "$IMAGE_PATH" ]; then
    printf '%s\n' "$IMAGE_PATH"
    return
  fi
  local candidate
  candidate="$(ls -d "$SDK"/system-images/*/google_apis_playstore/arm64-v8a 2>/dev/null | sort | tail -n 1)"
  [ -n "$candidate" ] || die "no arm64 google_apis_playstore image under $SDK/system-images"
  printf '%s\n' "$candidate"
}

image_rel() {
  local dir
  dir="$(sdk_image_dir)"
  printf '%s\n' "$dir" | sed "s|^$SDK/||"
}

image_target() {
  local dir
  dir="$(sdk_image_dir)"
  basename "$(dirname "$(dirname "$dir")")"
}

adb_cmd() {
  if [ -n "$ADB_SERIAL" ]; then
    "$ADB" -s "$ADB_SERIAL" "$@"
  else
    "$ADB" "$@"
  fi
}

wait_for_boot() {
  local limit="$1"
  [ -n "$limit" ] || limit=420
  local elapsed=0
  log "waiting for Android boot (max $limit s)"
  while [ "$elapsed" -lt "$limit" ]; do
    if [ "$(adb_cmd shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; then
      log "boot completed"
      return 0
    fi
    sleep 3
    elapsed=$((elapsed + 3))
  done
  die "emulator did not boot within $limit s"
}

emulator_running() {
  adb_cmd get-state >/dev/null 2>&1
}

stop_emulator() {
  if emulator_running; then
    log "stopping emulator"
    adb_cmd emu kill >/dev/null 2>&1 || true
    sleep 2
  fi
  pkill -f "qemu-system-aarch64" 2>/dev/null || true
  sleep 2
}

start_emulator() {
  local ramdisk="$1"
  configured_sdk
  set -- "@$AVD_NAME" -no-audio -no-boot-anim -no-snapshot -no-snapshot-save \
    -gpu "$GPU_MODE" -memory "$AVD_RAM_MB" -cores "$AVD_CORES" \
    -grpc 8554 -netdelay none -netspeed full
  if [ "$HEADLESS" = "1" ]; then
    set -- "$@" -no-window
  fi
  if [ -n "$ramdisk" ]; then
    set -- "$@" -ramdisk "$ramdisk"
  fi
  mkdir -p "$CACHE"
  log "starting emulator @$AVD_NAME (log: $CACHE/emulator.log)"
  "$EMULATOR" "$@" >"$CACHE/emulator.log" 2>&1 &
}

ensure_avd() {
  configured_sdk
  need unzip
  need curl
  need gzip
  need lz4
  mkdir -p "$AVD_HOME" "$CACHE"
  local target image skin config ini
  target="$(image_target)"
  image="$(image_rel)"
  skin="$SDK/skins/pixel_7_pro"
  config="$AVD_HOME/$AVD_NAME.avd/config.ini"
  ini="$AVD_HOME/$AVD_NAME.ini"
  if [ ! -f "$config" ]; then
    log "creating AVD $AVD_NAME from $image"
    mkdir -p "$AVD_HOME/$AVD_NAME.avd"
    sed -e "s|@AVD_NAME@|$AVD_NAME|g" \
        -e "s|@IMAGE_REL@|$image|g" \
        -e "s|@TARGET@|$target|g" \
        -e "s|@SKIN_PATH@|$skin|g" \
        "$ROOT/scripts/emulator/avd-config.ini.template" >"$config"
    if [ ! -d "$skin" ]; then
      grep -v '^skin.path=' "$config" >"$config.tmp" && mv "$config.tmp" "$config"
    fi
  fi
  if [ ! -f "$ini" ]; then
    printf 'avd.ini.encoding=UTF-8\npath=%s\npath.rel=avd/%s.avd\ntarget=%s\n' \
      "$AVD_HOME/$AVD_NAME.avd" "$AVD_NAME" "$target" >"$ini"
  fi
}

fetch_verified() {
  local url="$1" out="$2" sha="$3"
  if [ -f "$out" ] && printf '%s  %s\n' "$sha" "$out" | shasum -a 256 -c - >/dev/null 2>&1; then
    log "using cached $(basename "$out")"
    return
  fi
  log "downloading $(basename "$out")"
  curl --fail --location --retry 3 --output "$out" "$url"
  printf '%s  %s\n' "$sha" "$out" | shasum -a 256 -c -
}

download_artifacts() {
  fetch_verified "$MAGISK_URL" "$MAGISK_APK" "$MAGISK_SHA256"
  fetch_verified "$VECTOR_URL" "$VECTOR_ZIP" "$VECTOR_SHA256"
  mkdir -p "$VECTOR_DIR"
  unzip -o -q "$VECTOR_ZIP" manager.apk -d "$VECTOR_DIR"
}

patch_ramdisk() {
  local image_dir work magisk_dir
  image_dir="$(sdk_image_dir)"
  work="$CACHE/ramdisk-work"
  magisk_dir="$work/magisk"
  rm -rf "$work"
  mkdir -p "$magisk_dir"
  log "unpacking $image_dir/ramdisk.img"
  lz4 -d -c "$image_dir/ramdisk.img" >"$work/ramdisk.cpio"
  unzip -o -q "$MAGISK_APK" \
    'lib/arm64-v8a/libmagiskboot.so' \
    'lib/arm64-v8a/libmagiskinit.so' \
    'lib/arm64-v8a/libmagisk64.so' \
    'assets/stub.apk' \
    -d "$magisk_dir"

  adb_cmd shell 'rm -rf /data/local/tmp/.magisk-setup && mkdir -p /data/local/tmp/.magisk-setup'
  adb_cmd push "$magisk_dir/lib/arm64-v8a/libmagiskboot.so" /data/local/tmp/.magisk-setup/magiskboot
  adb_cmd push "$magisk_dir/lib/arm64-v8a/libmagiskinit.so" /data/local/tmp/.magisk-setup/magiskinit
  adb_cmd push "$magisk_dir/lib/arm64-v8a/libmagisk64.so" /data/local/tmp/.magisk-setup/magisk64
  adb_cmd push "$magisk_dir/assets/stub.apk" /data/local/tmp/.magisk-setup/stub.apk
  adb_cmd push "$work/ramdisk.cpio" /data/local/tmp/.magisk-setup/ramdisk.cpio

  cat >"$work/patch.sh" <<'PATCH'
#!/system/bin/sh
cd /data/local/tmp/.magisk-setup || exit 1
chmod 755 magiskboot magisk64 magiskinit
cp -af ramdisk.cpio ramdisk.cpio.orig
PREINIT=$(./magisk64 --preinit-device)
{
  echo KEEPVERITY=true
  echo KEEPFORCEENCRYPT=true
  echo "PREINITDEVICE=$PREINIT"
} > config
./magiskboot compress=xz magisk64 magisk64.xz
./magiskboot compress=xz stub.apk stub.xz
KEEPVERITY=true KEEPFORCEENCRYPT=true ./magiskboot cpio ramdisk.cpio \
  "add 0750 init magiskinit" \
  "mkdir 0750 overlay.d" \
  "mkdir 0750 overlay.d/sbin" \
  "add 0644 overlay.d/sbin/magisk64.xz magisk64.xz" \
  "add 0644 overlay.d/sbin/stub.xz stub.xz" \
  "patch" \
  "backup ramdisk.cpio.orig" \
  "mkdir 000 .backup" \
  "add 000 .backup/.magisk config"
PATCH
  adb_cmd push "$work/patch.sh" /data/local/tmp/.magisk-setup/patch.sh
  adb_cmd shell 'chmod 755 /data/local/tmp/.magisk-setup/patch.sh && sh /data/local/tmp/.magisk-setup/patch.sh'
  adb_cmd pull /data/local/tmp/.magisk-setup/ramdisk.cpio "$work/ramdisk-patched.cpio"
  gzip -c "$work/ramdisk-patched.cpio" >"$PATCHED_RAMDISK"
  log "patched ramdisk written to $PATCHED_RAMDISK"
}
