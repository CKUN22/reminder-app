# 轻待办

一个离线使用的 Android 待办提醒 MVP，支持 Android 8.0 及以上。

## 已实现

- 新建、编辑、删除事项，标记完成，未完成／已完成列表。
- 预计开始日期和时间；可选持续时间、15／30／60 分钟快捷输入、预计结束时间展示。
- 开始提醒，可同时选择提前 10 分钟和提前 24 小时提醒；过期选项禁用。
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

- 原生 Java／Android View 界面，两页浅色布局，无运行时外部库。
- `TaskStore` 管理本地数据，`ReminderRules` 计算提醒时间。
- `ReminderScheduler` 使用系统 `AlarmManager`，无需常驻后台服务；缺少精确提醒权限时退化为可能延迟的提醒。
- `ReminderReceiver` 在触发时复查事项存在性、状态和版本，防止已修改或完成的事项误提醒。
- `RestoreReceiver` 在开机、应用更新、系统时间修改和精确提醒授权后重建未来提醒。

强行停止应用、关闭通知或厂商省电限制可能影响提醒。手机关机期间不会提醒，重启后首次解锁时恢复未来提醒，不会批量补发已经过期的提醒。正式使用前还需在目标品牌真机上验证锁屏、省电和重启行为。
