package com.piloto.particles

import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Colores ARGB escritos como Long (0xAARRGGBBL) para evitar problemas de Int con alfa alto. */
fun col(h: Long): Int = h.toInt()

const val TAU = (2.0 * PI).toFloat()

/** Caja de herramientas de dibujo. Las unidades son las del lienzo en ese momento. */
class Kit(val d: Float) {
    lateinit var cv: Canvas
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val path = Path()
    val rect = RectF()
    private val tp = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val pm = PathMeasure()
    private val pos = FloatArray(2)

    fun fill(c: Int) { p.style = Paint.Style.FILL; p.color = c }
    fun stroke(c: Int, w: Float) { p.style = Paint.Style.STROKE; p.color = c; p.strokeWidth = w }
    fun circle(x: Float, y: Float, r: Float) = cv.drawCircle(x, y, r, p)
    fun oval(l: Float, t: Float, r: Float, b: Float) { rect.set(l, t, r, b); cv.drawOval(rect, p) }
    fun rrect(l: Float, t: Float, r: Float, b: Float, rad: Float) { rect.set(l, t, r, b); cv.drawRoundRect(rect, rad, rad, p) }
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) = cv.drawLine(x1, y1, x2, y2, p)

    fun text(s: String, x: Float, y: Float, size: Float, color: Int, align: Paint.Align = Paint.Align.CENTER, bold: Boolean = false) {
        p.style = Paint.Style.FILL; p.color = color; p.textSize = size; p.textAlign = align
        p.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        cv.drawText(s, x, y, p)
        p.typeface = Typeface.DEFAULT
    }

    /** Texto con fondo oscuro para que se lea sobre cualquier cosa. */
    fun label(s: String, x: Float, y: Float, size: Float, align: Paint.Align = Paint.Align.CENTER, color: Int = col(0xFFFFFFFFL)) {
        p.textSize = size; p.textAlign = align; p.style = Paint.Style.FILL
        val w = p.measureText(s)
        val l = when (align) { Paint.Align.LEFT -> x; Paint.Align.RIGHT -> x - w; else -> x - w / 2 }
        fill(col(0xB0000000L))
        rrect(l - size * 0.35f, y - size * 1.0f, l + w + size * 0.35f, y + size * 0.3f, size * 0.4f)
        text(s, x, y, size, color, align)
    }

    /** Camino con función de puntos: f(i / n) -> (x, y) escrito en out. */
    inline fun curve(n: Int, closed: Boolean = false, f: (Float, FloatArray) -> Unit) {
        val o = FloatArray(2)
        path.reset()
        for (i in 0..n) {
            f(i.toFloat() / n, o)
            if (i == 0) path.moveTo(o[0], o[1]) else path.lineTo(o[0], o[1])
        }
        if (closed) path.close()
    }

    fun drawPath() = cv.drawPath(path, p)

    /** Punto a la fracción f (0..1) del camino actual. */
    fun onPath(f: Float): FloatArray {
        pm.setPath(path, false)
        pm.getPosTan(pm.length * f.coerceIn(0f, 1f), pos, null)
        return pos
    }

    /** Doble hélice de ADN entre dos puntos horizontales. */
    fun helix(x0: Float, x1: Float, y: Float, amp: Float, turns: Float, phase: Float, c1: Int, c2: Int, w: Float) {
        val n = 60
        for (s in 0..1) {
            path.reset()
            for (i in 0..n) {
                val f = i.toFloat() / n
                val px = x0 + (x1 - x0) * f
                val py = y + amp * sin(f * turns * TAU + phase + s * PI.toFloat())
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            stroke(if (s == 0) c1 else c2, w); drawPath()
        }
        stroke(col(0x66FFFFFFL), w * 0.5f)
        for (i in 0..(turns * 8).toInt()) {
            val f = i / (turns * 8)
            val px = x0 + (x1 - x0) * f
            val a = amp * sin(f * turns * TAU + phase)
            line(px, y + a, px, y - a)
        }
    }
}

/** Contenido mostrable al tocar una parte: título, texto explicativo y (opcional) animación 300x240. */
class Info(val title: String, val body: String, val anim: ((Kit, Float) -> Unit)? = null)

/** Detecta un toque corto (sin arrastre). */
class TapDetector(private val slop: Float) {
    private var x0 = 0f
    private var y0 = 0f
    private var t0 = 0L
    private var moved = false
    fun down(x: Float, y: Float) { x0 = x; y0 = y; t0 = System.nanoTime(); moved = false }
    fun move(x: Float, y: Float) {
        val dx = x - x0; val dy = y - y0
        if (dx * dx + dy * dy > slop * slop) moved = true
    }
    fun up(): Boolean = !moved && System.nanoTime() - t0 < 500_000_000L
}

/** Panel a pantalla completa con animación ampliada y explicación. */
class Overlay(private val kit: Kit) {
    var info: Info? = null
        private set
    private var open = 0f
    private var target = 0f
    private var time = 0f
    private var layout: StaticLayout? = null
    private val tp = TextPaint(Paint.ANTI_ALIAS_FLAG)

    val isOpen get() = target > 0f

    fun open(i: Info) { info = i; target = 1f; time = 0f; layout = null }
    fun close() { target = 0f }

    fun update(dt: Float) {
        time += dt
        open += (target - open) * min(1f, dt * 10f)
        if (target == 0f && open < 0.01f) open = 0f
    }

    fun draw(cv: Canvas, w: Float, h: Float) {
        val i = info ?: return
        if (open <= 0.01f) return
        val d = kit.d
        val layer = cv.saveLayerAlpha(0f, 0f, w, h, (open * 255).toInt())
        cv.scale(0.94f + 0.06f * open, 0.94f + 0.06f * open, w / 2, h / 2)
        kit.cv = cv
        kit.fill(col(0xEE03060AL)); cv.drawRect(0f, 0f, w, h, kit.p)
        val m = 14f * d
        var y = 26f * d
        if (i.anim != null) {
            val bw = w - 2 * m
            val bh = min(h * 0.42f, bw * 0.8f)
            kit.fill(col(0xFF0B161CL)); kit.rrect(m, y, m + bw, y + bh, 16f * d)
            cv.save()
            cv.clipRect(m, y, m + bw, y + bh)
            val s = min(bw / 300f, bh / 240f)
            cv.translate(m + (bw - 300f * s) / 2, y + (bh - 240f * s) / 2)
            cv.scale(s, s)
            i.anim.invoke(kit, time)
            cv.restore()
            y += bh + 18f * d
        }
        kit.text(i.title, m, y + 20f * d, 21f * d, col(0xFFFFE08AL), Paint.Align.LEFT, true)
        y += 34f * d
        var lay = layout
        if (lay == null) {
            tp.color = col(0xFFE8EEF2L); tp.textSize = 14f * d
            lay = StaticLayout.Builder.obtain(i.body, 0, i.body.length, tp, (w - 2 * m).toInt())
                .setLineSpacing(0f, 1.12f).build()
            layout = lay
        }
        cv.save(); cv.translate(m, y); lay!!.draw(cv); cv.restore()
        kit.text("Toca para cerrar", w / 2, h - 18f * d, 12f * d, col(0x99FFFFFFL))
        cv.restoreToCount(layer)
    }
}

/** Escena con panel de información opcional. */
interface Scene {
    fun resize(width: Int, height: Int)
    fun touchDown(px: Float, py: Float)
    fun touchMove(px: Float, py: Float)
    fun touchUp()
    fun update(dt: Float)
    fun draw(canvas: Canvas)
    fun configure(prefs: SharedPreferences) {}
}
