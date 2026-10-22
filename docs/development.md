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

- 入口 `io.github.gythialy.sporteditor.hook.SportEditorModule` 继承
  `io.github.libxposed.api.XposedModule`，通过 `META-INF/xposed/java_init.list` 注册；
  模块信息在 `META-INF/xposed/module.prop`。
- 在目标进程 hook `android.hardware.SystemSensorManager` 的监听注册，记录
  `Sensor handle -> type` 映射；再 hook `SystemSensorManager$SensorEventQueue#dispatchSensorEvent`，
  仅改写 `TYPE_STEP_COUNTER` / `TYPE_STEP_DETECTOR` 的值。
- 模块界面通过 `io.github.libxposed:service` 连接框架，把每个应用的设置写入
  Remote Preferences；目标进程用 `getRemotePreferences()` 读取并注册变更监听，
  在进程运行/解冻时应用新配置。
- 步数计算集中在纯 Kotlin 的 `StepTransformer`，不依赖 Android，便于 JVM 单元测试。
- 每日基线与当日上限通过 `DailyStateStore` 持久化到目标应用的私有
  `sporteditor_daily_state` SharedPreferences，目标进程重启后可以继续当日累计。
- 所有应用默认关闭；微信的固定值模式会被 `AppTarget.sanitize()` 回退为默认倍率模式，
  因为微信运动按增量同步，固定值无法产生增量。

## 目录结构

```text
app/src/main/java/io/github/gythialy/sporteditor/
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

- JVM 单元测试：20 个用例，覆盖步数策略、每日上限裁剪、设置模型、取值范围、日志筛选。
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
- 上报的绝对值会按天累加，长期运行最终受 `9,999,999` 保护上限限制。
- 微信运动按增量同步，跨天边界与上限抖动仍需真机连续两天验证。
- 时区或系统日期大幅跳变时，当天基线可能被提前/推迟重置。
- 模拟器无法验证真实计步传感器。
