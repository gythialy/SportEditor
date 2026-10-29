# 布局审计与 layout 改造方案

> 状态：**已实施**（H1–H8 / S2 / S3 / D1 / D2 / D4 / D5 / A1 / A4 已落地；S1、D3 按下文说明保留）
>
> 实施后与原文的差异：
> - H1：断连卡从 243dp 收到约 110dp（16dp 细插画带 + 一行状态 + 一行做法），首屏能看到全部 4 个应用。
> - H2：卡片只留「该做什么」，完整步骤只在弹窗里出现一次。
> - H3/H4：`⋮` 移入「修改方式」行，头部恒定「标题 + 箭头 + 开关」。
> - H5：步进圆钮 48 → 56dp；单位从贴边的 suffix 移进字段标签（「倍率」），数字独占居中。
> - H6：去掉分段按钮的勾选图标后，411dp 英文 `Multiplier` 不再被切；320dp 英文改用 `× / + / Auto / Fixed`。
> - H7：预览行只保留「封顶」二字，重复的整句删除。
> - H8：每日上限输入框限宽 220dp 并右对齐。
> - S1：保留「检查更新」独占一行（英文下改 trailing 会把副标题挤成三行），改为与标题列同轴（48dp + 8dp 内边距）。
> - S2/S3：分组标题与卡片同轴；两页共用 `Dimensions` 的 16/12/8 刻度。
> - D1：tab 条移到 AppBar 下方。D2：环境页去掉卡内灰底块。D4：目标应用行补状态文字。D5：日志卡内边距统一到 16/12。
> - A4：内容列限宽 640dp 居中，顶栏保持通栏。
> - D3（长值碰撞风险）未复现，保留为已知风险。
> 命令：`/impeccable layout`
> 采样日期：2026-10-29
> 采样设备：Pixel 7 Pro AVD（1440×3120 / 560dpi = 411dp）＋ `wm size`/`wm density` 覆盖出的 320dp、360dp、1280dp 三档
> 采样状态：中文/英文 × 浅色/深色 × 1.0/1.3 字号 × 收起/展开/每日上限展开/弹窗
> 证据：[`layout-audit/`](layout-audit/) 下 13 张实机截图（编号即下文的「证据 N」）

---

## 0. 结论摘要

现有界面是「Material 3 + 卡片」的 Operate 型工具，视觉世界成立，**问题全部在结构层**：

1. **同一件事说了三遍**：顶栏状态胶囊 + 断连卡 + 弹窗，都在讲「未连接」；断连卡占掉首屏约 1/3，把 4 个 App 里的 2 个顶出屏幕。
2. **每张卡片头部挤了三簇控件**：标题区、展开箭头、开关，展开后箭头又变成 `⋮`。
3. **编辑态出现硬伤**：步进输入框 235×72dp 夹在两个 48dp 圆钮中间；分段控件在 411dp 下英文标签「Multiplier」被切（证据 2、3）。
4. **四个列表四种节奏**：首页卡间距 12dp、设置 8dp、诊断日志 10dp、诊断应用 8dp；页面内边距也各不相同。**全项目 28 种 dp 字面量、`spacedBy` 7 种取值**，没有间距刻度。
5. **宽屏失守**：1280dp 平板横屏时没有任何宽度上限，断连卡拉成 1248dp 宽的红色横带，App 卡片的开关离标题约 1000dp（证据 12）。

改造方向：**收紧信息重复、合并卡片头部簇、控件对齐、间距 token 化、内容宽度封顶**。视觉世界（配色、字体、卡片语言）不动。

---

## 1. 空间论点（spatial thesis）

| 项 | 结论 |
| --- | --- |
| 主路径 | 看连接状态是否要处理 → 找到目标 App → 开关或就地编辑 → 设置 / 诊断 |
| 属于一组 | 「App 名称 + 当前规则 + 开关 + 它的编辑控件」是一个决策单元；连接状态是全局的，只该占一条 |
| 谁领读 | 正常时：App 列表领读；异常时：一条紧凑状态条领读，而不是一张大卡 |
| 密度 | 收起态**一屏看全 4 个 App**（411dp 下 ≤ 640dp 内容高度）；展开态允许变高，但必须仍能感知「下面还有 3 个 App」 |
| 跨宽度 | 结构随宽度变：内容宽度封顶 640dp 居中；≥ 840dp 时不拉伸、不产生超长行宽 |

---

## 2. 逐屏诊断

### 2.1 首页（证据 1、2、3）

| # | 问题 | 证据 / 度量 |
| --- | --- | --- |
| H1 | 断连卡占屏过大：40dp 插画 + 标题 + 两段说明 + 右下按钮，卡片高约 850px ≈ **243dp**，占内容区（约 738dp）的 1/3；411dp 屏上首屏只露出 2 个 App | 证据 1：断连卡 y≈322–1173px；按 560dpi（3.5px/dp）换算 |
| H2 | 同一信息三处重复：AppBar 胶囊「未连接」、断连卡「LSPosed 服务未连接」、点「查看步骤」后弹窗又从标题到正文完整重述一遍 | 证据 4：弹窗正文与卡片正文逐字相同 |
| H3 | 卡片头部控件分三簇：图标+标题/摘要、展开箭头、开关；箭头与开关之间约 60dp 空档不携带信息 | 证据 1 |
| H4 | 展开后箭头位置换成 `⋮`（恢复默认），夹在标题与开关之间，成为第三簇；且 `⋮` 只在展开时出现，位置不稳定 | `HomeScreen.kt` 456–484 行 |
| H5 | 步进行三段高度不齐：输入框 235×72dp，两侧圆钮 48dp，上下各余 12dp；框内数字居中、单位 `x` 贴右边框，两者无视觉关联 | 证据 2：输入框 bounds `[308,903]–[1132,1155]` |
| H6 | **分段控件英文标签被裁**：411dp 下首段「Multiplier」文本右边界 416px，与分隔线重合，字母 r 被切 | 证据 2、3；`SingleChoiceSegmentedButtonRow` 4 段等分 + 选中态 check 图标占位 |
| H7 | 预览行与「每日上限」行重复「封顶」语义（「…· capped by the daily max」正下方就是「Daily max 20,094 steps」），且该行会折成两行 | 证据 3 |
| H8 | 每日上限展开态宽度失控：输入框满宽 347dp，而内容是 5–7 位数字 | 证据 3：`[112,1720]–[1328,2014]` |

### 2.2 设置（证据 5）

| # | 问题 | 证据 / 度量 |
| --- | --- | --- |
| S1 | 「版本更新」行把「检查更新」塞在副标题下方左对齐，破坏分组列表 4 行等节奏（其余行右侧箭头 trailing） | 证据 5 |
| S2 | 分组标题与卡片内容两套轴线：标题 `start = 4.dp`（相对卡片左缘 → 20dp），卡片内 `ListItem` 内容 32dp | `SettingsScreen.kt` `SectionLabel` |
| S3 | 分组节奏与首页不一致：`contentPadding = (16, 8)`、`spacedBy = 8dp`，首页是 `(16,4,16,32)`、`spacedBy = 12dp` | 源码 |

### 2.3 诊断（证据 6、7、8）

| # | 问题 | 证据 / 度量 |
| --- | --- | --- |
| D1 | `TabRow` 钉在屏幕**底部**（源码注释「tabs 固定在底部」），3dp 指示条贴在内容区下缘、紧邻手势条；底部这条 Tab 栏按标准「图标 + 文字」高度约 72dp，日志列表可用高度同比减少 | 证据 6：指示条与 Tab 文字贴在内容区最后 100px 内；`DiagnosticsScreen.kt` 底部 `DiagnosticsTabBar(pagerState)` |
| D2 | 环境页一屏 **4 种容器语言**：状态卡 / 卡内灰底「框架」块 / 键值卡 / 统计卡 / 提示卡——「框架」块是典型的用容器弥补 proximity | 证据 7 |
| D3 | 键值行值右对齐且不省略：长值（`Google sdk_gphone64_arm64`）已占满右侧 1/3；**风险**：1.3× 字号或更长设备名会与左标签碰撞（本轮未在渲染中复现碰撞） | 证据 7 |
| D4 | 目标应用行只有一个无标签灰点表示状态，右侧大片留白；摘要 `×50` 与首页 `×50 · 上限 80,000` 格式不一致 | 证据 8 |
| D5 | 日志卡行内边距/行距混用 14/12/16/10；消息行用等宽 11sp 且字距偏大 | 证据 6；`DiagnosticsScreen.kt` 543/562/611/694 行附近 |

### 2.4 跨尺寸（证据 9–13）

| # | 状态 | 结果 |
| --- | --- | --- |
| A1 | 320dp + 英文 | 可读，但 AppBar 标题与状态胶囊只剩约 18dp；1.3× 字号下进一步压缩 |
| A2 | 360dp + 英文 + 深色 | 可读；卡片摘要已靠 ellipsis 兜住 |
| A3 | 1.3× 字号 | 卡片与列表可读；AppBar 与分段控件是最先崩的两处 |
| A4 | **1280dp 平板横屏** | **结构性失败**：无宽度上限，断连卡 1248dp 宽，正文行长 100+ 字符；卡片标题到开关水平距离约 1000dp，proximity 完全失效 |

---

## 3. 间距与容器 token

### 3.1 现状（源码统计）

- dp 字面量：**28 种**（0.8 / 1 / 1.5 / 2 / 3 / 3.5 / 4 / 5 / 6 / 7 / 8 / 10 / 11 / 12 / 14 / 16 / 18 / 20 / 24 / 28 / 32 / 40 / 42 / 48 / 56 / 72 / 112 / 0）
- `spacedBy`：**7 种**（2 / 4 / 5 / 6 / 8 / 10 / 12）
- 卡片圆角：16dp；内部小块圆角 12 / 14 / 7 / 50 混用

### 3.2 目标刻度（新增 `ui/theme/Dimensions.kt`）

| token | 值 | 用途 |
| --- | --- | --- |
| `Space.hairline` | 2dp | 图标与文本的贴合间距 |
| `Space.xs` | 4dp | 标签与内容 |
| `Space.sm` | 8dp | 行内元素、chip 之间 |
| `Space.md` | 12dp | 卡片内分组、列表项之间 |
| `Space.lg` | 16dp | 页面左右内边距、卡片内边距 |
| `Space.xl` | 24dp | 页面底部、区块之间 |
| `Space.xxl` | 32dp | 页面首尾留白 |
| `ListGap` | 12dp | 三个列表统一卡片/行间距（替代 8 / 10 / 12 三套） |
| `PagePadding` | 16dp | 三个屏统一左右内边距 |
| `ContentMaxWidth` | **640dp** | 内容宽度上限（宽屏居中） |
| `CardCorner` | 16dp | 卡片圆角；内部小块一律 12dp |

规则：**新增 UI 不再写裸 dp**；确需一次值时在注释里写明理由。

---

## 4. 改动清单（按文件、可执行）

### 4.1 `ui/theme/Dimensions.kt`（新增）

`object Space`、`PagePadding`、`ListGap`、`ContentMaxWidth`、`CardCorner` 如上表。无行为变更。

### 4.2 `ui/HomeScreen.kt`

1. **断连卡 → 紧凑状态条**（解决 H1/H2）
   - 改为单行 `Surface`：`Warning` 图标 20dp + 标题「LSPosed 服务未连接」+ 右侧「查看步骤」文字按钮；高度 ≈ 56dp。
   - 删除 40dp `LinkBrokenArt` 与第二段说明（「当前设置只保存在本机…」）；两段说明保留在已有的 `ConnectionSheet` 里，**弹窗成为唯一的详情出口**。
   - `StaggeredAppear` 若因此无调用点，一并移除。
   - 验收：411dp 首屏可见全部 4 个 App 的收起态。
2. **卡片头部收簇**（解决 H3/H4）
   - 头部只保留：图标、标题/摘要、展开箭头、`Switch`。
   - 箭头固定在开关左侧 `Space.xs` 处，与开关同组；`⋮`（恢复默认）移到展开区「修改方式 / 说明」这一行的右侧，与「说明」并列。
   - 箭头加 `Modifier.animateContentSize` 之外的旋转动画（0°→180°）以替代换图标。
3. **步进行对齐**（解决 H5）
   - 输入框定宽 `150.dp`、高度 `56.dp`（`textStyle = headlineSmall`），两侧 48dp 圆钮与输入框垂直居中对齐；整行 `Arrangement.SpaceBetween`。
   - 单位后缀保留在框内右侧，但水平内边距 ≥ 12dp，避免与数字贴边。
4. **分段控件**（解决 H6）
   - 选中态不再叠加 check 图标（当前裁切的主因），改用「填充色 + 加粗」表达选中；`label` 保持单行。
   - 宽度 < 360dp 或字号 ≥ 1.3× 时，4 段改为两行两组（`SingleChoiceSegmentedButtonRow` × 2）。
   - 验收：411dp/360dp/320dp × 中英 × 1.0/1.3 字号下，`Multiplier` / `倍率` 均不裁切、不换行。
5. **预览行**（解决 H7）
   - 去掉「capped by the daily max」，封顶状态改由「每日上限」行右侧的小字表达（该行已显示实际值）。
   - 预览行 `maxLines = 1` + `TextOverflow.Ellipsis`。
6. **每日上限行**（解决 H8）：展开后的输入框宽度上限 `240dp` 或与步进框同宽，不再满宽。
7. **节奏 token 化**：页面 `PagePadding`、列表 `ListGap`、卡片内 `Space.lg`、底部 `Space.xl`。

### 4.3 `ui/SettingsScreen.kt`

1. 「检查更新」改为该行的 trailing 文字按钮（`ListItem.trailingContent`），恢复 4 行等节奏（解决 S1）。
2. `SectionLabel` 去掉 `start = 4.dp`，与卡片内容轴线对齐（解决 S2）。
3. `contentPadding` 与 `spacedBy` 改用 token（解决 S3）。

### 4.4 `ui/DiagnosticsScreen.kt`

1. **`TabRow` 移到 AppBar 下方**（已确认的决定，解决 D1）：作为 `Scaffold` 内 `Column` 的第一项，`HorizontalPager` 拿到释放出的约 72dp；删除底部 `DiagnosticsTabBar` 调用与底部占位。
2. 三个 page 的 `contentPadding` 统一 `PagePadding`、列表间距统一 `ListGap`（解决 D5 的节奏部分）。
3. 环境页「框架」灰块改为普通键值行，去掉卡内嵌套容器（解决 D2）。
4. 键值行：标签 `Modifier.weight(1f)`，值 `maxLines = 1` + ellipsis + 等宽字体（解决 D3 的碰撞风险）。
5. 目标应用行：无标签灰点改为带文字的 `Surface` 小标签（「已启用 / 未安装」），摘要格式与首页对齐（解决 D4）。

### 4.5 三屏共用：宽屏上限

每个 `LazyColumn` 外层包 `Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter)` + `Modifier.widthIn(max = ContentMaxWidth)`（解决 A4）。
验收：1280dp 下内容区宽 ≤ 640dp 居中，正文行长 ≤ 约 60 个字符，卡片标题到开关距离 ≤ 640dp。

---

## 5. 宽屏策略

本轮只做**宽度封顶**。平板双栏（左列表 + 右编辑面板）牵涉导航结构，仍列为后续议题；宽度封顶后单栏在 1280dp 上已可用，双栏是收益而非必需。

---

## 6. 验收矩阵

实施后需逐格复拍并与本方案对照：

| 维度 | 取值 |
| --- | --- |
| 屏宽 | 320dp / 360dp / 411dp / 600dp（平板竖）/ 1280dp（平板横） |
| 语言 | 简体中文 / English |
| 主题 | 浅色 / 深色（**注意动态取色**：Android 12+ 会用壁纸色替换绿色主色，深色下 errorContainer 呈亮砖红） |
| 字号 | 1.0× / 1.3×（关键看 AppBar、分段控件、键值行） |
| 状态 | 收起 / 展开 / 每日上限展开 / 连接弹窗 / 设置 / 诊断三 tab / 日志空态 |

硬性验收点：

- [ ] 411dp 首屏可见 4 个 App 收起态全部
- [ ] `Multiplier` 在任何宽度与字号下不裁切（含选中态）
- [ ] 卡片头部只剩两簇控件（标题区、开关组）
- [ ] 三个列表间距一致（12dp），三屏左右内边距一致（16dp）
- [ ] 1280dp 内容宽 ≤ 640dp
- [ ] `./gradlew spotlessApply` 与 `assembleDebug` 通过
- [ ] 机械扫描 `impeccable detect --scope layout` 复跑（当前对本项目返回 `[]`，见附录 B）

---

## 7. 不做 / 遗留

- 平板双栏、列表-详情
- 配色、字体、插画风格替换（本命令只改结构）
- 动效强度调整（展开/收起沿用 M3 默认）
- 日志行等宽字体字距（属 typography，另开 `typeset`）
- 诊断顶栏三个动作（搜索 / 采集等级 / 更多）的 IA 收敛：三者功能互不重复，本轮只保证 48dp 触摸目标与等距

---

## 附录 A：证据索引

| 编号 | 文件 | 说明 |
| --- | --- | --- |
| 1 | `layout-audit/01-home-zh-light.webp` | 首页 · 中文 · 浅色 · 断连态 + 4 卡收起 |
| 2 | `layout-audit/02-home-expanded-zh-light.webp` | 首页 · 展开态（步进行、分段控件、每日上限行） |
| 3 | `layout-audit/03-home-maxsteps-en-dark.webp` | 每日上限展开（英 · 深色） |
| 4 | `layout-audit/04-connection-sheet-en-dark.webp` | 连接弹窗（与断连卡正文重复） |
| 5 | `layout-audit/05-settings-zh-light.webp` | 设置 · 分组列表（注意动态取色的蓝色图标） |
| 6 | `layout-audit/06-diag-logs-zh-light.webp` | 诊断 · 日志（底部 TabRow 与指示条） |
| 7 | `layout-audit/07-diag-env-zh-light.webp` | 诊断 · 环境（4 种容器语言） |
| 8 | `layout-audit/08-diag-apps-zh-light.webp` | 诊断 · 目标应用（无标签灰点） |
| 9 | `layout-audit/09-home-en-dark.webp` | 首页 · 英 · 深色 |
| 10 | `layout-audit/10-narrow-360dp.webp` | 360dp |
| 11 | `layout-audit/11-narrow-320dp.webp` | 320dp |
| 12 | `layout-audit/12-tablet-1280dp.webp` | 平板横屏 1280dp（宽屏失守） |
| 13 | `layout-audit/13-font-130.webp` | 1.3× 字号 |

复现命令（工作区本地，不写 `~/.android`）：

```bash
export ANDROID_SDK_ROOT=~/Developer/android-sdk
export ANDROID_AVD_HOME=$PWD/.avd ANDROID_USER_HOME=$PWD/.android-home
$ANDROID_SDK_ROOT/emulator/emulator @SportEditorLayout -no-window -no-audio \
  -no-boot-anim -no-snapshot -gpu swiftshader_indirect -memory 3072 -grpc 8554
GRADLE_USER_HOME=$PWD/.build-home ./gradlew :app:assembleDebug
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell wm size 2560x1600 && adb -s emulator-5554 shell wm density 320
```

## 附录 B：方法与已知限制

- **Impeccable 机械扫描对本项目无效**：`impeccable detect --json --scope layout app/src/main/java/io/github/sporteditor/ui` 返回 `[]`——该检测器面向 web 源（HTML/CSS/JSX），不解析 Kotlin/Compose。因此本方案的所有结论来自**实机渲染 + `uiautomator` 边界 + 源码度量**，未使用检测器推断。
- **没有 PRODUCT.md / DESIGN.md**：`impeccable context` 报 `NO_PRODUCT_MD`。本命令是既有代码上的范围化改造，按 skill 规则以代码 + `design/ui-redesign/README.md` 为既有视觉权威继续；建议后续跑一次 `/impeccable init` 固化产品上下文。
- 断连态之外的「已连接」界面未能采样：连接态需要真实 LSPosed/Vector 框架环境（见 `docs/emulator-test.md`），本轮只验证了未连接态与编辑态。已连接态的差异仅在状态胶囊颜色与断连卡消失，不影响本方案的结构结论。
- 深色截图受动态取色影响，颜色与 `design/ui-redesign/README.md` 的绿色令牌不同；本方案不依赖具体色值。
