# 隐私政策 / Privacy Policy

## 中文

HyperOS 键盘助手不申请联网权限，不收集、上传、出售或共享个人数据，也不包含统计分析或广告 SDK。

以下信息仅保存在设备本地：

- 模块总开关、运行模式和各项功能设置；
- 用户选择的白名单应用包名；
- 自动切换输入法设置及所选输入法；
- 用户创建或导入的键盘及鼠标宏，包括宏名称、文本内容、按键序列、鼠标标记、延迟、循环和生效应用；
- 用户为鼠标宏主动导入的本地背景图片；该图片仅用于编辑定位，不会随宏分享；
- 通知、界面语言和配置版本等设置。

只有用户主动使用 Android 分享功能时，键盘宏才会导出为文件；应用不会自行上传宏内容。导入宏前请确认文件来源可信。

模块的只读配置 Provider 仅允许 Android 系统 UID、模块自身 UID，以及运行 Hook 所必需且经包名核验的 SystemUI 调用。配置读取失败时使用安全默认值，不会向其他普通应用开放修改接口。

通知权限仅用于用户主动启用的本地功能提示，可随时在系统设置或应用内关闭。

## English

HyperOS Keyboard Fix does not request Internet access and does not collect, upload, sell, or share personal data. It contains no analytics or advertising SDKs.

The following data is stored only on the device:

- the module master switch, operating mode, and feature settings;
- package names selected for the application allowlist;
- automatic IME switching settings and the selected IME;
- keyboard and mouse macros created or imported by the user, including names, text, key sequences, pointer markers, delays, loops, and target applications;
- local background images explicitly imported for mouse-macro positioning; these images are not included when a macro is shared;
- notification, interface language, and configuration-version settings.

A macro is exported only when the user explicitly invokes Android sharing. The app never uploads macro content automatically. Verify that an imported macro comes from a trusted source.

The module's read-only configuration provider permits only Android's system UID, the module's own UID, and required SystemUI callers verified by package name. It exposes no write interface to ordinary third-party applications. Safe defaults are used when configuration cannot be read.

Notification permission is used only for local feature notifications explicitly enabled by the user and can be disabled at any time.

Last updated: August 9, 2026.
