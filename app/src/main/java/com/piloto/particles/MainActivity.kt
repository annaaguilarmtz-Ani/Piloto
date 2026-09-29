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
import android.widget.LinearLayout

/** Vista previa interactiva de los fondos + botón para establecer el elegido. */
class MainActivity : Activity() {

    private class PreviewView(context: Context) : View(context), Choreographer.FrameCallback {
        var scene: Scene? = null
            set(value) {
                field = value
                if (width > 0) value?.resize(width, height)
            }
        private var last = 0L
        private var running = false

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) { scene?.resize(w, h) }

        override fun onDraw(canvas: android.graphics.Canvas) { scene?.draw(canvas) }

        override fun onTouchEvent(e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> scene?.touchDown(e.x, e.y)
                MotionEvent.ACTION_MOVE -> scene?.touchMove(e.x, e.y)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> scene?.touchUp()
            }
            return true
        }

        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            val dt = if (last == 0L) 0.016f else (frameTimeNanos - last) / 1_000_000_000f
            last = frameTimeNanos
            scene?.update(dt)
            invalidate()
            Choreographer.getInstance().postFrameCallback(this)
        }

        fun start() { running = true; last = 0L; Choreographer.getInstance().postFrameCallback(this) }
        fun stop() { running = false; Choreographer.getInstance().removeFrameCallback(this) }
    }

    private lateinit var preview: PreviewView
    private var selected = 0

    private val names = intArrayOf(R.string.name_particles, R.string.name_blood, R.string.name_cell)
    private val services = arrayOf<Class<*>>(
        ParticleWallpaperService::class.java, BloodWallpaperService::class.java, CellWallpaperService::class.java
    )

    private fun createScene(i: Int): Scene {
        val d = resources.displayMetrics.density
        return when (i) {
            1 -> BloodScene(d)
            2 -> ProkaryoteScene(d)
            else -> ParticleSystem(d)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val d = resources.displayMetrics.density
        preview = PreviewView(this)
        preview.scene = createScene(selected)

        val picker = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        names.forEachIndexed { i, res ->
            picker.addView(Button(this).apply {
                setText(res)
                textSize = 12f
                setOnClickListener { selected = i; preview.scene = createScene(i) }
            })
        }
        val set = Button(this).apply {
            setText(R.string.set_wallpaper)
            setOnClickListener {
                startActivity(
                    Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(this@MainActivity, services[selected])
                    )
                )
            }
        }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(picker)
            addView(set)
        }
        val root = FrameLayout(this)
        root.addView(preview)
        root.addView(bar, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        ).apply { bottomMargin = (24 * d).toInt() })
        setContentView(root)
    }

    override fun onResume() { super.onResume(); preview.start() }
    override fun onPause() { super.onPause(); preview.stop() }
}
