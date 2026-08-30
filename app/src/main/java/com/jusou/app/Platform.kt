package com.jusou.app

import android.net.Uri

/**
 * 一个深链候选：uri + 可选的目标包名。
 * 指定 packageName 可精确唤起某个 App 版本（如 B 站国内版 / 国际版 / 港澳台版）。
 */
data class DeepLink(val uri: Uri, val packageName: String? = null)

/**
 * 支持的搜索平台。
 * deepLinks 返回按优先级排列的原生 App 深链，webUrl 是网页搜索的兜底地址。
 * 各平台的 scheme / 路径若随版本变化，可在此集中修改。
 */
enum class Platform(val title: String, val short: String) {
    DOUYIN("抖音", "抖"),
    XIAOHONGSHU("小红书", "红"),
    ZHIHU("知乎", "知"),
    BILIBILI("哔哩哔哩", "B");

    fun deepLinks(encodedQuery: String): List<DeepLink> = when (this) {
        DOUYIN -> listOf(
            DeepLink(Uri.parse("snssdk1128://search?keyword=$encodedQuery")),
            DeepLink(Uri.parse("snssdk1128://search/keyword?keyword=$encodedQuery")),
        )
        XIAOHONGSHU -> listOf(
            DeepLink(Uri.parse("xhsdiscover://search/result?keyword=$encodedQuery")),
            DeepLink(Uri.parse("xhsdiscover://search_result?keyword=$encodedQuery")),
        )
        ZHIHU -> listOf(
            DeepLink(Uri.parse("zhihu://search?q=$encodedQuery")),
            DeepLink(Uri.parse("zhihu://search?type=content&q=$encodedQuery")),
        )
        BILIBILI -> listOf(
            // 国内版
            DeepLink(Uri.parse("bilibili://search?keyword=$encodedQuery"), "tv.danmaku.bili"),
            // 国际版 / 港澳台版
            DeepLink(Uri.parse("bilibili://search?keyword=$encodedQuery"), "com.bilibili.app.in"),
            // 通用兜底：不指定包名，交给系统选择已安装的版本
            DeepLink(Uri.parse("bilibili://search?keyword=$encodedQuery")),
        )
    }

    fun webUrl(encodedQuery: String): String = when (this) {
        DOUYIN -> "https://www.douyin.com/search/$encodedQuery?type=general"
        XIAOHONGSHU -> "https://www.xiaohongshu.com/search_result?keyword=$encodedQuery"
        ZHIHU -> "https://www.zhihu.com/search?type=content&q=$encodedQuery"
        BILIBILI -> "https://search.bilibili.com/all?keyword=$encodedQuery"
    }
}
