# HyperOS Keyboard Fix

[![Android CI](https://github.com/HMQYHM/HyperOSKeyboardFix/actions/workflows/build.yml/badge.svg)](https://github.com/HMQYHM/HyperOSKeyboardFix/actions/workflows/build.yml)

[简体中文](#简体中文) · [English](#english)

一款让 HyperOS 3 白名单应用完整接收实体键盘快捷键，并支持自动切换输入法、混合键盘宏与鼠标宏的 LSPosed 模块。

An LSPosed module that passes physical keyboard shortcuts to allowlisted apps on HyperOS 3, with automatic input-method switching and mixed keyboard/mouse macros.

## 简体中文

### 项目介绍

HyperOS 键盘助手是一款面向 HyperOS 3 的 LSPosed 模块，让白名单应用完整接收实体键盘快捷键，避免系统同时触发桌面、最近任务等操作。适用于远程桌面、云电脑、虚拟机、远程工作站、串流游戏和云游戏等场景。

### 主要功能

#### 实体键盘快捷键接管

- 仅在用户选择的白名单应用位于前台时生效，白名单外保持 HyperOS 原始行为。
- 支持 Meta、Alt、Ctrl、Shift、Tab、方向键、字母键及 F1–F12 等实体键盘按键。
- 避免 HyperOS 同时触发桌面、最近任务和其他系统快捷键。
- 不重新映射实体 Meta 键，不影响触屏导航、返回手势或软键盘。
- 提供相互独立的直通通道与兼容模式。

#### 白名单应用

- 显示已安装、可启动的用户应用，支持按名称或包名搜索。
- 已选择应用优先显示，支持选择多个应用及选择性显示系统应用。
- 未保存修改时退出会提示是否保存。
- 白名单仅以应用包名集合保存在设备本地。

适用于 Microsoft Remote Desktop、Windows App、RustDesk、AnyDesk、TeamViewer、Moonlight、Steam Link、Parsec，以及其他远程桌面、云电脑、虚拟机、串流和云游戏客户端。

#### 自动切换输入法

- 白名单应用进入前台时自动切换到用户选择的输入法，离开后可恢复之前的输入法。
- 显示设备上已安装并启用的输入法。
- 已安装时将搜狗输入法、百度输入法和讯飞输入法标记为推荐输入法。
- 支持“只自动切换一次”，用户手动切换后不再强制切回。
- 设置直接显示在“其他”页面，首页入口会自动定位到对应区域。
- 不读取或保存用户输入内容。

#### 键盘宏

- 使用统一动作时间线，在同一个宏中任意组合一键输入文字、组合键、连续按键和模拟鼠标。
- 支持调整混合动作顺序或单独删除某一步，旧版单类型宏可自动兼容。
- 支持同一按键重复录制，以及 Esc、F1–F12、Menu、Insert、Home、End、Page Up、Page Down 和方向键等功能键。
- 支持 Ctrl、Shift、Alt、Meta 修饰键组合。
- 支持按下、抬起、步骤间隔及每个按键的独立延迟。
- 支持固定次数循环、按住连续触发和持续执行。
- 支持可视化键盘录制与双指缩放。
- 支持编辑、删除确认、TXT 导入和以宏名称命名的 TXT 分享。
- 导入前显示安全提醒，导入后由用户检查并保存。
- 每个宏可以选择一个或多个白名单应用。
- 首页和键盘宏页面共用同一个宏总开关。
- 支持鼠标左键、右键、上滑和下滑宏，并可连续添加多个位置标记。
- 鼠标画布提供可收缩工具栏、默认辅助背景、用户图片导入及 90° 旋转。
- 鼠标位置按屏幕比例保存，可分别设置点击时长、滑动时长、距离和动作延迟。
- 全屏鼠标画布跟随当前横竖屏比例；标记支持长按拖动，悬浮工具栏可移动并自动吸附屏幕边缘。

### 使用场景

- 在远程桌面应用中将 Meta、Alt + Tab 等组合键完整传递到远端电脑。
- 使用实体键盘操作虚拟机、云电脑和远程工作站中的 Windows 或 Linux。
- 避免 HyperOS 抢占 Moonlight、Steam Link、Parsec 等串流应用的快捷键。
- 使用键盘宏快速输入常用内容或执行固定按键序列。

### 应用界面

- Material 3 设计，横屏使用可收缩侧边栏，竖屏使用底部导航栏。
- 支持页面浮起过渡、弹窗动画、预测性返回及取消返回回弹。
- 支持简体中文、繁体中文和 English。
- 提供恢复初始设置、项目主页和相关项目入口。

### 通知

支持白名单应用进入前台、自动切换输入法、执行键盘宏和测试通知。可以设置通知总开关、独立事件开关、自定义标题和自动撤回。通知权限被拒绝不会影响模块的其他功能。

### 运行模式

- **直通通道**：优先将符合条件的实体键盘快捷键传递给前台白名单应用，并绕过对应的 HyperOS 系统行为。
- **兼容模式**：为部分无法正常使用直通通道的应用或系统版本保留兼容处理路径。

### 兼容性

- 已测试：Android 16（API 36）、HyperOS 3、LSPosed。
- 最低 Android 版本：Android 15（API 35）；目标版本：Android 16（API 36）。
- 基于小米悬浮键盘开发，并已确认小米悬浮键盘可用。
- 预计兼容小米平板 6 及以上机型的官方键盘式保护壳以及悬浮键盘。
- 不支持小米平板 6 Max 官方键盘最上方的功能键，也不支持使用这些功能键设置键盘宏。
- 其他品牌及型号的键盘可能兼容，尚未完整测试。
- 当前主要 Hook 针对 HyperOS 3 的 `com.android.server.policy.BaseMiuiPhoneWindowManager`。其他 ROM 或 HyperOS 大版本可能需要额外适配。

### 安装

1. 从 [Releases](https://github.com/HMQYHM/HyperOSKeyboardFix/releases) 下载正式签名 APK。
2. 安装 APK，在 LSPosed 中启用模块并保留推荐作用域。
3. 重启设备。
4. 打开模块，选择生效应用，开启快捷键接管并选择运行模式。

LSPosed 模块页面：[HyperOS Keyboard Fix](https://modules.lsposed.org/module/io.github.hmqyhm.hyperoskeyboardfix/)

### 隐私与稳定性

模块不申请联网权限，不收集、上传或共享个人数据。白名单、输入法设置、键盘宏、通知和语言配置仅保存在设备本地。配置通过仅允许系统 UID、模块自身及必要 SystemUI 进程访问的只读 `ContentProvider` 提供给 Hook 进程。

Hook 热路径不在每次按键时读取磁盘。system_server 使用内存缓存和 `ContentObserver` 刷新配置；读取失败时保留上一份有效配置，没有有效配置时使用全部关闭的安全默认值。模块不扫描整个 Framework，也不访问网络服务相关类。

### 问题反馈

提交 [Issue](https://github.com/HMQYHM/HyperOSKeyboardFix/issues) 时，请提供设备型号、HyperOS/Android/LSPosed 版本、键盘型号、目标应用包名、运行模式、相关快捷键或宏、复现步骤，以及已移除个人信息的 LSPosed 日志。

请勿上传未处理的完整 bugreport、账号信息、密码、验证码、个人输入内容或未经授权的系统 JAR。

### 构建

要求 Android Studio、Android SDK 36、JDK 17 或更高版本、Kotlin 和 Gradle Kotlin DSL。

```shell
./gradlew :app:assembleDebug
```

Debug APK：`app/build/outputs/apk/debug/app-debug.apk`

完整发布与 LSPosed 仓库同步流程见 [RELEASING.md](RELEASING.md)。

### 相关项目

#### [HMQYHM/FocusPenProX](https://github.com/HMQYHM/FocusPenProX)

为小米焦点触控笔 Pro 打造的 LSPosed 手势增强模块，提供触控笔按键、手势和应用适配能力。

### 许可证

Copyright © 2026 HMQYHM。

本项目仅使用 GNU General Public License v3.0，详情请参阅 [LICENSE](LICENSE)。

---

## English

### Introduction

HyperOS Keyboard Fix is an LSPosed module for HyperOS 3. It allows physical-keyboard shortcuts to reach allowlisted foreground apps while preventing HyperOS from simultaneously triggering Home, Recents, and other system actions. It is designed for remote desktop, cloud PC, virtual machine, remote workstation, game-streaming, and cloud-gaming applications.

### Main features

#### Physical-keyboard shortcut takeover

- Works only while a user-selected allowlisted app is in the foreground; original HyperOS behavior is preserved elsewhere.
- Supports Meta, Alt, Ctrl, Shift, Tab, arrow keys, letters, and F1–F12.
- Prevents HyperOS from simultaneously triggering Home, Recents, and other system shortcuts.
- Does not remap the physical Meta key or affect touch navigation, back gestures, or software keyboards.
- Provides mutually exclusive Direct Channel and Compatibility modes.

#### Application allowlist

- Displays installed and launchable user apps and supports name/package search.
- Places selected apps first, supports multiple selections, and can optionally display system apps.
- Warns about unsaved changes before leaving.
- Stores package names locally on the device.

Suitable apps include Microsoft Remote Desktop, Windows App, RustDesk, AnyDesk, TeamViewer, Moonlight, Steam Link, Parsec, and other remote-desktop, cloud-PC, virtual-machine, streaming, and cloud-gaming clients.

#### Automatic input-method switching

- Selects a configured input method when an allowlisted app enters the foreground and can restore the previous one on exit.
- Displays installed and enabled input methods.
- Marks Sogou, Baidu, and iFlytek as recommended when installed.
- Supports switching only once without overriding later manual changes.
- Settings are embedded on the Other page; the Home shortcut scrolls directly to them.
- Does not read or store typed content.

#### Keyboard macros

- Uses one unified timeline that can mix predefined text, key combinations, sequential keys, and mouse actions in the same macro.
- Mixed actions can be reordered or removed individually, while legacy single-type macros remain compatible.
- Supports repeated keys, Esc, F1–F12, Menu, Insert, Home, End, Page Up, Page Down, and arrow keys.
- Supports Ctrl, Shift, Alt, and Meta modifiers.
- Configurable key-down, key-up, per-step, and per-key delays.
- Supports fixed repeats, hold-to-repeat, and continuous execution.
- Includes a pinch-to-zoom visual keyboard recorder.
- Supports editing, delete confirmation, TXT import, and TXT sharing named after the macro.
- Shows a security warning before import and opens imported macros for review before saving.
- Assigns each macro to one or multiple allowlisted apps.
- Shares one macro master switch between Home and Keyboard Macros pages.
- Supports left-click, right-click, scroll-up, and scroll-down mouse macros with multiple numbered markers.
- The mouse canvas includes a collapsible toolbar, a built-in guide background, imported images, and 90-degree rotation.
- Pointer locations use normalized screen coordinates, with per-action click duration, scroll duration, distance, and delay.
- The full-screen mouse canvas follows the current display orientation; markers support long-press dragging, and the floating toolbar can be moved and docked to either edge.

### Use cases

- Pass Meta, Alt + Tab, and other combinations through remote-desktop clients.
- Use physical keyboards with Windows or Linux virtual machines, cloud PCs, and workstations.
- Prevent HyperOS from taking shortcuts away from Moonlight, Steam Link, Parsec, and similar clients.
- Enter common text or execute predefined sequences with macros.

### User interface

- Material 3 design with a collapsible landscape navigation rail and portrait bottom navigation.
- Floating page transitions, dialog animations, predictive back, and natural cancellation rebound.
- Simplified Chinese, Traditional Chinese, and English.
- Restore-defaults, project-homepage, and related-project entries.

### Notifications

Optional notifications are available for foreground allowlisted apps, automatic IME switching, macro execution, and testing. Users can configure a master switch, individual events, a custom title, and automatic dismissal. Denying notification permission does not affect other features.

### Input modes

- **Direct Channel** prioritizes delivery to the foreground allowlisted app while bypassing the matching HyperOS action.
- **Compatibility Mode** keeps a compatibility-oriented path for apps or system versions that cannot use Direct Channel correctly.

### Compatibility

- Tested on Android 16 (API 36), HyperOS 3, and LSPosed.
- Minimum Android version: Android 15 (API 35); target: Android 16 (API 36).
- Built for and verified with the Xiaomi Floating Keyboard.
- Expected to support official Xiaomi keyboard cases and Floating Keyboards for Xiaomi Pad 6 and newer models.
- Top-row function keys on the official Xiaomi Pad 6 Max keyboard are not supported and cannot be used to configure keyboard macros.
- Other keyboard brands and models may be compatible but have not yet been fully tested.
- Primary hooks target HyperOS 3's `com.android.server.policy.BaseMiuiPhoneWindowManager`; other ROMs or major HyperOS versions may require adaptation.

### Installation

1. Download the signed APK from [Releases](https://github.com/HMQYHM/HyperOSKeyboardFix/releases).
2. Install it, enable the module in LSPosed, and keep the recommended scope.
3. Reboot the device.
4. Select target apps, enable shortcut takeover, and choose an input mode.

LSPosed module page: [HyperOS Keyboard Fix](https://modules.lsposed.org/module/io.github.hmqyhm.hyperoskeyboardfix/)

### Privacy and stability

The module does not request Internet access and does not collect, upload, or share personal data. Allowlist, IME, macro, notification, and language settings remain on the device. A read-only `ContentProvider` restricted to the system UID, the module itself, and required SystemUI processes provides configuration to hook processes.

Key-event hot paths do not read disk synchronously. system_server uses an in-memory cache and `ContentObserver`; refresh failures keep the last valid snapshot, and fully disabled defaults are used when none exists. The module does not scan the entire framework or access network-service classes.

### Issue reports

When opening an [Issue](https://github.com/HMQYHM/HyperOSKeyboardFix/issues), include the device, HyperOS/Android/LSPosed versions, keyboard model, target package, input mode, affected shortcut or macro, reproduction steps, and redacted LSPosed logs.

Do not upload unredacted bugreports, account data, passwords, verification codes, personal typed content, or unauthorized system JAR files.

### Build

Requires Android Studio, Android SDK 36, JDK 17 or newer, Kotlin, and Gradle Kotlin DSL.

```shell
./gradlew :app:assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

See [RELEASING.md](RELEASING.md) for the complete GitHub and LSPosed release checklist.

### Related project

#### [HMQYHM/FocusPenProX](https://github.com/HMQYHM/FocusPenProX)

An LSPosed gesture-enhancement module for Xiaomi Focus Pen Pro, providing improved stylus buttons, gestures, and per-app behavior.

### License

Copyright © 2026 HMQYHM.

This project is licensed under the GNU General Public License v3.0 only. See [LICENSE](LICENSE).
