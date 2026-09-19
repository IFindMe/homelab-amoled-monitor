package com.ifindme.homelabmonitor

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var monitorView: MonitorView
    private val api = ApiClient()

    private val poll = object : Runnable {
        override fun run() {
            executor.execute {
                try {
                    val value = api.fetch()
                    handler.post { monitorView.setStatus(value) }
                } catch (_: Exception) {
                    handler.post { monitorView.setOffline() }
                }
            }
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(Window.FEATURE_NO_TITLE)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        hideSystemBars()
        monitorView = MonitorView()
        setContentView(monitorView)
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        handler.removeCallbacks(poll)
        handler.post(poll)
    }

    override fun onPause() {
        handler.removeCallbacks(poll)
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun hideSystemBars() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    private inner class MonitorView : View(this@MainActivity) {
        private var status: ServerStatus? = null
        private var offline = true
        private var offsetX = 0
        private var offsetY = 0
        private var lastShift = System.currentTimeMillis()

        private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            typeface = android.graphics.Typeface.MONOSPACE
            color = Color.WHITE
        }

        init { setBackgroundColor(Color.BLACK) }

        fun setStatus(value: ServerStatus) {
            status = value
            offline = false
            invalidate()
        }

        fun setOffline() {
            offline = true
            invalidate()
        }

        override fun onDraw(canvas: android.graphics.Canvas) {
            super.onDraw(canvas)

            if (System.currentTimeMillis() - lastShift > 180_000) {
                offsetX = when (offsetX) { 0 -> 2; 2 -> -2; else -> 0 }
                offsetY = when (offsetY) { 0 -> 1; 1 -> -1; else -> 0 }
                lastShift = System.currentTimeMillis()
            }

            canvas.save()
            canvas.translate(offsetX.toFloat(), offsetY.toFloat())

            val x = 28f
            var y = 72f
            paint.textSize = 25f
            paint.color = Color.WHITE
            canvas.drawText("HOMELAB", x, y, paint)

            paint.textSize = 13f
            paint.color = Color.rgb(130, 130, 130)
            y += 28f
            canvas.drawText("SYSTEM", x, y, paint)
            y += 32f

            val s = status
            if (s != null && !offline) {
                paint.textSize = 19f
                paint.color = Color.WHITE
                canvas.drawText("CPU    " + percent(s.cpu), x, y, paint); y += 30f
                canvas.drawText("RAM    " + percent(s.ram), x, y, paint); y += 30f
                canvas.drawText("DISK   " + percent(s.disk), x, y, paint); y += 30f
                canvas.drawText("UPTIME " + uptime(s.uptime), x, y, paint); y += 46f

                paint.textSize = 13f
                paint.color = Color.rgb(130, 130, 130)
                canvas.drawText("NETWORK", x, y, paint); y += 30f

                paint.textSize = 19f
                paint.color = Color.WHITE
                canvas.drawText("RX     " + bytes(s.rxBytes), x, y, paint); y += 30f
                canvas.drawText("TX     " + bytes(s.txBytes), x, y, paint); y += 48f

                paint.textSize = 12f
                paint.color = Color.rgb(100, 100, 100)
                canvas.drawText("100.100.1.2:8181  •  LIVE", x, y, paint)
            } else {
                paint.textSize = 19f
                paint.color = Color.WHITE
                canvas.drawText("SERVER OFFLINE", x, y, paint); y += 32f
                paint.textSize = 13f
                paint.color = Color.rgb(120, 120, 120)
                canvas.drawText("Waiting for 100.100.1.2:8181", x, y, paint)
            }

            canvas.restore()
        }

        private fun percent(v: Double) = String.format(Locale.US, "%5.1f%%", v)

        private fun bytes(value: Long): String {
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            var v = value.toDouble()
            var i = 0
            while (v >= 1024 && i < units.lastIndex) { v /= 1024.0; i++ }
            return if (i == 0) value.toString() + " " + units[i]
            else String.format(Locale.US, "%.1f %s", v, units[i])
        }

        private fun uptime(seconds: Long): String {
            val d = seconds / 86400
            val h = (seconds % 86400) / 3600
            val m = (seconds % 3600) / 60
            return if (d > 0) d.toString() + "d " + h + "h " + m + "m"
            else h.toString() + "h " + m + "m"
        }
    }
}
