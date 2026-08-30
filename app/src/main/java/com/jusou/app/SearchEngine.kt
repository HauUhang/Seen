package com.jusou.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

/**
 * 打开目标平台的搜索结果：
 * 1. 优先尝试原生 App 深链（直接落到搜索页，避开推荐流）；
 * 2. 未安装或深链不可用时，降级为浏览器打开网页搜索。
 *
 * 不依赖 resolveActivity 预判（Android 11+ 包可见性会误判「未安装」），
 * 而是直接 startActivity + try/catch，唤起失败再尝试下一条。
 */
object SearchEngine {

    fun open(context: Context, platform: Platform, query: String) {
        val encoded = encode(query)

        for (link in platform.deepLinks(encoded)) {
            if (tryStart(context, link)) return
        }

        // 网页兜底
        tryStart(context, DeepLink(Uri.parse(platform.webUrl(encoded))))
    }

    private fun tryStart(context: Context, link: DeepLink): Boolean =
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, link.uri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            link.packageName?.let { intent.setPackage(it) }
            context.startActivity(intent)
            true
        }.getOrDefault(false)

    private fun encode(raw: String): String =
        URLEncoder.encode(raw, "UTF-8").replace("+", "%20")
}
