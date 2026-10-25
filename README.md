# 卿音

简单美观的本地 Android 音乐播放器：只读设备上的音频文件（MediaStore），不提供流媒体、账号或云同步。

启动器与界面显示名：**卿音**。应用包名保持 `com.localmp3.player`（未改 `applicationId`）。

## 功能（v1）

- 扫描本机音频（MP3 / FLAC / M4A / WAV 等）；曲名显示为**文件名（去扩展名）**，不用 ID3/MediaStore 标题
- 权限、加载中、空曲库、权限拒绝状态
- **正在播放**为主界面，可切换到歌曲列表再返回
- 按文件夹浏览；点播后默认队列为**当前文件夹**内全部曲目
- 正在播放页可查看并跳转当前播放列表
- 用户自建歌单（本地持久化）：新建 / 添加 / 移除 / 删除
- 退出后续播：记住当前播放列表与进度，下次打开停在正在播放（暂停待点）
- 播放 / 暂停 / 上一首 / 下一首 / 拖动进度
- 后台播放与媒体通知控制（Media3 Session）
- 封面大图（有专辑图时）
- 播放模式：顺序 / 单曲循环 / 随机

## 技术栈

- Kotlin
- Jetpack Compose + Material 3
- Media3（ExoPlayer + MediaSession）
- Min SDK 26（Android 8.0+）

## 在 Android Studio 中打开

1. 安装 [Android Studio](https://developer.android.com/studio)（推荐最新稳定版）。
2. **File → Open**，选择本仓库根目录。
3. 等待 Gradle 同步完成；若提示安装 SDK / Build-Tools，按向导安装。
4. 连接真机（推荐，便于验证音频与 MediaStore）或启动模拟器。
5. 运行 `app` 配置（绿色 Run）。

命令行构建（需已配置 Android SDK，并在 `local.properties` 中设置 `sdk.dir`）：

```bash
./gradlew :app:assembleDebug
```

安装到设备：

```bash
./gradlew :app:installDebug
# 或
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 使用说明

1. 首次打开会请求读取音频权限（Android 13+ 为「音乐和音频」）。
2. 授权后自动扫描本地曲库。
3. 在列表中点选歌曲，进入正在播放界面控制播放。
4. 下拉通知栏可使用系统媒体控件；切到后台仍可继续播放。

## 说明与限制

- **纯本地**：无网络播放、无账号、无歌词 / EQ / 搜索（留给后续版本）。歌单仅存本机。
- 真机上的 MediaStore 曲库通常比模拟器完整；验证音频体验建议用实体设备。
- 应用包名：`com.localmp3.player`
