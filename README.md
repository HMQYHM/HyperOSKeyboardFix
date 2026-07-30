# HyperOS Keyboard Fix

[![Android CI](https://github.com/HMQYHM/HyperOSKeyboardFix/actions/workflows/build.yml/badge.svg)](https://github.com/HMQYHM/HyperOSKeyboardFix/actions/workflows/build.yml)

[简体中文](#简体中文) · [English](#english)

一款用于在 HyperOS 3 上将实体键盘快捷键传递给远程桌面应用的 LSPosed
模块。

An LSPosed module for passing physical keyboard shortcuts through to remote
desktop applications on HyperOS 3.

## 简体中文

### 功能

- 仅在用户选择的白名单应用中接管快捷键。
- 接管实体键盘的 Meta、Alt、Ctrl、Fn 修饰组合键与 F1–F12 功能键。
- 阻止 HyperOS 同时执行桌面、最近任务等系统动作。
- 不修改 Meta 键，不注入按键事件。
- 现代化 Material 3 首页与设置界面，支持预测性返回手势。
- 支持简体中文、繁體中文和 English。

### 1.1.0 更新内容

- 将模块总开关与“接管全部组合键”合并为“接管当前白名单软件快捷键”。
- 白名单为空时提供 5 秒倒计时引导，自动进入应用选择页面。
- 已选应用优先显示，保存或返回后回到模块首页。
- 扩充远程桌面、远控、串流与云游戏应用的默认白名单目录；仅自动选择设备上已安装的匹配应用。
- 扩展为接管白名单应用中的全部受支持实体键盘组合键。
- 移除实验性的物理键盘直通和软键盘隐藏功能。
- 更新首页布局、动画、状态展示与三种语言文案。

### 使用场景

- 在 Microsoft Remote Desktop、RustDesk 等远程桌面应用中，将
  Meta、Alt + Tab 等组合键完整传递到远端电脑。
- 在虚拟机、云电脑和远程工作站应用中使用实体键盘操作 Windows 或
  Linux 客户机。
- 在 Moonlight、Steam Link 等串流应用中，避免 HyperOS 抢占键盘快捷键。

### 兼容性

- 已测试：Android 16（API 36）、HyperOS 3、LSPosed。
- 基于小米悬浮键盘开发，并已确认小米悬浮键盘可用。
- 其他键盘暂未测试，预计兼容小米键盘式保护壳。
- 最低 Android 版本：Android 15（API 35）。
- 当前 Hook 针对 HyperOS 3 的
  `com.android.server.policy.BaseMiuiPhoneWindowManager` 实现。其他 ROM
  或 HyperOS 大版本可能不兼容。

### 安装

1. 从 [Releases](https://github.com/HMQYHM/HyperOSKeyboardFix/releases)
   下载正式签名 APK。
2. 安装 APK，在 LSPosed 中启用模块并使用推荐作用域。
3. 重启设备。
4. 打开模块，选择需要生效的应用，然后启用快捷键接管。

### 隐私

模块不申请联网权限，不收集或上传任何个人数据。白名单和快捷键配置仅
保存在设备本地，并通过只允许系统 UID 与模块自身访问的
`ContentProvider` 提供给 Hook 进程。

### 问题反馈

提交 Issue 时请附上：

- 设备和 HyperOS 完整版本；
- Android 与 LSPosed 版本；
- 问题对应的快捷键；
- 已移除个人信息的 LSPosed 模块日志。

请勿上传完整 bugreport、账号信息或未经授权的系统 JAR。

## English

### Features

- Takes over shortcuts only while an allowlisted app is in the foreground.
- Handles physical-keyboard combinations using Meta, Alt, Ctrl, and Fn,
  together with the F1–F12 function keys.
- Prevents HyperOS from executing Home, Recents, and other system actions at
  the same time.
- Does not remap Meta or inject synthetic key events.
- Modern Material 3 Home and Settings screens with predictive back support.
- Available in Simplified Chinese, Traditional Chinese, and English.

### What's new in 1.1.0

- Merged the master switch and all-combinations switch into one allowlist-aware
  shortcut takeover control.
- Added a five-second redirect prompt when no allowlisted app is selected.
- Keeps selected apps at the top and returns to the module Home screen after
  saving.
- Expanded the built-in catalog for remote desktop, remote control, streaming,
  and cloud gaming apps; only matching apps actually installed on the device
  are selected.
- Expanded takeover to all supported physical-keyboard combinations inside
  allowlisted apps.
- Removed the experimental physical-keyboard passthrough and soft-keyboard
  suppression feature.
- Refreshed the Home layout, animation, status presentation, and translations.

### Use cases

- Pass Meta, Alt + Tab, and other combinations through Microsoft Remote
  Desktop, RustDesk, and similar remote desktop clients.
- Use a physical keyboard with Windows or Linux guests in virtual machine,
  cloud desktop, and remote workstation apps.
- Prevent HyperOS from taking over keyboard shortcuts in Moonlight, Steam Link,
  and other streaming clients.

### Compatibility

- Tested on Android 16 (API 36), HyperOS 3, and LSPosed.
- Built for and verified with the Xiaomi Floating Keyboard.
- Other keyboards are currently untested. The Xiaomi Keyboard Case is expected
  to be compatible.
- Minimum supported Android version: Android 15 (API 35).
- The current hooks target HyperOS 3's
  `com.android.server.policy.BaseMiuiPhoneWindowManager`. Other ROMs or major
  HyperOS versions may not be compatible.

### Installation

1. Download the signed APK from
   [Releases](https://github.com/HMQYHM/HyperOSKeyboardFix/releases).
2. Install it, enable the module in LSPosed, and keep the recommended scope.
3. Reboot the device.
4. Open the module, choose the apps where it should work, and enable shortcut
   takeover.

## Build

Requirements:

- Android Studio with Android SDK 36
- JDK 17 or newer

```shell
./gradlew :app:assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## License

Copyright © 2026 HMQYHM.

This project is licensed under the GNU General Public License v3.0 only. See
[LICENSE](LICENSE).
