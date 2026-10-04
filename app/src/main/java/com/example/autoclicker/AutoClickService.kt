package com.example.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.random.Random

class AutoClickService : AccessibilityService() {
    companion object { var instance: AutoClickService? = null }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var wm: WindowManager
    private lateinit var prefs: Prefs
    private var panel: View? = null
    private var playBtn: TextView? = null
    private val markers = mutableListOf<TextView>()
    private var running = false
    private var index = 0
    private var round = 0
    private val dp get() = resources.displayMetrics.density
    private val markerSize get() = (52 * dp).toInt()

    override fun onServiceConnected() {
        instance = this
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = Prefs(this)
    }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() { stopClicking() }
    override fun onUnbind(intent: android.content.Intent?): Boolean {
        hideOverlay(); instance = null; return super.onUnbind(intent)
    }

    // ---------- Overlay ----------
    private fun params(w: Int, h: Int, x: Int, y: Int) = WindowManager.LayoutParams(
        w, h, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START; this.x = x; this.y = y }

    fun showOverlay() {
        if (panel != null) return
        prefs = Prefs(this)
        buildPanel()
        prefs.points.forEach { addMarker(it.first - markerSize / 2, it.second - markerSize / 2) }
    }

    fun hideOverlay() {
        stopClicking()
        markers.forEach { runCatching { wm.removeView(it) } }; markers.clear()
        panel?.let { runCatching { wm.removeView(it) } }; panel = null
    }

    private fun buildPanel() {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xDD222222.toInt())
        }
        fun btn(t: String, click: (() -> Unit)?) = TextView(this).apply {
            text = t; textSize = 24f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            setPadding((14 * dp).toInt(), (8 * dp).toInt(), (14 * dp).toInt(), (8 * dp).toInt())
            click?.let { c -> setOnClickListener { c() } }
        }
        val drag = btn("⠿", null)
        playBtn = btn("▶") { toggle() }
        bar.addView(drag); bar.addView(playBtn)
        bar.addView(btn("+") { addMarker(200, 400) })
        bar.addView(btn("🗑") { clearMarkers() })
        bar.addView(btn("✕") { savePoints(); hideOverlay() })

        val lp = params(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, 50, 100)
        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0
        drag.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> { sx = ev.rawX; sy = ev.rawY; ox = lp.x; oy = lp.y }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = ox + (ev.rawX - sx).toInt(); lp.y = oy + (ev.rawY - sy).toInt()
                    wm.updateViewLayout(bar, lp)
                }
            }
            true
        }
        wm.addView(bar, lp); panel = bar
    }

    private fun addMarker(x: Int, y: Int) {
        val m = TextView(this).apply {
            gravity = Gravity.CENTER; setTextColor(Color.WHITE); textSize = 18f
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xAAE53935.toInt()); setStroke(4, Color.WHITE) }
        }
        val lp = params(markerSize, markerSize, x, y)
        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0
        m.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> { sx = ev.rawX; sy = ev.rawY; ox = lp.x; oy = lp.y }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = ox + (ev.rawX - sx).toInt(); lp.y = oy + (ev.rawY - sy).toInt()
                    wm.updateViewLayout(m, lp)
                }
                MotionEvent.ACTION_UP -> savePoints()
            }
            true
        }
        m.tag = lp
        wm.addView(m, lp); markers.add(m); renumber(); savePoints()
    }

    private fun renumber() = markers.forEachIndexed { i, m -> m.text = "${i + 1}" }

    private fun clearMarkers() {
        stopClicking()
        markers.forEach { runCatching { wm.removeView(it) } }; markers.clear(); savePoints()
    }

    private fun savePoints() {
        prefs.points = markers.map { val lp = it.tag as WindowManager.LayoutParams
            Pair(lp.x + markerSize / 2, lp.y + markerSize / 2) }
    }

    /** ขณะทำงานให้วงกลมโปร่งสัมผัส เพื่อให้การแตะทะลุไปยังแอปด้านล่าง */
    private fun setMarkersTouchable(touchable: Boolean) = markers.forEach {
        val lp = it.tag as WindowManager.LayoutParams
        lp.flags = if (touchable) lp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                   else lp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        wm.updateViewLayout(it, lp)
    }

    // ---------- Clicking ----------
    private fun toggle() = if (running) stopClicking() else startClicking()

    private fun startClicking() {
        if (markers.isEmpty() || running) return
        prefs = Prefs(this)
        running = true; index = 0; round = 0
        playBtn?.text = "■"; setMarkersTouchable(false)
        handler.postDelayed({ tap() }, 300)
    }

    private fun stopClicking() {
        running = false; handler.removeCallbacksAndMessages(null)
        playBtn?.text = "▶"; setMarkersTouchable(true)
    }

    private fun tap() {
        if (!running || markers.isEmpty()) return
        val lp = markers[index].tag as WindowManager.LayoutParams
        val path = Path().apply { moveTo((lp.x + markerSize / 2).toFloat(), (lp.y + markerSize / 2).toFloat()) }
        val g = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 50)).build()
        val ok = dispatchGesture(g, object : GestureResultCallback() {
            override fun onCompleted(d: GestureDescription?) = scheduleNext()
            override fun onCancelled(d: GestureDescription?) = scheduleNext()
        }, null)
        if (!ok) scheduleNext()
    }

    private fun scheduleNext() {
        if (!running) return
        index++
        if (index >= markers.size) {
            index = 0; round++
            if (prefs.rounds in 1..round) { stopClicking(); return }
        }
        val delay = prefs.intervalMs + if (prefs.jitterMs > 0) Random.nextLong(prefs.jitterMs + 1) else 0L
        handler.postDelayed({ tap() }, delay)
    }
}
