package com.piloto.particles

import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Corazón didáctico en cuatro cortes: sagital, coronal (frontal), axial de cuatro cámaras y
 * axial alto (grandes vasos). Muestra el ciclo cardiaco con válvulas, flujo de sangre y conducción eléctrica.
 * Ajustable: plano, frecuencia, cámara lenta, etiquetas y conducción. Toca una estructura para leer su función.
 */
class HeartScene(private val d: Float) : Scene {

    private class Cav(val name: String, val cx: Float, val cy: Float, val rx: Float, val ry: Float, val rot: Float,
                      val wall: Float, val oxy: Boolean, val kind: Int, val flowOut: Boolean = true)
    // kind: 1 aurícula, 2 ventrículo, 3 arteria (corte), 4 vena (corte), 5 vía aérea, 6 otra estructura
    private class Ves(val name: String, val pts: FloatArray, val w: Float, val oxy: Boolean, val artery: Boolean)
    private class Valve(val name: String, val x: Float, val y: Float, val dx: Float, val dy: Float, val gap: Float, val semi: Boolean) { var open = 0f }
    private class Route(val oxy: Boolean, val pts: FloatArray) {
        val n = pts.size / 3
        val cum = FloatArray(n)
        init { for (i in 1 until n) cum[i] = cum[i - 1] + hypot(pts[i * 3] - pts[i * 3 - 3], pts[i * 3 + 1] - pts[i * 3 - 2]) }
        val total get() = cum[n - 1]
    }
    private class Lab(val key: String, val ax: Float, val ay: Float, val left: Boolean, val text: String = key)
    private class Cond(val pts: FloatArray, val start: Float, val dur: Float)
    private class Node(val key: String, val x: Float, val y: Float, val t0: Float)
    private class Part(var route: Int, var dist: Float, val lat: Float, val k: Float) { var seg = 0 }
    private class Layout(
        val cavs: List<Cav>, val conns: List<FloatArray>, val ves: List<Ves>, val valves: List<Valve>,
        val routes: List<Route>, val labs: List<Lab>, val conds: List<Cond>, val nodes: List<Node>, val kind: Int
    )

    companion object {
        private fun hypot(x: Float, y: Float) = sqrt(x * x + y * y)
        private fun f(vararg v: Float) = v
        private fun cat(vararg a: FloatArray): FloatArray { var r = FloatArray(0); for (x in a) r += x; return r }
        private fun fr(x: Float) = x - kotlin.math.floor(x)
        val PLANES = arrayOf("Sagital", "Coronal (frontal)", "Axial 4 cámaras", "Axial grandes vasos")
    }

    private val kit = Kit(d)
    private val overlay = Overlay(kit)
    private val tap = TapDetector(12f * d)
    private var consumed = false
    private var w = 0f; private var h = 0f
    private var s = 1f; private var ox = 0f; private var oy = 0f
    private var lay: Layout = build(1)
    private var plane = 1
    private var bpm = 72f
    private var speed = 1f
    private var showLabels = true
    private var showCond = true
    private var beat = 0f      // latidos acumulados
    private var time = 0f
    private var ph = 0f
    private var sA = 0f; private var sV = 0f; private var pulse = 0f
    private var avOpen = false; private var semiOpen = false
    private val parts = ArrayList<Part>()
    private var tx = 0f; private var ty = 0f

    override fun configure(prefs: SharedPreferences) {
        bpm = prefs.getInt("heart_bpm", 72).toFloat().coerceIn(30f, 200f)
        speed = prefs.getInt("heart_speed", 100) / 100f
        showLabels = prefs.getBoolean("heart_labels", true)
        showCond = prefs.getBoolean("heart_cond", true)
        val p = prefs.getInt("heart_plane", 1).coerceIn(0, 3)
        if (p != plane || parts.isEmpty()) setPlane(p)
    }

    private fun setPlane(p: Int) {
        plane = p
        lay = build(p)
        parts.clear()
        for ((i, r) in lay.routes.withIndex()) repeat(18) { parts.add(Part(i, Random.nextFloat() * r.total, (Random.nextFloat() - 0.5f) * 2.2f, 0.8f + Random.nextFloat() * 0.4f)) }
        for (v in lay.valves) v.open = 0f
        overlay.close()
    }

    override fun resize(width: Int, height: Int) {
        w = width.toFloat(); h = height.toFloat()
        s = min(w * 0.96f / 100f, h * 0.56f / 112f)
        ox = (w - 100f * s) / 2f; oy = h * 0.19f
        if (parts.isEmpty()) setPlane(plane)
    }

    // ---------- Geometría de cada plano ----------
    private fun build(p: Int): Layout = when (p) {
        0 -> sagittal()
        2 -> axial4()
        3 -> axialHigh()
        else -> frontal()
    }

    private fun frontal(): Layout {
        val cavs = listOf(
            Cav("Aurícula derecha", 30f, 52f, 11f, 14f, 0f, 3f, false, 1),
            Cav("Ventrículo derecho", 40f, 80f, 11f, 17f, -6f, 4f, false, 2),
            Cav("Aurícula izquierda", 73f, 46f, 10f, 11f, 0f, 3f, true, 1),
            Cav("Ventrículo izquierdo", 68f, 80f, 13f, 20f, 8f, 8f, true, 2)
        )
        val conns = listOf(f(35f, 64f, 6f, 4f, 0f), f(70f, 58f, 5f, 4f, 1f))
        val ves = listOf(
            Ves("Venas pulmonares", f(97f, 44f, 80f, 46f), 5f, true, false),
            Ves("Venas pulmonares", f(97f, 54f, 80f, 52f), 5f, true, false),
            Ves("Vena cava superior", f(20f, 2f, 22f, 20f, 26f, 42f), 8f, false, false),
            Ves("Vena cava inferior", f(22f, 111f, 22f, 80f, 24f, 62f), 8f, false, false),
            Ves("Aorta", f(60f, 62f, 58f, 44f, 58f, 28f, 62f, 16f, 72f, 11f, 82f, 17f, 85f, 32f, 86f, 60f, 86f, 111f), 10f, true, true),
            Ves("Aorta", f(66f, 14f, 64f, 3f), 4f, true, true), Ves("Aorta", f(72f, 11f, 72f, 2f), 4f, true, true), Ves("Aorta", f(78f, 13f, 82f, 3f), 4f, true, true),
            Ves("Tronco pulmonar", f(46f, 64f, 46f, 46f, 46f, 38f), 8f, false, true),
            Ves("Arteria pulmonar", f(46f, 40f, 34f, 36f, 10f, 38f), 6f, false, true),
            Ves("Arteria pulmonar", f(46f, 39f, 58f, 32f, 78f, 34f, 96f, 38f), 6f, false, true)
        )
        val valves = listOf(
            Valve("Válvula tricúspide", 35f, 64f, 0f, 1f, 6f, false), Valve("Válvula mitral", 70f, 58f, 0f, 1f, 5f, false),
            Valve("Válvula pulmonar", 46f, 60f, 0f, -1f, 4f, true), Valve("Válvula aórtica", 60f, 62f, 0f, -1f, 4f, true)
        )
        val mid = f(32f, 58f, 1f, 35f, 66f, 1f, 40f, 84f, 2f, 46f, 66f, 2f, 46f, 50f, 0f, 46f, 40f, 0f)
        val svc = f(20f, 2f, 0f, 24f, 24f, 0f, 26f, 46f, 4f)
        val ivc = f(22f, 108f, 0f, 23f, 82f, 0f, 25f, 64f, 0f, 28f, 55f, 4f)
        val tr = f(36f, 36f, 0f, 10f, 38f, 0f)
        val tl = f(58f, 34f, 0f, 78f, 34f, 0f, 96f, 38f, 0f)
        val pv1 = f(96f, 44f, 0f, 82f, 46f, 0f, 74f, 49f, 4f)
        val pv2 = f(96f, 54f, 0f, 82f, 52f, 0f, 74f, 49f, 4f)
        val midO = f(70f, 55f, 1f, 70f, 65f, 1f, 67f, 84f, 2f, 60f, 64f, 2f, 58f, 46f, 0f, 58f, 28f, 0f, 62f, 16f, 0f, 72f, 11f, 0f, 82f, 17f, 0f, 85f, 32f, 0f, 86f, 60f, 0f, 86f, 110f, 0f)
        val routes = listOf(
            Route(false, cat(svc, mid, tr)), Route(false, cat(svc, mid, tl)), Route(false, cat(ivc, mid, tr)), Route(false, cat(ivc, mid, tl)),
            Route(true, cat(pv1, midO)), Route(true, cat(pv2, midO))
        )
        val labs = listOf(
            Lab("Vena cava superior", 21f, 14f, true), Lab("Tronco pulmonar", 46f, 48f, true), Lab("Aurícula derecha", 28f, 50f, true),
            Lab("Válvula tricúspide", 35f, 64f, true), Lab("Tabique interventricular", 54f, 82f, true), Lab("Ventrículo derecho", 38f, 88f, true),
            Lab("Vena cava inferior", 22f, 98f, true),
            Lab("Aorta", 78f, 13f, false, "Arco aórtico"), Lab("Arteria pulmonar", 82f, 34f, false), Lab("Venas pulmonares", 92f, 45f, false),
            Lab("Aurícula izquierda", 72f, 44f, false), Lab("Válvula mitral", 70f, 58f, false), Lab("Válvula aórtica", 60f, 62f, false),
            Lab("Ventrículo izquierdo", 70f, 86f, false), Lab("Aorta descendente", 86f, 96f, false)
        )
        val conds = listOf(
            Cond(f(24f, 40f, 32f, 50f, 42f, 63f), 0f, 0.08f), Cond(f(24f, 40f, 50f, 42f, 68f, 44f), 0f, 0.09f),
            Cond(f(42f, 63f, 47f, 69f, 50f, 74f), 0.15f, 0.03f),
            Cond(f(50f, 74f, 46f, 88f, 42f, 98f), 0.18f, 0.04f), Cond(f(52f, 74f, 58f, 90f, 66f, 102f), 0.18f, 0.05f)
        )
        val nodes = listOf(Node("Nodo sinusal", 24f, 40f, 0f), Node("Nodo auriculoventricular", 42f, 63f, 0.08f), Node("Haz de His y Purkinje", 50f, 74f, 0.16f))
        return Layout(cavs, conns, ves, valves, routes, labs, conds, nodes, 1)
    }

    private fun sagittal(): Layout {
        val cavs = listOf(
            Cav("Ventrículo derecho", 26f, 64f, 11f, 17f, 0f, 4f, false, 2),
            Cav("Aurícula derecha", 28f, 92f, 9f, 9f, 0f, 3f, false, 1),
            Cav("Ventrículo izquierdo", 60f, 80f, 17f, 12f, -12f, 7f, true, 2),
            Cav("Aurícula izquierda", 72f, 50f, 10f, 12f, 0f, 3f, true, 1)
        )
        val conns = listOf(f(27f, 82f, 5f, 4f, 0f), f(66f, 65f, 5f, 4f, 1f), f(50f, 66f, 5f, 5f, 1f))
        val ves = listOf(
            Ves("Venas pulmonares", f(101f, 42f, 82f, 47f), 5f, true, false), Ves("Venas pulmonares", f(101f, 56f, 82f, 53f), 5f, true, false),
            Ves("Vena cava inferior", f(28f, 111f, 28f, 96f), 8f, false, false),
            Ves("Aorta", f(46f, 60f, 46f, 40f, 50f, 22f, 58f, 12f, 70f, 11f, 80f, 18f, 84f, 34f, 88f, 60f, 90f, 111f), 10f, true, true),
            Ves("Aorta", f(56f, 13f, 54f, 2f), 4f, true, true), Ves("Aorta", f(64f, 11f, 66f, 2f), 4f, true, true),
            Ves("Tronco pulmonar", f(26f, 52f, 26f, 36f, 34f, 26f, 48f, 23f, 62f, 25f, 72f, 30f), 8f, false, true)
        )
        val valves = listOf(
            Valve("Válvula tricúspide", 27f, 82f, 0f, -1f, 5f, false), Valve("Válvula mitral", 66f, 65f, 0f, 1f, 4f, false),
            Valve("Válvula pulmonar", 26f, 46f, 0f, -1f, 4f, true), Valve("Válvula aórtica", 46f, 60f, 0f, -1f, 4f, true)
        )
        val dx = f(28f, 110f, 0f, 28f, 98f, 4f, 27f, 86f, 1f, 26f, 70f, 2f, 26f, 46f, 0f, 26f, 36f, 0f, 34f, 26f, 0f, 48f, 23f, 0f, 62f, 25f, 0f, 72f, 30f, 0f)
        val pv1 = f(100f, 44f, 0f, 84f, 48f, 0f, 74f, 50f, 4f)
        val pv2 = f(100f, 56f, 0f, 84f, 53f, 0f, 74f, 50f, 4f)
        val mo = f(66f, 58f, 1f, 66f, 68f, 1f, 60f, 78f, 2f, 52f, 68f, 2f, 46f, 58f, 0f, 46f, 40f, 0f, 50f, 22f, 0f, 58f, 12f, 0f, 70f, 11f, 0f, 80f, 18f, 0f, 84f, 34f, 0f, 88f, 60f, 0f, 90f, 110f, 0f)
        val routes = listOf(Route(false, dx), Route(false, dx), Route(true, cat(pv1, mo)), Route(true, cat(pv2, mo)))
        val labs = listOf(
            Lab("Esternón", 4f, 50f, true), Lab("Tronco pulmonar", 30f, 30f, true), Lab("Ventrículo derecho", 24f, 62f, true),
            Lab("Válvula tricúspide", 27f, 82f, true), Lab("Aurícula derecha", 28f, 92f, true), Lab("Vena cava inferior", 28f, 104f, true),
            Lab("Aorta", 66f, 11f, false, "Aorta ascendente"), Lab("Válvula aórtica", 46f, 60f, false), Lab("Aurícula izquierda", 72f, 48f, false),
            Lab("Venas pulmonares", 95f, 44f, false), Lab("Válvula mitral", 66f, 65f, false), Lab("Ventrículo izquierdo", 62f, 82f, false),
            Lab("Aorta descendente", 89f, 90f, false)
        )
        return Layout(cavs, conns, ves, valves, routes, labs, emptyList(), emptyList(), 0)
    }

    private fun axial4(): Layout {
        val cavs = listOf(
            Cav("Ventrículo derecho", 36f, 32f, 16f, 10f, 15f, 4f, false, 2),
            Cav("Aurícula derecha", 24f, 64f, 10f, 14f, 0f, 3f, false, 1),
            Cav("Ventrículo izquierdo", 68f, 52f, 13f, 20f, 15f, 8f, true, 2),
            Cav("Aurícula izquierda", 54f, 84f, 13f, 8f, 0f, 3f, true, 1)
        )
        val conns = listOf(f(29f, 47f, 5f, 5f, 0f), f(58f, 74f, 5f, 4f, 1f))
        val valves = listOf(Valve("Válvula tricúspide", 29f, 47f, 0f, -1f, 5f, false), Valve("Válvula mitral", 58f, 73f, 0f, -1f, 4f, false))
        val dr = f(20f, 72f, 4f, 26f, 58f, 1f, 29f, 47f, 1f, 36f, 38f, 2f, 24f, 28f, 2f, 16f, 16f, 0f)
        val lr = f(54f, 90f, 4f, 56f, 78f, 1f, 58f, 73f, 1f, 64f, 62f, 2f, 68f, 48f, 2f, 76f, 34f, 0f)
        val routes = listOf(Route(false, dr), Route(false, dr), Route(true, lr), Route(true, lr))
        val labs = listOf(
            Lab("Esternón", 46f, 4f, true), Lab("Ventrículo derecho", 34f, 32f, true), Lab("Válvula tricúspide", 29f, 47f, true),
            Lab("Aurícula derecha", 22f, 66f, true), Lab("Columna vertebral", 48f, 104f, true),
            Lab("Tabique interventricular", 54f, 44f, false), Lab("Ventrículo izquierdo", 70f, 52f, false), Lab("Válvula mitral", 58f, 73f, false),
            Lab("Aurícula izquierda", 56f, 86f, false), Lab("Aorta descendente", 68f, 98f, false)
        )
        return Layout(cavs, conns, emptyList(), valves, routes, labs, emptyList(), emptyList(), 2)
    }

    private fun axialHigh(): Layout {
        val cavs = listOf(
            Cav("Vena cava superior", 28f, 52f, 8f, 8f, 0f, 2f, false, 4, false),
            Cav("Aorta", 46f, 50f, 11f, 11f, 0f, 3f, true, 3, false),
            Cav("Tronco pulmonar", 64f, 36f, 14f, 10f, 20f, 3f, false, 3, false),
            Cav("Tráquea", 44f, 76f, 7f, 7f, 0f, 2f, false, 5),
            Cav("Aorta descendente", 66f, 78f, 7f, 7f, 0f, 2f, true, 3, false),
            Cav("Columna vertebral", 50f, 100f, 9f, 6f, 0f, 2f, false, 6)
        )
        val labs = listOf(
            Lab("Esternón", 50f, 8f, true), Lab("Vena cava superior", 26f, 52f, true), Lab("Aorta", 44f, 50f, true, "Aorta ascendente"),
            Lab("Tráquea", 42f, 76f, true), Lab("Columna vertebral", 46f, 102f, true),
            Lab("Tronco pulmonar", 66f, 34f, false), Lab("Aorta descendente", 68f, 80f, false)
        )
        return Layout(cavs, emptyList(), emptyList(), emptyList(), emptyList(), labs, emptyList(), emptyList(), 3)
    }

    // ---------- Interacción ----------
    override fun touchDown(px: Float, py: Float) {
        if (overlay.isOpen) { overlay.close(); consumed = true; return }
        consumed = false; tx = px; ty = py; tap.down(px, py)
    }
    override fun touchMove(px: Float, py: Float) { if (!consumed) tap.move(px, py) }
    override fun touchUp() { if (!consumed && tap.up()) hit(tx, ty)?.let { overlay.open(it) } }

    private fun distSeg(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val vx = bx - ax; val vy = by - ay
        val l2 = vx * vx + vy * vy
        val t = if (l2 == 0f) 0f else (((px - ax) * vx + (py - ay) * vy) / l2).coerceIn(0f, 1f)
        return hypot(px - (ax + vx * t), py - (ay + vy * t))
    }

    private fun hit(x: Float, y: Float): Info? {
        val ux = (x - ox) / s; val uy = (y - oy) / s
        for (n in lay.nodes) if (hypot(ux - n.x, uy - n.y) < 5f) return HeartInfo.map[n.key]
        for (v in lay.valves) if (hypot(ux - v.x, uy - v.y) < 6f) return HeartInfo.map[v.name]
        for (vs in lay.ves) for (i in 0 until vs.pts.size / 2 - 1)
            if (distSeg(ux, uy, vs.pts[i * 2], vs.pts[i * 2 + 1], vs.pts[i * 2 + 2], vs.pts[i * 2 + 3]) < vs.w / 2f + 1f) return HeartInfo.map[vs.name]
        for (c in lay.cavs) {
            val a = -c.rot * 0.0174533f
            val dx = ux - c.cx; val dy = uy - c.cy
            val rx = dx * cos(a) - dy * sin(a); val ry = dx * sin(a) + dy * cos(a)
            if ((rx / c.rx) * (rx / c.rx) + (ry / c.ry) * (ry / c.ry) < 1f) return HeartInfo.map[c.name]
        }
        for (c in lay.cavs) {
            val a = -c.rot * 0.0174533f
            val dx = ux - c.cx; val dy = uy - c.cy
            val rx = dx * cos(a) - dy * sin(a); val ry = dx * sin(a) + dy * cos(a)
            val e = (rx / (c.rx + c.wall)).pow(2) + (ry / (c.ry + c.wall)).pow(2)
            if (e < 1f) return Info("Miocardio", "Músculo del corazón. Es más grueso en el ventrículo izquierdo, que debe vencer la presión de todo el cuerpo, y más fino en las aurículas. Se nutre de las arterias coronarias, que salen de la raíz de la aorta.")
        }
        var best: Lab? = null; var bd = 9f
        for (l in lay.labs) { val dd = hypot(ux - l.ax, uy - l.ay); if (dd < bd) { bd = dd; best = l } }
        return best?.let { HeartInfo.map[it.key] }
    }

    // ---------- Simulación ----------
    private fun bump(p: Float, a: Float, b: Float): Float = if (p in a..b) sin(Math.PI.toFloat() * (p - a) / (b - a)) else 0f

    override fun update(dt: Float) {
        overlay.update(dt)
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
        for (v in lay.valves) {
            val target = if (v.semi) (if (semiOpen) 1f else 0f) else (if (avOpen) 1f else 0f)
            v.open += (target - v.open) * min(1f, t * 14f)
        }
        val k = (bpm / 72f).coerceIn(0.5f, 2.2f)
        for (p in parts) {
            val r = lay.routes[p.route]
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
    private fun bloodDark(oxy: Boolean) = if (oxy) col(0xFF9A1F2AL) else col(0xFF2B3F94L)
    private fun bloodLight(oxy: Boolean) = if (oxy) col(0xFFFF7A82L) else col(0xFF86A4FFL)

    override fun draw(canvas: Canvas) {
        kit.cv = canvas
        canvas.drawColor(col(0xFF05070AL))
        with(kit) {
            drawEcg(canvas)
            canvas.save(); canvas.translate(ox, oy); canvas.scale(s, s)
            drawContext(lay.kind)
            // paredes de vasos
            for (v in lay.ves) polyline(v.pts, v.w * (if (v.artery) 1f + 0.07f * pulse else 1f) + 2.2f, col(0xFF8C3A40L))
            // miocardio
            for (c in lay.cavs) if (c.kind in 1..2) cav(c, true)
            // cavidades y conexiones
            for (c in lay.cavs) cav(c, false)
            for (q in lay.conns) { fill(bloodDark(q[4] > 0.5f)); oval(q[0] - q[2], q[1] - q[3], q[0] + q[2], q[1] + q[3]) }
            for (v in lay.ves) polyline(v.pts, v.w * (if (v.artery) 1f + 0.07f * pulse else 1f), bloodDark(v.oxy))
            // partículas (sangre)
            for (p in parts) {
                val r = lay.routes[p.route]
                val sg = p.seg.coerceIn(0, r.n - 2)
                val x0 = r.pts[sg * 3]; val y0 = r.pts[sg * 3 + 1]; val x1 = r.pts[sg * 3 + 3]; val y1 = r.pts[sg * 3 + 4]
                val len = max(0.01f, hypot(x1 - x0, y1 - y0))
                val u = ((p.dist - r.cum[sg]) / len).coerceIn(0f, 1f)
                val nx = -(y1 - y0) / len; val ny = (x1 - x0) / len
                fill(bloodLight(r.oxy))
                circle(x0 + (x1 - x0) * u + nx * p.lat, y0 + (y1 - y0) * u + ny * p.lat, 0.95f)
            }
            for (c in lay.cavs) if (c.kind == 3 || c.kind == 4) crossFlow(c)
            for (v in lay.valves) valve(v)
            if (showCond) conduction()
            canvas.restore()
            if (showLabels) labels(canvas)
            status(canvas)
            if (!overlay.isOpen && time < 10f) text("Toca una estructura para ver su función", w / 2, h * 0.955f, 12f * d, col(0x88FFFFFFL))
        }
        overlay.draw(canvas, w, h)
    }

    private fun Kit.polyline(pts: FloatArray, width: Float, color: Int) {
        path.reset(); path.moveTo(pts[0], pts[1])
        for (i in 1 until pts.size / 2) path.lineTo(pts[i * 2], pts[i * 2 + 1])
        stroke(color, width); drawPath()
    }

    private fun Kit.cav(c: Cav, wallPass: Boolean) {
        val k = when (c.kind) { 1 -> 1f - 0.13f * sA; 2 -> 1f - 0.17f * sV; 3 -> 1f + 0.08f * pulse; else -> 1f }
        cv.save(); cv.translate(c.cx, c.cy); cv.rotate(c.rot); cv.scale(k, k)
        if (wallPass) {
            val wl = c.wall * (1f + 0.25f * (if (c.kind == 2) sV else 0f))
            fill(if (c.kind == 2) col(0xFF6E2229L) else col(0xFF8E4048L)); oval(-c.rx - wl, -c.ry - wl, c.rx + wl, c.ry + wl)
        } else if (c.kind == 5) {
            fill(col(0xFF020304L)); oval(-c.rx, -c.ry, c.rx, c.ry)
            stroke(col(0xFF8A9AA8L), 1f); oval(-c.rx - 1f, -c.ry - 1f, c.rx + 1f, c.ry + 1f)
        } else if (c.kind == 6) {
            fill(col(0xFFC8BBA0L)); oval(-c.rx, -c.ry, c.rx, c.ry)
        } else {
            if (c.kind >= 3) { fill(col(0xFF8C3A40L)); oval(-c.rx - c.wall, -c.ry - c.wall, c.rx + c.wall, c.ry + c.wall) }
            fill(bloodDark(c.oxy)); oval(-c.rx, -c.ry, c.rx, c.ry)
            stroke(col(0x66FFB0B8L), 0.8f); oval(-c.rx, -c.ry, c.rx, c.ry)
        }
        cv.restore()
    }

    private fun Kit.crossFlow(c: Cav) {
        val n = 16
        for (i in 0 until n) {
            val a = i * 2.4f + time * (0.5f + (i % 3) * 0.3f)
            val rr = 0.85f * (0.25f + 0.75f * fr(i * 0.37f + time * 0.15f))
            cv.save(); cv.translate(c.cx, c.cy); cv.rotate(c.rot)
            fill(bloodLight(c.oxy)); circle(cos(a) * c.rx * rr, sin(a) * c.ry * rr, 0.9f)
            cv.restore()
        }
        stroke(col(0xCCFFFFFFL), 0.7f); circle(c.cx, c.cy, 2.2f)
        if (c.flowOut) { fill(col(0xCCFFFFFFL)); circle(c.cx, c.cy, 0.7f) }
        else { line(c.cx - 1.5f, c.cy - 1.5f, c.cx + 1.5f, c.cy + 1.5f); line(c.cx - 1.5f, c.cy + 1.5f, c.cx + 1.5f, c.cy - 1.5f) }
    }

    private fun Kit.drawContext(kind: Int) {
        if (kind == 0) {
            fill(col(0xFFD8CBB0L)); rrect(0f, 22f, 4f, 92f, 2f)
            for (i in 0..7) { fill(col(0xFFC8BBA0L)); rrect(94f, 12f + i * 12f, 100f, 21f + i * 12f, 2f) }
            stroke(col(0x55AACCFFL), 1.2f); path.reset(); path.moveTo(6f, 108f); path.quadTo(50f, 100f, 92f, 108f); drawPath()
        } else if (kind == 2 || kind == 3) {
            fill(col(0xFFD8CBB0L)); rrect(28f, 0f, 72f, 4f, 2f)
            if (kind == 2) { fill(col(0x22AACCFFL)); oval(0f, 22f, 12f, 92f); oval(88f, 22f, 100f, 92f) }
            else { fill(col(0x22AACCFFL)); oval(0f, 20f, 14f, 90f); oval(86f, 20f, 100f, 90f) }
        }
    }

    private fun Kit.valve(v: Valve) {
        val nx = -v.dy; val ny = v.dx
        val cl = 1f - v.open
        for (sd in intArrayOf(-1, 1)) {
            val ax = v.x + nx * v.gap * sd; val ay = v.y + ny * v.gap * sd
            val openX = ax + v.dx * 4.5f; val openY = ay + v.dy * 4.5f
            val closedX = v.x - v.dx * 1.2f + nx * sd * 0.3f; val closedY = v.y - v.dy * 1.2f + ny * sd * 0.3f
            stroke(col(0xFFF4E4B0L), 1.3f)
            line(ax, ay, openX + (closedX - openX) * cl, openY + (closedY - openY) * cl)
            fill(col(0xFFF4E4B0L)); circle(ax, ay, 1.1f)
        }
    }

    private fun Kit.conduction() {
        for (c in lay.conds) {
            stroke(col(0x66FFD84AL), 0.9f)
            path.reset(); path.moveTo(c.pts[0], c.pts[1])
            for (i in 1 until c.pts.size / 2) path.lineTo(c.pts[i * 2], c.pts[i * 2 + 1])
            drawPath()
            val q = (ph - c.start) / c.dur
            if (q in 0f..1f) {
                val n = c.pts.size / 2 - 1
                val seg = min(n - 1, (q * n).toInt()); val u = q * n - seg
                val bx = c.pts[seg * 2] + (c.pts[seg * 2 + 2] - c.pts[seg * 2]) * u
                val by = c.pts[seg * 2 + 1] + (c.pts[seg * 2 + 3] - c.pts[seg * 2 + 1]) * u
                fill(col(0x55FFF08AL)); circle(bx, by, 3.2f); fill(col(0xFFFFF08AL)); circle(bx, by, 1.4f)
            }
        }
        for (n in lay.nodes) {
            val on = ph in n.t0..(n.t0 + 0.07f)
            fill(if (on) col(0x66FFF08AL) else col(0x00000000L)); if (on) circle(n.x, n.y, 4f)
            fill(col(0xFFFFD84AL)); circle(n.x, n.y, 1.6f)
        }
    }

    private fun ecg(p: Float): Float {
        fun g(c: Float, wd: Float, a: Float) = a * kotlin.math.exp(-((p - c) * (p - c)) / (2f * wd * wd))
        return g(0.05f, 0.022f, 0.16f) + g(0.15f, 0.008f, -0.12f) + g(0.175f, 0.010f, 1f) + g(0.195f, 0.010f, -0.25f) + g(0.36f, 0.045f, 0.3f)
    }

    private fun Kit.drawEcg(canvas: Canvas) {
        val x0 = 14f * d; val x1 = w - 14f * d; val y0 = 40f * d; val hh = 26f * d
        stroke(col(0x22FF6070L), 1f)
        for (i in 0..10) line(x0 + (x1 - x0) * i / 10f, y0 - hh, x0 + (x1 - x0) * i / 10f, y0 + hh * 0.4f)
        stroke(col(0xFF7CFF9AL), 1.8f * d)
        path.reset()
        val n = 160
        for (i in 0..n) {
            val f = i.toFloat() / n
            val pp = fr(beat - (1f - f) * 3f)
            val x = x0 + (x1 - x0) * f; val y = y0 - ecg(pp) * hh
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath()
        fill(col(0xFF7CFF9AL)); circle(x1, y0 - ecg(ph) * hh, 3f * d)
        text("ECG", x0, y0 - hh - 4f * d, 10f * d, col(0x99FFFFFFL), Paint.Align.LEFT)
    }

    private fun Kit.status(canvas: Canvas) {
        val name = when {
            ph in 0.04f..0.16f -> "Sístole auricular"
            ph in 0.18f..0.22f -> "Contracción isovolumétrica"
            ph in 0.22f..0.48f -> "Sístole ventricular · eyección"
            ph in 0.48f..0.52f -> "Relajación isovolumétrica"
            else -> "Diástole · llenado"
        }
        text("${bpm.toInt()} lpm · $name", w / 2, 92f * d, 14f * d, col(0xFFFFE08AL), Paint.Align.CENTER, true)
        text(PLANES[plane], w / 2, 110f * d, 12f * d, col(0xAAFFFFFFL))
    }

    private fun Kit.labels(canvas: Canvas) {
        val size = 10.5f * d
        val margin = 6f * d
        for (left in booleanArrayOf(true, false)) {
            val list = lay.labs.filter { it.left == left }.sortedBy { it.ay }
            var lastY = -1000f
            for (l in list) {
                var y = oy + l.ay * s
                if (y < lastY + size * 1.9f) y = lastY + size * 1.9f
                lastY = y
                val ax = ox + l.ax * s; val ay = oy + l.ay * s
                p.textSize = size
                val tw = p.measureText(l.text)
                val tx0 = if (left) margin else w - margin
                val endX = if (left) tx0 + tw + size * 0.35f else tx0 - tw - size * 0.35f
                stroke(col(0x88FFFFFFL), 1f * d); line(endX, y - size * 0.35f, ax, ay)
                fill(col(0xFFFFFFFFL)); circle(ax, ay, 2.2f * d)
                label(l.text, tx0, y, size, if (left) Paint.Align.LEFT else Paint.Align.RIGHT)
            }
        }
    }
}
