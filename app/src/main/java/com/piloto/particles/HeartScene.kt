package com.piloto.particles

import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Corazón anatómico en cuatro cortes (sagital, coronal, axial de cuatro cámaras y axial de grandes vasos).
 * El dibujo (paredes con fibras, cavidades, trabéculas, músculos papilares, vasos con volumen) se genera
 * con tools/heart-art y se carga desde res/raw. Aquí se anima: contracción, válvulas con cuerdas tendinosas,
 * flujo sanguíneo, conducción eléctrica y ECG. Toca una estructura para leer su función.
 */
class HeartScene(private val d: Float) : Scene {

    private class Ch(val id: String, val px: Float, val py: Float, val type: String, val cav: Float, val wall: Float, val inn: Float)
    private class Lay(val t: Int, val ch: Int, val kind: Int, val path: Path?, val pts: FloatArray,
                      val fill: Paint?, val stroke: Paint?, val cols: IntArray?, val w: Float, val off: FloatArray)
    private class Reg(val name: String, val pts: FloatArray)
    private class Valve(val name: String, val x: Float, val y: Float, val dx: Float, val dy: Float, val gap: Float, val semi: Boolean, val chordae: FloatArray) { var open = 0f }
    private class Route(val oxy: Boolean, val pts: FloatArray) {
        val n = pts.size / 3
        val cum = FloatArray(n)
        init { for (i in 1 until n) cum[i] = cum[i - 1] + hypot(pts[i * 3] - pts[i * 3 - 3], pts[i * 3 + 1] - pts[i * 3 - 2]) }
        val total get() = cum[n - 1]
    }
    private class Flow(val name: String, val cx: Float, val cy: Float, val rx: Float, val ry: Float, val rot: Float, val oxy: Boolean, val out: Boolean)
    private class Lab(val key: String, val text: String, val x: Float, val y: Float, val left: Boolean)
    private class Cond(val pts: FloatArray, val start: Float, val dur: Float)
    private class Node(val key: String, val x: Float, val y: Float, val t0: Float)
    private class Part(val route: Int, var dist: Float, val lat: Float, val k: Float) { var seg = 0 }
    private class Art(
        val chambers: List<Ch>, val layers: List<Lay>, val regions: List<Reg>, val walls: List<Reg>, val valves: List<Valve>,
        val routes: List<Route>, val flows: List<Flow>, val labels: List<Lab>, val conds: List<Cond>, val nodes: List<Node>
    )

    companion object {
        val PLANES = arrayOf("Sagital", "Coronal (frontal)", "Axial 4 cámaras", "Axial grandes vasos", "Vista exterior")
        private val RES = intArrayOf(R.raw.heart_sagittal, R.raw.heart_coronal, R.raw.heart_axial4, R.raw.heart_axialhigh, R.raw.heart_exterior)
        private val cache = arrayOfNulls<Art>(5)
        private fun fr(x: Float) = x - floor(x)

        private fun farr(a: JSONArray?): FloatArray { if (a == null) return FloatArray(0); return FloatArray(a.length()) { a.getDouble(it).toFloat() } }
        private fun color(s: String): Int = java.lang.Long.parseLong(s, 16).toInt()
        private fun stops(a: JSONArray): Pair<IntArray, FloatArray> =
            Pair(IntArray(a.length()) { color(a.getJSONArray(it).getString(1)) }, FloatArray(a.length()) { a.getJSONArray(it).getDouble(0).toFloat() })

        private fun fillPaint(o: Any): Paint {
            val p = Paint(Paint.ANTI_ALIAS_FLAG); p.style = Paint.Style.FILL
            if (o is String) p.color = color(o)
            else {
                val j = o as JSONObject
                val l = j.optJSONArray("lin")
                if (l != null) { val (c, pos) = stops(l.getJSONArray(4)); p.shader = LinearGradient(l.getDouble(0).toFloat(), l.getDouble(1).toFloat(), l.getDouble(2).toFloat(), l.getDouble(3).toFloat(), c, pos, Shader.TileMode.CLAMP) }
                else { val r = j.getJSONArray("rad"); val (c, pos) = stops(r.getJSONArray(3)); p.shader = RadialGradient(r.getDouble(0).toFloat(), r.getDouble(1).toFloat(), r.getDouble(2).toFloat(), c, pos, Shader.TileMode.CLAMP) }
            }
            return p
        }

        private fun strokePaint(c: Int, w: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; color = c; strokeWidth = w; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }

        private fun pathOf(pts: FloatArray, close: Boolean): Path {
            val p = Path(); if (pts.size < 4) return p
            p.moveTo(pts[0], pts[1]); var i = 2
            while (i + 1 < pts.size) { p.lineTo(pts[i], pts[i + 1]); i += 2 }
            if (close) p.close(); return p
        }

        private fun load(plane: Int): Art {
            cache[plane]?.let { return it }
            val text = AppCtx.ctx.resources.openRawResource(RES[plane]).bufferedReader().use { it.readText() }
            val j = JSONObject(text)
            val chs = ArrayList<Ch>(); val ids = HashMap<String, Int>()
            val ca = j.getJSONArray("chambers")
            for (i in 0 until ca.length()) {
                val o = ca.getJSONObject(i)
                ids[o.getString("id")] = i
                chs.add(Ch(o.getString("id"), o.getDouble("px").toFloat(), o.getDouble("py").toFloat(), o.getString("type"), o.getDouble("cav").toFloat(), o.getDouble("wall").toFloat(), o.getDouble("inn").toFloat()))
            }
            val lay = ArrayList<Lay>()
            val la = j.getJSONArray("layers")
            for (i in 0 until la.length()) {
                val o = la.getJSONObject(i)
                val g = o.getString("g"); val dot = g.indexOf('.')
                val ch = if (dot > 0) ids[g.substring(0, dot)] ?: -1 else -1
                val kind = if (dot > 0) when (g.substring(dot + 1)) { "cav" -> 1; "in" -> 2; else -> 0 } else 0
                val pts = farr(o.optJSONArray("pts"))
                when (o.getString("t")) {
                    "poly" -> {
                        val sa = o.optJSONArray("stroke")
                        lay.add(Lay(0, ch, kind, pathOf(pts, true), pts, fillPaint(o.get("fill")), if (sa != null) strokePaint(color(sa.getString(0)), sa.getDouble(1).toFloat()) else null, null, 0f, FloatArray(0)))
                    }
                    "tube" -> {
                        val ca2 = o.getJSONArray("cols")
                        lay.add(Lay(1, ch, kind, pathOf(pts, false), pts, null, null, IntArray(ca2.length()) { color(ca2.getString(it)) }, o.getDouble("w").toFloat(), farr(o.getJSONArray("off"))))
                    }
                    "lines" -> lay.add(Lay(2, ch, kind, null, pts, null, strokePaint(color(o.getString("c")), o.getDouble("w").toFloat()), null, 0f, FloatArray(0)))
                    else -> lay.add(Lay(3, ch, kind, pathOf(pts, false), pts, null, strokePaint(color(o.getString("c")), o.getDouble("w").toFloat()), null, 0f, FloatArray(0)))
                }
            }
            fun regs(k: String): List<Reg> { val a = j.getJSONArray(k); return List(a.length()) { Reg(a.getJSONObject(it).getString("name"), farr(a.getJSONObject(it).getJSONArray("pts"))) } }
            val va = j.getJSONArray("valves")
            val valves = List(va.length()) {
                val o = va.getJSONObject(it)
                val dx = o.getDouble("dx").toFloat(); val dy = o.getDouble("dy").toFloat(); val l = hypot(dx, dy)
                val src = o.optJSONArray("chordae")
                val chord = FloatArray((src?.length() ?: 0) * 2)
                if (src != null) for (q in 0 until src.length()) { chord[q * 2] = src.getJSONArray(q).getDouble(0).toFloat(); chord[q * 2 + 1] = src.getJSONArray(q).getDouble(1).toFloat() }
                Valve(o.getString("name"), o.getDouble("x").toFloat(), o.getDouble("y").toFloat(), dx / l, dy / l, o.getDouble("gap").toFloat(), o.getBoolean("semi"), chord)
            }
            val ra = j.getJSONArray("routes")
            val routes = List(ra.length()) { Route(ra.getJSONObject(it).getBoolean("oxy"), farr(ra.getJSONObject(it).getJSONArray("pts"))) }
            val fa = j.optJSONArray("flows") ?: JSONArray()
            val flows = List(fa.length()) { val o = fa.getJSONObject(it); Flow(o.getString("name"), o.getDouble("cx").toFloat(), o.getDouble("cy").toFloat(), o.getDouble("rx").toFloat(), o.getDouble("ry").toFloat(), o.getDouble("rot").toFloat(), o.getBoolean("oxy"), o.getBoolean("out")) }
            val lb = j.getJSONArray("labels")
            val labels = List(lb.length()) { val o = lb.getJSONObject(it); Lab(o.getString("key"), o.getString("text"), o.getDouble("x").toFloat(), o.getDouble("y").toFloat(), o.getBoolean("left")) }
            val cd = j.getJSONArray("conds")
            val conds = List(cd.length()) { val o = cd.getJSONObject(it); Cond(farr(o.getJSONArray("pts")), o.getDouble("start").toFloat(), o.getDouble("dur").toFloat()) }
            val nd = j.getJSONArray("nodes")
            val nodes = List(nd.length()) { val o = nd.getJSONObject(it); Node(o.getString("key"), o.getDouble("x").toFloat(), o.getDouble("y").toFloat(), o.getDouble("t0").toFloat()) }
            return Art(chs, lay, regs("regions"), regs("walls"), valves, routes, flows, labels, conds, nodes).also { cache[plane] = it }
        }
    }

    private val kit = Kit(d)
    private val overlay = Overlay(kit)
    private val tap = TapDetector(12f * d)
    private var consumed = false
    private var w = 0f; private var h = 0f
    private var s = 1f; private var ox = 0f; private var oy = 0f
    private var plane = 4
    private var art: Art? = null
    private var bpm = 72f
    private var speed = 1f
    private var showLabels = true
    private var showCond = true
    private var beat = 0f
    private var time = 0f
    private var ph = 0f
    private var sA = 0f; private var sV = 0f; private var pulse = 0f
    private var avOpen = false; private var semiOpen = false
    private val parts = ArrayList<Part>()
    private var tx = 0f; private var ty = 0f
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun configure(prefs: SharedPreferences) {
        bpm = prefs.getInt("heart_bpm", 72).toFloat().coerceIn(30f, 200f)
        speed = prefs.getInt("heart_speed", 100) / 100f
        showLabels = prefs.getBoolean("heart_labels", true)
        showCond = prefs.getBoolean("heart_cond", true)
        val p = prefs.getInt("heart_plane", 4).coerceIn(0, 4)
        if (p != plane || art == null) setPlane(p)
    }

    private fun setPlane(p: Int) {
        plane = p
        val a = load(p)
        art = a
        parts.clear()
        for ((i, r) in a.routes.withIndex()) repeat(22) { parts.add(Part(i, Random.nextFloat() * r.total, (Random.nextFloat() - 0.5f) * 2.2f, 0.8f + Random.nextFloat() * 0.4f)) }
        for (v in a.valves) v.open = 0f
        overlay.close()
    }

    override fun resize(width: Int, height: Int) {
        w = width.toFloat(); h = height.toFloat()
        val top = 62f * d; val bottom = 10f * d
        s = min(w * 0.99f / 100f, (h - top - bottom) / 112f)
        ox = (w - 100f * s) / 2f
        oy = top + ((h - top - bottom) - 112f * s) / 2f
        if (art == null) setPlane(plane)
    }

    // ---------- Interacción ----------
    override fun touchDown(px: Float, py: Float) {
        if (overlay.isOpen) { overlay.close(); consumed = true; return }
        consumed = false; tx = px; ty = py; tap.down(px, py)
    }
    override fun touchMove(px: Float, py: Float) { if (!consumed) tap.move(px, py) }
    override fun touchUp() { if (!consumed && tap.up()) hit(tx, ty)?.let { overlay.open(it) } }

    private fun inside(pts: FloatArray, x: Float, y: Float): Boolean {
        var c = false; var j = pts.size / 2 - 1
        for (i in 0 until pts.size / 2) {
            val xi = pts[i * 2]; val yi = pts[i * 2 + 1]; val xj = pts[j * 2]; val yj = pts[j * 2 + 1]
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) c = !c
            j = i
        }
        return c
    }

    private fun hit(x: Float, y: Float): Info? {
        val a = art ?: return null
        val ux = (x - ox) / s; val uy = (y - oy) / s
        for (n in a.nodes) if (hypot(ux - n.x, uy - n.y) < 4f) return HeartInfo.map[n.key]
        for (v in a.valves) if (hypot(ux - v.x, uy - v.y) < max(6f, v.gap)) return HeartInfo.map[v.name]
        for (r in a.regions) if (inside(r.pts, ux, uy)) return HeartInfo.map[r.name]
        for (r in a.walls) if (inside(r.pts, ux, uy)) return HeartInfo.map["Miocardio"]
        return null
    }

    // ---------- Simulación ----------
    private fun bump(p: Float, a: Float, b: Float): Float = if (p in a..b) sin(Math.PI.toFloat() * (p - a) / (b - a)) else 0f

    override fun update(dt: Float) {
        overlay.update(dt)
        val a = art ?: return
        if (w == 0f) return
        val t = min(dt, 0.05f) * speed
        time += t
        beat += t * bpm / 60f
        ph = fr(beat)
        sA = bump(ph, 0.04f, 0.16f)
        sV = bump(ph, 0.18f, 0.48f)
        avOpen = !(ph in 0.18f..0.50f)
        semiOpen = ph in 0.22f..0.48f
        pulse += (sV - pulse) * min(1f, t * 8f)
        for (v in a.valves) {
            val target = if (v.semi) (if (semiOpen) 1f else 0f) else (if (avOpen) 1f else 0f)
            v.open += (target - v.open) * min(1f, t * 14f)
        }
        val k = (bpm / 72f).coerceIn(0.5f, 2.2f)
        for (p in parts) {
            val r = a.routes[p.route]
            val mode = r.pts[p.seg * 3 + 2].toInt()
            val base = when (mode) {
                1 -> if (avOpen) 95f else 4f
                2 -> if (semiOpen) 170f else 3f
                4 -> 24f
                else -> 42f + 25f * pulse
            }
            p.dist += base * p.k * k * t
            if (p.dist >= r.total) { p.dist -= r.total; p.seg = 0 }
            while (p.seg < r.n - 2 && p.dist >= r.cum[p.seg + 1]) p.seg++
        }
    }

    // ---------- Dibujo ----------
    override fun draw(canvas: Canvas) {
        kit.cv = canvas
        canvas.drawColor(col(0xFF05070AL))
        val a = art ?: return
        with(kit) {
            canvas.save(); canvas.translate(ox, oy); canvas.scale(s, s)
            val beatK = 1f - 0.012f * sV
            canvas.scale(beatK, beatK, 50f, 56f)
            drawLayers(canvas, a)
            drawFlow(a)
            for (v in a.valves) valve(v)
            for (f in a.flows) cross(f)
            if (showCond) conduction(a)
            canvas.restore()
            if (showLabels) labels(canvas, a)
            topStrip(canvas)
            if (!overlay.isOpen && time < 10f) text("Toca una estructura para ver su función", w / 2, h - 6f * d, 11f * d, col(0x77FFFFFFL))
        }
        overlay.draw(canvas, w, h)
    }

    private fun drawLayers(canvas: Canvas, a: Art) {
        val ratios = floatArrayOf(1f, 0.78f, 0.5f, 0.2f)
        for (l in a.layers) {
            canvas.save()
            if (l.ch >= 0) {
                val c = a.chambers[l.ch]
                val sys = when (c.type) { "v" -> sV; "p" -> pulse; else -> sA }
                val amt = (if (l.kind == 1) c.cav else if (l.kind == 2) c.inn else c.wall) * sys
                canvas.scale(1f - amt, 1f - amt, c.px, c.py)
            }
            when (l.t) {
                0 -> { canvas.drawPath(l.path!!, l.fill!!); l.stroke?.let { canvas.drawPath(l.path, it) } }
                1 -> for ((i, c) in l.cols!!.withIndex()) {
                    val k = l.w * (1f - ratios[i])
                    canvas.save(); canvas.translate(l.off[0] * k, l.off[1] * k)
                    kit.stroke(c, l.w * ratios[i]); canvas.drawPath(l.path!!, kit.p)
                    canvas.restore()
                }
                2 -> canvas.drawLines(l.pts, l.stroke!!)
                else -> canvas.drawPath(l.path!!, l.stroke!!)
            }
            canvas.restore()
        }
    }

    private fun Kit.drawFlow(a: Art) {
        for (p in parts) {
            val r = a.routes[p.route]
            val sg = p.seg.coerceIn(0, r.n - 2)
            val x0 = r.pts[sg * 3]; val y0 = r.pts[sg * 3 + 1]; val x1 = r.pts[sg * 3 + 3]; val y1 = r.pts[sg * 3 + 4]
            val len = max(0.01f, hypot(x1 - x0, y1 - y0))
            val u = ((p.dist - r.cum[sg]) / len).coerceIn(0f, 1f)
            val nx = -(y1 - y0) / len; val ny = (x1 - x0) / len
            val x = x0 + (x1 - x0) * u + nx * p.lat; val y = y0 + (y1 - y0) * u + ny * p.lat
            fill(if (r.oxy) col(0x33FF9AA0L) else col(0x33A8BFFFL)); circle(x, y, 1.9f)
            fill(if (r.oxy) col(0xFFFF8E94L) else col(0xFF9CB4FFL)); circle(x, y, 0.8f)
        }
    }

    private fun Kit.cross(f: Flow) {
        for (i in 0 until 16) {
            val an = i * 2.4f + time * (0.5f + (i % 3) * 0.3f)
            val rr = 0.85f * (0.25f + 0.75f * fr(i * 0.37f + time * 0.15f))
            cv.save(); cv.translate(f.cx, f.cy); cv.rotate(Math.toDegrees(f.rot.toDouble()).toFloat())
            fill(if (f.oxy) col(0xFFFF8E94L) else col(0xFF9CB4FFL)); circle(cos(an) * f.rx * rr, sin(an) * f.ry * rr, 0.8f)
            cv.restore()
        }
        stroke(col(0xCCFFFFFFL), 0.6f); circle(f.cx, f.cy, 2f)
        if (f.out) { fill(col(0xCCFFFFFFL)); circle(f.cx, f.cy, 0.6f) }
        else { line(f.cx - 1.3f, f.cy - 1.3f, f.cx + 1.3f, f.cy + 1.3f); line(f.cx - 1.3f, f.cy + 1.3f, f.cx + 1.3f, f.cy - 1.3f) }
    }

    /** Valva con dos velos curvos (o tres cúspides en las semilunares) y cuerdas tendinosas. */
    private fun Kit.valve(v: Valve) {
        val nx = -v.dy; val ny = v.dx
        val cl = 1f - v.open
        val len = v.gap * (if (v.semi) 0.9f else 1.15f)
        for (sd in intArrayOf(-1, 1)) {
            val ax = v.x + nx * v.gap * sd; val ay = v.y + ny * v.gap * sd
            val openX = ax - nx * sd * v.gap * 0.12f + v.dx * len; val openY = ay - ny * sd * v.gap * 0.12f + v.dy * len
            val back = if (v.semi) 0.15f else 0.9f
            val closedX = v.x - v.dx * back + nx * sd * v.gap * 0.05f; val closedY = v.y - v.dy * back + ny * sd * v.gap * 0.05f
            val tipX = openX + (closedX - openX) * cl; val tipY = openY + (closedY - openY) * cl
            val bulge = (0.30f + 0.5f * cl) * v.gap * 0.5f * (if (v.semi) 1f else -1f)
            val cx = (ax + tipX) / 2f + nx * sd * bulge * -0.6f - v.dx * bulge * 0.6f * cl
            val cy = (ay + tipY) / 2f + ny * sd * bulge * -0.6f - v.dy * bulge * 0.6f * cl
            path.reset(); path.moveTo(ax, ay); path.quadTo(cx, cy, tipX, tipY)
            path.quadTo(cx + nx * sd * 0.7f, cy + ny * sd * 0.7f, ax + nx * sd * 0.5f, ay + ny * sd * 0.5f); path.close()
            fill(col(0xE6F1DCCCL)); drawPath()
            stroke(col(0xFFB48C74L), 0.5f); drawPath()
            if (!v.semi && v.chordae.isNotEmpty()) {
                stroke(col(0x99F6EAD8L), 0.35f)
                var i = 0
                while (i + 1 < v.chordae.size) {
                    val px = v.chordae[i]; val py = v.chordae[i + 1]
                    val sag = 1.2f * v.open
                    line(tipX, tipY, px + sag * sd, py); line(tipX - nx * sd * 0.6f, tipY - ny * sd * 0.6f, px + sag * sd * 0.5f, py)
                    i += 2
                }
            }
        }
    }

    private fun Kit.conduction(a: Art) {
        for (c in a.conds) {
            stroke(col(0x55FFD84AL), 0.7f)
            path.reset(); path.moveTo(c.pts[0], c.pts[1])
            for (i in 1 until c.pts.size / 2) path.lineTo(c.pts[i * 2], c.pts[i * 2 + 1])
            drawPath()
            val q = (ph - c.start) / c.dur
            if (q in 0f..1f) {
                val n = c.pts.size / 2 - 1
                val seg = min(n - 1, (q * n).toInt()); val u = q * n - seg
                val bx = c.pts[seg * 2] + (c.pts[seg * 2 + 2] - c.pts[seg * 2]) * u
                val by = c.pts[seg * 2 + 1] + (c.pts[seg * 2 + 3] - c.pts[seg * 2 + 1]) * u
                fill(col(0x66FFF08AL)); circle(bx, by, 3f); fill(col(0xFFFFF08AL)); circle(bx, by, 1.2f)
            }
        }
        for (n in a.nodes) {
            if (ph in n.t0..(n.t0 + 0.07f)) { fill(col(0x66FFF08AL)); circle(n.x, n.y, 3.6f) }
            fill(col(0xFFFFD84AL)); circle(n.x, n.y, 1.3f)
        }
    }

    private fun ecg(p: Float): Float {
        fun g(c: Float, wd: Float, a: Float) = a * exp(-((p - c) * (p - c)) / (2f * wd * wd))
        return g(0.05f, 0.022f, 0.16f) + g(0.15f, 0.008f, -0.12f) + g(0.175f, 0.010f, 1f) + g(0.195f, 0.010f, -0.25f) + g(0.36f, 0.045f, 0.3f)
    }

    /** Franja superior mínima: frecuencia, fase y ECG. */
    private fun Kit.topStrip(canvas: Canvas) {
        val name = when {
            ph in 0.04f..0.16f -> "Sístole auricular"
            ph in 0.18f..0.22f -> "Contracción isovolumétrica"
            ph in 0.22f..0.48f -> "Sístole ventricular · eyección"
            ph in 0.48f..0.52f -> "Relajación isovolumétrica"
            else -> "Diástole · llenado"
        }
        text("♥ ${bpm.toInt()} lpm · $name", 10f * d, 15f * d, 11f * d, col(0xFFFFE08AL), Paint.Align.LEFT, true)
        text(PLANES[plane], w - 10f * d, 15f * d, 10f * d, col(0x99FFFFFFL), Paint.Align.RIGHT)
        val x0 = 10f * d; val x1 = w - 10f * d; val y0 = 46f * d; val hh = 20f * d
        stroke(col(0x1FFF6070L), 0.6f * d)
        for (i in 0..12) line(x0 + (x1 - x0) * i / 12f, y0 - hh, x0 + (x1 - x0) * i / 12f, y0 + 6f * d)
        stroke(col(0xFF7CFF9AL), 1.4f * d)
        path.reset()
        val n = 140
        for (i in 0..n) {
            val f = i.toFloat() / n
            val pp = fr(beat - (1f - f) * 3f)
            val x = x0 + (x1 - x0) * f; val y = y0 - ecg(pp) * hh
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath()
        fill(col(0xFF7CFF9AL)); circle(x1, y0 - ecg(ph) * hh, 2.6f * d)
    }

    private fun Kit.labels(canvas: Canvas, a: Art) {
        val size = 9f * d
        val margin = 5f * d
        for (left in booleanArrayOf(true, false)) {
            val list = a.labels.filter { it.left == left }.sortedBy { it.y }
            var lastY = -1000f
            for (l in list) {
                var y = oy + l.y * s
                if (y < lastY + size * 1.7f) y = lastY + size * 1.7f
                lastY = y
                val ax = ox + l.x * s; val ay = oy + l.y * s
                p.textSize = size
                val tw = p.measureText(l.text)
                val tx0 = if (left) margin else w - margin
                val endX = if (left) tx0 + tw + 2f * d else tx0 - tw - 2f * d
                stroke(col(0x66FFFFFFL), 0.7f * d); line(endX, y - size * 0.35f, ax, ay)
                fill(col(0xDDFFFFFFL)); circle(ax, ay, 1.7f * d)
                p.style = Paint.Style.FILL; p.textSize = size; p.textAlign = if (left) Paint.Align.LEFT else Paint.Align.RIGHT
                p.setShadowLayer(3f * d, 0f, 0f, col(0xFF000000L)); p.color = col(0xE6FFFFFFL)
                canvas.drawText(l.text, tx0, y, p)
                p.clearShadowLayer()
            }
        }
    }
}
