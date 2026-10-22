# SportEditor 运动修改器

<p align="center">
  <img src="design/banner.png" alt="SportEditor — LSPosed 步数修改器" width="100%">
</p>

<p align="center">
  <a href="https://github.com/gythialy/SportEditor/actions/workflows/ci.yml"><img src="https://github.com/gythialy/SportEditor/actions/workflows/ci.yml/badge.svg" alt="CI"></a>
  <a href="https://github.com/gythialy/SportEditor/actions/workflows/codeql.yml"><img src="https://github.com/gythialy/SportEditor/actions/workflows/codeql.yml/badge.svg" alt="CodeQL"></a>
  <a href="https://github.com/gythialy/SportEditor/actions/workflows/release.yml"><img src="https://github.com/gythialy/SportEditor/actions/workflows/release.yml/badge.svg" alt="Release"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-yellow.svg" alt="License: MIT"></a>
</p>

基于 **libxposed（LSPosed 现代 Xposed API）** 的步数修改模块，使用 Kotlin 与 Jetpack Compose 编写。
可为微信、支付宝、QQ、微博分别设置步数、修改方式和每日上限。

## 功能

- **默认关闭，手动启用**：只修改已打开开关且在 LSPosed 作用域内的应用。
- **每个应用独立配置**：开关、修改方式、步数、每日上限互不影响。
- **四种修改方式**：倍率、增加、自动增加、固定值（微信因增量同步不支持固定值）。
- **每日步数上限**：默认 80,000，每天随机 +0–100；跨天自动重置，进程重启后保留当日进度。
- **实时生效**：设置写入 Remote Preferences，目标应用运行/解冻后自动应用，通常无需重启。
- **现代 API**：基于 libxposed API 102，不使用已废弃的 `de.robv.android.xposed` API。
- **内置诊断页**：环境快照 + 框架/配置/Hook/错误日志，支持筛选、复制、分享与清空。
- **Material 3**：动态取色、深色模式、edge-to-edge、自适应图标。
- **多语言**：默认简体中文，可在设置页手动切换 English。
- **检查更新**：设置页可手动从 GitHub Releases 检查新版本，并打开发布页。

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
- 已安装并启用 **LSPosed 2.0+**（支持 libxposed API 101/102 的发行版，如 Vector / JingMatrix 维护版；官方 1.9.2 仅支持 API 100）。
- 设备已 root（Magisk / KernelSU / APatch 任一方案）。

## 安装与使用

1. 安装 APK，在 LSPosed 管理器中启用「运动修改器」。
2. 在作用域中勾选微信 / 支付宝 / QQ / 微博（模块已通过 `scope.list` 预置）。
3. 强制停止目标应用（或重启手机），使其加载模块。
4. 打开本应用，手动打开要修改的应用开关，再设置修改方式、步数与上限；设置会实时同步。
5. 重新打开目标应用查看步数。

> 若本应用顶部提示「LSPosed 服务未连接」，说明模块尚未在 LSPosed 中启用，或框架版本过低。

## 构建

需要 JDK 21、Android SDK Platform 37.1 与 Build-Tools 37.0.0：

```bash
./gradlew :app:assembleDebug        # debug APK
./gradlew :app:assembleRelease      # R8 混淆后的 release APK
./gradlew :app:testDebugUnitTest    # 单元测试
./gradlew spotlessApply             # 提交前格式化
```

完整的开发环境、代码结构、测试与 CI 说明见 **[开发文档](docs/development.md)**。

## 文档

| 文档 | 内容 |
| --- | --- |
| [开发文档](docs/development.md) | 架构原理、目录结构、构建环境、测试与 CI/CD |
| [模拟器测试](docs/emulator-test.md) | root AVD + Magisk + Vector 的端到端验证环境 |
| [发布与签名](docs/release.md) | 本地签名、GitHub Secrets 与发布流程 |

## 许可证

本项目基于 [MIT License](LICENSE) 开源，Copyright © 2026 Goren G。

## 声明

本项目仅用于学习 Android Hook 技术与测试，请勿用于违反应用服务条款或法律法规的用途。
使用本模块可能违反相关应用的用户协议，由此产生的一切后果由使用者自行承担。
