# 即见（Seen）

<p align="center">
  <img src="https://github.com/HauUhang/Seen/blob/main/png/app-icon.png" width="200"/>
  <br>所见即所得
</p>

一款极简的 Android 工具 App：**一个输入框直达抖音 / 小红书 / 知乎 / 哔哩哔哩的搜索结果页**，绕过打开 App 时的算法推荐流，避免「想搜 A，结果刷了半天 B」的分心问题。

> **你的注意力，应该由你自己支配。**
>
> 我们每一次打开这些 App，本意往往只是查一个东西。可打开的瞬间，推荐流早已等在那里——精心计算的标题、恰好在走神时出现的视频，于是一个「30 秒的搜索」变成了一小时的被动浏览。这不是你意志力不够，而是成百上千位工程师在优化「如何让你多停留一秒」。
>
> 注意力是我们最宝贵、也最有限的东西。Seen 的做法很简单：**在打开 App 之前，先完成搜索。** 输入你想查的，直达结果页，拿到答案就离开。不猜你喜欢什么，不为你准备下一条。

## 功能

- 🔍 统一搜索框，输入想查的内容
- 🎯 四平台一键直达（抖音 / 小红书 / 知乎 / 哔哩哔哩）
- 📺 B 站覆盖国内版、国际版、港澳台版（按包名精确唤起）
- 🕘 本地搜索历史（去重、最多 20 条、可清空、点击回填）

## 如何使用

1. 进入本仓库的 [Releases](https://github.com/HauUhang/Seen/releases) 页面，下载最新版本的 **APK 文件**。
2. 将 APK 传到自己的手机上（微信 / QQ / 数据线均可），点击安装；系统提示「允许安装未知来源应用」时选择允许。
3. 安装完成后桌面出现「即见」，打开 → 输入想查的内容 → 点对应平台，直达搜索结果页。

## 视频演示

本机环境：
- 抖音：已安装未登录
- 小红书：已安装已登录
- bilibili：国际版已登录
- 知乎：未安装为登录

https://github.com/user-attachments/assets/c20d8117-7c73-4d74-82ef-205db3a1b410

## 打开策略

优先唤起已安装的 App 深链直达搜索页；未安装时降级为浏览器网页搜索。

| 平台   | 已安装 App（深链直达搜索页）                       | 未安装（浏览器网页搜索）                                   |
| ---- | -------------------------------------- | ---------------------------------------------- |
| 抖音   | `snssdk1128://search?keyword=`         | `https://www.douyin.com/search/`               |
| 小红书  | `xhsdiscover://search/result?keyword=` | `https://www.xiaohongshu.com/search_result`    |
| 知乎   | `zhihu://search?q=`                    | `https://www.zhihu.com/search?type=content&q=` |
| 哔哩哔哩 | `bilibili://search?keyword=`           | `https://search.bilibili.com/all?keyword=`     |

> 各平台深链 scheme 可能随版本调整，全部集中在 `Platform.kt`，一处修改即可。
>
> 深链唤起**不依赖 `resolveActivity` 预判**（Android 11+ 包可见性会误判「未安装」），而是直接 `startActivity` + try/catch，失败才降级网页。

### B 站多版本说明

B 站搜索按包名逐个尝试，命中即停：

| 顺序 | 目标           | 包名                    |
| -- | ------------ | --------------------- |
| 1  | 国内版          | `tv.danmaku.bili`     |
| 2  | 国际版 / 港澳台版   | `com.bilibili.app.in` |
| 3  | 通用兜底（任意已装版本） | 不指定                   |

## 技术栈

- Kotlin + AndroidX + Material3 + ViewBinding
- minSdk 26（Android 8.0+） / targetSdk 34
- AGP 8.13.2 / Gradle 8.13 / Kotlin 1.9.24（JDK 17）

> 若 Android Studio 提示升级 AGP，按提示更新即可，不影响代码逻辑。

## 项目结构

```
JusouApp/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/jusou/app/
│   │   ├── MainActivity.kt          # 界面与交互
│   │   ├── Platform.kt              # 四平台深链/网页地址（DeepLink 含包名机制）
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
