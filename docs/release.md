# 发布与签名

每次发版会同时产出**两个 release**，缺一不可：

| 渠道 | 谁在读 | 产物 |
| --- | --- | --- |
| `gythialy/SportEditor` 的 GitHub Release | 应用内「设置 → 检查更新」（`UpdateChecker`） | `SportEditor-<tag>.apk` |
| `Xposed-Modules-Repo/io.github.gythialy.sporteditor` 的 GitHub Release | LSPosed / Vector 的模块列表与更新、modules.lsposed.org | 同一份 APK，tag 为 `<versionCode>-<versionName>` |

两个 release 由 `.github/workflows/release.yml` 在同一个 tag 构建里一起发布，正文取自同一份 notes，
**不要单独手工上传**：只换 asset 不同步 release 时，模块仓库的版本与更新检查不会刷新。

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
- `.github/workflows/release.yml`：推送 `v*` tag 时校验 tag 与版本号、用 secrets 解码 keystore、构建 release、校验签名，
  创建 GitHub Release 并同步发布到 Xposed 模块仓库；手动触发只上传 APK artifact，不创建 release。
- `.github/workflows/codeql.yml`：push / PR / 每周定时执行 CodeQL Java/Kotlin 静态分析。
- `.github/dependabot.yml`：每周检查 Gradle 依赖与 GitHub Actions 更新。

### 需要在仓库手动创建的 Secrets

| Secret | 值 |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | `base64 -i ~/.vault/sporteditor-release.jks`（Linux 用 `base64 -w0`） |
| `RELEASE_KEYSTORE_PASSWORD` | properties 中的 `storePassword` |
| `RELEASE_KEY_ALIAS` | `sporteditor` |
| `RELEASE_KEY_PASSWORD` | properties 中的 `keyPassword` |
| `XPOSED_REPO_TOKEN` | 对 `Xposed-Modules-Repo/io.github.gythialy.sporteditor` 有 Contents 写权限的 PAT |

`GITHUB_TOKEN` 由 Actions 自动提供，只对 `gythialy/SportEditor` 有效，不能跨仓库建 release，
所以模块仓库需要单独的 `XPOSED_REPO_TOKEN`。

## Xposed 模块仓库发布

模块仓库 [Xposed-Modules-Repo/io.github.gythialy.sporteditor](https://github.com/Xposed-Modules-Repo/io.github.gythialy.sporteditor)
由 LSPosed 组织托管，收录规则见 [org README](https://github.com/Xposed-Modules-Repo/.github/blob/main/profile/README.md)：

1. repo 名必须是模块包名；
2. repo description 非空，会作为模块名展示；
3. 至少有一个 release；
4. release 至少附一个 apk，且 tag 必须是 `VersionCode-VersionName`（如 `107-1.0.0`）。
   tag 写错时 org 的 bot 会自动纠正，但**只替换 asset、不改 release 本身不会触发 bot**。

`release.yml` 的最后一步「Publish to Xposed module repo」会：

- 从 `app/build.gradle.kts` 读 `versionCode` / `versionName` 拼出 `MODULE_TAG`；
- 用 `release-body.md`（与主仓 release 同一份正文）建 release，附 `SportEditor-<tag>.apk`；
- 重跑时若该 tag 的 release 已存在，则更新正文并 `--clobber` 覆盖 asset，不会重复建 release。

手动补发（workflow 失败或需要重建议 release 时）：

```bash
gh release download v1.1.0 -R gythialy/SportEditor -p '*.apk' -D dist
gh release view v1.1.0 -R gythialy/SportEditor --json body -q .body > /tmp/notes.md
gh release create 108-1.1.0 "dist/SportEditor-v1.1.0.apk" \
  -R Xposed-Modules-Repo/io.github.gythialy.sporteditor \
  --title "1.1.0" --notes-file /tmp/notes.md
```

模块仓库的 `README.md` 与 `SUMMARY` 只在模块描述或功能变化时更新，不必每次发版。

### 创建 XPOSED_REPO_TOKEN

1. 打开 <https://github.com/settings/personal-access-tokens/new>（Fine-grained token）；
2. **Resource owner** 选 `gythialy`，**Repository access** → `Only select repositories` → `Xposed-Modules-Repo/io.github.gythialy.sporteditor`；
   列表里没有该仓库时，说明组织限制了 fine-grained token，见下方「排错」；
3. **Permissions → Repository permissions** 只勾 `Contents: Read and write`（`Metadata` 会自动带上只读）；
4. 有效期按需设置（建议 90 天，到期前换新）；
5. 生成后复制 `github_pat_...`，写入本仓库 secret：

   ```bash
   gh secret set XPOSED_REPO_TOKEN --repo gythialy/SportEditor
   # 粘贴 token 回车
   ```

6. 验证 token 能访问模块仓库：

   ```bash
   export GH_TOKEN=<你的 token>
   gh release list -R Xposed-Modules-Repo/io.github.gythialy.sporteditor
   ```

### 排错

- **token 看不到模块仓库（404）**：组织关掉了 fine-grained token。改用带 `repo` scope 的 classic token，
  或让组织 owner 在 *Organization settings → Personal access tokens → Fine-grained tokens* 里授权。
- **workflow 报 `Missing XPOSED_REPO_TOKEN`**：secret 没建在 `gythialy/SportEditor` 上，或名字拼错。
- **modules.lsposed.org 版本没变**：站点按 release 重新索引，通常几分钟内生效；先确认模块仓库的 latest release
  tag 和 APK 都在。

## 版本号与 tag

- `versionCode` 必须单调递增（模块仓库和系统安装都靠它判断更新），`versionName` 是展示版本；
- git tag 必须等于 `v<versionName>`（如 `versionName = "1.1.0"` → `v1.1.0`），workflow 会校验，
  不一致直接失败，避免两个 release 的版本号对不上；
- 带 `-` 的 tag（如 `v1.1.0-beta.1`）会被标为 prerelease。

## Release notes

`release.yml` 优先使用 `.github/release-notes/<tag>.md`（如 `.github/release-notes/v1.1.0.md`），
不存在时用 GitHub 自动生成的 notes；同一份正文会写进两个 release，避免两边不一致。

建议沿用 1.0.0 的结构：

```markdown
## 本版主题

一句话说明这版的重点。

## 主要变化

- ...

## 安装要求

- ...

## 说明

- versionCode / 覆盖升级说明
```

## 触发发布

```bash
# 1. 改 app/build.gradle.kts：versionCode +1，versionName 改成即将打的 tag（去掉 v）
# 2. 写好 .github/release-notes/vX.Y.Z.md（可选，但推荐）
# 3. 提交并合并到 main 后打 tag
git tag v1.2.0
git push origin v1.2.0
```

或用 GitHub Actions 页面的 **Release → Run workflow** 手动构建（手动触发只上传 APK artifact，不创建 release）。

## 发布后检查

1. 主仓 release 的 APK 与模块仓库 release 的 APK 大小/签名一致（`apksigner verify --print-certs`）；
2. `modules.lsposed.org/module/io.github.gythialy.sporteditor` 显示的版本已更新；
3. 应用内「设置 → 检查更新」能发现新版本；
4. 旧版本用户能直接覆盖安装（同签名、versionCode 递增）。
