# 轻待办

一个离线使用的 Android 待办提醒 MVP，支持 Android 8.0 及以上。

## 已实现

- 新建、编辑、删除事项，标记完成，未完成／已完成列表；已完成事项可以在详情页改为未完成。
- 预计开始日期和时间；可选持续时间、15／30／60 分钟快捷输入、预计结束时间展示。
- 日期时间使用同一个圆角弹窗：月历选择日期、左右切月、年月快速跳转；小时／分钟滚轮使用 24 小时制。顶部日期和时间可切换，仅点击确定后应用，不包含农历。
- 独立提醒子页面，可多选发生时、5／10／15／30 分钟、1／2 小时、1／2／7 天前；支持自定义分钟数和不提醒。已过期的时间不可新增选择。
- 系统通知打开事项及直接完成；修改、完成或删除后取消旧提醒。
- SQLite 本地保存，重启恢复未来提醒，通知及精确闹钟权限引导。
- 无联网权限、无服务器或运行时第三方服务，关闭云备份与设备迁移备份。

范围说明见 [PRODUCT.md](PRODUCT.md)。卸载应用或清除应用数据会删除本地事项，当前版本不提供导出功能。

## 构建

使用 JDK 17、Android SDK Platform 35、Build Tools 35.0.0。Gradle Wrapper 固定为 8.11.1，Android Gradle Plugin 为 8.9.1。

设置 `JAVA_HOME` 和 `ANDROID_HOME`，或用 Android Studio 打开项目并配置 SDK。Windows 可运行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build.ps1
```

本项目工作目录下已有的 `.tools` 开发工具会被脚本自动识别；该目录不提交到 Git。首次构建可能需要联网下载构建依赖，安装后的应用运行不需要联网。

通用 Gradle 命令：

```text
./gradlew assembleDebug lintDebug testReminderRules assembleDebugAndroidTest
```

APK 输出：`app/build/outputs/apk/debug/app-debug.apk`。这是开发测试签名版本，尚未配置正式发布签名。

## 验证

纯 JVM 测试可独立运行，不需要模拟器：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test.ps1
```

设备测试需要专用模拟器，测试会创建和清理应用测试数据，请勿在保存个人事项的手机上执行。安装 APK 后，先开启通知及“闹钟和提醒”权限，再执行：

```text
./gradlew connectedDebugAndroidTest
```

本地专用模拟器完整验证（含重启恢复）：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/device-test.ps1 -Reboot
```

测试覆盖提醒计算边界、跨天结束时间、SQLite 持久化与更新、旧通知防误完成、界面录入完成流程和系统闹钟实际通知触发。验证记录见 [TESTING.md](TESTING.md)。

## 实现说明

- 原生 Java／Android View 界面，列表、事项详情及提醒子页面采用浅色布局，无运行时外部库。
- 数据库从旧版自动升级，保留已有事项和提醒配置。改为未完成时只重新安排未来提醒，已过期的不补发。
- `TaskStore` 管理本地数据，`ReminderRules` 计算提醒时间。
- `ReminderScheduler` 使用系统 `AlarmManager`，无需常驻后台服务；缺少精确提醒权限时退化为可能延迟的提醒。
- `ReminderReceiver` 在触发时复查事项存在性、状态和版本，防止已修改或完成的事项误提醒。
- `RestoreReceiver` 在开机、应用更新、系统时间修改和精确提醒授权后重建未来提醒。

强行停止应用、关闭通知或厂商省电限制可能影响提醒。手机关机期间不会提醒，重启后首次解锁时恢复未来提醒，不会批量补发已经过期的提醒。正式使用前还需在目标品牌真机上验证锁屏、省电和重启行为。
