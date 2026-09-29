package com.piloto.particles

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Choreographer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView

/** Vista previa a pantalla completa; los ajustes viven en un pequeño desplegable en la esquina. */
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
    private lateinit var heartBox: LinearLayout
    private var selected = 0

    private val names = intArrayOf(R.string.name_particles, R.string.name_blood, R.string.name_cell, R.string.name_eukaryote, R.string.name_plant, R.string.name_heart)
    private val services = arrayOf<Class<*>>(
        ParticleWallpaperService::class.java, BloodWallpaperService::class.java, CellWallpaperService::class.java,
        EukaryoteWallpaperService::class.java, PlantWallpaperService::class.java, HeartWallpaperService::class.java
    )
    private val heartIndex = 5

    private fun createScene(i: Int): Scene {
        val d = resources.displayMetrics.density
        val s: Scene = when (i) {
            1 -> BloodScene(d)
            2 -> ProkaryoteScene(d)
            3 -> EukaryoteScene(d)
            4 -> PlantScene(d)
            5 -> HeartScene(d)
            else -> ParticleSystem(d)
        }
        s.configure(prefs)
        return s
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun label(text: String) = TextView(this).apply { this.text = text; setTextColor(Color.WHITE); textSize = 11f }

    private fun applyPref(block: SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(block).apply()
        preview.scene?.configure(prefs)
    }

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
        return LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(value); addView(sb) }
    }

    private fun check(text: String, key: String, def: Boolean) = CheckBox(this).apply {
        this.text = text; setTextColor(Color.WHITE); textSize = 11f
        isChecked = prefs.getBoolean(key, def)
        setOnCheckedChangeListener { _, on -> applyPref { putBoolean(key, on) } }
    }

    private fun spinner(items: List<String>, initial: Int, onPick: (Int) -> Unit) = Spinner(this).apply {
        adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, items)
        setSelection(initial)
        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { onPick(pos) }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCtx.ctx = applicationContext
        prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
        preview = PreviewView(this)
        preview.scene = createScene(selected)

        // Ajustes del corazón (solo visibles con el corazón elegido)
        heartBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        heartBox.addView(label("Plano de corte"))
        heartBox.addView(spinner(HeartScene.PLANES.toList(), prefs.getInt("heart_plane", 4)) { i -> if (i != prefs.getInt("heart_plane", 4)) applyPref { putInt("heart_plane", i) } })
        heartBox.addView(seek("Frecuencia", 30, 200, "heart_bpm", 72, " lpm"))
        heartBox.addView(seek("Velocidad", 10, 100, "heart_speed", 100, " %"))
        heartBox.addView(check("Etiquetas", "heart_labels", true))
        heartBox.addView(check("Conducción eléctrica", "heart_cond", true))

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply { setColor(Color.argb(215, 8, 10, 14)); cornerRadius = dp(14).toFloat() }
        }
        panel.addView(label("Fondo"))
        panel.addView(spinner(names.map { getString(it) }, selected) { i ->
            if (i != selected) {
                selected = i
                preview.scene = createScene(i)
                heartBox.visibility = if (i == heartIndex) View.VISIBLE else View.GONE
            }
        })
        panel.addView(Button(this).apply {
            setText(R.string.set_wallpaper); textSize = 12f; setAllCaps(false)
            setOnClickListener {
                startActivity(
                    Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(this@MainActivity, services[selected])
                    )
                )
            }
        })
        panel.addView(heartBox)
        val scroll = ScrollView(this).apply { addView(panel); visibility = View.GONE }

        val gear = Button(this).apply {
            text = "⚙"; textSize = 18f; alpha = 0.65f
            setOnClickListener { scroll.visibility = if (scroll.visibility == View.VISIBLE) View.GONE else View.VISIBLE }
        }
        val root = FrameLayout(this)
        root.addView(preview)
        root.addView(scroll, FrameLayout.LayoutParams(dp(250), FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.END).apply {
            bottomMargin = dp(62); rightMargin = dp(10)
        })
        root.addView(gear, FrameLayout.LayoutParams(dp(48), dp(48), Gravity.BOTTOM or Gravity.END).apply { bottomMargin = dp(10); rightMargin = dp(10) })
        setContentView(root)
        scroll.viewTreeObserver.addOnGlobalLayoutListener {
            val maxH = (root.height * 0.62f).toInt()
            if (maxH > 0 && scroll.height > maxH) scroll.layoutParams = scroll.layoutParams.apply { height = maxH }
        }
    }

    override fun onResume() { super.onResume(); preview.start() }
    override fun onPause() { super.onPause(); preview.stop() }
}
