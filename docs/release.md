# 发布与签名

## 本地 release 签名

签名材料放在仓库外（默认 **~/.vault**，可用 `SPORTEDITOR_VAULT_DIR` 覆盖）：

```
~/.vault/sporteditor-release.jks
~/.vault/sporteditor-signing.properties   # 600 权限
```

`sporteditor-signing.properties` 格式：

```properties
storeFile=/Users/you/.vault/sporteditor-release.jks
storePassword=...
keyAlias=sporteditor
keyPassword=...
```

本机已生成上述文件，密码只保存在该 properties 文件中，**不要提交到 git**。

构建：

```bash
./gradlew spotlessApply        # 提交前格式化（Kotlin / KTS / Markdown / YAML）
./gradlew :app:assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
```

如果本地没有 `~/.vault` 签名配置，release 构建会回退到 debug 签名，仅用于开发调试；
发布工作流会设置 `REQUIRE_RELEASE_SIGNING=true`，缺少签名材料时直接失败，避免发布未签名的包。
CI 工作流不设置该变量，因此会产出 debug 签名的 `release-apk-ci` artifact（仅用于测试，不用于发布）。

> 请务必离线备份 `sporteditor-release.jks` 和 properties 文件。丢失私钥后，已安装用户无法覆盖升级。

## GitHub Actions

- `.github/workflows/ci.yml`：push / PR 时运行 `spotlessCheck`、单元测试、Lint，构建 debug 与 release APK，并上传 `release-apk-ci` artifact 与测试报告。
- `.github/workflows/release.yml`：推送 `v*` tag（或手动触发）时，用 secrets 解码 keystore、构建 release、校验签名并上传 APK；tag 构建会创建 GitHub Release。
- `.github/workflows/codeql.yml`：push / PR / 每周定时执行 CodeQL Java/Kotlin 静态分析。
- `.github/dependabot.yml`：每周检查 Gradle 依赖与 GitHub Actions 更新。

### 需要在仓库手动创建的 Secrets

| Secret | 值 |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | `base64 -i ~/.vault/sporteditor-release.jks`（Linux 用 `base64 -w0`） |
| `RELEASE_KEYSTORE_PASSWORD` | properties 中的 `storePassword` |
| `RELEASE_KEY_ALIAS` | `sporteditor` |
| `RELEASE_KEY_PASSWORD` | properties 中的 `keyPassword` |

`GITHUB_TOKEN` 由 Actions 自动提供，不需要手动创建。

### 触发发布

```bash
git tag v1.2.0
git push origin v1.2.0
```

或用 GitHub Actions 页面的 **Release → Run workflow** 手动构建（手动触发只上传 APK，不创建 Release）。
