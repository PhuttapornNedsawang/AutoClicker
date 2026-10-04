package com.example.autoclicker

import android.content.Context

/** เก็บการตั้งค่า: ช่วงเวลา, ค่าสุ่ม, จำนวนรอบ, ตำแหน่งจุดคลิก */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("autoclicker", Context.MODE_PRIVATE)

    var intervalMs: Long
        get() = sp.getLong("interval", 1000L)
        set(v) = sp.edit().putLong("interval", v).apply()

    var jitterMs: Long
        get() = sp.getLong("jitter", 0L)
        set(v) = sp.edit().putLong("jitter", v).apply()

    /** 0 = วนไม่จำกัด */
    var rounds: Int
        get() = sp.getInt("rounds", 0)
        set(v) = sp.edit().putInt("rounds", v).apply()

    var points: List<Pair<Int, Int>>
        get() = (sp.getString("points", "") ?: "").split(";").mapNotNull {
            val p = it.split(",")
            if (p.size == 2) Pair(p[0].toInt(), p[1].toInt()) else null
        }
        set(v) = sp.edit().putString("points", v.joinToString(";") { "${it.first},${it.second}" }).apply()
}
