package com.piloto.particles

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Célula vegetal: pared celular con plasmodesmos, gran vacuola central, cloroplastos y mitocondrias que
 * circulan por el citoplasma (corriente citoplasmática), núcleo, retículos, Golgi y ribosomas.
 * Toca un orgánulo para ver cómo funciona.
 */
class PlantScene(private val d: Float) : Scene {

    private class Ribo(var x: Float, var y: Float) { var vx = 0f; var vy = 0f }
    private class Mover(var u: Float, val speed: Float, val lat: Float, val ph: Float)

    private val kit = Kit(d)
    private val overlay = Overlay(kit)
    private val tap = TapDetector(12f * d)
    private var consumed = false
    private var w = 0f; private var h = 0f
    private var time = 0f
    private var touching = false; private var tx = 0f; private var ty = 0f

    private var cl = 0f; private var ct = 0f; private var cr = 0f; private var cb = 0f      // célula
    private var vl = 0f; private var vt = 0f; private var vr = 0f; private var vb = 0f      // vacuola
    private var nx = 0f; private var ny = 0f; private var nr = 0f
    private var gx = 0f; private var gy = 0f; private var sx = 0f; private var sy = 0f
    private val loop = Path()
    private val pm = PathMeasure()
    private val pos = FloatArray(2); private val tan = FloatArray(2)
    private var loopLen = 1f

    private val chloros = ArrayList<Mover>()
    private val mitos = ArrayList<Mover>()
    private val ribos = ArrayList<Ribo>()
    private var cytoPaint: Paint? = null
    private var wallTicks = FloatArray(0)
    private var pd = FloatArray(0)

    override fun resize(width: Int, height: Int) {
        val first = w == 0f
        w = width.toFloat(); h = height.toFloat()
        cl = 0.04f * w; cr = 0.96f * w; ct = 0.06f * h; cb = 0.86f * h
        val g = 0.15f * w
        vl = cl + g; vr = cr - g; vt = ct + 0.46f * w; vb = cb - g
        nx = cl + 0.34f * w; ny = ct + 0.22f * w; nr = 0.13f * w
        gx = cr - 0.26f * w; gy = ct + 0.2f * w; sx = cr - 0.22f * w; sy = ct + 0.34f * w
        loop.reset()
        loop.addRoundRect(cl + g / 2f, vt - g / 2f, cr - g / 2f, cb - g / 2f, 60f * d, 60f * d, Path.Direction.CW)
        pm.setPath(loop, true); loopLen = pm.length
        cytoPaint = Paint().apply {
            shader = RadialGradient(w / 2f, h / 2f, h * 0.5f, intArrayOf(col(0xFF2B5E44L), col(0xFF1F4A36L), col(0xFF163A2AL)), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        }
        // texturas de la pared y plasmodesmos
        val ticks = ArrayList<Float>()
        val per = 2f * ((cr - cl) + (cb - ct))
        var s = 0f
        while (s < per) {
            val q = pointOnRect(s); ticks.add(q[0]); ticks.add(q[1]); ticks.add(q[0] + q[2] * 9f * d); ticks.add(q[1] + q[3] * 9f * d)
            s += 11f * d
        }
        wallTicks = ticks.toFloatArray()
        val pl = ArrayList<Float>()
        for (i in 0 until 9) { val q = pointOnRect(per * (i + 0.5f) / 9f); pl.add(q[0]); pl.add(q[1]); pl.add(q[4]); pl.add(q[5]) }
        pd = pl.toFloatArray()
        if (first) {
            repeat(9) { chloros.add(Mover(Random.nextFloat(), 0.010f + Random.nextFloat() * 0.006f, (Random.nextFloat() - 0.5f) * 0.08f * w, Random.nextFloat() * 6f)) }
            repeat(6) { mitos.add(Mover(Random.nextFloat(), 0.014f + Random.nextFloat() * 0.006f, (Random.nextFloat() - 0.5f) * 0.08f * w, Random.nextFloat() * 6f)) }
            repeat(150) {
                val x = cl + 12f * d + Random.nextFloat() * (cr - cl - 24f * d); val y = ct + 12f * d + Random.nextFloat() * (cb - ct - 24f * d)
                if (!inVacuole(x, y) && hypot2(x - nx, y - ny) > nr + 40f * d) ribos.add(Ribo(x, y))
            }
        }
    }

    private fun hypot2(dx: Float, dy: Float) = sqrt(dx * dx + dy * dy)
    private fun inVacuole(x: Float, y: Float) = x > vl && x < vr && y > vt && y < vb

    /** Punto sobre el perímetro de la pared: [x, y, tx, ty, nx, ny] (t = tangente hacia dentro del lado, n = normal). */
    private fun pointOnRect(s0: Float): FloatArray {
        val wd = cr - cl; val ht = cb - ct
        var s = s0
        return if (s < wd) floatArrayOf(cl + s, ct, 1f, 0f, 0f, 1f)
        else { s -= wd; if (s < ht) floatArrayOf(cr, ct + s, 0f, 1f, -1f, 0f)
        else { s -= ht; if (s < wd) floatArrayOf(cr - s, cb, -1f, 0f, 0f, -1f)
        else { s -= wd; floatArrayOf(cl, cb - s, 0f, -1f, 1f, 0f) } } }
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

    private fun place(m: Mover, out: FloatArray) {
        val u = ((m.u % 1f) + 1f) % 1f
        pm.getPosTan(u * loopLen, pos, tan)
        out[0] = pos[0] - tan[1] * m.lat; out[1] = pos[1] + tan[0] * m.lat
        out[2] = tan[0]; out[3] = tan[1]
    }

    private fun near(x: Float, y: Float, px: Float, py: Float, r: Float): Boolean { val dx = x - px; val dy = y - py; return dx * dx + dy * dy < r * r }

    private fun hit(x: Float, y: Float): Info? {
        val o = FloatArray(4)
        for (r in ribos) if (near(x, y, r.x, r.y, 14f * d)) return CellDetails.ribosome(false)
        for (m in chloros) { place(m, o); if (near(x, y, o[0], o[1], 30f * d)) return CellDetails.chloroplast }
        for (m in mitos) { place(m, o); if (near(x, y, o[0], o[1], 26f * d)) return CellDetails.mitochondrion }
        if (near(x, y, gx, gy, 44f * d)) return CellDetails.golgi
        if (near(x, y, sx, sy, 42f * d)) return CellDetails.ser
        val dn = hypot2(x - nx, y - ny)
        if (dn < nr) return CellDetails.nucleus
        if (dn < nr + 40f * d) return CellDetails.rer
        val edge = min(min(x - cl, cr - x), min(y - ct, cb - y))
        if (edge in -8f * d..18f * d) return CellDetails.plantWall
        if (edge in 18f * d..32f * d) return CellDetails.membrane(false)
        if (inVacuole(x, y)) return CellDetails.vacuole
        return null
    }

    override fun update(dt: Float) {
        overlay.update(dt)
        if (w == 0f) return
        val t = min(dt, 0.05f)
        time += t
        for (m in chloros) m.u += m.speed * t
        for (m in mitos) m.u += m.speed * t
        val rad = 100f * d
        for (r in ribos) {
            r.vx += (Random.nextFloat() - 0.5f) * 800f * d * t; r.vy += (Random.nextFloat() - 0.5f) * 800f * d * t
            if (touching && !overlay.isOpen) {
                val dx = r.x - tx; val dy = r.y - ty; val dist = max(hypot2(dx, dy), 1f)
                if (dist < rad) { val p = (1f - dist / rad) * 1500f * d; r.vx += dx / dist * p * t; r.vy += dy / dist * p * t }
            }
            val damp = max(0f, 1f - 3f * t); r.vx *= damp; r.vy *= damp
            var px = r.x + r.vx * t; var py = r.y + r.vy * t
            if (px < cl + 12f * d || px > cr - 12f * d) { r.vx = -r.vx * 0.5f; px = r.x }
            if (py < ct + 12f * d || py > cb - 12f * d) { r.vy = -r.vy * 0.5f; py = r.y }
            if (inVacuole(px, py)) { r.vx = -r.vx; r.vy = -r.vy; px = r.x; py = r.y }
            val dn = hypot2(px - nx, py - ny)
            if (dn < nr + 40f * d) { px = r.x; py = r.y; r.vx = -r.vx; r.vy = -r.vy }
            r.x = px; r.y = py
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
        val o = FloatArray(4)
        with(kit) {
            // Pared celular: lámina media, pared primaria con microfibrillas, membrana
            stroke(col(0xFFE8E0A0L), 5f * d); rrect(cl - 9f * d, ct - 9f * d, cr + 9f * d, cb + 9f * d, 42f * d)
            stroke(col(0xFF7A9A3CL), 15f * d); rrect(cl - 1f * d, ct - 1f * d, cr + 1f * d, cb + 1f * d, 38f * d)
            p.style = Paint.Style.STROKE; p.strokeWidth = 1.3f * d; p.color = col(0xFFD4EC8AL)
            canvas.drawLines(wallTicks, p)
            stroke(col(0xFFBBD46AL), 3f * d); rrect(cl + 6f * d, ct + 6f * d, cr - 6f * d, cb - 6f * d, 32f * d)
            rect.set(cl + 8f * d, ct + 8f * d, cr - 8f * d, cb - 8f * d)
            canvas.drawRoundRect(rect, 30f * d, 30f * d, cyto)
            stroke(col(0xFFE0B45AL), 2f * d); rrect(cl + 8f * d, ct + 8f * d, cr - 8f * d, cb - 8f * d, 30f * d)
            // plasmodesmos
            var i = 0
            while (i + 3 < pd.size) {
                fill(col(0xFF2B5E44L)); circle(pd[i], pd[i + 1], 6f * d)
                stroke(col(0xFF4FB0C0L), 2f * d); line(pd[i] - pd[i + 2] * 12f * d, pd[i + 1] - pd[i + 3] * 12f * d, pd[i] + pd[i + 2] * 12f * d, pd[i + 1] + pd[i + 3] * 12f * d)
                i += 4
            }

            // Vacuola central con tonoplasto
            fill(col(0xFF1E5E7EL)); rrect(vl, vt, vr, vb, 90f * d)
            stroke(col(0xFF7FC4E0L), 2.4f * d); rrect(vl, vt, vr, vb, 90f * d)
            canvas.save(); canvas.clipRect(vl + 8f * d, vt + 8f * d, vr - 8f * d, vb - 8f * d)
            for (k in 0 until 26) {
                val px = vl + (vr - vl) * (0.5f + 0.42f * sin(k * 2.3f + time * 0.15f))
                val py = vt + (vb - vt) * (0.5f + 0.44f * cos(k * 1.7f + time * 0.12f + k))
                if (k % 4 == 0) { fill(col(0xAAB05AD8L)); circle(px, py, 5f * d) } else { fill(col(0x88E8D06AL)); circle(px, py, 2.2f * d) }
            }
            stroke(col(0x14FFFFFFL), 14f * d)
            for (k in 0..3) { val yy = vt + ((time * 20f * d + k * (vb - vt) / 4f) % (vb - vt)); line(vl, yy, vr, yy - 120f * d) }
            canvas.restore()

            // Retículo liso y Golgi
            dp(sx, sy) {
                for (c in arrayOf(floatArrayOf(-40f, 10f, -20f, -36f, 10f, 36f, 40f, -10f), floatArrayOf(-34f, -26f, -8f, 10f, 20f, -30f, 40f, 24f))) {
                    path.reset(); path.moveTo(c[0], c[1]); path.cubicTo(c[2], c[3], c[4], c[5], c[6], c[7])
                    stroke(col(0xFF4FB0C0L), 7f); drawPath(); stroke(col(0xFF17434DL), 3.5f); drawPath()
                }
            }
            dp(gx, gy, -10f) {
                val colors = intArrayOf(col(0xFF7FD69AL), col(0xFF9AD98AL), col(0xFFC3D77AL), col(0xFFE3B95AL), col(0xFFE8935AL))
                for (q in 0..4) { val y = -18f + q * 9f; path.reset(); path.moveTo(-30f + q, y + 5f); path.quadTo(0f, y - 12f, 30f - q, y + 5f); stroke(colors[q], 4f); drawPath() }
                fill(col(0xFFE8935AL)); circle(34f + sin(time * 2f) * 2f, 24f, 4f)
            }

            // Núcleo y retículo rugoso
            dp(nx, ny) {
                val r = nr / d
                for (q in 0..2) {
                    val rr = r + 9f + q * 9f
                    curve(60) { f, oo -> val an = -1.2f + q * 0.35f + f * 4.4f; val z = rr + 2f * sin(an * 8f + q + time * 0.6f); oo[0] = cos(an) * z; oo[1] = sin(an) * z }
                    stroke(col(0xFF3E9AA8L), 5f); drawPath(); stroke(col(0xFF0F2A30L), 2f); drawPath()
                    fill(col(0xFFC79CFFL)); for (k in 0 until 30) { val an = -1.2f + q * 0.35f + k / 30f * 4.4f; circle(cos(an) * (rr + 4.5f), sin(an) * (rr + 4.5f), 1.3f) }
                }
                fill(col(0xFF161C5CL)); circle(0f, 0f, r)
                stroke(col(0xFF8F9BFFL), 2.4f); circle(0f, 0f, r)
                fill(col(0xFFFFD27FL)); for (k in 0 until 16) { val an = k * TAU / 16f; circle(cos(an) * (r - 2f), sin(an) * (r - 2f), 2.2f) }
                stroke(col(0x88A6B0FFL), 1.4f)
                for (q in 0..7) { curve(24) { f, oo -> val an = q * 0.8f + f * 5f + sin(time * 0.2f + q) * 0.3f; val z = r * 0.78f * (0.25f + 0.75f * f); oo[0] = cos(an) * z; oo[1] = sin(an) * z }; drawPath() }
                fill(col(0xFF5A3A9AL)); circle(r * 0.3f, r * 0.2f, r * 0.2f)
            }

            // Mitocondrias y cloroplastos en corriente citoplasmática
            for (m in mitos) { place(m, o); dp(o[0], o[1], Math.toDegrees(kotlin.math.atan2(o[3], o[2]).toDouble()).toFloat()) {
                fill(col(0xFF7A2E1EL)); rrect(-19f, -8f, 19f, 8f, 8f)
                stroke(col(0xFFE89A5AL), 1.6f); rrect(-19f, -8f, 19f, 8f, 8f)
                stroke(col(0xFFFFC98AL), 1.8f); for (q in 0..3) { val x = -12f + q * 8f; if (q % 2 == 0) line(x, -7f, x, 1.5f) else line(x, 7f, x, -1.5f) }
            } }
            for (m in chloros) { place(m, o); dp(o[0], o[1], Math.toDegrees(kotlin.math.atan2(o[3], o[2]).toDouble()).toFloat() + m.ph * 8f) {
                fill(col(0xFF1E6B2EL)); oval(-24f, -12f, 24f, 12f)
                stroke(col(0xFF7FD88AL), 1.5f); oval(-24f, -12f, 24f, 12f)
                fill(col(0xFF3EBE55L))
                for (g in 0..2) for (q in 0..2) oval(-15f + g * 13f - 5f, -5f + q * 3.4f, -15f + g * 13f + 5f, -5f + q * 3.4f + 2.2f)
                fill(col(0xFFEDEDE0L)); oval(9f, -8f, 16f, -4f)
            } }

            // Ribosomas libres
            fill(col(0xFFB58CE8L))
            for (r in ribos) circle(r.x, r.y, 1.9f * d)

            if (!overlay.isOpen && time < 12f) text("Toca un orgánulo para verlo en detalle", w / 2, h * 0.93f, 13f * d, col(0x88FFFFFFL))
        }
        overlay.draw(canvas, w, h)
    }
}
