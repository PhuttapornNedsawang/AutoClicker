package com.example.autoclicker

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import android.app.Activity

class MainActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var interval: EditText
    private lateinit var jitter: EditText
    private lateinit var rounds: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad) }

        fun label(t: String) = TextView(this).apply { text = t; textSize = 16f; setPadding(0, pad / 2, 0, 0) }
        fun field(v: String) = EditText(this).apply {
            setText(v); inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        root.addView(label("ช่วงเวลาระหว่างการแตะ (มิลลิวินาที, 1000 = 1 วินาที)"))
        interval = field(prefs.intervalMs.toString()); root.addView(interval)
        root.addView(label("สุ่มเวลาเพิ่ม 0 ถึง ... มิลลิวินาที (0 = ไม่สุ่ม)"))
        jitter = field(prefs.jitterMs.toString()); root.addView(jitter)
        root.addView(label("จำนวนรอบ (0 = ไม่จำกัด)"))
        rounds = field(prefs.rounds.toString()); root.addView(rounds)

        fun button(t: String, onClick: () -> Unit) = Button(this).apply { text = t; setOnClickListener { onClick() } }

        root.addView(button("1) เปิดบริการ Accessibility") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        root.addView(button("2) บันทึก และแสดงแผงควบคุมลอย") {
            save()
            val svc = AutoClickService.instance
            if (svc == null) {
                Toast.makeText(this, "ยังไม่ได้เปิดบริการ Accessibility", Toast.LENGTH_LONG).show()
            } else {
                svc.showOverlay()
                moveTaskToBack(true)
            }
        })
        root.addView(TextView(this).apply {
            textSize = 14f; setPadding(0, pad, 0, 0)
            text = "วิธีใช้: กด + เพื่อเพิ่มจุดคลิก ลากวงกลมเลขไปวางตำแหน่งที่ต้องการ " +
                "แล้วกด ▶ เพื่อเริ่ม / ■ เพื่อหยุด (✕ ปิดแผง)"
        })
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun save() {
        prefs.intervalMs = (interval.text.toString().toLongOrNull() ?: 1000L).coerceAtLeast(10L)
        prefs.jitterMs = jitter.text.toString().toLongOrNull() ?: 0L
        prefs.rounds = rounds.text.toString().toIntOrNull() ?: 0
    }
}
