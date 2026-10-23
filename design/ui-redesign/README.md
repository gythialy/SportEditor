# SportEditor UI 改版设计稿

使用 Google Stitch 生成的高保真设计稿，目标是让现有功能（4 个应用 × 4 种修改方式 + 每日上限 + 诊断）在
Material 3 下更简洁、层级更清楚。本目录只放设计稿与规格，不改动 `app/src` 代码。

| 文件 | 内容 |
| --- | --- |
| `01-home.png` | 首页 · 已连接 + 微信卡片展开态 |
| `02-home-disconnected.png` | 首页 · LSPosed 未连接 + 全部收起 |
| `03-settings.png` | 设置页 · 分组列表 |
| `04-diagnostics.png` | 诊断页 · 环境 / 目标应用 / 日志 |
| `05-home-dark.png` | 首页 · 深色主题 |

Stitch 项目：`12147274081285217635`（[stitch.withgoogle.com/projects/12147274081285217635](https://stitch.withgoogle.com/projects/12147274081285217635)），
设计系统 `Motion Engine Material System`。任一屏都可以继续用自然语言改稿（例如“微信卡片的预设改成 5 档”）。

## 设计原则

1. **一屏看全状态**：4 个应用常驻首页，收起态一行讲清「应用 + 当前规则」，点开就地编辑，不跳二级页。
2. **开发者信息降级**：包名、enabled/mode/value 等原始字段只在诊断页出现，首页不再出现 `包名：com.tencent.mm`。
3. **少一个维度就少一层控件**：修改方式用四段分段按钮，数值用大号数字 + 步进 + 预设，每日上限收进可展开行。
4. **状态用色不多但明确**：连接状态一个胶囊、未连接一张提示卡、错误只在输入非法时出现。

## 设计令牌

| 角色 | 浅色 | 深色 |
| --- | --- | --- |
| 页面背景 | `#F1F6F2` | `#0F1512` |
| 卡片 / 表面 | `#FFFFFF` | `#1A221E` |
| 描边 / 分隔线 | `#DDE7E0` / `#E5EDE7` | `#263029` / `#253029` |
| 主色 primary | `#006C4C` | `#7EDCB2` |
| 主色容器 primaryContainer | `#CFF2E1` | `#24352C` |
| 主文字 | `#1A1C1B` | `#E6EDE7` |
| 次要文字 | `#5B6B60` | `#9FAEA5` |
| 弱文字 / 页脚 | `#7C8A80` | `#6F7E74` |
| 禁用文字 | `#B6C2BA` | `#67766C` |
| 错误容器 / 错误 | `#FFDAD6` / `#93000A` | 由 M3 深色 error 角色派生 |
| 开关关闭轨道 | `#D3DDD6` | `#263029` |

- 字体：Noto Sans SC / Roboto；标题 26sp SemiBold，卡片标题 16sp Medium，正文 14sp，辅助 12sp，大号数值 40sp Medium。
- 间距：8dp 网格，页面左右 16dp，卡片间距 12dp，卡片内边距 16dp，行高 ≥ 48dp。
- 形状：卡片 16dp 圆角；胶囊 / 分段按钮 999dp；预设 chip 8dp。
- 层级：不用阴影，靠表面色差 + 1dp 描边；动态取色生效时上表仅作 fallback。

## 屏幕结构

### 首页（`01` / `02` / `05`）

- `LargeTopAppBar`：标题「运动修改器」+ 一行统计副标题「4 个应用 · 2 个已启用」；右侧状态胶囊 + 齿轮。
- 状态胶囊：已连接为 `primaryContainer` 底 + 圆点；未连接为 `errorContainer` 底 + 红点。点击弹出底部弹窗显示
  框架名称 / 版本 / API 与服务说明。
- 未连接提示卡（仅未连接时）：`errorContainer`、标题 + 两行说明 +「查看步骤」。
- 应用卡片：
  - 收起态：图标 48dp、标题、摘要（`×100 · 上限 80,000`、`自动增加 · 10,000 步`、`已关闭`）、chevron、`Switch`。
  - 展开态：头部相同（无 chevron），下方依次为分段按钮、数值步进、预设 chips、换算说明、「每日上限」可展开行、
    右上角 `⋮` 菜单（恢复默认）。
  - 未启用时图标与副标题 72% 不透明度；未安装时图标位置仍显示字母徽标，摘要后追加「未安装」标签、开关禁用。
- 页脚：免责声明、版本，`bodySmall` + 弱文字色（已去掉作用域一行）。
- 应用图标通过 `PackageManager` 自动读取已安装应用的真实图标，未安装时才使用字母徽标。

### 设置（`03`）

- `TopAppBar` 返回箭头 + 「设置」。
- 分组列表：一张卡片一组、行间 1dp 分隔线，组标题 12sp；「通用」= 语言 / 主题 / 版本更新 / 诊断，「关于」= 项目主页 /
  开源许可证 / 版本。每行左侧线性图标 24dp，右侧箭头或操作按钮。
- 主题行打开对话框，可选「跟随系统 / 浅色 / 深色」，与语言选择一致。

### 诊断（`04`）

- `TopAppBar` 返回 + 溢出菜单 `⋮`（复制 / 分享 / 清空），替代原来的三个并列文字按钮。
- 环境：6 行键值对，右侧数值等宽字体。
- 目标应用：每行应用名 + `enabled · multiply · 100 · max 80,000` + 状态点。
- 日志：分段筛选（全部 / 框架 / 配置 / Hook / 错误）+ 等宽时间 + 类型色标 + 单行省略的消息。

## Compose 组件映射

| 设计元素 | Compose |
| --- | --- |
| 可折叠大标题 | `LargeTopAppBar` + `TopAppBarDefaults.exitUntilCollapsedScrollBehavior` |
| 状态胶囊 | `Surface(shape = CircleShape, color = …)` + `Row`（8dp 圆点 + `labelMedium`） |
| 应用卡片 | `Card(onClick = …, colors = cardColors(containerColor = surface), border = BorderStroke(1.dp, outlineVariant))` + `Modifier.animateContentSize()` |
| 展开 / 收起 | `AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn())` |
| 修改方式 | `SingleChoiceSegmentedButtonRow` + `SegmentedButton(shape = SegmentedButtonDefaults.itemShape(i, 4))`，微信固定值 `enabled = false` |
| 数值步进 | `FilledTonalIconButton`（− / +）+ `OutlinedTextField(keyboardType = Number)`；长按重复步进用 `LaunchedEffect` + `delay` |
| 预设 | `FilterChip`（选中）/ `AssistChip` |
| 每日上限 | `ListItem` + `trailingContent`（数值 + `KeyboardArrowDown`）→ 展开 `OutlinedTextField` |
| 恢复默认 | `IconButton(Icons.MoreVert)` + `DropdownMenu` + `AlertDialog` 二次确认 |
| 设置分组 | 单张 `Card` 内 `Column` + `ListItem` + `HorizontalDivider` |
| 诊断溢出菜单 | `TopAppBar(actions = { IconButton(MoreVert) + DropdownMenu })` |
| 日志行 | `Row` + `Text(FontFamily.Monospace, 11.sp)` + 类型 `Surface(labelSmall)` + 单行省略 |

## 交互规则

1. 点击卡片任意空白处展开 / 收起；点击 `Switch` 只切换开关。打开开关时自动展开一次，关闭时自动收起。
2. 步进按钮长按 500ms 后每 80ms 连续调整；输入框失焦或点键盘「完成」时提交，非法值回滚并提示范围。
3. 每日上限默认折叠，行内显示当前值；展开后编辑，范围 1–9,999,999。
4. 微信的「固定值」保持禁用（WeRun 按增量同步），长按或「说明」里解释原因。
5. 诊断页日志筛选为空选 = 全部；保留现有的复制 / 分享 / 清空与空状态文案。
6. 所有二级页返回沿用系统返回手势，不新增底部导航。

## 实现落点（已实现）

- `ui/theme/Color.kt` / `Theme.kt`：补全 light/dark fallback 令牌，新增 `ThemeMode` 与 `cardContainerColor()`；动态取色优先级不变。
- `ui/HomeScreen.kt`：卡片折叠列表、分段按钮、步进编辑器、每日上限折叠行、连接状态胶囊与底部弹窗。
- `ui/SettingsScreen.kt`：分组列表（`ListItem` + 分隔线），含主题 / 语言 / 更新 / 诊断 / 关于。
- `ui/DiagnosticsScreen.kt`：顶栏溢出菜单、环境键值行、目标应用状态行、单选日志筛选与等宽字体。
- `data/DailyLimit.kt`：hook 与 UI 共用的每日上限计算（基础上限 + 0–100 随机抖动）。
- `res/values*/strings.xml`：状态徽章、主题、恢复默认、实际生效上限等文案，中英同步。

## 与设计稿的差异（2026-10-23 实现）

- **首页标题栏**：为了和状态胶囊同排且避免大标题折叠时错位，使用紧凑 `TopAppBar` + 双行标题（标题 22sp + 统计 12sp），滚动时收起。
- **每日上限**：卡片摘要与折叠行显示「今天实际生效」的上限（基础上限 + 当日抖动），编辑器内是「基础上限」，下方提示实际值。
- **日志筛选**：从多选改为单选（全部 / 框架 / 配置 / Hook / 错误），与分段控件语义一致。
- **卡片底色**：浅色用 `surfaceContainerLowest`（白卡），深色用 `surfaceContainerLow`，保证两种主题下卡片都在页面之上。
- **应用图标**：读取系统真实图标，设计稿中的字母徽标只在应用未安装时出现。

## 待确认

- 平板 / 折叠屏是否要双栏布局；当前为单栏自适应。
- 动效强度（列表展开、状态切换）按 Material 3 默认时长，未做自定义曲线。
- 主题胶囊点击弹窗为新增交互，如需保持零新增交互可改为长按。
