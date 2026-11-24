# 调研：基于 Material 3 Expressive 的 UI 重构

> 日期：2026-11-05 · 分支：`feat/ui-strategy-star-update-row`
> 结论基于两类证据：**本地字节码实测**（`material3` 1.4.0 与 1.5.0-rc01 的 AAR，Google Maven 下载，`javap` 读取公开 API 与 token 常量）与**官方文档/发布说明**；二手来源单独标注。
> 本文只做调研与方案，不含代码改动。

---

## 0. 结论先说

1. **Expressive 不是 M4**，是 M3 之上的一套战术：14 个新增/更新组件、动效改为弹簧物理、35 个形状与形状变形、字重强调（emphasized）字号体系。官方原话："*M3 Expressive isn't a new version of the system… this isn't 'M4'*"。
2. **组件层面今天拿不到**：`compose-bom 2026.09.00` 把 `material3` 固定在 **1.4.0**（stable，2025-09-24），而 expressive 组件在 1.4.0-beta01 被**整批移除**，改由 1.5.0 承载（当前 **1.5.0-rc01**，2026-11-04）。要真组件必须显式 pin 版本，BOM 里没有。
3. **但有两块收益今天就能拿，且不升版本**：
   - ~~`MaterialTheme` 在 **1.4.0 已公开 `motionScheme` 参数**……自己实现一个 expressive `MotionScheme` 就能让全部组件走弹簧。~~ **更正（2026-11-13）**：1.4.0 的 `MotionScheme` 在 Kotlin 层是 `internal interface`，`MaterialTheme.motionScheme` 属性与 `MotionScheme.expressive()` 工厂同样是 internal，公开的 `MaterialTheme(motionScheme = …)` 重载因此参数类型不可见——**应用侧无法自建 scheme**，这条 P0 不成立，必须等 1.5.0-rc01 起 `MotionScheme` 转 public 之后才能接线。`javap` 显示 `public interface` 只是 Kotlin `internal` 在 JVM 的擦除结果，不能当作可调用证据。
   - emphasized 只是"同尺寸、字重 +100~200"的 `TextStyle`；即便 1.5.0 才公开 `titleLargeEmphasized`，手写 4 个样式风险为零。
4. **组件替换里唯一有功能收益的是 ButtonGroup 的溢出折叠**：首页 4 段「修改方式」在 320dp 英文下靠短标签才没被切（`layout-audit.md` H6）；ButtonGroup 能把塞不下的项折进菜单，而不是压缩标签。
5. **设计取向要先定**：`DESIGN.md` 的既有主张是"安静的仪器"（不用阴影、靠色调与描边、克制动效），而 expressive 的默认倾向更响。全量 expressive 会推翻现有设计论点，因此建议**选择性采用**：动效与字重（几乎无感、纯增益）→ 组件与形状（显式可见）→ shape morph（品牌性玩法，最后做）。

---

## 1. 设计层事实（规范说了什么）

### 1.1 五个维度

| 维度 | Expressive 的实际改动 | 数字/事实 |
| --- | --- | --- |
| 动效 | 从 duration+easing 改为**弹簧物理**，分 spatial / effects 两条通道、三档速度 | 见下表 |
| 形状 | 圆角刻度从 5 档扩到 9 档，新增 35 个形状与 shape morph | 0 / 4 / 8 / 12 / 16 / **20** / 28 / **32** / **48** dp + full |
| 字体 | 15 baseline + 15 emphasized；**尺寸不变，只加字重** | display/headline/body → 500；titleMedium/Small、label → 700 |
| 颜色 | 不是给每个 role 派生多级 emphasis；新增 3 档对比度（standard/medium/high）+ `*-fixed` 角色 | 角色配对 ≥3:1，文字 ≥4.5:1 |
| 组件 | 5 个新增（Button group、FAB menu、Loading indicator、Split button、Toolbar）+ 9 个更新 | 按钮 5 档尺寸：XS 32 / S 40 / M 56 / L 96 / XL 136 dp |

### 1.2 动效 token（官方常量，本地字节码核对一致）

| Token | Expressive（阻尼/刚度） | Standard |
| --- | --- | --- |
| fast spatial | 0.6 / 800 | 0.9 / 1400 |
| default spatial | 0.8 / 380 | 0.9 / 700 |
| slow spatial | 0.8 / 200 | 0.9 / 300 |
| fast effects | 1.0 / 3800 | 1.0 / 3800 |
| default effects | 1.0 / 1600 | 1.0 / 1600 |
| slow effects | 1.0 / 800 | 1.0 / 800 |

要点：expressive 的 **spatial 阻尼更低（会轻微回弹）、刚度更低（更"肉"）**；effects 两套完全相同（透明度/颜色永不过冲）。Web 侧无法用弹簧，官方换算成曲线：spatial fast/default/slow = 350/500/650 ms，effects = 150/200/300 ms。

### 1.3 使用边界（官方明确）

- "*Not a one-size-fits-all*"：官方举例，把竖向列表换成散乱图片、把按钮文字去掉，现代感上升但**可用性分数下降**。
- 适合：少量关键动作、状态切换、hero 时刻；不适合：信息密度高的工具界面、强规范流程、企业后台。
- 硬约束：XS/S 图标按钮点击区仍须 ≥48dp；大圆角/full 圆角**不得用于信息密集卡片**（会裁切内容）；不要只靠颜色区分状态（颜色 + 形状 + 字重一起用，才是色盲友好方案）。

---

## 2. 实现层事实（Compose 侧能拿到什么）

### 2.1 版本矩阵（本地实测）

| 版本 | 状态 | 关键内容 |
| --- | --- | --- |
| `material3` **1.4.0**（本项目） | stable（2025-09-24） | `MotionScheme` 接口 public / 工厂 internal；`MaterialTheme(motionScheme=)` 公开；`Typography.*Emphasized` internal；**零 expressive 组件**；`ShapeDefaults` 无 20/32/48 |
| `material3` **1.5.0-rc01** | RC（2026-11-04） | `MotionScheme.expressive()` public；`TitleLargeEmphasized` 等全公开；`MaterialShapes`；ButtonGroup / SplitButton / ToggleButton / FloatingToolbar / FAB menu / LoadingIndicator / WavyProgressIndicator / 滚动条 / ComponentStyle 体系 |
| `compose-bom` 2026.09.00 | stable | 固定 material3 **1.4.0** + compose ui/foundation **1.12.1** |

**升级成本比预想低**：1.5.0-rc01 的 POM 要求 compose foundation/ui/runtime ≥ **1.12.0**，而 BOM 2026.09.00 已给 1.12.1 —— 只需显式声明 `material3:1.5.0-rc01` 覆盖 BOM，**不需要连带升级其它库**。它会带入 `androidx.graphics:graphics-shapes`（`MaterialShapes` 的形状类型就是它的 `RoundedPolygon`）。

### 2.2 opt-in 现状（1.5.0-rc01 字节码实测，不是文档转述）

- **仍是实验 API**：`MaterialShapes`、`LoadingIndicator`、`MenuKt`（expressive 菜单重载）、`SliderDefaults`、`PullToRefreshDefaults`。
- **已转稳定、无需 opt-in**：`ButtonGroup`、`SplitButton`、`ToggleButton`、`FloatingToolbar`、`FloatingActionButtonMenu`、`WavyProgressIndicator`。

### 2.3 陷阱

1. **`MaterialExpressiveTheme` 省略参数会打穿深色模式**：它默认取 `expressiveLightColorScheme()`，而 1.5.0-rc01 里**没有 `expressiveDarkColorScheme()`**。本项目必须继续显式传 `dynamicLightColorScheme/dynamicDarkColorScheme` + 自建 fallback 配色，只把 `motionScheme` 换成 expressive。
2. `MaterialExpressiveTheme` **不改变任何现有组件的默认样式**（`LocalUsingExpressiveTheme` 全库只有 `MaterialThemeKt` 引用），所以"换成 expressive 主题"不会自动帮你把组件变成 expressive——组件要自己换。
3. shape morph 是每帧重建 `Path`，不要用在大面积或高频动画上（此条为社区经验，官方无性能文档）。

---

## 3. 本仓库现状盘点

| 维度 | 现状 | 与 expressive 的差距 |
| --- | --- | --- |
| 动效 | `ui/theme/Motion.kt` 四个角色（press/settle/enter/leave），`settle` 用 0.8/380、`enter` 用 1.0/1600 —— **数值与官方 expressive default token 完全一致**，但只有 2 档、且是手抄常量 | 缺 fast/slow 两档；组件自身动效（Switch、弹窗、tab、FAB、日期选择器）仍未走 expressive scheme |
| 字体 | `SportEditorTypography = Typography()` 全默认，UI 层**零硬编码 sp**（全部走 token） | 无 emphasized；标题 22sp/卡片标题 16sp 用的都是 baseline 字重 |
| 形状 | 4 / 8 / 12 / 16 / 16（`extraLarge` 被刻意压到 16）+ 例外 14/7/2 | 缺 20 / 28 / 32 / 48；底部弹窗和对话框用的是 16 而非 28 |
| 组件 | `SegmentedButton`(4 段)、`FilterChip`、`Switch`、手写双行 `TopAppBar`、`ModalBottomSheet`、`ListItem`、`IconButton`、`AlertDialog`、`TabRow` | 无 ButtonGroup / SplitButton / FloatingToolbar / LoadingIndicator / MaterialShapes |
| 设计论点 | "安静的仪器"：不用阴影、靠色调与描边、单栏限宽 640dp、13 张实机截图审计过的节奏 | expressive 的默认语气更响，需要逐项判断"要不要响" |

---

## 4. 逐屏重构机会

| 位置 | 现状 | expressive 选项 | 收益 | 风险 | 阶段 |
| --- | --- | --- | --- | --- | --- |
| 全局主题 | `MaterialTheme(colorScheme, typography, shapes)` | 1.4.0：传入自建 expressive `MotionScheme`；1.5.0：`MotionScheme.expressive()` | 所有组件动效统一到弹簧，删掉手抄常量 | 组件动效变慢/回弹，需看一遍所有弹层 | **P0** |
| 首页顶栏 | 手写双行标题（22sp + 12sp 副标题） | 1.5.0 的 expressive `TopAppBar`（含 subtitle 槽） | 少一段自定义布局，折叠行为由库负责 | 折叠高度/对齐需重新截图核对 | P1 |
| 连接状态胶囊 | 自绘 `Surface(CircleShape)` + 圆点 | 保持自绘（它是状态，不是动作）；或用 SplitButton 承载"状态 + 查看步骤" | 少一个入口 | SplitButton 语义是"主动作 + 次动作"，状态指示用它不合适 | 不做 |
| 断连提示卡 | 告警淡染 + 一行状态 | 保持；若要更"响"，用 emphasized 标题而非大圆角 | — | 大圆角会裁切两行说明 | 不做 |
| 修改方式（4 段） | `SingleChoiceSegmentedButtonRow` + 短标签规避切字 | **ButtonGroup + ToggleButton**（可溢出折叠） | 去掉 320dp 的标签硬约束；选中态有形状/字重变化 | ToggleButton 默认尺寸更高，卡片会长 | **P1** |
| 步进圆钮 −/+ | 56dp `FilledTonalIconButton` | 保持尺寸档 M（56dp）语义；按下可加形状 morph | 与 expressive 尺寸档对齐 | 变形在小圆钮上收益有限 | P2 |
| 大数字读数 | `OutlinedTextField` 数字 + 动画 settle | 读数文本用 `titleLargeEmphasized` / `displaySmallEmphasized` | 层级更清楚，和"仪表盘"隐喻一致 | 需确认 CJK 字重跳档是否可用（Roboto 仅 500/700 有效） | **P0** |
| 每日上限折叠行 | `ListItem` + 展开 | 保持 | — | — | 不做 |
| 检查更新 | 自绘 `UpdateSpinner` | `ContainedLoadingIndicator`（实验 API） | 更 expressive 的等待态 | 实验 API，官方建议 <200ms 不显示 | P1（可选） |
| 诊断顶栏 | 溢出菜单（复制/分享/清空） | `HorizontalFloatingToolbar` 放 3 个动作 | 动作可见，少一次点击 | 新增交互面；工具型界面易显吵 | P2（需评审） |
| 诊断 tab | `TabRow` + 单选筛选 | 保持 TabRow（官方：tabs 适合探索），筛选可换 ButtonGroup | 筛选控件与首页一致 | 与首页一致的收益是次要的 | P1 |
| 状态切换动效 | 弹窗/卡片展开 | `MaterialShapes` + morph（如圆点→胶囊） | 品牌性 | 每帧重建 Path；1.5.0 stable 之前不做 | P2 |

---

## 5. 分阶段方案

### P0 · 1.4.0 上就能做（无版本跳跃，可独立回滚）

1. **`SportEditorMotionScheme`**：实现公开的 `MotionScheme` 接口，提供 expressive 的 fast/default/slow × spatial/effects 六个 spec（数值取官方 token），在 `SportEditorTheme` 里 `MaterialTheme(motionScheme = …)`。
2. **`Motion.kt` 收编**：`settle()/enter()` 改为读 `MaterialTheme.motionScheme`；`press()`/`leave()` 保留为 tween 例外，并在注释里说明"这两个是时长语义，不是物理语义"。
3. **emphasized 手写**：新增 `emphasized(TextStyle) = copy(fontWeight = +200)` 三个变体，用在：首页 App 名（titleMedium → 700）、大数字读数（titleLarge → 500）、设置分组标题（labelLarge → 700）。
4. **形状刻度补全**：`Shapes` 的 `extraLarge` 从 16 → 28（仅底部弹窗用），新增 `largeIncreased = 20`（卡片可选，需评审）；`AppShapes` 的 14/7/2 保留。
5. **验收**：`scripts/emulator/run-avd.sh` 起 AVD，按 `design/ui-redesign/layout-audit.md` 同样的 13 张截图口径重拍，与旧图逐张比对；重点看弹层动效是否变慢、Switch 是否回弹过度。

### P1 · 1.5.0-rc01（只在分支试，等 stable 再合主干）

1. 显式 pin `androidx.compose.material3:material3:1.5.0-rc01`（BOM 不动）。
2. `MaterialTheme(motionScheme = MotionScheme.expressive())`，删除自建 scheme；保留显式 colorScheme（否则深色模式被打穿）。
3. 修改方式 → `ButtonGroup` + `toggleableItem`（含溢出菜单），同步删掉短标签的规避逻辑。
4. 检查更新 spinner → `ContainedLoadingIndicator`（`@OptIn(ExperimentalMaterial3ExpressiveApi::class)`）。
5. 顶栏 → expressive `TopAppBar` 的 subtitle 槽。

### P2 · 1.5.0 stable 之后

- 诊断页 `HorizontalFloatingToolbar`（需要产品评审：是否值得多一个交互面）。
- `MaterialShapes` + shape morph：只做 1–2 处（例如连接状态点 → 胶囊、步进按钮按下），不做全局换形状。

---

## 6. 明确不做

- **不用 `MaterialExpressiveTheme` 的默认参数**（浅色陷阱）。
- **不把卡片圆角拉到 28/32**：信息密集卡片用大圆角会被官方明确列为 caution。
- **不用浮动工具栏当主导航**：工具型界面应克制，官方也说 "*use it sparingly*"。
- **不做 loading → progress 的转换**（官方明令禁止）。
- **不为 expressive 牺牲标签**：XS/S 尺寸按钮点击区不足 48dp 时不得使用。

---

## 7. 决策记录（2026-11-05）

- **本次范围：只保留调研，不动代码。** 报告合入文档即止，`app/src` 无改动。
- **目标强度：激进**（动效 + 字重 + 组件 + 形状刻度 + 浮动工具栏 + shape morph）。也就是说下次实施时，P2 的浮动工具栏与 morph 是**目标的一部分**，不是可选加分项；P0/P1 全部变成它的前置。
- **由此推出的硬约束**：激进档里的浮动工具栏与 `MaterialShapes` 都依赖 `material3` 1.5.0，因此**实施必须等 1.5.0 stable**（当前 1.5.0-rc01，官方承诺 2026 年内）。不主张在 rc 上直接落激进档。
- **实施时需要补做的功课**：① `HorizontalFloatingToolbar` 会新增一个交互面，与现有"零新增交互"的取向冲突，需要单独评审；② shape morph 是每帧重建 `Path`，需要先确认在低端机上的预算；③ 形状"是通用语言而非语义"（官方原话），不能把波浪/morph 当作进度或状态的语义载体。
- **待办（实施阶段）**：同步更新 `DESIGN.md`（新增 motion/shape/type 的 expressive 段落）与 `design/ui-redesign/README.md` 的"待确认"小节。

---

## 附录：证据

**本地实测**（`~/.gradle` 缓存 + Google Maven 下载的两个 AAR，`javap` 读取）
- `material3` 1.4.0：`MotionScheme` 接口 public、`MotionScheme$Companion.standard$material3/expressive$material3` internal；`MaterialTheme(... motionScheme ...)` 与 `MaterialExpressiveTheme(...)` 均存在；`Typography.*Emphasized` 带 `$material3` 后缀（internal）；无 `MaterialShapes` / `ButtonGroup` / `ToggleButton` / `SplitButton` / `FloatingToolbar` / `LoadingIndicator` / `WavyProgressIndicator`；58 个类引用 `MotionScheme`。
- `material3` 1.5.0-rc01：具备上述全部 expressive 组件；`ExpressiveMotionTokens` 常量 = 0.6/800、0.8/380、0.8/200、1.0/3800、1.0/1600、1.0/800；`ShapeTokens` = 4/8/12/16/20/28/32/48；POM 依赖 `foundation/ui/runtime 1.12.0`、`graphics-shapes`。
- `compose-bom` 2026.09.00：material3 1.4.0、compose 1.12.1；`material3` 最新版本 1.5.0-rc01（lastUpdated 2026-11-04）。

**官方**
- [Start building with Material 3 Expressive](https://m3.material.io/blog/building-with-m3-expressive)
- [Motion physics system](https://m3.material.io/styles/motion/overview/specs) · [Shape](https://m3.material.io/styles/shape) · [Shape morph](https://m3.material.io/styles/shape/shape-morph)
- [Type scale & tokens](https://m3.material.io/styles/typography/type-scale-tokens) · [Color roles](https://m3.material.io/styles/color/roles)
- [Compose Material 3 release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3) · [MotionScheme API](https://developer.android.com/reference/kotlin/androidx/compose/material3/MotionScheme)
- [Google Design：Expressive design 研究](https://design.google/library/expressive-material-design-google-research)
- [Android Developers Blog：Androidify（官方 expressive 参考实现）](https://android-developers.googleblog.com/2025/05/androidify-building-delightful-ui-with-compose.html)

**二手（已标注，用于节奏/采用情况）**
- [9to5Google：Android 16 QPR1 推送](https://9to5google.com/2025/09/03/android-16-qpr1-pixel) · [Android Authority：设置页改造](https://www.androidauthority.com/material-3-expressive-more-android-settings-3574011)
- Material Theme Builder 至今未完整支持 expressive token（issue #393，仓库已归档）。
