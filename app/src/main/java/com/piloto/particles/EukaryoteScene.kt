package com.piloto.particles

import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Célula eucariota animal: núcleo con nucléolo y poros, retículo rugoso y liso, aparato de Golgi,
 * mitocondrias, lisosomas, centrosoma, citoesqueleto, vesículas y ribosomas libres.
 * Toca un orgánulo para ver cómo funciona en detalle.
 */
class EukaryoteScene(private val d: Float) : Scene {

    private class Ribo(var x: Float, var y: Float) { var vx = 0f; var vy = 0f }
    private class Mito(val u: Float, val v: Float, val rot: Float, val ph: Float)

    private val kit = Kit(d)
    private val overlay = Overlay(kit)
    private val tap = TapDetector(12f * d)
    private var consumed = false

    private var w = 0f; private var h = 0f
    private var cx = 0f; private var cy = 0f; private var a = 0f; private var b = 0f
    private var time = 0f
    private var touching = false; private var tx = 0f; private var ty = 0f

    private var nx = 0f; private var ny = 0f; private var nr = 0f
    private var gx = 0f; private var gy = 0f
    private var sx = 0f; private var sy = 0f
    private var ex = 0f; private var ey = 0f
    private val mitos = listOf(
        Mito(-0.60f, -0.38f, 30f, 0f), Mito(0.62f, -0.50f, -25f, 1.5f), Mito(0.12f, 0.62f, 80f, 3f),
        Mito(-0.64f, 0.05f, -60f, 4.5f), Mito(-0.32f, 0.72f, 10f, 2f), Mito(0.42f, 0.74f, -35f, 5.5f), Mito(-0.42f, -0.68f, 50f, 3.5f)
    )
    private val lysos = listOf(Mito(0.60f, 0.55f, 0f, 0.5f), Mito(-0.12f, 0.80f, 0f, 2.5f), Mito(0.72f, -0.16f, 0f, 4f), Mito(0.36f, 0.36f, 0f, 1f), Mito(-0.55f, 0.55f, 0f, 3f), Mito(0.2f, -0.78f, 0f, 2f), Mito(0.0f, 0.5f, 0f, 5f))
    private val ribos = ArrayList<Ribo>()

    private var cytoPaint: Paint? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var heads = FloatArray(0)
    private var tails = FloatArray(0)
    private var proteins = FloatArray(0)

    override fun resize(width: Int, height: Int) {
        val first = w == 0f
        w = width.toFloat(); h = height.toFloat()
        cx = w / 2f; cy = h * 0.46f; a = w * 0.47f; b = h * 0.40f
        nx = cx - 0.05f * a; ny = cy - 0.15f * b; nr = 0.30f * a
        gx = cx + 0.52f * a; gy = cy + 0.18f * b
        sx = cx - 0.45f * a; sy = cy + 0.42f * b
        ex = cx + 0.36f * a; ey = cy - 0.46f * b
        cytoPaint = Paint().apply {
            shader = RadialGradient(cx, cy, b, intArrayOf(col(0xFF1F4258L), col(0xFF15303FL), col(0xFF0B1C2AL)),
                floatArrayOf(0f, 0.65f, 1f), Shader.TileMode.CLAMP)
        }
        val n = 190
        heads = FloatArray(n * 4); tails = FloatArray(n * 4); proteins = FloatArray(0)
        for (i in 0 until n) {
            val t = i.toFloat() / n * TAU
            val ox = cx + (a + 5f * d) * cos(t); val oy = cy + (b + 5f * d) * sin(t)
            val ix = cx + (a - 5f * d) * cos(t); val iy = cy + (b - 5f * d) * sin(t)
            heads[i * 4] = ox; heads[i * 4 + 1] = oy; heads[i * 4 + 2] = ix; heads[i * 4 + 3] = iy
            tails[i * 4] = ox; tails[i * 4 + 1] = oy; tails[i * 4 + 2] = ix; tails[i * 4 + 3] = iy
        }
        if (first) repeat(320) {
            val u = Random.nextFloat() * 1.8f - 0.9f; val v = Random.nextFloat() * 1.8f - 0.9f
            if (u * u + v * v < 0.8f) ribos.add(Ribo(cx + u * a, cy + v * b))
        }
    }

    private var scopeOn = true
    override fun configure(prefs: SharedPreferences) { scopeOn = prefs.getBoolean("scope", true) }

    override fun touchDown(px: Float, py: Float) {
        if (overlay.isOpen) { overlay.close(); consumed = true; return }
        consumed = false; touching = true; tx = px; ty = py; tap.down(px, py)
    }
    override fun touchMove(px: Float, py: Float) { if (consumed) return; tx = px; ty = py; tap.move(px, py) }
    override fun touchUp() {
        touching = false
        if (!consumed && tap.up()) hit(tx, ty)?.let { overlay.open(it) }
    }

    private fun mx(m: Mito) = cx + a * m.u + sin(time * 0.25f + m.ph) * 7f * d
    private fun my(m: Mito) = cy + b * m.v + cos(time * 0.2f + m.ph) * 7f * d

    private fun near(x: Float, y: Float, px: Float, py: Float, r: Float): Boolean {
        val dx = x - px; val dy = y - py; return dx * dx + dy * dy < r * r
    }

    private fun hit(x: Float, y: Float): Info? {
        for (r in ribos) if (near(x, y, r.x, r.y, 14f * d)) return CellDetails.ribosome(false)
        for (m in lysos) if (near(x, y, mx(m), my(m), 20f * d)) return CellDetails.lysosome
        if (near(x, y, ex, ey, 36f * d)) return CellDetails.centrosome
        for (m in mitos) if (near(x, y, mx(m), my(m), 46f * d)) return CellDetails.mitochondrion
        if (near(x, y, gx, gy, 46f * d)) return CellDetails.golgi
        if (near(x, y, sx, sy, 52f * d)) return CellDetails.ser
        val dn = sqrt((x - nx) * (x - nx) + (y - ny) * (y - ny))
        if (dn < nr) return CellDetails.nucleus
        if (dn < nr + 44f * d) return CellDetails.rer
        val u = (x - cx) / a; val v = (y - cy) / b
        val e = (sqrt(u * u + v * v) - 1f) * min(a, b)
        if (e in (-16f * d)..(16f * d)) return CellDetails.membrane(false)
        return null
    }

    override fun update(dt: Float) {
        overlay.update(dt)
        if (w == 0f) return
        val t = min(dt, 0.05f)
        time += t
        val rad = 100f * d
        for (r in ribos) {
            r.vx += (Random.nextFloat() - 0.5f) * 800f * d * t
            r.vy += (Random.nextFloat() - 0.5f) * 800f * d * t
            if (touching && !overlay.isOpen) {
                val dx = r.x - tx; val dy = r.y - ty
                val dist = max(sqrt(dx * dx + dy * dy), 1f)
                if (dist < rad) { val p = (1f - dist / rad) * 1500f * d; r.vx += dx / dist * p * t; r.vy += dy / dist * p * t }
            }
            val damp = max(0f, 1f - 3f * t)
            r.vx *= damp; r.vy *= damp
            r.x += r.vx * t; r.y += r.vy * t
            val u = (r.x - cx) / (a * 0.93f); val v = (r.y - cy) / (b * 0.93f)
            if (u * u + v * v > 1f) { r.x = cx + (r.x - cx) * 0.98f; r.y = cy + (r.y - cy) * 0.98f; r.vx = -r.vx * 0.5f; r.vy = -r.vy * 0.5f }
            val dx = r.x - nx; val dy = r.y - ny
            val dn = sqrt(dx * dx + dy * dy)
            if (dn < nr + 48f * d) { r.x = nx + dx / dn * (nr + 49f * d); r.y = ny + dy / dn * (nr + 49f * d) }
        }
    }

    override fun draw(canvas: Canvas) {
        kit.cv = canvas
        canvas.drawColor(col(0xFF000000L))
        Sprites.bg(canvas, "bg_euk", w, h)
        with(kit) {
            // Microtúbulos desde el centrosoma
            stroke(col(0x22FFC24DL), 1.2f * d)
            for (i in 0 until 22) {
                val an = i * TAU / 22f + 0.3f
                val len = (120f + 90f * (0.5f + 0.5f * sin(time * 0.5f + i * 1.7f))) * d
                line(ex, ey, ex + cos(an) * len, ey + sin(an) * len)
            }
            // Retículo rugoso alrededor del núcleo, liso y Golgi
            Sprites.draw(canvas, "er", nx - nr * 0.55f, ny + nr * 0.95f, nr * 2.5f, 12f)
            Sprites.draw(canvas, "er", nx + nr * 0.95f, ny + nr * 0.6f, nr * 2.2f, -28f)
            Sprites.draw(canvas, "er", nx - nr * 0.95f, ny - nr * 0.2f, nr * 1.9f, 78f)
            Sprites.draw(canvas, "ser", sx, sy, 150f * d, sin(time * 0.2f) * 3f)
            Sprites.draw(canvas, "golgi", gx, gy, 135f * d, -12f)
            Sprites.draw(canvas, "nucleus", nx, ny, nr * 2.35f, sin(time * 0.1f) * 3f)
            for ((i, m) in mitos.withIndex()) Sprites.draw(canvas, if (i % 2 == 0) "mito1" else "mito2", mx(m), my(m), 108f * d, m.rot + sin(time * 0.3f + m.ph) * 6f)
            for (m in lysos) Sprites.draw(canvas, "lyso", mx(m), my(m), 32f * d)
            Sprites.draw(canvas, "centriole", ex, ey, 74f * d, sin(time * 0.2f) * 8f)
            // Vesículas del Golgi hacia la membrana
            for (i in 0 until 9) {
                val f = ((time * 0.12f + i / 9f) % 1f)
                val vx = gx + (a * 0.96f - (gx - cx)) * f * 0.9f
                val vy = gy + (b * 0.35f) * f
                Sprites.draw(canvas, "lyso", vx, vy, 14f * d * (1f - f * 0.4f))
            }
            for (r in ribos) Sprites.draw(canvas, "ribo", r.x, r.y, 10f * d)
            if (!overlay.isOpen && time < 12f) text("Toca un orgánulo para verlo en detalle", w / 2, h * 0.93f, 13f * d, col(0x88FFFFFFL))
        }
        if (scopeOn) Sprites.scope(canvas, w, h, time, d)
        overlay.draw(canvas, w, h)
    }
}
