---
name: Sport Editor
description: LSPosed 步数改写器 —— 一套安静、分层靠色调与描边的 Material 3 仪器界面。
colors:
  huizhou-pine: "#006C4C"
  huizhou-pine-light: "#7EDCB2"
  pine-wash: "#CFF2E1"
  pine-ink: "#00382A"
  slate-teal: "#4D6357"
  teal-wash: "#D0E8D9"
  page-mist: "#F1F6F2"
  page-void: "#0F1512"
  card-white: "#FFFFFF"
  card-charcoal: "#1A221E"
  panel-light: "#EAF1EC"
  panel-dark: "#1F2823"
  ink: "#1A1C1B"
  ink-dark: "#E6EDE7"
  ink-muted: "#5B6B60"
  ink-muted-dark: "#9FAEA5"
  row-rule: "#DDE7E0"
  row-rule-dark: "#263029"
  alert: "#BA1A1A"
  alert-light: "#FFB4AB"
  alert-wash: "#FFDAD6"
  alert-wash-dark: "#690005"
  alert-ink: "#410002"
typography:
  headline:
    fontFamily: "Roboto, 'Noto Sans SC', system-ui, sans-serif"
    fontSize: "24px"
    fontWeight: 400
    lineHeight: "32px"
  title-lg:
    fontFamily: "Roboto, 'Noto Sans SC', system-ui, sans-serif"
    fontSize: "22px"
    fontWeight: 400
    lineHeight: "28px"
  title:
    fontFamily: "Roboto, 'Noto Sans SC', system-ui, sans-serif"
    fontSize: "16px"
    fontWeight: 500
    lineHeight: "24px"
  title-sm:
    fontFamily: "Roboto, 'Noto Sans SC', system-ui, sans-serif"
    fontSize: "14px"
    fontWeight: 500
    lineHeight: "20px"
  body:
    fontFamily: "Roboto, 'Noto Sans SC', system-ui, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: "20px"
  body-sm:
    fontFamily: "Roboto, 'Noto Sans SC', system-ui, sans-serif"
    fontSize: "12px"
    fontWeight: 400
    lineHeight: "16px"
  label:
    fontFamily: "Roboto, 'Noto Sans SC', system-ui, sans-serif"
    fontSize: "12px"
    fontWeight: 500
    lineHeight: "16px"
  label-sm:
    fontFamily: "Roboto, 'Noto Sans SC', system-ui, sans-serif"
    fontSize: "11px"
    fontWeight: 500
    lineHeight: "16px"
  reading-mono:
    fontFamily: "'Roboto Mono', ui-monospace, SFMono-Regular, Menlo, monospace"
    fontSize: "12px"
    fontWeight: 400
    lineHeight: "16px"
rounded:
  card: "16px"
  log-card: "14px"
  block: "12px"
  chip: "8px"
  micro: "7px"
  field: "4px"
  indicator: "2px"
  pill: "999px"
spacing:
  hairline: "2px"
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "24px"
  xxl: "32px"
components:
  card-surface:
    backgroundColor: "{colors.card-white}"
    textColor: "{colors.ink}"
    rounded: "{rounded.card}"
    padding: "16px"
    width: "100%"
  status-pill-connected:
    backgroundColor: "{colors.pine-wash}"
    textColor: "{colors.pine-ink}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "6px 12px"
    height: "28px"
  status-pill-disconnected:
    backgroundColor: "{colors.alert-wash}"
    textColor: "{colors.alert-ink}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "6px 12px"
    height: "28px"
  step-button:
    backgroundColor: "{colors.pine-wash}"
    textColor: "{colors.pine-ink}"
    typography: "{typography.title-lg}"
    rounded: "{rounded.pill}"
    size: "48px"
  step-button-disabled:
    backgroundColor: "{colors.panel-light}"
    textColor: "{colors.ink-muted}"
    typography: "{typography.title-lg}"
    rounded: "{rounded.pill}"
    size: "48px"
  value-field:
    backgroundColor: "transparent"
    textColor: "{colors.ink}"
    typography: "{typography.headline}"
    rounded: "{rounded.field}"
    padding: "12px 16px"
    height: "72px"
  segmented-selected:
    backgroundColor: "{colors.pine-wash}"
    textColor: "{colors.pine-ink}"
    typography: "{typography.title-sm}"
    rounded: "{rounded.pill}"
    padding: "0 12px"
    height: "40px"
  segmented-unselected:
    backgroundColor: "transparent"
    textColor: "{colors.ink}"
    typography: "{typography.title-sm}"
    rounded: "{rounded.pill}"
    padding: "0 12px"
    height: "40px"
  preset-chip-selected:
    backgroundColor: "{colors.pine-wash}"
    textColor: "{colors.pine-ink}"
    typography: "{typography.label}"
    rounded: "{rounded.chip}"
    padding: "0 12px"
    height: "32px"
  log-row:
    backgroundColor: "{colors.card-white}"
    textColor: "{colors.ink}"
    typography: "{typography.reading-mono}"
    rounded: "{rounded.log-card}"
    padding: "12px 14px"
  settings-row:
    backgroundColor: "transparent"
    textColor: "{colors.ink}"
    typography: "{typography.title}"
    padding: "16px"
    height: "68px"
---

# Design System: Sport Editor

## Overview

**Creative North Star: "The Quiet Instrument"**

这套界面是一件仪器，不是一块仪表盘。它在一切正常时不说话：没有阴影、没有装饰性渐变、没有为每个状态准备的彩色胶囊；页面底色本身就承担了"分层"的职责，卡片只是比底色再亮一档。用户打开它的理由只有两个——确认某个 App 现在被改成什么样，或者把它改掉。其余的一切（版本号、框架版本、日志、包名）都被压到诊断页。

语气是**克制、可预测、低声**的三层关系。克制体现在颜色配额：徽州松绿只出现在"当前值""选中态""主要操作"上，一屏不超过三处；可预测体现在位置：四个目标 App 常驻首页同一个位置，卡片头部永远是"图标 → 名称 → 当前规则 → 开关"，展开不改变这条轴；低声体现在文字：说明性文案用 12sp 的正文色次级，只有跨过阈值的数值才升到 24sp。

唯一的"表达"留给状态本身——未连接时出现一条告警淡染的提示卡，每日上限旁边出现一枚刻度尺，日志为空时出现一张手绘纸页。它们都是**可在静止帧读懂**的插画，不用动效讲信息。

**Key Characteristics:**
- 纯色调分层：零阴影，卡片靠 `{colors.card-white}` 与底色 `{colors.page-mist}` 的 1 档差 + 1dp 描边浮起
- 一张卡片 = 一个目标 App；展开就地编辑，不跳二级页
- 颜色是状态语言，不是装饰：绿 = 当前值/选中，红 = 需要处理
- 字阶完全等于 Material 3 基线，没有自定义字号
- 插画全部为 `Canvas` 手绘（断链、空纸、更新环、上限刻度），跟随主题与动态取色

## Colors

整套色彩是"墨绿 + 雾白"的单强调色系统：一个深绿承载所有"当前值/选中"，一个近白底承载页面，其余全是灰阶与一条错误红。

### Primary
- **Huizhou Pine（徽州松）** (`#006C4C` / 深色 `#7EDCB2`): 唯一强调色。用于当前选中的分段按钮、步进圆钮的容器、每日上限的现行数值、`Switch` 打开态、`TabRow` 指示条。深色主题把它提亮到 `#7EDCB2`，因为深绿在 `#0F1512` 上无法达到文字对比度。
- **Pine Wash（松雾）** (`#CFF2E1`): 主色的容器档。承载"被选中"的填充面（分段按钮、chip、步进钮），是**唯一允许大面积铺浅绿**的地方。
- **Pine Ink（松墨）** (`#00382A`): 松雾上的文字色。松雾 + 松墨是深色文字配浅绿容器的最小对比对。

### Secondary
- **Slate Teal（板岩青）** (`#4D6357`): 材质层的第二中性色，用于 `secondary`/`secondaryContainer` 角色；实际渲染里出场极少，不要主动扩大它的使用面积。

### Neutral
- **Page Mist（雾面）** (`#F1F6F2`): 浅色主题的页面底。带极轻的绿灰，让白卡能"浮"起来。
- **Page Void（空膛）** (`#0F1512`): 深色主题的页面底，绿调近黑。
- **Card White（纸白）** (`#FFFFFF`) / **Card Charcoal（炭面）** (`#1A221E`): 卡片表面。浅色用 `surfaceContainerLowest`（纯白），深色用 `surfaceContainerLow`，保证两种主题下卡片都比页面亮一档。
- **Panel Light（面板）** (`#EAF1EC`) / **Panel Dark** (`#1F2823`): `surfaceContainer` 档，用于滚动后的顶栏、按下态底、嵌套小块。
- **Ink（墨）** (`#1A1C1B`) / **Ink Dark** (`#E6EDE7`): 主文字。
- **Ink Muted（墨次级）** (`#5B6B60`) / **Ink Muted Dark** (`#9FAEA5`): 说明文字、摘要、页脚。
- **Row Rule（行线）** (`#DDE7E0`) / **Row Rule Dark** (`#263029`): 1dp 分隔线与卡片描边（`outlineVariant`）。
- **Alert（告警红）** (`#BA1A1A` / 深色 `#FFB4AB`): 只用于"需要用户处理"的状态。
- **Alert Wash（告警底）** (`#FFDAD6` / 深色 `#690005`): 未连接提示卡的底，配 **Alert Ink** (`#410002`) 作正文色。实现上用 `alertWash()`：把告警色以 12% 叠在卡片底色上，而不是直接铺 `errorContainer` —— 后者在深色模式的动态取色下会变成整块高饱和红，成为全 App 最响的表面。

### Named Rules

**The Pine Quota Rule.** 徽州松一屏最多出现三处"活的"用法（当前值、选中态、主操作）。第四处出现即视为装饰，必须改回墨色。

**The Alert Is Earned Rule.** `alert` 与 `alertWash` 只回答一个问题：「现在有没有东西坏了、需要用户动手？」未启用、未安装、只读、空列表都**不是**告警，用墨次级色表达。状态圆点与它描述的文字同色：未连接时是告警红且静止，只有真正连上的一端才呼吸。

**The Wallpaper Substitution Rule.** Android 12+ 的动态取色是合法的第一优先级：壁纸主色会整组替换徽州松及其容器档（`SportEditorTheme(dynamicColor = true)`）。因此本文件的绿色是**fallback**，真正的不变量是关系而非色值：页面底永远低于卡片、卡片永远高于页面、主色永远只占"活的"位置。新增界面必须在动态取色开启和关闭两种情况下各看一遍。

## Typography

**Display Font:** 无 display 层级 —— 这套界面没有需要 36sp 以上的场景。
**Body Font:** Roboto / Noto Sans SC（系统默认，未打包字体）
**Label/Mono Font:** Roboto Mono（仅诊断页读数）

**Character:** 完全使用 Material 3 基线字阶，不做字号自定义。层级靠**weight + 颜色 + 位置**建立，而不是靠放大。中文与英文共用同一套 M3 字阶，行高按 M3 默认，因此中英混排时基线天然对齐。

### Hierarchy
- **Headline** (400, 24/32): 数值输入框里的数字、连接弹窗的标题。整个 App 里最大的字号。
- **Title Large** (400, 22/28): 首页顶栏标题。
- **Title** (500, 16/24): 卡片里的 App 名称、设置行标题。
- **Title Small** (500, 14/20): 分段按钮标签、提示卡标题行。
- **Body** (400, 14/20): 正文、列表副标题、键值行的值。
- **Body Small** (400, 12/16): 卡片摘要、说明文字、页脚、换算预览。正文行长控制在 ≤ 45 个中文字符或 ~70 英文字符。
- **Label** (500, 12/16): 分组标题、状态胶囊文字、chip 标签、字段标签。
- **Label Small** (500, 11/16): "未安装"这类紧密徽标、日志类型标。
- **Reading Mono** (400, 12/16, mono): 诊断页的时间戳、日志消息、环境值。

### Named Rules

**The Baseline-Only Rule.** 不新增字号、不写 `fontSize = 18.sp`。要更强，用 weight 或颜色；要更弱，降一档字阶。当前代码里没有任何 `fontSize` 覆盖，这个状态要保持。

**The One Monospace Zone Rule.** 等宽字体只属于诊断页的"读数"（时间、日志正文、环境值、配置串）。首页、设置页出现等宽字体即视为串味。

## Layout

单栏、自上而下、行内编辑。页面左右内边距 `{spacing.lg}`（16dp），列表项之间 `{spacing.md}`（12dp），卡片内边距 `{spacing.lg}`（16dp），页面底部 `{spacing.xl}`–`{spacing.xxl}`。滚动容器统一 `LazyColumn` + `contentPadding`，不靠子项 margin 制造间距。

首页的拓扑是一条**决策轴**：顶栏（标题 + 全局状态）→ 状态提示（仅异常时出现）→ 每个目标 App 一张卡片 → 页脚。卡片内部再分两层：常驻头部（图标 48dp、名称、当前规则摘要、展开指示、开关）与展开区（修改方式分段控件 → 数值步进 → 预设 chip → 换算预览 → 每日上限）。展开区只影响这一张卡片的高度，不影响其它卡片的结构。

密度由使用频率决定：首页收起态最密（一屏要能看全所有目标 App），展开态最松（编辑中），设置页居中（分组列表），诊断页最松且可滚动（日志）。触摸目标一律 ≥ 48dp —— 步进圆钮 48dp、设置行 68dp、开关 52×32dp、图标按钮 48dp。

响应式是**单栏限宽**：320dp 到平板横屏共用一条轴，内容列以 `Dimensions.ContentMaxWidth = 640dp` 为上限并居中，超出的宽度留白而不是把卡片拉满。顶栏与状态栏保持通栏——栏底色与页面同色，把它也限宽只会留下一条可见的接缝。任何新增布局都必须先在 320dp、411dp、1.3× 字号三个条件下验证不裁切、不换行错位。

## Elevation & Depth

**零阴影。** 整个 App 没有一处 `shadowElevation` 或 `tonalElevation > 0`：卡片一律 `CardDefaults.cardElevation(defaultElevation = 0.dp)`。深度由三层关系表达：

1. **表面色差**：页面底（`page-mist` / `page-void`）→ 卡片（`card-white` / `card-charcoal`）→ 卡内小块（`panel-light` / `panel-dark`），每层差一档 `surfaceContainer*`。
2. **1dp 描边**：卡片右用 `outlineVariant`（`row-rule` / `row-rule-dark`）画 1dp 边框，让白卡在浅色底上仍有轮廓，替代阴影的"边缘"职责。
3. **层叠顺序即层高**：越靠近"全局"的元素（顶栏、底部 tab）用 `surface`，越"内联"的元素用更亮的容器。

弹窗、底部 sheet、`DropdownMenu` 沿用 Material 3 自带的高程，不额外叠加自定义阴影。

### Named Rules

**The No-Shadow Rule.** 不加 `shadowElevation`。需要让一个面"浮起来"，就把它的 `surfaceContainer` 档提升一级或加 1dp 描边，不要加阴影。已确认的反参考：玻璃拟态与模糊背景。

## Shapes

圆角是这套系统的主要"手感"来源，只有三个层级 + 两个例外：

- **16dp** —— 卡片与分组容器（应用卡、设置组、诊断信息卡）。这是唯一的"大面"圆角。
- **12dp** —— 卡内嵌的小块（连接弹窗里的框架信息块、日志行内的类型标底、诊断页的可复制块）。
- **999dp** —— 所有胶囊：状态胶囊、步进圆钮、分段按钮的两端、Tab 指示条两端的圆头。
- **8dp** —— chip（预设、筛选）。
- **4dp** —— `OutlinedTextField` 的默认形状（数值输入框）。
- **7dp / 14dp / 2dp** —— 例外：日志类型标 7dp、日志行 14dp、Tab 指示条 2dp。它们各自的尺寸决定了圆角，不要扩散。

描边统一 1dp `outlineVariant`，不做双重边框、不用 inset ring。分割线用 1dp `HorizontalDivider`，与描边同色。

### Named Rules

**The 16 / 12 Rule.** 大面 16dp、卡内小块 12dp、胶囊 999dp。三个值之外的圆角必须能说出理由（当前只有日志行 14dp、日志标 7dp、指示条 2dp 三处）。

## Components

### Buttons

- **Shape:** 圆形步进钮（999dp 胶囊），48×48dp。
- **Primary（步进钮）:** `{colors.pine-wash}` 底 + `{colors.pine-ink}` 字，符 号用 `title-lg`（22sp）；禁用态掉到 `{colors.panel-light}` + `{colors.ink-muted}`。长按 400ms 后每 80ms 连续步进。
- **Hover / Focus:** 移动端无 hover；按下态由 Material 3 ripple 承担，不自绘。
- **Text button（「说明」「查看步骤」「检查更新」）:** 无底色，`{colors.huizhou-pine}` 文字，`label` 或 `body` 字阶，`contentPadding` 水平 8dp。
- **Filled / Elevated:** 不使用。整个 App 没有主 CTA 按钮 —— 卡片本身就是操作面。

### Chips
- **Style:** 预设 chip 用 `FilterChip`：未选中透明底 + `outlineVariant` 描边，选中 `{colors.pine-wash}` 底 + `{colors.pine-ink}` 字，圆角 8dp，高 32dp。
- **State:** 诊断页的筛选 chip 用同一套规则，选中态额外加主色描边；chip 内可以带计数（「全部 3」）。

### Cards / Containers
- **Corner Style:** 16dp。
- **Background:** 浅色 `{colors.card-white}`，深色 `{colors.card-charcoal}`（由 `cardContainerColor()` 按主题亮度选 `surfaceContainerLowest` / `surfaceContainerLow`）。
- **Shadow Strategy:** 无（见 Elevation & Depth）。
- **Border:** 1dp `{colors.row-rule}` / `{colors.row-rule-dark}`。
- **Internal Padding:** 16dp；卡内分组间距 12dp。

### Inputs / Fields
- **Style:** `OutlinedTextField`，4dp 圆角，1dp 描边，透明底；数值用 `headline`（24sp）居中，右侧带单位后缀（「倍」「步」）。
- **Focus:** 描边变主色、label 浮起到缺口；不发光、不加阴影。
- **Error / Disabled:** 非法值时描边与 `supportingText` 转 `{colors.alert}`，输入框本身不填色；只读字段用 `{colors.ink-muted}` 文字。
- **行为约定:** 输入框失焦或 IME「完成」时提交，非法值回滚；每日上限的草稿值在输入过程中即时反映到卡片摘要。

### Navigation
- **Style:** 首页 `TopAppBar`（双行标题：22sp 标题 + 12sp 统计）+ 右侧状态胶囊与设置图标；二级页 `TopAppBar` + 返回箭头。深色/浅色都让顶栏与页面同底，滚动后切到 `{colors.panel-light}`。
- **Tab:** 诊断页用 `TabRow`（图标 18dp + 文字），选中态是一条 3dp 主色短指示条（圆头，两端留 42dp）。它贴在 AppBar 下方，与内容列同轴；不钉在底部、也不充当导航。
- **Menu:** 顶栏溢出菜单用 `DropdownMenu`（复制 / 分享 / 清空），破坏性操作（清空、恢复默认）必须二次确认。

### Signature Components

**State Pill（状态胶囊）.** 8dp 圆点 + `label` 文字，胶囊 999dp：已连接用 `{colors.pine-wash}` + 主色圆点，未连接用 `{colors.alert-wash}` + 告警红圆点。它是全 App 唯一"始终可见"的状态表达，点击打开连接详情 sheet。

**Effective Limit Strip（上限刻度尺）.** 每日上限下方的一条刻度尺：`outlineVariant` 轨道 + `outline` 刻度 + 主色指针，标注"基础值"与"今日随机偏移"。刻意做成**刻度而不是进度条**——数值不是"用了多少"，而是"落在随机区间的哪一格"。动画 600ms `FastOutSlowIn`。

**Hand-drawn Canvas Art.** 三件插画都是 `Canvas` 原语绘制、随主题取色：断链（两节点始终连不上，信号点飞入缺口后淡出）、空纸页（带折角，搜索态配放大镜）、更新环（旋转弧）。它们必须在**静止帧**就能读懂，动效只是补充；系统关闭动画时退化为静态图形。

## Do's and Don'ts

### Do:
- **Do** 让首页收起态一屏放下全部目标 App（4 个 × 80dp + 间距），异常提示只占一条而不是一张大卡。
- **Do** 用 24sp 的数值 + 12sp 的说明建立层级：数字是主角，解释是注脚。
- **Do** 用 `{rounded.card}` 16dp 包住每一个"可整体操作"的对象，内部再用 12dp 小块分组。
- **Do** 在动态取色开启与关闭两种情况下各验证一次主色、告警色、卡片与页面底的关系。
- **Do** 保持 48dp 以上的触摸目标，即使视觉尺寸更小（图标按钮 48dp、chip 高 32dp 时用外扩点击区）。
- **Do** 任何新增插画都用 `Canvas` + 主题色绘制，保证跟随深色与动态取色。

### Don't:
- **Don't** 加阴影或玻璃拟态；层级只用表面色差 + 1dp 描边表达（已确认的反参考）。
- **Don't** 把 `alert` 红用在中性状态上 —— 「未安装」「已关闭」「空日志」都不是告警，用次级墨色表达（已按此修：未安装徽标改用 `onSurfaceVariant`）。
- **Don't** 为每个状态配一种颜色；主色一屏 ≤ 3 处"活的"用法，其余用墨色分层。
- **Don't** 新增字号或覆盖 `fontSize`；层级靠 weight 与颜色。
- **Don't** 在诊断页之外使用等宽字体。
- **Don't** 把 tab 条钉在屏幕底部当导航用 —— 诊断页的三个视图属于同一页内的切换，应贴在 AppBar 下方（已实施：tab 条位于 AppBar 下方）；二级页只靠系统返回手势退出，不新增抽屉或底部导航。
- **Don't** 在卡片里重复全局状态：顶栏胶囊已经说明「已连接 / 未连接」，卡片摘要不再重复表达连接状态。
