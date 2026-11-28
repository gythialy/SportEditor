# SportEditor 运动修改器

基于 **libxposed（LSPosed 现代 Xposed API）** 的步数修改模块，可为微信、支付宝、QQ、微博分别设置步数、修改方式和每日上限。每个应用的开关与规则互相独立，默认关闭，只有手动启用并纳入 LSPosed 作用域的应用才会被修改。

## 快速入口

| 入口 | 说明 |
| --- | --- |
| [使用手册](user-guide.md) | 安装后的配置步骤、四种修改方式与常见问题 |
| [下载安装](https://github.com/gythialy/SportEditor/releases) | 从 GitHub Releases 获取最新 APK；签名与双通道发布见[发布与签名](release.md) |
| [开发文档](development.md) | 架构原理、目录结构、构建环境、测试与 CI/CD |
| [设计系统](design/README.md) | 当前落地的色彩、字阶、形状、动效与组件规格 |

## 特性摘要

- **独立配置**：每个目标应用单独设置开关、修改方式、步数与每日上限，互不影响。
- **四种修改方式**：倍率、增加、自动、固定（微信不提供固定）。
- **每日上限**：以本地自然日为周期累计并封顶；上报值跨天保持连续，目标应用每天从 0 重新累计，零点不会回跳。
- **实时生效**：设置写入 Remote Preferences，目标应用运行或解冻后自动应用，通常无需重启；当日进度与随机额度持久化，进程或设备重启后继续。
- **现代 API**：基于 libxposed API 102，不使用已废弃的 `de.robv.android.xposed` API。
- **Material 3**：动态取色、跟随系统/浅色/深色主题、edge-to-edge 与自适应图标。

=== "支持的应用"

| 应用 | 包名 |
| --- | --- |
| 微信 | `com.tencent.mm` |
| 支付宝 | `com.eg.android.AlipayGphone` |
| QQ | `com.tencent.mobileqq` |
| 微博 | `com.sina.weibo` |

=== "启用步骤"

1. 安装 APK，在 LSPosed 管理器中启用「运动修改器」。
2. 在作用域中勾选微信 / 支付宝 / QQ / 微博（模块已通过 `scope.list` 预置）。
3. 强行停止目标应用后重新打开，使其加载模块（首次启用或升级后必做）。
4. 打开本应用，手动打开目标应用的开关并设置规则。
5. 重新打开目标应用查看步数。

!!! warning "环境要求"

    - Android 8.0（API 26）及以上，arm64 设备。
    - 已安装并启用 **LSPosed 2.0+**：需支持 libxposed API 101/102 的发行版（如 Vector / JingMatrix 维护版）；官方 1.9.2 仅支持 API 100。
    - 设备已 root（Magisk / KernelSU / APatch 任一方案）。

!!! note "声明"

    本项目仅用于学习 Android Hook 技术与测试，请勿用于违反应用服务条款或法律法规的用途。使用本模块可能违反相关应用的用户协议，由此产生的一切后果由使用者自行承担。
