# 即见（Seen）

<p align="center">
  <img src="https://github.com/HauUhang/Seen/blob/main/png/app-icon.png" width="200"/>
  <br>所见即所得
</p>

一款极简的 Android 工具 App：**一个输入框直达抖音 / 小红书 / 知乎 / 哔哩哔哩 / X / Reddit / YouTube 的搜索结果页**，绕过打开 App 时的算法推荐流，避免「想搜 A，结果刷了半天 B」的分心问题。

> **你的注意力，应该由你自己支配。**
>
> 我们每一次打开这些 App，本意往往只是查一个东西。可打开的瞬间，推荐流早已等在那里——精心计算的标题、恰好在走神时出现的视频，于是一个「30 秒的搜索」变成了一小时的被动浏览。这不是你意志力不够，而是成百上千位工程师在优化「如何让你多停留一秒」。
>
> 注意力是我们最宝贵、也最有限的东西。Seen 的做法很简单：**在打开 App 之前，先完成搜索。** 输入你想查的，直达结果页，拿到答案就离开。不猜你喜欢什么，不为你准备下一条。

## 功能

- 🔍 输入关键词，选择抖音、小红书、知乎、哔哩哔哩、X、Reddit 或 YouTube，直达搜索结果。
- 🕘 点击搜索框查看历史记录，可再次使用、删除单条或清空全部。
- ⚙️ 自由选择首页显示的常用平台。

## 界面预览

当前版本的浅色和深色首页：

<p align="center">
  <img src="png/ui-home-light.png" width="240" alt="即见浅色首页" />
  <img src="png/ui-home-dark.png" width="240" alt="即见深色首页" />
</p>

## 如何使用

1. 前往 [Releases](https://github.com/HauUhang/Seen/releases) 页面下载最新的 APK。
2. 将 APK 传到自己的手机上（微信 / QQ / 数据线均可），点击安装；系统提示「允许安装未知来源应用」时选择允许。
3. 安装完成后桌面出现「即见」，打开 → 输入想查的内容 → 点对应平台，直达搜索结果页。

## 视频演示

本机环境：
- 抖音：已安装未登录
- 小红书：已安装已登录
- bilibili：国际版已登录
- 知乎：未安装为登录

https://github.com/user-attachments/assets/328718fc-f9aa-4376-9573-59d190ff64fd

## 打开策略

选择平台后，即见会打开该平台的搜索结果。抖音、小红书、知乎和哔哩哔哩优先尝试已安装的 App；无法打开时会转到浏览器。X、Reddit 和 YouTube 通过各自的搜索结果链接打开。

## 技术栈

- Kotlin + AndroidX + Material3 + ViewBinding
- minSdk 26（Android 8.0+） / targetSdk 34
- AGP 8.13.2 / Gradle 8.13 / Kotlin 1.9.24（JDK 17）

> 若 Android Studio 提示升级 AGP，按提示更新即可，不影响代码逻辑。

## 本地构建与检查

使用 JDK 17 或更高版本，在 Android Studio 中安装 Android SDK Platform 34 和 Build Tools 35.0.0。通过 SDK 设置或本机 `local.properties` 的 `sdk.dir` 指定 SDK 路径。

Windows：

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

macOS / Linux：

```bash
bash ./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

- 本地 Debug 构建产物：`app/build/outputs/apk/debug/app-debug.apk`。
- 交互测试报告：`app/build/reports/tests/testDebugUnitTest/index.html`。
- 原生布局渲染：`app/build/reports/ui/`，覆盖浅色、深色、320dp 窄屏和 800dp 平板。
- 自动检查覆盖平台入口、空输入、历史回填、逐条删除与清空确认、平台显示设置、输入恢复与网页降级。第三方 App 的实际搜索落点仍需在安装对应 App 的真机上验证。

界面配色集中在 `res/values/colors.xml` 与 `res/values-night/colors.xml`，平台卡片共用 `res/layout/item_platform.xml`。历史记录沿用原有存储格式。

## 项目结构

```
JusouApp/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/jusou/app/
│   │   ├── MainActivity.kt          # 界面与交互
│   │   ├── Platform.kt              # 七平台深链/网页地址（DeepLink 含包名机制）
│   │   ├── PlatformPreferences.kt   # 常用平台显示设置
│   │   ├── SearchEngine.kt          # 深链优先 + 网页降级
│   │   └── SearchHistoryManager.kt  # 历史记录持久化
│   └── res/
│       ├── layout/activity_main.xml
│       ├── values/  (strings / colors / themes)
│       ├── drawable/ (图标与形状)
│       └── mipmap-anydpi-v26/ (自适应图标)
```

## 已知限制

- 小红书、B 站等平台的 App 搜索结果页可能要求登录态；深链已命中 App，但未登录时可能停在首页或登录页，属平台行为。
- 未申请任何运行时权限，不联网、不采集数据，历史仅存本机。

## 许可证

本项目基于 [MIT License](LICENSE) 开源。
