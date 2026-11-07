package com.jusou.app

import android.content.Context

object PlatformPreferences {
    private const val PREFS = "jusou_prefs"
    private const val KEY = "enabled_platforms"

    fun enabled(context: Context): Set<Platform> {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY, null) ?: return Platform.values().toSet()
        return Platform.values().filterTo(mutableSetOf()) { it.name in saved }
    }

    fun save(context: Context, platforms: Set<Platform>) {
        require(platforms.isNotEmpty())
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY, platforms.map { it.name }.toSet()).apply()
    }
}
