package com.piloto.particles

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Torrente sanguíneo visto por dentro de un vaso que corre de arriba abajo:
 * glóbulos rojos (discos bicóncavos), glóbulos blancos (núcleo lobulado y gránulos),
 * plaquetas y proteínas del plasma. El flujo es más rápido en el centro del vaso.
 * Al tocar, las células se apartan del dedo.
 */
class BloodScene(private val d: Float) : Scene {

    private class Ent(var x: Float, var y: Float, val r: Float, val phase: Float) {
        var vx = 0f
        var vy = 0f
        var ang = Random.nextFloat() * 6.28f
        var spin = 0f
    }

    private val rbc = ArrayList<Ent>()
    private val wbc = ArrayList<Ent>()
    private val plt = ArrayList<Ent>()
    private val dots = ArrayList<Ent>()

    private var w = 0f
    private var h = 0f
    private var wall = 0f
    private var time = 0f
    private var touching = false
    private var tx = 0f
    private var ty = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val oval = RectF()
    private val path = Path()
    private var bg: Paint? = null

    private val lumenCx get() = w / 2f
    private val lumenHw get() = w / 2f - wall

    private fun rndX() = lumenCx - lumenHw * 0.9f + Random.nextFloat() * lumenHw * 1.8f

    private fun make(list: ArrayList<Ent>, n: Int, rMin: Float, rMax: Float, spin: Float) {
        repeat(n) {
            val r = (rMin + Random.nextFloat() * (rMax - rMin)) * d
            list.add(Ent(rndX(), Random.nextFloat() * h, r, Random.nextFloat() * 6.28f).also {
                it.spin = (Random.nextFloat() - 0.5f) * 2f * spin
            })
        }
    }

    override fun resize(width: Int, height: Int) {
        val first = w == 0f
        w = width.toFloat(); h = height.toFloat()
        wall = w * 0.09f
        bg = Paint().apply {
            shader = LinearGradient(
                0f, 0f, w, 0f,
                intArrayOf(0xFF1C0206.toInt(), 0xFF4A0810.toInt(), 0xFF6A101A.toInt(), 0xFF4A0810.toInt(), 0xFF1C0206.toInt()),
                floatArrayOf(0f, 0.09f, 0.5f, 0.91f, 1f), Shader.TileMode.CLAMP
            )
        }
        if (first) {
            make(dots, 170, 0.6f, 1.3f, 0f)
            make(plt, 70, 2.4f, 3.6f, 3f)
            make(rbc, 38, 9f, 12.5f, 1.6f)
            make(wbc, 4, 17f, 21f, 0.4f)
        }
    }

    override fun touchDown(px: Float, py: Float) { touching = true; tx = px; ty = py }
    override fun touchMove(px: Float, py: Float) { tx = px; ty = py }
    override fun touchUp() { touching = false }

    private fun step(list: ArrayList<Ent>, k: Float, dt: Float) {
        val base = 110f * d * k
        val cx = lumenCx
        val hw = lumenHw
        val rad = 120f * d
        for (e in list) {
            if (touching) {
                val dx = e.x - tx
                val dy = e.y - ty
                val dist = max(sqrt(dx * dx + dy * dy), 1f)
                if (dist < rad) {
                    val push = (1f - dist / rad) * 1400f * d
                    e.vx += dx / dist * push * dt
                    e.vy += dy / dist * push * dt
                    e.spin += (dx / dist) * 4f * dt
                }
            }
            val damp = max(0f, 1f - 2.5f * dt)
            e.vx *= damp; e.vy *= damp
            val u = ((e.x - cx) / hw).coerceIn(-1f, 1f)
            val flow = base * max(0.2f, 1f - u * u)
            e.x += (e.vx + sin(time * 0.8f + e.phase) * 6f * d) * dt
            e.y += (flow + e.vy) * dt
            e.ang += e.spin * dt
            val minX = cx - hw + e.r
            val maxX = cx + hw - e.r
            if (e.x < minX) { e.x = minX; e.vx = abs(e.vx) } else if (e.x > maxX) { e.x = maxX; e.vx = -abs(e.vx) }
            if (e.y > h + e.r * 2f || e.y < -h * 0.5f) { e.y = -e.r * 2f; e.x = rndX(); e.vx = 0f; e.vy = 0f }
        }
    }

    override fun update(dt: Float) {
        if (w == 0f) return
        val t = min(dt, 0.05f)
        time += t
        step(dots, 1.15f, t)
        step(plt, 1f, t)
        step(rbc, 1f, t)
        step(wbc, 0.55f, t)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, w, h, bg ?: return)

        // Pared del vaso: células endoteliales aplanadas
        val period = 78f * d
        var y = -period + (time * 6f * d) % period
        while (y < h) {
            for (side in 0..1) {
                val x0 = if (side == 0) wall * 0.08f else w - wall * 0.92f
                oval.set(x0, y, x0 + wall * 0.84f, y + period * 0.86f)
                paint.style = Paint.Style.FILL
                paint.color = 0x55240408
                canvas.drawOval(oval, paint)
                paint.color = 0x66120204
                canvas.drawCircle(oval.centerX(), oval.centerY(), wall * 0.16f, paint)
            }
            y += period
        }

        // Proteínas del plasma
        paint.style = Paint.Style.FILL
        paint.color = 0x88E8A890.toInt()
        for (e in dots) canvas.drawCircle(e.x, e.y, e.r, paint)

        // Plaquetas
        paint.color = 0xFFE7B0D2.toInt()
        for (e in plt) {
            canvas.save(); canvas.rotate(Math.toDegrees(e.ang.toDouble()).toFloat(), e.x, e.y)
            oval.set(e.x - e.r * 1.4f, e.y - e.r * 0.8f, e.x + e.r * 1.4f, e.y + e.r * 0.8f)
            canvas.drawOval(oval, paint)
            canvas.restore()
        }

        // Glóbulos rojos: disco bicóncavo que voltea
        for (e in rbc) {
            val sx = max(0.35f, abs(cos(e.ang)))
            canvas.save(); canvas.rotate(e.phase * 20f, e.x, e.y)
            oval.set(e.x - e.r, e.y - e.r * sx, e.x + e.r, e.y + e.r * sx)
            paint.style = Paint.Style.FILL; paint.color = 0xFFC81E2A.toInt()
            canvas.drawOval(oval, paint)
            oval.set(e.x - e.r * 0.5f, e.y - e.r * sx * 0.5f, e.x + e.r * 0.5f, e.y + e.r * sx * 0.5f)
            paint.color = 0xCC8E1020.toInt()
            canvas.drawOval(oval, paint)
            oval.set(e.x - e.r, e.y - e.r * sx, e.x + e.r, e.y + e.r * sx)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.2f * d; paint.color = 0x88F0505A.toInt()
            canvas.drawOval(oval, paint)
            canvas.restore()
        }

        // Glóbulos blancos
        for (e in wbc) {
            path.reset()
            val n = 16
            for (i in 0..n) {
                val a = i.toFloat() / n * 6.2832f
                val rr = e.r * (1f + 0.07f * sin(time * 1.6f + i * 1.7f + e.phase))
                val px = e.x + cos(a) * rr
                val py = e.y + sin(a) * rr
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            paint.style = Paint.Style.FILL; paint.color = 0xF0E9DDCB.toInt()
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.5f * d; paint.color = 0xAAFFF5E4.toInt()
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.FILL; paint.color = 0xFFB58FC9.toInt()
            for (i in 0 until 7) {
                val a = e.phase * 3f + i * 0.9f + e.ang
                canvas.drawCircle(e.x + cos(a) * e.r * 0.62f, e.y + sin(a) * e.r * 0.62f, e.r * 0.06f, paint)
            }
            paint.color = 0xFF6D4C9F.toInt()
            for (i in 0 until 3) {
                val a = e.ang + i * 2.1f
                canvas.drawCircle(e.x + cos(a) * e.r * 0.33f, e.y + sin(a) * e.r * 0.33f, e.r * 0.3f, paint)
            }
        }
    }
}
