package com.piloto.particles

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Interior de una bacteria (célula procariota) en funcionamiento:
 * cápsula, pared, membrana, nucleoide (ADN) con plásmidos, ribosomas que traducen,
 * gránulos de reserva, bombas de protones + ATP sintasas que producen ATP,
 * pili y un flagelo con su motor rotatorio. Al tocar, los ribosomas se apartan.
 */
class ProkaryoteScene(private val d: Float) : Scene {

    private class Ribo(var x: Float, var y: Float, val r: Float) {
        var vx = 0f; var vy = 0f
        var glow = 0f // >0 mientras "traduce"
    }
    private class Motor(val x: Float, val y: Float) {
        var pulse = 0f; var ang = Random.nextFloat() * 6.28f
    }
    private class Proton(var x: Float, var y: Float, val target: Motor)
    private class Atp(var x: Float, var y: Float, val vx: Float, val vy: Float, var life: Float)
    private class Granule(val u: Float, val v: Float, val r: Float, val ph: Float)

    private val ribos = ArrayList<Ribo>()
    private val motors = ArrayList<Motor>()
    private val protons = ArrayList<Proton>()
    private val atps = ArrayList<Atp>()
    private val granules = ArrayList<Granule>()

    private var w = 0f; private var h = 0f
    private var cx = 0f; private var cy = 0f; private var a = 0f; private var b = 0f
    private var time = 0f
    private var protonTimer = 0f
    private var touching = false; private var tx = 0f; private var ty = 0f
    private var cytoPaint: Paint? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val kit = Kit(d)
    private val overlay = Overlay(kit)
    private val tap = TapDetector(12f * d)
    private var consumed = false
    private val rect = RectF()
    private val path = Path()

    override fun resize(width: Int, height: Int) {
        val first = w == 0f
        w = width.toFloat(); h = height.toFloat()
        cx = w / 2f; cy = h * 0.44f; a = w * 0.44f; b = h * 0.36f
        cytoPaint = Paint().apply {
            shader = RadialGradient(cx, cy, b, intArrayOf(0xFF1E4C4A.toInt(), 0xFF123A3C.toInt(), 0xFF0A252A.toInt()),
                floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        }
        if (first) {
            repeat(230) {
                val (px, py) = randomInside(0.9f)
                ribos.add(Ribo(px, py, (1.3f + Random.nextFloat() * 0.9f) * d))
            }
            repeat(5) {
                granules.add(Granule(Random.nextFloat() * 1.4f - 0.7f, Random.nextFloat() * 1.4f - 0.7f,
                    (6f + Random.nextFloat() * 4f) * d, Random.nextFloat() * 6.28f))
            }
        }
        motors.clear()
        val n = 12
        for (i in 0 until n) {
            val t = i.toFloat() / n * 2f * PI.toFloat()
            if (sin(t) > 0.8f) continue // hueco para el flagelo
            val k = 0.955f
            motors.add(Motor(cx + a * k * cos(t), cy + b * k * sin(t)))
        }
    }

    private fun randomInside(scale: Float): Pair<Float, Float> {
        while (true) {
            val u = Random.nextFloat() * 2f - 1f
            val v = Random.nextFloat() * 2f - 1f
            if (u * u + v * v < 1f) return Pair(cx + u * a * scale, cy + v * b * scale)
        }
    }

    override fun touchDown(px: Float, py: Float) {
        if (overlay.isOpen) { overlay.close(); consumed = true; return }
        consumed = false; touching = true; tx = px; ty = py; tap.down(px, py)
    }
    override fun touchMove(px: Float, py: Float) { if (consumed) return; tx = px; ty = py; tap.move(px, py) }
    override fun touchUp() {
        touching = false
        if (!consumed && tap.up()) hit(tx, ty)?.let { overlay.open(it) }
    }

    private fun plasmidPos(k: Int, out: FloatArray) {
        val t = time * (0.16f + 0.07f * k) + k * 3.1f
        out[0] = cx + a * 0.62f * cos(t) * (if (k == 0) 1f else -1f)
        out[1] = cy + b * 0.55f * sin(t * 1.3f + k)
    }

    /** Qué orgánulo hay bajo el dedo (los más pequeños tienen prioridad). */
    private fun hit(x: Float, y: Float): Info? {
        for (r in ribos) { val dx = x - r.x; val dy = y - r.y; if (dx * dx + dy * dy < 196f * d * d) return CellDetails.ribosome(true) }
        val pp = FloatArray(2)
        for (k in 0..1) { plasmidPos(k, pp); val dx = x - pp[0]; val dy = y - pp[1]; if (dx * dx + dy * dy < 400f * d * d) return CellDetails.plasmid }
        for (g in granules) {
            val gx = cx + a * (g.u + 0.05f * sin(time * 0.3f + g.ph)); val gy = cy + b * (g.v + 0.05f * cos(time * 0.25f + g.ph))
            val dx = x - gx; val dy = y - gy; val rr = g.r + 10f * d
            if (dx * dx + dy * dy < rr * rr) return CellDetails.granule
        }
        for (m in motors) { val dx = x - m.x; val dy = y - m.y; if (dx * dx + dy * dy < 400f * d * d) return CellDetails.respiration }
        val by = cy + b
        if (y > by - 4f * d && kotlin.math.abs(x - cx) < 45f * d) return CellDetails.flagellum
        val u = (x - cx) / a; val v = (y - cy) / b
        val e = (sqrt(u * u + v * v) - 1f) * min(a, b)
        if (e in (-14f * d)..(-3f * d)) return CellDetails.respiration
        if (e in (-3f * d)..(16f * d)) return CellDetails.wall
        if (e in (16f * d)..(46f * d)) return CellDetails.pili
        val nu = (x - cx) / (a * 0.5f); val nv = (y - cy + b * 0.04f) / (b * 0.33f)
        if (nu * nu + nv * nv < 1f) return CellDetails.nucleoid
        return null
    }

    override fun update(dt: Float) {
        overlay.update(dt)
        if (w == 0f) return
        val t = min(dt, 0.05f)
        time += t
        val rad = 110f * d

        for (r in ribos) {
            r.vx += (Random.nextFloat() - 0.5f) * 900f * d * t
            r.vy += (Random.nextFloat() - 0.5f) * 900f * d * t
            if (touching) {
                val dx = r.x - tx; val dy = r.y - ty
                val dist = max(sqrt(dx * dx + dy * dy), 1f)
                if (dist < rad) {
                    val p = (1f - dist / rad) * 1600f * d
                    r.vx += dx / dist * p * t; r.vy += dy / dist * p * t
                }
            }
            val damp = max(0f, 1f - 3f * t)
            r.vx *= damp; r.vy *= damp
            r.x += r.vx * t; r.y += r.vy * t
            val u = (r.x - cx) / (a * 0.92f); val v = (r.y - cy) / (b * 0.92f)
            if (u * u + v * v > 1f) { // rebote suave contra la membrana
                r.x = cx + (r.x - cx) * 0.98f; r.y = cy + (r.y - cy) * 0.98f
                r.vx = -r.vx * 0.5f; r.vy = -r.vy * 0.5f
            }
            if (r.glow > 0f) r.glow -= t
            else if (Random.nextFloat() < 0.15f * t) r.glow = 0.6f // empieza a traducir
        }

        // Los protones se bombean hacia las ATP sintasas
        protonTimer += t
        while (protonTimer > 0.04f && protons.size < 70 && motors.isNotEmpty()) {
            protonTimer -= 0.04f
            val (px, py) = randomInside(0.85f)
            protons.add(Proton(px, py, motors.random()))
        }
        protonTimer = min(protonTimer, 0.1f)
        val it = protons.iterator()
        while (it.hasNext()) {
            val p = it.next()
            val dx = p.target.x - p.x; val dy = p.target.y - p.y
            val dist = max(sqrt(dx * dx + dy * dy), 0.01f)
            val sp = 90f * d * t
            if (dist < sp + 3f * d) {
                p.target.pulse = 1f
                val ang = atan2(cy - p.target.y, cx - p.target.x) + (Random.nextFloat() - 0.5f) * 1.6f
                val s = (35f + Random.nextFloat() * 25f) * d
                atps.add(Atp(p.target.x, p.target.y, cos(ang) * s, sin(ang) * s, 2.5f))
                it.remove()
            } else {
                p.x += dx / dist * sp + (Random.nextFloat() - 0.5f) * 2f * d
                p.y += dy / dist * sp + (Random.nextFloat() - 0.5f) * 2f * d
            }
        }
        val ai = atps.iterator()
        while (ai.hasNext()) {
            val p = ai.next()
            p.x += p.vx * t; p.y += p.vy * t; p.life -= t
            if (p.life <= 0f) ai.remove()
        }
        for (m in motors) {
            m.pulse = max(0f, m.pulse - 2.5f * t)
            m.ang += (2f + m.pulse * 14f) * t
        }
    }

    private fun stroke(color: Int, width: Float) {
        paint.style = Paint.Style.STROKE; paint.color = color; paint.strokeWidth = width
    }

    private fun fill(color: Int) {
        paint.style = Paint.Style.FILL; paint.color = color
    }

    private fun ring(inset: Float, grow: Float = 0f) = rect.set(cx - a - grow + inset, cy - b - grow + inset, cx + a + grow - inset, cy + b + grow - inset)

    override fun draw(canvas: Canvas) {
        canvas.drawColor(0xFF02050A.toInt())
        val cyto = cytoPaint ?: return

        // Cápsula
        ring(0f, 16f * d); fill(0x331F6A48); canvas.drawOval(rect, paint)

        // Flagelo: filamento ondulante y motor basal
        val by = cy + b
        path.reset()
        val steps = 40
        val len = h - by
        for (i in 0..steps) {
            val f = i.toFloat() / steps
            val px = cx + 22f * d * f * sin(f * 9f - time * 7f)
            val py = by + 6f * d + f * len
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        stroke(0xFF8FD0A8.toInt(), 2.6f * d); canvas.drawPath(path, paint)

        // Pili
        stroke(0x996FAE7A.toInt(), 1.2f * d)
        for (i in 0 until 28) {
            val t = i / 28f * 2f * PI.toFloat()
            if (sin(t) > 0.85f) continue
            val nx = cos(t) / a; val ny = sin(t) / b
            val nl = sqrt(nx * nx + ny * ny)
            val sx = cx + a * cos(t); val sy = cy + b * sin(t)
            val l = (10f + (i % 3) * 4f) * d
            val sway = sin(time * 2f + i) * 2f * d
            canvas.drawLine(sx, sy, sx + nx / nl * l + sway, sy + ny / nl * l, paint)
        }

        // Citoplasma, pared celular y membrana plasmática
        ring(7f * d); canvas.drawOval(rect, cyto)
        ring(0f); stroke(0xFF4F8F6A.toInt(), 6f * d); canvas.drawOval(rect, paint)
        ring(7f * d); stroke(0xFFE0B45A.toInt(), 3f * d); canvas.drawOval(rect, paint)

        // Nucleoide: ADN circular superenrollado, se retuerce lentamente
        for (strand in 0..1) {
            path.reset()
            val n = 150
            val ph = strand * 1.7f
            for (i in 0..n) {
                val t = i.toFloat() / n * 2f * PI.toFloat()
                val rr = 1f + 0.18f * sin(5f * t + time * 0.5f + ph) + 0.12f * sin(9f * t - time * 0.7f + ph) +
                    0.08f * sin(14f * t + time * 0.9f)
                val px = cx + a * 0.42f * rr * cos(t) + a * 0.08f * sin(3f * t + time * 0.3f + ph)
                val py = cy - b * 0.04f + b * 0.26f * rr * sin(t) + b * 0.03f * cos(4f * t + time * 0.4f)
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            stroke(if (strand == 0) 0x40FFCF66 else 0x30FFB347, 5f * d); canvas.drawPath(path, paint)
            stroke(if (strand == 0) 0xFFFFE08A.toInt() else 0xFFFFB347.toInt(), 1.4f * d); canvas.drawPath(path, paint)
        }

        // Plásmidos
        for (k in 0..1) {
            val t = time * (0.16f + 0.07f * k) + k * 3.1f
            val px = cx + a * 0.62f * cos(t) * (if (k == 0) 1f else -1f)
            val py = cy + b * 0.55f * sin(t * 1.3f + k)
            stroke(0xFFFF8FA3.toInt(), 1.6f * d); canvas.drawCircle(px, py, 8f * d, paint)
            fill(0xFFFF8FA3.toInt()); canvas.drawCircle(px + 8f * d, py, 1.8f * d, paint)
        }

        // Gránulos de reserva
        for (g in granules) {
            val px = cx + a * (g.u + 0.05f * sin(time * 0.3f + g.ph))
            val py = cy + b * (g.v + 0.05f * cos(time * 0.25f + g.ph))
            fill(0xB8E8EEF0.toInt()); canvas.drawCircle(px, py, g.r, paint)
            fill(0xCCFFFFFF.toInt()); canvas.drawCircle(px - g.r * 0.3f, py - g.r * 0.3f, g.r * 0.3f, paint)
        }

        // Ribosomas (brillan cuando traducen)
        for (r in ribos) {
            if (r.glow > 0f) {
                fill(0x55FF9CE0); canvas.drawCircle(r.x, r.y, r.r * 3.2f * (0.5f + r.glow), paint)
                fill(0xFFFFD6F5.toInt())
            } else fill(0xFFB58CE8.toInt())
            canvas.drawCircle(r.x, r.y, r.r, paint)
        }

        // ATP sintasas en la membrana
        val mr = 4.6f * d
        for (m in motors) {
            fill(0xFFE0B45A.toInt()); canvas.drawCircle(m.x, m.y, mr + m.pulse * 1.5f * d, paint)
            stroke(0xFFFFF2C0.toInt(), 1.4f * d)
            canvas.drawLine(m.x, m.y, m.x + cos(m.ang) * mr * 1.5f, m.y + sin(m.ang) * mr * 1.5f, paint)
        }

        // Protones (H+) y moléculas de ATP
        fill(0xFF58E6FF.toInt())
        for (p in protons) canvas.drawCircle(p.x, p.y, 1.7f * d, paint)
        for (p in atps) {
            val al = (min(1f, p.life) * 255).toInt()
            paint.style = Paint.Style.FILL
            paint.color = (al / 3 shl 24) or 0xFFD84A; canvas.drawCircle(p.x, p.y, 5.5f * d, paint)
            paint.color = (al shl 24) or 0xFFD84A; canvas.drawCircle(p.x, p.y, 2.4f * d, paint)
        }

        // Motor basal del flagelo (rotor)
        fill(0xFFE0B45A.toInt()); canvas.drawCircle(cx, by, 7f * d, paint)
        stroke(0xFFFFF2C0.toInt(), 1.8f * d)
        for (i in 0..2) {
            val an = time * 9f + i * 2.094f
            canvas.drawLine(cx, by, cx + cos(an) * 7f * d, by + sin(an) * 7f * d, paint)
        }

        if (!overlay.isOpen && time < 12f) {
            kit.cv = canvas
            kit.text("Toca un orgánulo para verlo en detalle", w / 2, h * 0.93f, 13f * d, col(0x88FFFFFFL))
        }
        overlay.draw(canvas, w, h)
    }
}
