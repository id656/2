package com.superproxy.app

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

data class Profile(val name: String, val url: String, val isDefault: Boolean) {
    val hostPort: String get() = Uri.parse(url).let { "${it.host}:${it.port}" }
}

object ProfileStore {
    // формат строки: http://user:pass@host:port "Название" [*]
    private val LINE = Regex("""^(\S+)\s+"([^"]*)"\s*(\*)?\s*$""")

    fun parse(text: String): List<Profile> = text.removePrefix("\uFEFF").lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .mapNotNull { l -> LINE.find(l)?.let { Profile(it.groupValues[2], it.groupValues[1], it.groupValues[3] == "*") } }
        .toList()

    private fun prefs(c: Context) = c.getSharedPreferences("profiles", Context.MODE_PRIVATE)

    fun load(c: Context): List<Profile> {
        val raw = prefs(c).getString("list", null)
        if (raw == null) {
            val seed = c.assets.open("default_profiles.txt").bufferedReader().use { parse(it.readText()) }
            save(c, seed)
            setSelected(c, seed.indexOfFirst { it.isDefault }.coerceAtLeast(0))
            return seed
        }
        val arr = JSONArray(raw)
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Profile(o.getString("name"), o.getString("url"), o.optBoolean("def"))
        }
    }

    fun save(c: Context, list: List<Profile>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("name", it.name).put("url", it.url).put("def", it.isDefault)) }
        prefs(c).edit().putString("list", arr.toString()).apply()
    }

    fun selected(c: Context) = prefs(c).getInt("sel", 0)
    fun setSelected(c: Context, i: Int) = prefs(c).edit().putInt("sel", i).apply()
}
