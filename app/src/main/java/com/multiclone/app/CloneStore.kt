package com.multiclone.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object CloneStore {
    private const val PREF = "clones"
    private const val KEY = "list"

    fun load(ctx: Context): MutableList<CloneItem> {
        val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val arr = JSONArray(sp.getString(KEY, "[]"))
        val out = mutableListOf<CloneItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(CloneItem(o.getString("id"), o.getString("pkg"), o.getString("label")))
        }
        return out
    }

    fun save(ctx: Context, list: List<CloneItem>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("pkg", it.pkg).put("label", it.label))
        }
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }
}
