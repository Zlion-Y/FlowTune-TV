# FlowTune

Android TV 原生音乐播放器（Jetpack Compose）。

## 特性

- **原生渲染**：全部界面 Jetpack Compose 绘制，D-pad 原生焦点系统，无 WebView 兼容性问题
- **ExoPlayer 硬解**：全格式支持，MediaSession 蓝牙/遥控媒体键
- **逐字歌词**：Canvas 自绘 AMLL 风格逐字渐变歌词
- **动效档位**：关闭 / 低 / 中 / 高 四档，TV 默认关闭，按盒子性能在设置页调整
  - 关闭：15fps 逐行歌词、无动画（老盒子推荐）
  - 低：+封面倒影，20fps
  - 中：逐字弹簧歌词 + 动态背景，30fps
  - 高：+未唱行模糊，45fps
- **本地音乐**：文件夹扫描、标签/内嵌封面读取、歌词侧车（UTF-8/GBK 自动识别）
- **播放队列**：顺序/单曲/列表循环/随机

## 构建

```bash
cd android-native
gradle assembleRelease
# 产物: app/build/outputs/apk/release/
```

要求：JDK 17、Android SDK 35。

## 协议

MIT
