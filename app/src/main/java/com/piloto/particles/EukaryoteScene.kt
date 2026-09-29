package com.piloto.particles

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
        Mito(-0.64f, 0.05f, -60f, 4.5f), Mito(-0.32f, 0.72f, 10f, 2f)
    )
    private val lysos = listOf(Mito(0.60f, 0.55f, 0f, 0.5f), Mito(-0.12f, 0.80f, 0f, 2.5f), Mito(0.72f, -0.16f, 0f, 4f), Mito(0.36f, 0.36f, 0f, 1f))
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
        if (first) repeat(170) {
            val u = Random.nextFloat() * 1.8f - 0.9f; val v = Random.nextFloat() * 1.8f - 0.9f
            if (u * u + v * v < 0.8f) ribos.add(Ribo(cx + u * a, cy + v * b))
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

    private inline fun dp(x: Float, y: Float, rot: Float = 0f, block: () -> Unit) {
        kit.cv.save(); kit.cv.translate(x, y); kit.cv.rotate(rot); kit.cv.scale(d, d)
        block(); kit.cv.restore()
    }

    override fun draw(canvas: Canvas) {
        kit.cv = canvas
        val cyto = cytoPaint ?: return
        canvas.drawColor(col(0xFF03060BL))
        with(kit) {
            fill(col(0x22FFC24DL)); oval(cx - a - 12f * d, cy - b - 12f * d, cx + a + 12f * d, cy + b + 12f * d)
            fill(col(0xFF15303FL)); oval(cx - a, cy - b, cx + a, cy + b)
            canvas.drawOval(cx - a, cy - b, cx + a, cy + b, cyto)

            // Microtúbulos desde el centrosoma
            stroke(col(0x22FFC24DL), 1.4f * d)
            for (i in 0 until 12) {
                val an = i * TAU / 12f + 0.3f
                val len = (120f + 90f * (0.5f + 0.5f * sin(time * 0.5f + i * 1.7f))) * d
                line(ex, ey, ex + cos(an) * len, ey + sin(an) * len)
            }

            // Membrana plasmática: doble capa de fosfolípidos
            p.style = Paint.Style.STROKE; p.strokeWidth = 1.2f * d; p.color = col(0xFFC98F3AL)
            canvas.drawLines(tails, p)
            p.strokeWidth = 4.6f * d; p.color = col(0xFFF2C46BL)
            canvas.drawPoints(heads, 0, heads.size, p)
            fill(col(0xFF3FA37AL))
            for (i in 0 until heads.size / 4 step 14) circle(heads[i * 4 + 2] * 0.5f + heads[i * 4] * 0.5f, heads[i * 4 + 3] * 0.5f + heads[i * 4 + 1] * 0.5f, 4.2f * d)
            stroke(col(0xFF8FD0FFL), 1.3f * d)
            for (i in 0 until heads.size / 4 step 9) line(heads[i * 4], heads[i * 4 + 1], heads[i * 4] + (heads[i * 4] - cx) / a * 9f * d, heads[i * 4 + 1] + (heads[i * 4 + 1] - cy) / b * 9f * d)

            // Retículo liso
            dp(sx, sy) {
                val c = arrayOf(floatArrayOf(-46f, 12f, -20f, -42f, 10f, 42f, 46f, -12f), floatArrayOf(-40f, -30f, -10f, 10f, 20f, -34f, 44f, 28f), floatArrayOf(-30f, 40f, 0f, 20f, 26f, 46f, 44f, 4f))
                for (cc in c) { path.reset(); path.moveTo(cc[0], cc[1]); path.cubicTo(cc[2], cc[3], cc[4], cc[5], cc[6], cc[7])
                    stroke(col(0xFF4FB0C0L), 8f); drawPath(); stroke(col(0xFF17434DL), 4f); drawPath() }
            }

            // Núcleo y retículo rugoso
            dp(nx, ny) {
                val r = nr / d
                for (k in 0..3) {
                    val rr = r + 9f + k * 9f
                    curve(70) { f, o -> val an = -1.2f + k * 0.35f + f * 4.4f; val q = rr + 2f * sin(an * 8f + k + time * 0.6f); o[0] = cos(an) * q; o[1] = sin(an) * q }
                    stroke(col(0xFF3E9AA8L), 5f); drawPath(); stroke(col(0xFF0F2A30L), 2f); drawPath()
                    fill(col(0xFFC79CFFL))
                    for (i in 0 until 36) { val an = -1.2f + k * 0.35f + i / 36f * 4.4f; circle(cos(an) * (rr + 4.5f), sin(an) * (rr + 4.5f), 1.3f) }
                }
                fill(col(0xFF161C5CL)); circle(0f, 0f, r)
                stroke(col(0xFF8F9BFFL), 2.4f); circle(0f, 0f, r)
                stroke(col(0xFF5E6BE0L), 1.4f); circle(0f, 0f, r - 4f)
                fill(col(0xFFFFD27FL))
                for (i in 0 until 18) { val an = i * TAU / 18f; circle(cos(an) * (r - 2f), sin(an) * (r - 2f), 2.2f) }
                stroke(col(0x88A6B0FFL), 1.4f)
                for (s in 0..8) {
                    curve(30) { f, o -> val an = s * 0.7f + f * 5f + sin(time * 0.2f + s) * 0.3f; val q = r * 0.78f * (0.25f + 0.75f * f); o[0] = cos(an) * q; o[1] = sin(an) * q }
                    drawPath()
                }
                fill(col(0xFF5A3A9AL)); circle(r * 0.3f, r * 0.2f, r * 0.2f)
                fill(col(0xFF8A62C8L)); circle(r * 0.3f + 3f, r * 0.2f - 2f, 2f)
            }

            // Golgi
            dp(gx, gy, -20f) {
                val colors = intArrayOf(col(0xFF7FD69AL), col(0xFF9AD98AL), col(0xFFC3D77AL), col(0xFFE3B95AL), col(0xFFE8935AL))
                for (i in 0..4) {
                    val y = -18f + i * 9f
                    path.reset(); path.moveTo(-30f + i, y + 5f); path.quadTo(0f, y - 12f, 30f - i, y + 5f)
                    stroke(colors[i], 4f); drawPath()
                }
                fill(col(0xFFE8935AL)); circle(34f + sin(time * 2f) * 2f, 24f, 4f); circle(-36f, -8f + cos(time * 2f), 3f)
            }

            // Mitocondrias
            for (m in mitos) dp(mx(m), my(m), m.rot) {
                fill(col(0xFF7A2E1EL)); rrect(-40f, -18f, 40f, 18f, 18f)
                stroke(col(0xFFE89A5AL), 2.4f); rrect(-40f, -18f, 40f, 18f, 18f)
                stroke(col(0xFFFFC98AL), 3f)
                for (i in 0..4) { val x = -28f + i * 14f; if (i % 2 == 0) line(x, -16f, x, 3f) else line(x, 16f, x, -3f) }
                fill(col(0xFFFFD84AL)); circle(-30f + ((time * 15f + m.ph * 9f) % 60f), sin(time * 3f + m.ph) * 6f, 1.8f)
            }

            // Lisosomas
            for (m in lysos) dp(mx(m), my(m)) {
                fill(col(0xFF5C3A12L)); circle(0f, 0f, 9f)
                stroke(col(0xFFE0A04AL), 1.6f); circle(0f, 0f, 9f)
                fill(col(0xFFFF9D3AL))
                for (i in 0 until 4) circle(cos(time + i * 1.6f) * 4f, sin(time * 1.2f + i * 1.6f) * 4f, 1.4f)
            }

            // Centrosoma
            dp(ex, ey) {
                fill(col(0xFFC7D2FFL)); rrect(-9f, -3f, 9f, 3f, 3f); rrect(-3f, 5f, 3f, 23f, 3f)
                stroke(col(0xFF8FA8FFL), 1f); circle(0f, 0f, 14f)
            }

            // Vesículas del Golgi a la membrana
            for (i in 0 until 5) {
                val f = ((time * 0.12f + i / 5f) % 1f)
                val tx2 = gx + (a * 0.96f - (gx - cx)) * f * 0.9f
                val ty2 = gy + (b * 0.35f) * f
                fill(col(0xFF7FD8E6L)); circle(tx2, ty2, 3.4f * d * (1f - f * 0.5f))
                fill(col(0xFFFFD84AL)); circle(tx2, ty2, 1.5f * d)
            }

            // Ribosomas libres
            fill(col(0xFFB58CE8L))
            for (r in ribos) circle(r.x, r.y, 1.9f * d)

            if (!overlay.isOpen && time < 12f) text("Toca un orgánulo para verlo en detalle", w / 2, h * 0.93f, 13f * d, col(0x88FFFFFFL))
        }
        overlay.draw(canvas, w, h)
    }
}
