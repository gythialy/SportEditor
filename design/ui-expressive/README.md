# M3 Expressive 重构设计稿

目标强度：**激进档**（动效 + 字重 + 组件 + 形状刻度 + 浮动工具栏 + shape morph）。
调研依据见 [`../../docs/ui-expressive-research.md`](../../docs/ui-expressive-research.md)。

| 文件 | 内容 |
| --- | --- |
| `expressive-mockup.html` | 高保真可交互稿：4 屏 + 组件标本 + 11 条标注，单文件、无外部依赖 |
| `preview-light.png` | 浅色 · 品牌松绿的整页截图（**不入库**，本地渲染） |
| `preview-monet.png` | 浅色 · 系统 Monet（TONAL_SPOT）的整页截图（不入库） |
| `preview-dark.png` | 深色 · 系统 Monet 的整页截图（不入库） |
| `preview-tints.png` | 同一张壁纸下的四种取色对照（首页单屏并排，不入库） |

PNG 截图只作本地核对，已由 `.gitignore` 排除；仓库里只保留 HTML 稿。

## 打开方式

直接双击 `expressive-mockup.html`，或：

```bash
python3 -m http.server 4311 --directory design
# http://127.0.0.1:4311/ui-expressive/expressive-mockup.html
```

## 可交互的部分

- 右上角切换 **浅色 / 深色**、**品牌绿 / 动态取色**、**标注**显隐。
- 首页卡片点击展开 / 收起（用 `grid-template-rows` 的 expo 曲线近似 expressive 空间弹簧，约 520ms）。
- 「修改方式」切换、`−/+` 步进、预设 chip 会联动读数与预览文本。
- 「检查更新」的等待态是 CSS 驱动的形状 morph（真机上是 `LoadingIndicator`）。

## 与原实现的差异（设计意图）

1. 顶栏保留双行标题，但标题走 emphasized 字重；副标题 12sp/700。
2. 状态胶囊 → SplitButton（状态 + 步骤入口合一）。
3. 四段分段控件 → ButtonGroup（可溢出折叠，解决 320dp 英文切字）。
4. 卡片 20dp / 弹层 28dp / 选中分段内侧 8dp（原为 16/16/8）。
5. 步进圆钮 56dp（M 档），按下圆 → 20dp 方圆，即 shape morph 的最小落点。
6. 读数 36sp/500 + `tnum` 等宽数字。
7. 设置行图标进 40dp 圆角容器（Android 16 设置页语言）。
8. 告警保持淡染（10% error），不做实心色块。
9. 诊断页 3 个动作从溢出菜单改为 HoverToolbar 式浮动工具栏 —— **唯一新增交互面，需评审**。
10. 有意不做：32/48dp 大圆角卡片、浮动工具栏当导航、loading → progress 转换、为压缩宽度牺牲标签。
11. 取色来源做了四态对照：品牌绿（fallback）、系统 Monet 的两档 theme style、以及品牌绿 harmonize 到壁纸主色。harmonize 只转色相（≤15°）、保留色度与明度，所以绿会变青绿而不是变紫。

## 关于 Monet（莫奈）

Monet 是 Android 12 动态取色引擎的代码名（`com.android.systemui.monet`），不是画风，也不是 M3 的子风格。系统从壁纸抽种子色 → 5 组色调板 × 13 阶 → 65 个颜色，Android 13 起还有 6 种 theme style（TONAL_SPOT 默认 / VIBRANT / EXPRESSIVE / SPRITZ / RAINBOW / FRUIT_SALAD）。应用只能消费结果，改不了系统调色板；想要「自己的 Monet」必须自己算（Material Theme Builder 那条路）。

本稿里的四种取色都是**按色卡手工近似**的，不是真机 Monet 的逐位输出——真机值随壁纸像素分布而变。

## 尚未覆盖

- 平板 / 折叠屏双栏姿态。
- 真机弹簧参数（本稿的 CSS 曲线只是近似，落实现时要换成 `MotionScheme.expressive()`）。
- 动态取色只给了紫色示例，实际由壁纸决定。
