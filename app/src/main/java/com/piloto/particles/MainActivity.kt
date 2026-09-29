package com.piloto.particles

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.Choreographer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

/** Vista previa interactiva de los fondos + ajustes del corazón + botón para establecer el elegido. */
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
    private lateinit var prefs: SharedPreferences
    private lateinit var heartPanel: LinearLayout
    private var selected = 0

    private val names = intArrayOf(R.string.name_particles, R.string.name_blood, R.string.name_cell, R.string.name_eukaryote, R.string.name_heart)
    private val services = arrayOf<Class<*>>(
        ParticleWallpaperService::class.java, BloodWallpaperService::class.java, CellWallpaperService::class.java,
        EukaryoteWallpaperService::class.java, HeartWallpaperService::class.java
    )

    private fun createScene(i: Int): Scene {
        val d = resources.displayMetrics.density
        val s: Scene = when (i) {
            1 -> BloodScene(d)
            2 -> ProkaryoteScene(d)
            3 -> EukaryoteScene(d)
            4 -> HeartScene(d)
            else -> ParticleSystem(d)
        }
        s.configure(prefs)
        return s
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun label(text: String) = TextView(this).apply { this.text = text; setTextColor(Color.WHITE); textSize = 12f }

    private fun seek(title: String, min: Int, max: Int, key: String, def: Int, unit: String): LinearLayout {
        val value = label("")
        val sb = SeekBar(this).apply {
            this.max = max - min
            progress = prefs.getInt(key, def) - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar, p: Int, fromUser: Boolean) {
                    value.text = "$title: ${p + min}$unit"
                    if (fromUser) applyPref { putInt(key, p + min) }
                }
                override fun onStartTrackingTouch(bar: SeekBar) {}
                override fun onStopTrackingTouch(bar: SeekBar) {}
            })
        }
        value.text = "$title: ${sb.progress + min}$unit"
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(value); addView(sb)
        }
    }

    private fun applyPref(block: SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(block).apply()
        preview.scene?.configure(prefs)
    }

    private fun check(text: String, key: String, def: Boolean) = CheckBox(this).apply {
        this.text = text; setTextColor(Color.WHITE); textSize = 12f
        isChecked = prefs.getBoolean(key, def)
        setOnCheckedChangeListener { _, on -> applyPref { putBoolean(key, on) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
        val d = resources.displayMetrics.density
        preview = PreviewView(this)
        preview.scene = createScene(selected)

        // Ajustes del corazón
        heartPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.argb(150, 0, 0, 0))
            setPadding(dp(10), dp(6), dp(10), dp(6))
            visibility = View.GONE
        }
        val planes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        HeartScene.PLANES.forEachIndexed { i, name ->
            planes.addView(Button(this).apply {
                text = name; textSize = 10f; setAllCaps(false)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { applyPref { putInt("heart_plane", i) } }
            })
        }
        heartPanel.addView(planes)
        heartPanel.addView(seek("Frecuencia", 30, 200, "heart_bpm", 72, " lpm"))
        heartPanel.addView(seek("Velocidad (cámara lenta)", 10, 100, "heart_speed", 100, " %"))
        val checks = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        checks.addView(check("Etiquetas", "heart_labels", true))
        checks.addView(check("Conducción eléctrica", "heart_cond", true))
        heartPanel.addView(checks)

        val picker = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        names.forEachIndexed { i, res ->
            picker.addView(Button(this).apply {
                setText(res); textSize = 12f; setAllCaps(false)
                setOnClickListener {
                    selected = i
                    preview.scene = createScene(i)
                    heartPanel.visibility = if (i == 4) View.VISIBLE else View.GONE
                }
            })
        }
        val scroll = HorizontalScrollView(this).apply { addView(picker); isHorizontalScrollBarEnabled = false }
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
            addView(heartPanel, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(scroll)
            addView(set)
        }
        val root = FrameLayout(this)
        root.addView(preview)
        root.addView(bar, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
        ).apply { bottomMargin = (12 * d).toInt() })
        setContentView(root)
    }

    override fun onResume() { super.onResume(); preview.start() }
    override fun onPause() { super.onPause(); preview.stop() }
}
