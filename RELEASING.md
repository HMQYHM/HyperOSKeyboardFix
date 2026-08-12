# 发布清单 / Release Checklist

每次正式发布必须同时更新主 GitHub 仓库与 LSPosed 模块仓库。两边应使用同一份已签名 APK，不得只更新其中一个仓库。

## 1. 发布前检查

- 更新 `app/build.gradle.kts` 中的 `versionCode` 与 `versionName`。
- 更新 `README.md`、`CHANGELOG.md` 和必要的隐私或兼容性说明。
- 编译 Debug 与 Release，并确认 Release APK 使用正式私有密钥签名。
- 在真机验证模块加载、白名单、当前发布涉及的功能和安全回退。
- 记录 Release APK 的 SHA-256，后续核对两个仓库的附件完全一致。

## 2. 主 GitHub 仓库

仓库：`HMQYHM/HyperOSKeyboardFix`

- 合并并推送本次发布代码。
- 标签使用 `v<versionName>`，例如 `v1.4.0`。
- Release 标题和更新说明同时提供中文与英文。
- 上传已签名 APK，文件名使用 `HyperOS键盘助手<versionName>.apk`；GitHub 若自动规范化文件名，以实际附件名为准。
- 检查 GitHub Actions 构建结果和 Release 下载是否正常。

## 3. LSPosed 模块仓库

仓库：`Xposed-Modules-Repo/io.github.hmqyhm.hyperoskeyboardfix`

- 同步主仓库的 README 与版本更新说明。
- 更新 `SUMMARY`，保留正确的 `SOURCE_URL` 和 `SCOPE`。
- 标签使用 `<versionCode>-<versionName>`，例如 `22-1.4.0`。
- 上传与主 GitHub Release 完全相同的已签名 APK。
- 比较两个 Release 附件的 SHA-256，必须一致。
- 检查 LSPosed Release 页面；等待 `modules.lsposed.org` 完成索引更新。

## 4. 发布完成条件

- 主 GitHub 与 LSPosed 仓库均已发布相同版本。
- 两边 APK 哈希一致且能够安装。
- GitHub Actions 没有阻断发布的失败检查。
- README、更新说明、版本号和下载链接一致。

Do not consider a release complete until both the main GitHub repository and the LSPosed module repository contain the same signed APK and matching release notes.
