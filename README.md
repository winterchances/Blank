# 留白（Blank）

Android 本地相册整理应用。

## 项目配置

- 应用名称：留白
- Application ID：`com.blank.gallery`
- Minimum SDK：29（Android 10）
- Compile / Target SDK：36
- 语言：Kotlin
- UI：Jetpack Compose
- JDK：17

## 当前阶段

当前分支用于建立 Android 工程骨架，暂不包含媒体扫描、相似度分析、清理和删除逻辑。

后续实现遵循本地处理原则：不上传照片或分析结果，不申请网络权限；删除前由用户确认，并优先使用 Android 系统回收站。

项目不配置提交即自动构建的 GitHub Actions；仅在明确需要时再增加构建流程。
