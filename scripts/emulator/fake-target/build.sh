#!/usr/bin/env bash
# Build a tiny com.tencent.mm test APK used to verify that the module loads and
# installs its hooks in a scoped process (the emulator has no step sensor).
set -eo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$ROOT"
# shellcheck source=../common.sh
. scripts/emulator/common.sh

need unzip
need javac
need zip

BUILD_TOOLS="$(ls -d "$SDK"/build-tools/* 2>/dev/null | sort -V | tail -n 1)"
PLATFORM="$(ls -d "$SDK"/platforms/android-* 2>/dev/null | sort -V | tail -n 1)"
[ -n "$BUILD_TOOLS" ] || die "no build-tools installed"
[ -n "$PLATFORM" ] || die "no platform installed"
ANDROID_JAR="$PLATFORM/android.jar"

WORK="$CACHE/fake-target-build"
OUT="$CACHE/fake-wechat.apk"
KEYSTORE="$CACHE/fake-target.keystore"
rm -rf "$WORK"
mkdir -p "$WORK/classes" "$WORK/dex"

log "compiling fake com.tencent.mm target"
"$BUILD_TOOLS/aapt2" link -o "$WORK/base.apk" -I "$ANDROID_JAR" \
  --manifest scripts/emulator/fake-target/AndroidManifest.xml \
  --min-sdk-version 26 --target-sdk-version 34

javac -source 17 -target 17 -classpath "$ANDROID_JAR" -d "$WORK/classes" \
  scripts/emulator/fake-target/src/com/tencent/mm/MainActivity.java 2>/dev/null

"$BUILD_TOOLS/d8" --lib "$ANDROID_JAR" --min-api 26 --output "$WORK/dex" \
  "$WORK/classes/com/tencent/mm/MainActivity.class"

cp "$WORK/base.apk" "$WORK/unsigned.apk"
( cd "$WORK" && zip -j -q unsigned.apk dex/classes.dex )
"$BUILD_TOOLS/zipalign" -f 4 "$WORK/unsigned.apk" "$WORK/aligned.apk"

if [ ! -f "$KEYSTORE" ]; then
  keytool -genkeypair -keystore "$KEYSTORE" -alias androiddebugkey \
    -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1
fi
"$BUILD_TOOLS/apksigner" sign --ks "$KEYSTORE" --ks-pass pass:android \
  --key-pass pass:android --ks-key-alias androiddebugkey "$WORK/aligned.apk"
cp "$WORK/aligned.apk" "$OUT"
log "fake target written to $OUT"
