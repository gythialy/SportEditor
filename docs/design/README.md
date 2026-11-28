# Design System: Sport Editor

本文描述当前代码里**已经落地**的设计规格，是修改 UI 时的真源。形状、颜色与间距数值都可以在 `app/src/main/java/io/github/gythialy/sporteditor/ui/` 下找到对应实现；字阶数值来自 material3 1.5.0 BOM 的 M3 token，`Type.kt` 只是 `Typography()` 扩展点，没有写死。改样式时先改本文，再改代码，保持一致。

## 方向

**Live Circuit（通电仪表）**：同一台仪器，从待机拨到通电——链路会走线，读数会充能，卡片只在状态真的变了时亮一下。界面是深色为基准、单一信号色的仪器：发光只属于活的连接，数字仍是主角。它不是霓虹满屏、玻璃拟态、彩虹胶囊、无限光效，也不是游戏 HUD；没连通时界面更冷，不是更闪。用户打开它只做两件事：确认某个 App 现在的规则，或者改掉它。版本号、框架版本、日志、包名都收在诊断页。

语气靠三层关系建立：受控（松绿只出现在当前值/选中/主操作与活的连接，动画光源一屏不超过两处）、可预测（四个目标 App 常驻首页同一位置，卡片头部恒为「图标 → 名称 → 当前规则 → 开关」）、低声（说明文字 12sp 次级色，只有数值升到 36sp）。唯一的表达留给状态本身：断链提示、上限刻度尺、空日志纸页，都必须在静止帧可读，动效只是补充。

## 真源对照

| 主题 | 代码 |
| --- | --- |
| 调色板与派生色 | `theme/Color.kt`、`theme/Theme.kt`、`theme/color/Blend.kt` |
| 信号色 token | `theme/Signal.kt` |
| 字阶 | material3 1.5.0 BOM 的 M3 token；`theme/Type.kt` 只有 `Typography()` 与两个 ReadingMono 字号 |
| 形状 | `theme/Shapes.kt`、`theme/Morph.kt` |
| 动效 | `theme/Motion.kt` |
| 间距与宽度 | `theme/Dimensions.kt` |
| 插画与刻度尺 | `art/LinkBrokenArt.kt`、`art/EmptyLogArt.kt`、`art/EffectiveLimitStrip.kt`、`art/ConnectionMap.kt` |

## Colors

单强调色系统：一个深绿承载「当前值/选中」，一个近白底承载页面，其余是灰阶、一条错误红，以及唯一的第三种状态色 `tertiary`——它只用于首页推荐星标和诊断页 WARN 徽标，不为其它状态再配色相。下表是无动态取色时的 fallback；Android 12+ 默认走 `dynamicColor = true`，壁纸调色板会整组替换这些角色。

| 角色 | 浅色 | 深色 |
| --- | --- | --- |
| primary / onPrimaryContainer | `#006C4C` / `#00382A` | `#7EDCB2` / `#A9F0CC` |
| primaryContainer | `#CFF2E1` | `#24352C` |
| secondary / secondaryContainer | `#4D6357` / `#D0E8D9` | `#B3CCBC` / `#364B40` |
| background / surface | `#F1F6F2` | `#0F1512` |
| surfaceContainerLowest / Low | `#FFFFFF` | `#0A0F0C` / `#1A221E` |
| surfaceContainer | `#EAF1EC` | `#1F2823` |
| onSurface / onSurfaceVariant | `#1A1C1B` / `#5B6B60` | `#E6EDE7` / `#9FAEA5` |
| outline / outlineVariant | `#B6C2BA` / `#DDE7E0` | `#67766C` / `#263029` |
| error / onErrorContainer | `#BA1A1A` / `#410002` | `#FFB4AB` / `#FFDAD6` |

四个派生色不要重新发明：

- `cardContainerColor()`：用 `scheme.background.luminance() < 0.5f` 判断明暗——亮背景取 `surfaceContainerLowest`，暗背景取 `surfaceContainerLow`，保证卡片永远比页面亮一档。
- `cardBorderColor()`：同一个明暗判断下的 1dp 描边专用色——亮背景 `#7C8A80`，暗背景用 `outline`（`#67766C`）。它对卡片表面的对比度 ≥3:1；`outlineVariant` 只有约 1.2:1，不足以替代阴影的边缘职责。
- `alertWash()`：把 `error` 以 12% 叠在 `surfaceContainerLowest` 上，用于断链提示卡。不要直接铺 `errorContainer`——动态取色下它会变成一整块高饱和红。
- `brandAccent()`：同样按 `scheme.background.luminance() < 0.5f` 选品牌绿基准（深色 `DarkPrimary`、浅色 `GreenPrimary`），再向主机调色板 harmonize（最多转 15°色相，尽量保持色度与 tone，极端色相会被 gamut 映射裁剪色度）。只用于品牌锚点：状态圆点、关于行、版本标记；不要替代所有 `primary` 用法。

信号色集中在 Signal token 层管理：`signalCore = brandAccent()`、`signalWash = primary 16% over card`、`signalRim = cardBorderColor()`、`halo = primary 28%`。发光、走线与信标只从这里取色，不在组件里重算；文字永不压在光效上，正文对比度 ≥4.5:1、图形对比度 ≥3:1。

**Live Signal Rule**：一屏 ≤2 个动画光源；静态强调可出现在启用轨，不占用发光名额。第三个动画光源出现即视为装饰，改回静态墨色。

**Alert Is Earned Rule**：`error` 与 `alertWash` 只回答一个问题——现在有没有东西坏了、需要用户动手。未启用、未安装、只读、空列表都不是告警，用墨次级色表达；glow 永不用于告警。

**Wallpaper Substitution Rule**：绿色是 fallback，真正的不变量是关系：页面底低于卡片、卡片高于页面、主色只占「活的」位置。Signal token 扩展也随动态取色一起派生，不在组件里写死绿色；新增界面要在动态取色开/关两种状态下各看一遍。

## Typography

正文完全使用 Material 3 字阶：`Type.kt` 里只是 `Typography()`，36/44、22/28 等数值来自 material3 1.5.0 BOM 的 M3 token，不在 `ui/` 下写死。不打包字体；M3 字阶之外只有 `ReadingMono`（12sp）与 `ReadingMonoLabel`（11sp）两个自定义字号。层级靠 weight + 颜色 + 位置建立，中文与英文共用 M3 行高，混排时基线天然对齐。

- **Display Small Emphasized**（500，36/44，`tnum`）：数值输入框里的步数读数，全 App 最大字号，等宽数字保证 9→10 不顶开整行。
- **Title Large Emphasized**（500，22/28）：首页双行顶栏标题。
- **Title Medium**（500，16/24）：卡片里的 App 名称。
- **Title Small**（500，14/20）：修改方式标签、断链提示标题。
- **Body / Body Small**（400，14/20 与 12/16）：正文、摘要、换算预览、页脚。行长 ≤ 45 个中文字符或约 70 英文字符。
- **Label / Label Medium**（500/700，12/16）：胶囊、chip、分组标题、顶栏副标题。
- **Label Small**（500，11/16）：「未安装」徽标、日志类型标。
- **ReadingMono**：系统等宽字体，12/16，仅用于诊断页的时间戳、日志正文、环境值；`ReadingMonoLabel` 为 11sp。不打包 Roboto Mono——12sp 的诊断行不值得为 APK 增加约 200 KB 字体资源。

**Baseline-Only Rule**：要更强就用 weight 或颜色，要更弱就降一档字阶，不新增第三个自定义字号。

**Tabular Rule**：`withTabularNumerals()` 不只用于读数——顶栏副标题和诊断页多处（时间戳、环境值等）也用它，数字变化时位置不跳。

**One Monospace Zone Rule**：等宽字体只属于诊断页的「读数」，首页与设置页出现即视为串味。

## Shapes

走 Material 3 Expressive 八档刻度：`4 / 8 / 12 / 16 / 20 / 28 / 32 / 48`。

- **20dp**（`largeIncreased`）：卡片与分组容器，由 `cardShape()` 统一提供。信息密集的容器只用加大一档，不用 28/32dp。
- **28dp**（`extraLarge`）：底部 sheet 与对话框的常规大面圆角。
- **32dp**：星图内板；**48dp**：星图外板。只用于连接关系的封闭面，不扩散到卡片。
- **12dp**：卡内小块；**8dp**：chip；**4dp**：`OutlinedTextField` 默认形状。
- **CircleShape（50% 圆角）**：所有胶囊——连接按钮、步进圆钮静止态、分段控件两端、Tab 指示条圆头，统一由 `AppShapes.pill` 提供。
- **例外**：日志行 14dp、日志类型标 7dp、Tab 指示条 3dp 高 / 2dp 圆角（`AppShapes.tabIndicator`）。三者由自身高度决定，不要扩散。

形状变形只允许出现在小而短的表面：步进圆钮按下时从圆 morph 到 M3 `MaterialShapes.Square`（圆角约 0.3×56 ≈ 17dp，`rememberMorphShape` 每帧重建 `Path`，成本与面积成正比），以及星图 Hub 的连接/断链一次性状态切换。卡片、列表行、任何铺满宽度的面都不做 morph。

**20 / 28 / 12 Rule**：三个值之外的圆角必须能说出理由（当前是日志行、日志类型标、Tab 指示条与星图内外板）。

## Motion

动效是物理语义而非时长常量。`SportEditorTheme` 把 `motionScheme = MotionScheme.expressive()` 交给 `MaterialTheme`，Switch、sheet、dialog、Tab 等库组件自动走弹簧；刻意不用 `MaterialExpressiveTheme`，它的默认调色板会压平深色与动态取色。

| 角色 | 实现 |
| --- | --- |
| `press()` | 100ms tween，M3 standard easing；触摸回执必须在下一次输入前结束 |
| `settle()` | effects 弹簧，临界阻尼；读数永不过冲，且能吸收 80ms 连击的重新指向 |
| `glide()` | `DampingRatioNoBouncy` / `StiffnessMedium` 弹簧；选中块与 tab 信号头的位置过渡，不回弹 |
| `pulse()` | 520ms tween，标准 easing，一次性走线；不循环、不常亮 |
| `enter()` | spatial 弹簧；卡片展开、列表插入、页面进入 |
| `leave()` | 150ms tween，emphasized-accelerate；退出比进入快，不挡下一个状态 |

编排常量：开关翻转后 80ms 卡片才展开（`SWITCH_LEAD_MS`），逐行出现间隔 60ms（`STAGGER_STEP_MS`）；启用卡入场间隔 70ms（`ENERGY_STAGGER_MS`，四卡共 210ms），值提交闪光停留 900ms（`PULSE_DECAY_MS`），连接信标周期 2400ms（`BEACON_CYCLE_MS`）。

打断规则：值变化让进行中的动画 retarget，而不是排队重启；长按 morph 在松手时取消；exit 固定 150ms；日志只在稳定 key 入场。所有 delay/循环都先经过 `animationsEnabled()`；关闭时 tween/弹簧由系统时长缩放折叠，代码另外拦住 `delay()` 编排与无限循环插画。

## Layout

单栏、自上而下、行内编辑。间距只有四档（`Dimensions`）：页面内边距 16dp、卡片内边距 16dp、卡间距 12dp、组内间距 8dp。滚动容器统一 `LazyColumn` + `contentPadding`，不靠子项 margin 制造间距。

首页是一条决策轴：顶栏（标题 + 连接按钮）→ 断链提示（仅异常时）→ 每个目标 App 一张卡片 → 页脚。卡片展开只影响自身高度，不改变其它卡片结构。收起态最密（一屏看全四个 App），展开态最松，设置页居中，诊断页最松且可滚动。触摸目标一律 ≥ 48dp。

响应式是**单栏限宽**：内容列以 `Dimensions.ContentMaxWidth = 640dp` 为上限并居中（这 640dp 含两侧各 16dp 内边距，实际文字列 608dp），超出的宽度留白。用 `windowWidthDp()` 读窗口宽度而非屏幕宽度，分屏/折叠屏下才不会把窄窗当手机。顶栏保持通栏，限宽会留下可见接缝。新增布局都要在 320dp、411dp、1.3× 字号下验证不裁切。

## Elevation & Depth

**Earned Glow Rule**：推翻零阴影，但只推翻一点点——发光只给连接态，一屏 ≤2 处。光是同心 alpha 环，不用模糊或玻璃；浅色主题只留 1dp 亮边，卡片阴影依旧禁止（没有 `shadowElevation`，`CardDefaults.cardElevation(defaultElevation = 0.dp)`）；关动效时保留同形状的静态低 alpha。深度仍靠三层关系：页面底 → 卡片 → 卡内小块，每层差一档 `surfaceContainer*`；主卡、设置卡、日志卡用 1dp `cardBorderColor()` 描边替代阴影的边缘职责，断链卡与诊断页其余卡片不描边；越靠近全局的元素用越接近底色的容器（顶栏 `background`、Tab `surface`），越内联的用更亮的容器。弹层、sheet、`DropdownMenu` 沿用 M3 自带高程。

**No-Shadow Rule**：需要让一个面浮起来，就提升 `surfaceContainer` 档，或在主卡/设置卡/日志卡上加 1dp 描边；卡片阴影依旧禁止，玻璃拟态与模糊背景已确认是反参考。

## Components

- **ConnectionSplitButton**：顶栏的连接控件改为 32dp 迷你星图——中心 Hub + 四目标节点，连通时两层淡光环，断链时连线中点留断口 + 红点；点击仍报告状态并打开连接 sheet。
- **AppCard**：启用卡左侧一条 3dp 带电线；值提交时闪一次 900ms（只在真的提交时），读数计数落位并配扫光下划线。卡片不因常驻状态发光。
- **DisconnectedCard**：整卡可点，`alertWash()` 底，顶部是一条 16dp 高的 `LinkBrokenArt`，下面依次是警告图标 + 标题 + 右箭头、单行说明。它是提示而不是大横幅，不占一屏。
- **Mode row**：四段修改方式用 `ToggleButton` + `ButtonGroupDefaults` 的相连形状，等分宽度；建议标记做角标，不挤进标签文字。不用 `ButtonGroup` 组件本身——当前 material3 版本的溢出测量在满宽折叠时会抛异常。
- **StepButton**：56dp 圆钮，容器用 `surfaceContainer` 系；按下 400ms 后每 80ms 连续步进，每次外扩一圈 6dp 淡环，同时圆 → `MaterialShapes.Square`（约 17dp 圆角）做形状回执，松手取消 morph。禁用态掉到 `surfaceVariant` + `onSurfaceVariant`。
- **ValueField**：`OutlinedTextField`，读数用 `displaySmallEmphasized` + `tnum`，居中，单位写在 label 位（不再用右侧 suffix）；失焦或 IME 完成时提交，非法值回滚。
- **Preset chips**：横向滚动的 `FilterChip`，选中态用主色容器档。
- **EffectiveLimitStrip**：每日上限下方的刻度尺，五条刻度标出基础值到 +100 的完整随机区间，主色圆点标出今天的位置；启用段加 16% 洗光带与双层 `halo`。它刻意仍不是进度条——数值不是「用了多少」，而是「落在哪个区间」。位置变化 600ms `FastOutSlowInEasing`。每日上限行的数值与箭头、刻度尺的「今日 +N」都用墨次级色（`onSurfaceVariant`），不占动画光源名额。
- **EmptyLogArt**：空日志的折角纸页，随主题取色；搜索态可叠放大镜。
- **Navigation**：首页与二级页用 `TopAppBar`（容器色 `background`）；诊断页 `TabRow` 贴在 AppBar 下方、与内容列同轴，3dp 高、2dp 圆角（`AppShapes.tabIndicator`）的主色短指示条，不钉在底部也不充当导航。二级页只靠系统返回手势退出，不新增抽屉或底部导航。破坏性操作（清空、恢复默认）必须二次确认。
- **Diagnostics Tab**：tab 信号头随 `glide()` 走位；ERROR 行左侧一条 2dp 轨；空日志只在稳定 key 入场时一次性打印，不循环。
- **Settings**：设置页可切换主题模式（跟随系统 / 浅色 / 深色），入场按 70ms stagger；并可开关动态取色（仅 Android 12+ 有系统调色板，低版本隐藏该开关），开关旁给三色小样；选择写入偏好，进入应用时应用。
- **ConnectionSheet / Environment**：连接 sheet 与环境页各放一张 144dp 星图，沿用中心 Hub + 目标节点的实线/断口语义。

## 品牌记忆点：接线瞬间

连接成功时，一次 `pulse()` 从 Hub 沿连线跑向已启用节点，节点落下一圈 6dp 环后安静；断链时连线中点断开、闪一次火花，然后永久保留断口。动画只是讲述切换，静止帧就能读懂：实线 = 接通，断口 = 断链，空心节点 = 未启用。新增连接表现不得增加第二个循环光源，也不得把状态解释移到动效里。

## Do's and Don'ts

**Do**

- 首页收起态一屏放下全部目标 App，异常提示只占一条。
- 用 36sp 数值 + 12sp 说明建立层级：数字是主角，解释是注脚。
- 用 20dp 包住每个「可整体操作」的对象，内部再用 12dp 小块分组。
- 动态取色开/关各验证一次主色、告警色、卡片与页面底的关系。
- 保持 48dp 以上触摸目标；视觉更小时用外扩点击区。
- 新增插画用 `Canvas` + 主题色绘制，跟随深色与动态取色。

**Don't**

- 不加卡片阴影或玻璃；渐变只在 1-3dp 带电线；层级只用表面色差 + 限定在主卡/设置卡/日志卡的 1dp 描边。
- 不把 `error` 红用在中性状态：「未安装」「已关闭」「空日志」都不是告警。
- 不新增第四种色相；`tertiary` 只留给推荐星标与 WARN 徽标，动画光源一屏 ≤ 2 处。
- 不做无限循环光效；仅连接信标可循环（`BEACON_CYCLE_MS = 2400`），且必须走 `animationsEnabled()`。
- 不新增 M3 字阶之外的字号（现有例外只有 ReadingMono 12sp 与 ReadingMonoLabel 11sp）；层级靠 weight 与颜色。
- 不在诊断页之外使用等宽字体。
- 不把 tab 条钉在屏幕底部当导航，也不给二级页新增抽屉。
- 不在卡片里重复全局状态：顶栏连接按钮已经说明状态，卡片摘要不再重复。
