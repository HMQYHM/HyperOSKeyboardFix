# Changelog

## 1.5.0

- 底部导航栏延伸至手势提示线区域，并改善项目卡片的底部留白。
- 简化生效应用与键盘宏页面标题，保留系统返回和预测性返回行为。
- 新增鼠标宏画布，支持左键、右键、上滑、下滑、连续编号标记、逐项时长与延迟设置。
- 鼠标画布支持可收缩工具栏、内置辅助背景、导入图片及顺时针 90° 旋转。
- 鼠标标记按屏幕比例保存，并在 system_server 中针对当前显示器注入鼠标事件。
- 将文字输入、组合键/连续按键和模拟鼠标合并为统一动作时间线，同一个宏可混合编排三类动作。
- 旧版单类型宏会自动转换为兼容的动作时间线，无需重新建立。
- 鼠标画布标记支持长按拖动；复制标记会自动寻找附近空位，避免与原标记重合。
- 鼠标动作名称统一显示完整的本地化文字，不再使用 L、R 等缩写。
- 鼠标编辑器改为与当前显示方向和比例一致的全屏画布，所有背景与保存操作移入悬浮侧边栏。
- 收缩后的悬浮侧边栏支持长按移动，并修复标记长按拖动被缩放手势抢占的问题。
- 悬浮侧边栏收起后仅显示圆形鼠标图标，拖动结束会自动吸附最近的左右边缘。
- 侧边栏靠右时会向屏幕内部展开，并扩大展开宽度以完整显示操作名称。
- 鼠标、键盘及排序操作统一使用矢量图标，不再使用文字符号代替图标。
- 修复鼠标标记拖动时因动作列表更新导致手势监听重启、每次只能移动少量距离的问题。
- 画布缩放和平移改为仅由双指手势触发，单指长按拖动优先用于移动鼠标标记。

### English

- Added a unified macro timeline that can mix text, keyboard sequences, and mouse actions in one macro while preserving legacy macro compatibility.
- Added a full-screen mouse canvas with left click, right click, scroll up, scroll down, numbered markers, per-action timing, and normalized screen coordinates.
- Added an imported or built-in guide background, 90-degree rotation, marker duplication, ordering, editing, and long-press dragging.
- Added a floating vector-icon toolbar that collapses to one draggable button and automatically docks to the nearest screen edge.
- Improved gesture-area immersion, bottom-navigation spacing, predictive-back behavior, and application/macro page headers.
- Removed obsolete UI and provider-diagnostic source files without changing LSPosed loading or configuration behavior.

## 1.4.0

- 重构为响应式 Material 3 界面，横屏使用可折叠侧栏，竖屏使用底部导航，并完善预测性返回与页面动画。
- 新增直通通道与兼容模式，可按白名单应用接管 HyperOS 实体键盘快捷键。
- 新增键盘宏：文本输入、组合键、连续按键、循环、逐步延迟、可视化键盘录制、TXT 导入与分享，以及多应用生效范围。
- 新增白名单应用自动切换输入法、仅自动切换一次和可选功能通知。
- 完善白名单选择、通知设置、恢复初始设置、多语言界面及项目入口。
- 更新官方键盘兼容性说明：预计兼容小米平板 6 及以上的官方键盘式保护壳和悬浮键盘；小米平板 6 Max 官方键盘顶部功能键存在已知限制。

## 1.1.0

- 合并模块总开关与全部组合键接管开关。
- 支持在白名单应用内接管 Meta、Alt、Ctrl、Fn 组合键和 F1–F12 功能键。
- 白名单为空时显示倒计时提示并自动进入应用选择页面。
- 扩充远程桌面、远控、串流与云游戏应用的默认白名单目录。
- 更新 Material 3 首页布局、动画、状态展示和多语言文案。
- 移除实验性的物理键盘直通与软键盘隐藏功能。

## 1.0.1

- 改进设置导航与 LSPosed 推荐作用域。
- 增加小米悬浮键盘兼容性说明。
- 完善 GitHub Release 签名与持续集成配置。

## 1.0.0

- 首个开源版本。
