package com.piloto.particles

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin

/** Imágenes 3D prerenderizadas (assets/cells) de orgánulos, células y fondos. */
object Sprites {
    private val cache = HashMap<String, Bitmap>()
    private val missing = HashSet<String>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val m = Matrix()
    private val r = RectF()

    fun get(name: String): Bitmap? {
        cache[name]?.let { return it }
        if (name in missing) return null
        return try {
            AppCtx.ctx.assets.open("cells/$name.webp").use { BitmapFactory.decodeStream(it) }?.also { cache[name] = it }
        } catch (e: Exception) { missing.add(name); null }
    }

    /** Fondo a pantalla completa. */
    fun bg(c: Canvas, name: String, w: Float, h: Float) {
        val b = get(name) ?: return
        r.set(0f, 0f, w, h); paint.alpha = 255
        c.drawBitmap(b, null, r, paint)
    }

    private val bok = Paint(Paint.ANTI_ALIAS_FLAG)

    /** Efecto de microscopio: discos desenfocados que derivan + viñeta, grano y polvo. */
    fun scope(c: Canvas, w: Float, h: Float, time: Float, d: Float) {
        for (i in 0 until 8) {
            val x = w * (0.5f + 0.48f * sin(time * 0.05f * (1f + i * 0.13f) + i * 2.1f))
            val y = h * (0.5f + 0.48f * cos(time * 0.04f * (1f + i * 0.17f) + i * 1.3f))
            val rad = (26f + (i % 4) * 15f) * d
            bok.style = Paint.Style.FILL; bok.color = 0x0EFFFFF0
            c.drawCircle(x, y, rad, bok)
            bok.style = Paint.Style.STROKE; bok.strokeWidth = 1.5f * d; bok.color = 0x18FFFFF0
            c.drawCircle(x, y, rad, bok)
        }
        bg(c, "scope", w, h)
    }

    /** Dibuja el sprite ajustado a un rectángulo. */
    fun drawRect(c: Canvas, name: String, l: Float, t: Float, rr: Float, b: Float) {
        val bm = get(name) ?: return
        r.set(l, t, rr, b); paint.alpha = 255
        c.drawBitmap(bm, null, r, paint)
    }

    /** Dibuja el sprite centrado en (x, y) con ancho wPx y rotación en grados. */
    fun draw(c: Canvas, name: String, x: Float, y: Float, wPx: Float, rot: Float = 0f, alpha: Int = 255, flip: Boolean = false) {
        val b = get(name) ?: return
        val s = wPx / b.width
        m.reset()
        m.postTranslate(-b.width / 2f, -b.height / 2f)
        m.postScale(if (flip) -s else s, s)
        m.postRotate(rot)
        m.postTranslate(x, y)
        paint.alpha = alpha
        c.drawBitmap(b, m, paint)
    }
}
