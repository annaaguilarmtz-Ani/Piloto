package com.piloto.particles

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Choreographer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout

/** Vista previa interactiva + botón para establecer el fondo animado. */
class MainActivity : Activity() {

    private class PreviewView(context: Context) : View(context), Choreographer.FrameCallback {
        private val system = ParticleSystem(resources.displayMetrics.density)
        private var last = 0L
        private var running = false

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) = system.resize(w, h)

        override fun onDraw(canvas: android.graphics.Canvas) = system.draw(canvas)

        override fun onTouchEvent(e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> system.touchDown(e.x, e.y)
                MotionEvent.ACTION_MOVE -> system.touchMove(e.x, e.y)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> system.touchUp()
            }
            return true
        }

        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            val dt = if (last == 0L) 0.016f else (frameTimeNanos - last) / 1_000_000_000f
            last = frameTimeNanos
            system.update(dt)
            invalidate()
            Choreographer.getInstance().postFrameCallback(this)
        }

        fun start() { running = true; last = 0L; Choreographer.getInstance().postFrameCallback(this) }
        fun stop() { running = false; Choreographer.getInstance().removeFrameCallback(this) }
    }

    private lateinit var preview: PreviewView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preview = PreviewView(this)
        val button = Button(this).apply {
            setText(R.string.set_wallpaper)
            setOnClickListener {
                val i = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, ParticleWallpaperService::class.java)
                )
                startActivity(i)
            }
        }
        val root = FrameLayout(this)
        root.addView(preview)
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        ).apply { bottomMargin = (48 * resources.displayMetrics.density).toInt() }
        root.addView(button, lp)
        setContentView(root)
    }

    override fun onResume() { super.onResume(); preview.start() }
    override fun onPause() { super.onPause(); preview.stop() }
}
