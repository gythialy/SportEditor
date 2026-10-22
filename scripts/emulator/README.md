# scripts/emulator

Reproducible setup for the root-able Android emulator used to verify the module
end-to-end (Magisk 27 + Vector 2.2 / libxposed API 102). Everything is stored in
the repository workspace (ignored by git):

- `.avd/` – AVD and userdata
- `.emulator-cache/` – pinned Magisk / Vector downloads, patched ramdisk, fake target
- `.android-home/` – Android user home used by Gradle / emulator

## Usage

```bash
# 1. Requires an installed arm64 Play Store system image (API 34 recommended)
#    and lz4 (brew install lz4).
scripts/emulator/setup-avd.sh

# 2. Grant "[SharedUID] Shell" superuser in Magisk (Forever).

# 3. (optional) build the tiny com.tencent.mm test target first:
scripts/emulator/fake-target/build.sh

# 4. Install Vector, manager, module APK and the fake target (reboots twice):
scripts/emulator/install-vector.sh

# 5. In Vector manager: enable Sport Editor, check com.tencent.mm, Apply.

# 6. Reinstall the APK / inspect logs at any time:
scripts/emulator/install-module.sh --verify
```

Environment overrides: `ANDROID_SDK_ROOT`, `IMAGE_PATH`, `AVD_NAME`,
`HEADLESS=0`, `GPU_MODE`, `AVD_RAM_MB`, `ADB_SERIAL`, `FORCE_PATCH=1`.

## Notes

- The emulator must be started with `-grpc 8554`; otherwise it tries to write a
  JWT directory under `~/Library/Caches/TemporaryItems`, which fails in a
  workspace-write sandbox.
- The patched ramdisk is generated from the installed system image each time
  `FORCE_PATCH=1` is used; it is not committed.
- The goldfish sensor HAL has no step counter/detector, so the real value
  transformation can only be verified on a physical device. The fake target
  verifies module loading, scope, hook installation and remote preferences.
