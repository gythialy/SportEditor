# SportEditor 运动修改器

<p align="center"><img src="design/preview/icon-512.png" width="120" alt="Sport Editor icon"></p>

[![CI](https://github.com/gythialy/SportEditor/actions/workflows/ci.yml/badge.svg)](https://github.com/gythialy/SportEditor/actions/workflows/ci.yml)
[![CodeQL](https://github.com/gythialy/SportEditor/actions/workflows/codeql.yml/badge.svg)](https://github.com/gythialy/SportEditor/actions/workflows/codeql.yml)
[![Release](https://github.com/gythialy/SportEditor/actions/workflows/release.yml/badge.svg)](https://github.com/gythialy/SportEditor/actions/workflows/release.yml)

基于 **libxposed（LSPosed 现代 Xposed API）** 的运动步数修改模块，使用 Kotlin 与 Jetpack Compose 编写。
支持为微信、支付宝、QQ、微博分别设置步数与修改方式。

> 当前版本 **1.0.0**，包名 **`io.github.gythialy.sporteditor`**，与旧的 `name.caiyao.sporteditor` 独立发展。
>
> 本程序仅供学习与测试，使用风险自负。

## 功能

- 基于 LSPosed 现代 API（`io.github.libxposed:api:102.0.0`），不使用已废弃的 `de.robv.android.xposed` API。
- 每个应用独立配置：开关、修改方式、步数互不影响。
- **所有应用默认关闭**，首次使用需在卡片上手动打开开关；只有已启用且在 LSPosed 作用域内的应用才会被修改。
- 四种修改方式：
  | 方式 | 说明 |
  | --- | --- |
  | 倍率 | 显示步数 = 实际步数 × 倍率 |
  | 增加 | 显示步数 = 实际步数 + 设定值 |
  | 自动增加 | 每次传感器回调递增设定值，累计到 100000 后重新开始（适合支付宝） |
  | 固定值 | 始终返回设定值；微信运动按增量同步，因此微信不提供此模式 |
- 设置写入 LSPosed **Remote Preferences**；目标应用运行/解冻后自动应用，通常无需重启。
- 设置页读取已安装应用的真实图标；未安装时回退到字母徽标并提示。
- 步数编辑提供 −/+ 步进器、按模式区分的快捷预设和实时结果预览。
- 每个应用可设置**每日步数上限**（默认 80,000），超过后不再增加；微信的 Fixed 模式因增量同步不可用，已禁用。
- 内置诊断页：环境与目标快照、框架/配置/Hook/错误四类日志、类型多选筛选、复制/分享/清空。
- Material 3 / 动态取色 / 深色模式 / edge-to-edge / 自适应图标。

## 支持的应用

| 应用 | 包名 |
| --- | --- |
| 微信 | `com.tencent.mm` |
| 支付宝 | `com.eg.android.AlipayGphone` |
| QQ | `com.tencent.mobileqq` |
| 微博 | `com.sina.weibo` |

作用域清单见 [scope.list](app/src/main/resources/META-INF/xposed/scope.list)。

## 环境要求

- Android 8.0（API 26）及以上，arm64 设备。
- 已安装并启用 **LSPosed 2.0+**（支持 libxposed API 101/102 的发行版，如 Vector / JingMatrix 维护版；官方 1.9.2 仅支持 API 100，无法加载本模块）。
- 设备已 root（Magisk / KernelSU / APatch 任一方案）。

## 构建

需要 JDK 17+（推荐 21）与 Android SDK Platform 37.1、Build-Tools 37.0.0。

```bash
# 可选：把 Gradle / Android 用户目录放在仓库内，避免污染 ~/.gradle
export GRADLE_USER_HOME="$PWD/.build-home"
export ANDROID_USER_HOME="$PWD/.android-home"

./gradlew :app:assembleDebug        # 产物：app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleRelease      # 混淆后的 release 包
./gradlew :app:testDebugUnitTest    # 步数策略单元测试
./gradlew :app:lintDebug            # Lint
./gradlew spotlessApply             # 格式化 Kotlin / Gradle KTS / Markdown / YAML
./gradlew spotlessCheck             # 只检查格式，不修改文件（CI 使用）

# 渲染 512×512 图标预览（启动图标本身是矢量 adaptive icon，需要 rsvg-convert）
scripts/tools/render-icons.sh
```

已验证的构建矩阵：AGP 9.2.1 · Gradle 9.8.0 · Kotlin 2.2.10 · compileSdk 37.1（compileSdkMinor 1）· JDK 21。
当前改动已通过 `spotlessCheck`、`assembleDebug`、`assembleRelease`（R8 + 私有 release 签名）、`testDebugUnitTest`（20/20）与 `lintDebug`（0 error，仅剩依赖版本提示）。

代码格式由 [Spotless](https://github.com/diffplug/spotless) + [ktlint](https://github.com/pinterest/ktlint) 统一管理，配置位于根目录 `build.gradle.kts` 与 `.editorconfig`；提交前请先运行 `./gradlew spotlessApply`。

## 测试

- `./gradlew :app:testDebugUnitTest`：20 个 JVM 单元测试（步数策略、上限裁剪、设置模型、取值范围、日志筛选）。
- 模拟器端到端验证（Magisk 27 + Vector 2.2 / API 102 + API 34 arm64 AVD）：
  模块识别、目标进程加载、Hook 安装、Remote Preferences 读取与在线更新、开关/Reset/停用均已实测通过；
  诊断页的环境快照、四类日志、多选筛选与清空也已实测。
  环境可用 [scripts/emulator](scripts/emulator/README.md) 一键重建，详细步骤与限制见
  [docs/emulator-test.md](docs/emulator-test.md)。
- **限制**：模拟器没有计步传感器，真实步数改写只能在真机验证；微信/支付宝也需要登录账号才能查看运动数据。

## CI 与发布

- **CI**：push / PR 自动执行 `spotlessCheck`、单元测试、`lintDebug`，并构建 debug 与 release APK（见 [.github/workflows/ci.yml](.github/workflows/ci.yml)）。
- **CI release 产物**：每次 CI 都把 `app-release.apk` 作为 `release-apk-ci` artifact 保留 14 天。该包在未配置 release secrets 时使用 debug 签名，仅用于测试，不能用于正式发布。
- **Release**：推送 `v*` tag 或手动触发，自动格式化检查、构建、签名、校验 APK，并创建 GitHub Release（见 [.github/workflows/release.yml](.github/workflows/release.yml)）；缺少签名 secrets 时任务会直接失败。
- **Dependabot**：每周检查 Gradle 依赖与 GitHub Actions 更新，minor/patch 合并为单个 PR（见 [.github/dependabot.yml](.github/dependabot.yml)）。
- **CodeQL**：push / PR / 每周定时对 Java/Kotlin 做静态安全分析（见 [.github/workflows/codeql.yml](.github/workflows/codeql.yml)）。
- **本地签名**：签名材料默认在仓库外的 `~/.vault`：
  - `~/.vault/sporteditor-release.jks`
  - `~/.vault/sporteditor-signing.properties`（权限 600）
- **GitHub Secrets**（手动创建）：
  | Secret | 说明 |
  | --- | --- |
  | `RELEASE_KEYSTORE_BASE64` | release keystore 的 base64 |
  | `RELEASE_KEYSTORE_PASSWORD` | keystore 密码 |
  | `RELEASE_KEY_ALIAS` | key alias（当前为 `sporteditor`） |
  | `RELEASE_KEY_PASSWORD` | key 密码 |

详细命令、base64 生成方式和备份说明见 [docs/release.md](docs/release.md)。`GITHUB_TOKEN` 由 Actions 自动提供。

## 安装与使用

1. 安装 APK，在 LSPosed 管理器中启用「运动修改器」。
2. 在作用域中勾选微信 / 支付宝 / QQ / 微博（模块已通过 `scope.list` 预置）。
3. 强制停止目标应用（或重启手机），使其加载模块。
4. 打开本应用，手动打开要修改的应用开关，再设置修改方式与步数；设置会实时同步。
5. 重新打开目标应用查看步数。

> 若本应用顶部提示「LSPosed 服务未连接」，说明模块尚未在 LSPosed 中启用，或框架版本过低。

## 实现原理

- 入口 `io.github.gythialy.sporteditor.hook.SportEditorModule` 继承 `io.github.libxposed.api.XposedModule`，
  通过 `META-INF/xposed/java_init.list` 注册；模块信息在 `META-INF/xposed/module.prop`。
- 在目标进程 hook `android.hardware.SystemSensorManager` 的监听注册，记录 `Sensor handle -> type` 映射；
  再 hook `SystemSensorManager$SensorEventQueue#dispatchSensorEvent`，仅改写
  `TYPE_STEP_COUNTER` / `TYPE_STEP_DETECTOR` 的值。
- 模块界面通过 `io.github.libxposed:service` 连接框架，把设置写入 Remote Preferences；
  目标进程用 `getRemotePreferences()` 读取并注册变更监听，在进程运行/解冻时应用新配置。
- 步数计算集中在纯 Kotlin 的 `StepTransformer`，有 JVM 单元测试覆盖。

## 目录结构

```
app/src/main/java/io/github/gythialy/sporteditor/
├── SportEditorApp.kt            # Application，Xposed 服务连接
├── data/                        # 目标应用、设置模型、Remote Preferences 仓库
├── debug/                       # 诊断日志存储、跨进程发送/接收
├── hook/                        # 现代 API 入口、传感器 Hook、步数策略
└── ui/                          # Compose 界面（设置 + 诊断）+ ViewModel
app/src/main/resources/META-INF/xposed/   # java_init.list / module.prop / scope.list
build.gradle.kts                          # Spotless / ktlint 格式化配置
.editorconfig                             # 编辑器与 ktlint 代码风格
design/icon.svg                           # 启动图标源文件
scripts/tools/render-icons.sh             # 渲染 512×512 图标预览
scripts/emulator/                         # root AVD / Magisk / Vector 一键脚本
docs/emulator-test.md                     # 模拟器测试环境与验证记录
docs/release.md                           # 本地签名 / GitHub Secrets / 发布流程
.github/workflows/ci.yml                  # 格式检查 + 单元测试 + Lint + debug/release 构建
.github/workflows/release.yml             # tag 发布签名、校验与 GitHub Release
.github/workflows/codeql.yml              # CodeQL 静态安全分析
.github/dependabot.yml                    # Gradle / Actions 每周依赖更新
```

## 声明

- 本项目仅用于学习 Android Hook 技术与测试，请勿用于任何违反应用服务条款或法律法规的用途。
- 使用本模块可能违反相关应用的用户协议，由此产生的一切后果由使用者自行承担。
