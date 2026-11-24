# 模拟器测试环境（macOS / Apple Silicon）

本文件记录 2026-10-22 在本机搭建的 LSPosed（Vector）模拟器验证环境，以及验证结论与限制。
所有下载、AVD、镜像补丁都放在工作区内（`.avd/`、`.emulator-cache/`、`.android-home/`），并已加入 `.gitignore`。

## 一键脚本

环境已脚本化，推荐直接使用（细节与手工步骤见下文）：

```bash
scripts/emulator/setup-avd.sh          # 创建 AVD、下载 Magisk/Vector、打 ramdisk 补丁并启动
# 在 Magisk 中给 "[SharedUID] Shell" 永久 root 授权
scripts/emulator/install-vector.sh     # 安装 Vector、管理器、模块 APK 和测试目标（会重启两次）
# 在 Vector 管理器中启用模块、勾选 com.tencent.mm 并 Apply
scripts/emulator/install-module.sh --verify   # 重装 APK + 查看 Hook 日志
```

脚本固定 Magisk 27.0 与 Vector 2.2（3080），下载后校验 SHA-256；缓存和 AVD 都在
`.emulator-cache/` 与 `.avd/` 中，不进入 git。

## 环境

| 组件 | 版本 / 位置 |
| --- | --- |
| 主机 | macOS 27.0.1，Apple Silicon（arm64），HVF 可用 |
| Emulator | 37.2.12（Android SDK 自带） |
| 系统镜像 | `system-images;android-UpsideDownCakePrivacySandbox;google_apis_playstore;arm64-v8a`（API 34） |
| AVD | `SportEditorTest`（工作区 `.avd/`，RAM 4 GB，data 8 GB） |
| Root | Magisk 27.0，通过 ramdisk 补丁注入（无需 Magisk 内置的 boot 镜像流程） |
| 框架 | Vector 2.2（3080，JingMatrix 维护的 LSPosed fork），API 102 |
| 管理器 | `org.matrix.vector.manager`（Vector zip 内的 manager.apk） |
| 目标应用 | 自建 `com.tencent.mm` 测试 APK（仅启动、注册 step counter/detector 并打印日志） |

## 搭建步骤

### 1. 工作区 AVD

复制现有 AVD 配置到工作区，避免写入 `~/.android`：

```bash
export ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT:-$HOME/Developer/android-sdk}
export ANDROID_AVD_HOME=$PWD/.avd
export ANDROID_USER_HOME=$PWD/.android-home
# config.ini 中修改 AvdId / hw.ramSize / image.sysdir.1，指向已安装的 API 34 镜像
```

启动时需要 `-grpc 8554`：否则 emulator 会尝试在 `$HOME/Library/Caches/TemporaryItems` 创建 JWT 目录，
在沙箱下会直接 FATAL。headless 启动：

```bash
emulator @SportEditorTest -no-window -no-audio -no-boot-anim \
  -no-snapshot -no-snapshot-save -gpu swiftshader_indirect \
  -memory 4096 -cores 4 -grpc 8554 \
  -ramdisk $PWD/.emulator-cache/ramdisk-patched-magisk27.img
```

### 2. Magisk ramdisk 补丁（headless）

1. 解压 Magisk 27.0 APK，取 `lib/arm64-v8a/libmagiskboot.so`、`libmagiskinit.so`、`libmagisk64.so`、`assets/stub.apk`。
2. 解压系统镜像的 `ramdisk.img`（LZ4 legacy）为 `ramdisk.cpio`。
3. 推送上述文件到 `/data/local/tmp/.magisk27`，在设备上执行：

```sh
cd /data/local/tmp/.magisk27
cp -af ramdisk.cpio ramdisk.cpio.orig
echo KEEPVERITY=true > config
echo KEEPFORCEENCRYPT=true >> config
echo PREINITDEVICE=metadata >> config
./magiskboot compress=xz magisk64 magisk64.xz
./magiskboot compress=xz stub.apk stub.xz
./magiskboot cpio ramdisk.cpio \
  "add 0750 init magiskinit" \
  "mkdir 0750 overlay.d" \
  "mkdir 0750 overlay.d/sbin" \
  "add 0644 overlay.d/sbin/magisk64.xz magisk64.xz" \
  "add 0644 overlay.d/sbin/stub.xz stub.xz" \
  "patch" "backup ramdisk.cpio.orig" \
  "mkdir 000 .backup" "add 000 .backup/.magisk config"
```

4. 拉回 `ramdisk.cpio`，`gzip -c` 成 `ramdisk-patched27.img`，用 `-ramdisk` 启动。

### 3. 安装 Magisk / Vector

1. `adb install Magisk-v27.0.apk`，打开 Magisk 完成 "Additional Setup" 并重启。
2. 通过 Magisk 的 Superuser 界面给 Shell 永久授权，或直接用 UI 授权 `adb shell`。
3. `adb push Vector-v2.2-3080-Release.zip /data/local/tmp/`，然后
   `su -c "magisk --install-module /data/local/tmp/Vector.zip"`，重启。
4. 如果管理器没有自动出现，手动安装 Vector zip 内的 `manager.apk`：
   `adb install manager.apk`（包名 `org.matrix.vector.manager`）。

### 4. 安装模块并配置作用域

1. `adb install -r app/build/outputs/apk/debug/app-debug.apk`（或 release APK，两者同签名）。
2. Vector 管理器 → Modules → Sport Editor → 打开开关。
3. 在作用域列表勾选目标应用（`scope.list` 中的应用会显示 "Fixed by the module"，仍需勾选并 Apply）。
4. 启动目标应用，logcat 过滤 `SportEditor`。

### 5. 自建测试目标（可选）

模拟器镜像没有计步传感器，微信/支付宝也无法在无账号环境下查看运动数据，因此用一个小型 APK
（包名 `com.tencent.mm`）验证模块加载与配置链路。它只注册 `TYPE_STEP_COUNTER` /
`TYPE_STEP_DETECTOR` 并打印结果，不包含任何业务逻辑。

## 验证结果

在 API 34 + Vector 2.2（API 102）上：

- Vector 正确识别模块：管理器显示 Sport Editor / API 102 / 描述来自 `android:description`。
- 目标进程日志：
  ```
  VectorModuleManager: Loading module io.github.gythialy.sporteditor
  SportEditor: loaded in com.tencent.mm: framework=Vector 2.2 (versionCode=3080, libApi=102)
  SportEditor: com.tencent.mm: config at install -> TargetSettings(enabled=true, mode=MULTIPLY, value=100)
  SportEditor: hooks installed for com.tencent.mm
  ```
- Remote Preferences：在模块界面把倍率改为 250 后，目标进程收到
  `config updated -> TargetSettings(enabled=true, mode=MULTIPLY, value=250)`；
  `/data/adb/lspd/config/modules_config.db` 的 `configs` 表出现 `wechat.value=250`。
- 开关/Reset：关闭微信后 `wechat.enabled=false`，重启目标进程读到 `enabled=false`；Reset 恢复
  `enabled=true, value=100`。
- 在管理器中停用模块后重启目标进程，不再出现 `VectorModuleManager: Loading module`；
  重新启用后恢复。
- 设置页：已安装目标显示 `PackageManager` 返回的真实图标，未安装时回退字母徽标并显示
  "Not installed"；步进器、模式预设和预览文案均能即时写入 Remote Preferences。
- 启动图标：自适应图标在系统"应用信息"页正常裁切显示（API 34），并带 Android 13 单色层。
- 诊断页：环境/目标快照正确；目标进程重启后出现 Hook 日志；修改倍率后出现
  "config updated"；按 框架/配置/Hook/错误 多选筛选、清空、重启后重新收集均正常。
  跨进程日志用显式广播 + Remote Preferences 中的随机 token 校验。
- Debug 与 Release（R8）APK 均验证通过；release 安装后同样加载、读取配置并安装 Hook。

## 限制（重要）

- **无法验证真实步数改写**：模拟器 goldfish 传感器 HAL 只提供加速度计、陀螺仪、磁力计、光线、
  气压、距离、温湿度，没有 `TYPE_STEP_COUNTER` / `TYPE_STEP_DETECTOR`。测试目标打印
  `counter=null`、`detector=null`，因此 `dispatchSensorEvent` 不会被触发，Hook 的实际数值改写
  只能在真机验证。数值算法本身由 `StepTransformerTest`（JVM 单元测试）覆盖。
- 微信/支付宝需要登录账号才能查看运动步数，模拟器环境不做真实账号验证。
- Magisk ramdisk 补丁依赖具体镜像/版本；换镜像或升级 Magisk 后需要重新验证。
