package com.jusou.app

import android.content.Context
import org.json.JSONArray

/**
 * 搜索历史的本地持久化（SharedPreferences）。
 * 最新在前、自动去重、最多保留 [MAX] 条。
 */
object SearchHistoryManager {

    private const val PREFS = "jusou_prefs"
    private const val KEY = "history"
    private const val MAX = 20

    fun load(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrDefault(emptyList())
    }

    fun add(context: Context, query: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = load(context).toMutableList()
        current.remove(query)
        current.add(0, query)
        while (current.size > MAX) {
            current.removeAt(current.size - 1)
        }
        val arr = JSONArray()
        current.forEach { arr.put(it) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY).apply()
    }
}
