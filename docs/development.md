# 开发文档

本文档覆盖 SportEditor 的架构、构建环境、测试与 CI/CD。面向使用与安装的快速说明见
[README](../README.md)。

## 技术栈

| 组件 | 版本 / 说明 |
| --- | --- |
| 语言 | Kotlin 2.4.21 |
| 构建 | AGP 9.4.1 · Gradle 9.8.1 · JDK 21 |
| Android | compileSdk 37.1（compileSdkMinor 1）· minSdk 26 · targetSdk 37 |
| UI | Jetpack Compose · Material 3 |
| Hook API | libxposed `io.github.libxposed:api:102.0.0` |
| 模块服务 | libxposed `service:102.0.0` |

## 架构与实现

- 入口 `io.github.sporteditor.hook.SportEditorModule` 继承
  `io.github.libxposed.api.XposedModule`，通过 `META-INF/xposed/java_init.list` 注册；
  模块信息在 `META-INF/xposed/module.prop`。
- 在目标进程 hook `android.hardware.SystemSensorManager` 的监听注册，记录
  `Sensor handle -> type` 映射；再 hook `SystemSensorManager$SensorEventQueue#dispatchSensorEvent`，
  仅改写 `TYPE_STEP_COUNTER` / `TYPE_STEP_DETECTOR` 的值。
- 模块界面通过 `io.github.libxposed:service` 连接框架，把每个应用的设置写入
  Remote Preferences；目标进程用 `getRemotePreferences()` 读取并注册变更监听，
  在进程运行/解冻时应用新配置。
- 步数计算集中在纯 Kotlin 的 `StepTransformer`，不依赖 Android，便于 JVM 单元测试。
- 上报给目标应用的是连续递增的绝对计数，每个本地自然日有独立额度，额度为
  `maxSteps + 0–100` 随机浮动。每天只转换当日 raw 增量（倍率/增加）、固定值或自动回调；
  **单日增量封顶**：`当日增量` 不会超过当日额度，而绝对计数继续累加，因此目标应用自己
  算出的「当日步数」每天都会从 0 正常增长到上限，不会在零点后一直停在上限。
  跨天时上一日报值原样作为新一天起点，只有接近 9,999,999 时才会归零（等价于传感器重置）。
  中途调低上限只会停止增长，不会让上报值回跳（回跳会被目标应用当成重置而冻结显示）。
- `TYPE_STEP_COUNTER` 是开机以来的累计值，系统重启后从 0 重新计数；模块用
  `SystemClock.elapsedRealtime()` 判定重启，把重启前的当日增量折进基线补偿，重启后的
  raw 直接计入当天，因此重启不会丢当日进度。`TYPE_STEP_DETECTOR` 每步一个 `1.0` 事件，
  按事件累加处理。
- 每日基线、当日额度与重启标记通过 `DailyStateStore` 持久化到目标应用的私有
  `sporteditor_daily_state` SharedPreferences，目标进程重启后可以继续当日累计；
  1.4.1 之前的版本（schema 0/1）的状态会在首次使用时迁移：保留当前上报值、把 raw 基线
  重定到当前读数，避免升级瞬间把整天增量一次性甩给目标应用。
- 所有应用默认关闭；微信的固定值模式会被 `AppTarget.sanitize()` 回退为默认倍率模式，
  因为微信运动按增量同步，固定值无法产生增量。
- 界面语言由 `AppLanguageStore` 保存在应用私有 SharedPreferences，`MainActivity.attachBaseContext`
  按所选 locale 包装 Context；设置页可在简体中文 / English 之间切换，默认简体中文。
- `UpdateChecker` 通过 GitHub Releases API 检查新版本（需要 `INTERNET` 权限），由设置页手动触发；
  版本比较由纯 Kotlin 的 `VersionComparator` 完成。
- 主界面只保留应用配置与设置入口；语言、版本更新、诊断与关于信息集中在设置页。
- 每个应用卡片在启用后可单独重置为默认值（二次确认），不存在全局重置。
- 目标进程把传感器事件以结构化 key=value 写入诊断日志，关键计算过程不再需要 logcat：
  `step sensor=STEP_COUNTER type=19 raw=2151.0 -> reported=29025.0 day=20756 base=2133.0 delta=18.0 incr=9000.0 limit=20025 day_start=20025.0 auto=0 schema=2 mode=MULTIPLY value=500 rollover=NONE held=false`。
  字段含义：`base` 当日 raw 基线，`delta` 当日 raw 增量（含重启补偿），`incr` 经每日上限
  封顶后的当日增量，`limit` 今日生效上限，`day_start` 跨天结转的绝对计数起点，`auto`
  AUTO 模式本次回调序号，`rollover` 为 `NEW_DAY / REBOOT / COUNTER_RESET / MIGRATION`
  的组合（无则 `NONE`），`held` 表示本次因回退保护沿用了上一次上报值。
  跨天/重启/重置/迁移时额外必记一条
  `step rollover sensor=... day=... reason=... raw=... base=... delta=... day_start=... limit=... schema=...`。
  每个进程前 20 条逐条记录，之后最多每 60 秒记一条 `step summary` 汇总（带最近一次事件的
  计算字段），避免刷满 500 条日志缓冲。
- 诊断页「目标应用」使用目标应用的真实启动器图标（未安装回退字母徽标），展开后显示
  `最近传感器`：类型、raw → reported、相对时间，以及第二行计算摘要
  `计算：delta=… incr=… limit=… day_start=… base=… auto=… rollover=… held=…`，
  数据来自该包最近一条结构化日志。
- 采集等级在断连期间修改会写入 `local_settings` 的 `pending_capture_level`，
  重连后推送一次并清除，避免被远程旧值覆盖。

## 目录结构

```text
app/src/main/java/io/github/sporteditor/
├── SportEditorApp.kt          # Application，Xposed 服务连接
├── data/                      # 目标应用、设置模型、Remote Preferences 仓库
├── debug/                     # 诊断日志存储、跨进程发送/接收
├── hook/                      # 现代 API 入口、传感器 Hook、步数策略
└── ui/                        # Compose 界面（设置 + 诊断）+ ViewModel
app/src/main/resources/META-INF/xposed/  # java_init.list / module.prop / scope.list
app/src/test/                            # JVM 单元测试
build.gradle.kts                         # Spotless / ktlint 配置
.github/workflows/                       # CI / CodeQL / Release
```

## 本地构建

```bash
export GRADLE_USER_HOME="$PWD/.build-home"    # 可选，避免污染 ~/.gradle
export ANDROID_USER_HOME="$PWD/.android-home"

./gradlew :app:assembleDebug                  # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleRelease                # 需 ~/.vault 签名，否则回退 debug 签名
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

release 签名材料与 GitHub Secrets 见 [发布与签名](release.md)。

## 代码格式

使用 [Spotless](https://github.com/diffplug/spotless) + [ktlint](https://github.com/pinterest/ktlint)：

```bash
./gradlew spotlessApply     # 格式化 Kotlin / Gradle KTS / Markdown / YAML
./gradlew spotlessCheck     # 只检查（CI 使用）
```

配置位于根目录 `build.gradle.kts` 与 `.editorconfig`；Compose 函数允许 PascalCase。
**提交前必须先执行 `spotlessApply`。**

## 测试

- JVM 单元测试：82 个用例，覆盖步数策略、每日上限与抖动、跨天/重启持久化、计算轨迹格式化与日志解析、语言选择、版本比较、设置模型、取值范围、日志筛选。
  ```bash
  ./gradlew :app:testDebugUnitTest
  ```
- 模拟器端到端：Magisk 27 + Vector 2.2（API 102）+ API 34 arm64 AVD，验证模块识别、
  目标进程加载、Hook 安装、Remote Preferences 读取与在线更新、开关/Reset/停用、诊断页。
  环境可用 [scripts/emulator](../scripts/emulator/README.md) 一键重建，详细步骤见
  [模拟器测试](emulator-test.md)。
- **限制**：模拟器没有计步传感器，真实步数改写只能在真机验证；微信/支付宝需要登录账号
  才能查看运动数据。

## CI / CD

| Workflow | 触发 | 内容 |
| --- | --- | --- |
| [ci.yml](../.github/workflows/ci.yml) | push / PR | `spotlessCheck`、单元测试、`lintDebug`、debug 与 release 构建；上传 `release-apk-ci`（14 天）与测试报告 |
| [codeql.yml](../.github/workflows/codeql.yml) | push / PR / 每周 | CodeQL Java/Kotlin 静态分析；构建时禁用 Gradle 缓存并强制重编译，确保提取到 Kotlin 代码 |
| [release.yml](../.github/workflows/release.yml) | `v*` tag / 手动 | 解码 secrets、构建并校验签名 APK，tag 构建创建 GitHub Release |

CI 的 release APK 在没有签名 secrets 时使用 debug 签名，仅用于测试；
release workflow 设置 `REQUIRE_RELEASE_SIGNING=true`，缺少签名材料会直接失败。

Dependabot 每周检查 Gradle 依赖与 GitHub Actions，配置见
[`.github/dependabot.yml`](../.github/dependabot.yml)。

## 已知限制

- 每日基线保存在目标应用的私有 SharedPreferences 中；清除目标应用数据或重装会重置基线。
- 绝对计数只在接近 9,999,999 时次日归零一次；目标应用会把这次回跳当成传感器重置。
- 中途调低每日上限只会停止增长，已经上报给目标应用的当日步数不会随之减少。
- 微信运动按增量同步，跨天边界与上限抖动仍需真机连续两天验证。
- 时区或系统日期大幅跳变时，当天基线可能被提前/推迟重置。
- 模拟器无法验证真实计步传感器。
