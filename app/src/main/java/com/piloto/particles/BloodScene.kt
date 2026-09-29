package com.piloto.particles

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Torrente sanguíneo por dentro de un vaso (flujo pulsátil, más rápido en el centro):
 * glóbulos rojos en tres planos de profundidad, neutrófilos, linfocito y monocito,
 * plaquetas, anticuerpos, lipoproteínas, oxígeno cediéndose a la pared y un neutrófilo que persigue y fagocita bacterias.
 * Al tocar, las células se apartan del dedo; un toque corto sobre una célula explica qué es.
 */
class BloodScene(private val d: Float) : Scene {

    private open class Ent(var x: Float, var y: Float, val r: Float, val phase: Float, val depth: Float = 1f) {
        var vx = 0f; var vy = 0f
        var ang = Random.nextFloat() * 6.28f
        var spin = 0f
    }
    private class Wbc(x: Float, y: Float, r: Float, ph: Float, val type: Int) : Ent(x, y, r, ph) { var target = -1 }
    private class Bact(x: Float, y: Float, ph: Float) : Ent(x, y, 6f, ph) { var eaten = 0f; var inside = false }
    private class O2(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float)

    private val rbc = ArrayList<Ent>()
    private val wbc = ArrayList<Wbc>()
    private val plt = ArrayList<Ent>()
    private val dots = ArrayList<Ent>()
    private val ldl = ArrayList<Ent>()
    private val abs_ = ArrayList<Ent>()
    private val bact = ArrayList<Bact>()
    private val o2 = ArrayList<O2>()

    private var w = 0f; private var h = 0f; private var wall = 0f
    private var time = 0f
    private var o2Timer = 0f
    private var touching = false; private var tx = 0f; private var ty = 0f

    private val kit = Kit(d)
    private val overlay = Overlay(kit)
    private val tap = TapDetector(12f * d)
    private var consumed = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val mat = Matrix()
    private var bg: Paint? = null
    private var vignette: Paint? = null
    private var sprite: Bitmap? = null

    private val cx get() = w / 2f
    private val hw get() = w / 2f - wall
    private fun rndX() = cx - hw * 0.9f + Random.nextFloat() * hw * 1.8f

    private fun make(list: ArrayList<Ent>, n: Int, rMin: Float, rMax: Float, spin: Float, depth: Float = 1f) {
        repeat(n) {
            val r = (rMin + Random.nextFloat() * (rMax - rMin)) * d * depth
            list.add(Ent(rndX(), Random.nextFloat() * h, r, Random.nextFloat() * 6.28f, depth).also { it.spin = (Random.nextFloat() - 0.5f) * 2f * spin })
        }
    }

    private fun buildSprite() {
        val bm = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        val c = Canvas(bm)
        val pt = Paint(Paint.ANTI_ALIAS_FLAG)
        pt.shader = RadialGradient(48f, 48f, 46f,
            intArrayOf(col(0xFF7E0C18L), col(0xFFA7182BL), col(0xFFD12E3AL), col(0xFFEC5560L), col(0x00EC5560L)),
            floatArrayOf(0f, 0.3f, 0.66f, 0.9f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(48f, 48f, 46f, pt)
        sprite = bm
    }

    override fun resize(width: Int, height: Int) {
        val first = w == 0f
        w = width.toFloat(); h = height.toFloat(); wall = w * 0.09f
        bg = Paint().apply {
            shader = LinearGradient(0f, 0f, w, 0f,
                intArrayOf(col(0xFF1C0206L), col(0xFF4A0810L), col(0xFF651019L), col(0xFF7A1620L), col(0xFF651019L), col(0xFF4A0810L), col(0xFF1C0206L)),
                floatArrayOf(0f, 0.09f, 0.3f, 0.5f, 0.7f, 0.91f, 1f), Shader.TileMode.CLAMP)
        }
        vignette = Paint().apply {
            shader = RadialGradient(w / 2f, h / 2f, h * 0.75f, intArrayOf(0x00000000, 0x00000000, col(0xAA000000L)), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        }
        if (sprite == null) buildSprite()
        if (first) {
            make(dots, 130, 0.6f, 1.2f, 0f)
            make(ldl, 28, 1.6f, 2.2f, 0f)
            make(abs_, 22, 2.6f, 3.2f, 1.5f)
            make(plt, 60, 2.4f, 3.6f, 3f)
            make(rbc, 22, 9f, 12f, 1.6f, 0.6f)
            make(rbc, 30, 9f, 12.5f, 1.6f, 1f)
            make(rbc, 12, 9f, 12f, 1.4f, 1.35f)
            wbc.add(Wbc(rndX(), h * 0.3f, 19f * d, 0.5f, 0)); wbc.add(Wbc(rndX(), h * 0.75f, 18f * d, 2f, 0))
            wbc.add(Wbc(rndX(), h * 0.55f, 15f * d, 4f, 1)); wbc.add(Wbc(rndX(), h * 0.05f, 22f * d, 1f, 2))
            repeat(3) { bact.add(Bact(rndX(), Random.nextFloat() * h * 0.5f, Random.nextFloat() * 6f)) }
        }
    }

    override fun touchDown(px: Float, py: Float) {
        if (overlay.isOpen) { overlay.close(); consumed = true; return }
        consumed = false; touching = true; tx = px; ty = py; tap.down(px, py)
    }
    override fun touchMove(px: Float, py: Float) { if (consumed) return; tx = px; ty = py; tap.move(px, py) }
    override fun touchUp() { touching = false; if (!consumed && tap.up()) hit(tx, ty)?.let { overlay.open(it) } }

    private fun near(e: Ent, x: Float, y: Float, pad: Float) = (e.x - x) * (e.x - x) + (e.y - y) * (e.y - y) < (e.r + pad) * (e.r + pad)

    private fun hit(x: Float, y: Float): Info? {
        for (e in wbc) if (near(e, x, y, 6f * d)) return when (e.type) {
            0 -> Info("Neutrófilo", "Es el glóbulo blanco más abundante. Su núcleo tiene de 3 a 5 lóbulos y su citoplasma está lleno de gránulos con enzimas. Es la primera línea de defensa: sale del vaso hacia la infección, persigue a las bacterias guiado por señales químicas (quimiotaxis), las engulle (fagocitosis) y las destruye. Mira cómo uno de ellos persigue a las bacterias verdes.")
            1 -> Info("Linfocito", "Glóbulo blanco de núcleo grande y redondo que casi llena la célula. Los linfocitos B fabrican anticuerpos, los T coordinan la respuesta inmune o destruyen células infectadas, y los NK eliminan células tumorales. Aportan la memoria inmunológica.")
            else -> Info("Monocito", "El mayor de los glóbulos blancos, con un núcleo en forma de riñón. Al salir de la sangre hacia los tejidos se convierte en macrófago: fagocita restos, bacterias y células muertas, y presenta antígenos a los linfocitos.")
        }
        for (e in bact) if (near(e, x, y, 12f * d)) return Info("Bacteria", "Microorganismo invasor. Cuando entra en la sangre, el sistema inmune la detecta por moléculas de su superficie. Los neutrófilos la persiguen por quimiotaxis, la rodean con su membrana (fagocitosis) y la digieren en un fagolisosoma.")
        for (e in plt) if (near(e, x, y, 8f * d)) return Info("Plaqueta (trombocito)", "Fragmento celular sin núcleo de 2–3 µm que nace del megacariocito de la médula ósea. Ante una lesión se adhiere al vaso, se activa y se agrega con otras formando el tapón plaquetario; además libera factores que inician la coagulación y la formación de fibrina.")
        for (e in abs_) if (near(e, x, y, 8f * d)) return Info("Anticuerpo (inmunoglobulina G)", "Proteína en forma de Y fabricada por los linfocitos B. Cada anticuerpo reconoce un antígeno concreto: se une a bacterias o virus, los marca para que los fagocitos los destruyan y puede neutralizarlos directamente.")
        for (e in rbc.sortedByDescending { it.depth }) if (near(e, x, y, 5f * d)) return Info("Glóbulo rojo (eritrocito)", "Disco bicóncavo sin núcleo de unos 7 µm. Su hemoglobina, con hierro, capta el oxígeno en los pulmones y lo cede a los tejidos (las burbujas claras que ves soltarse), y recoge parte del CO₂. Es tan flexible que se pliega para pasar por capilares más estrechos que él. Vive unos 120 días; hay unos 5 millones por mm³ de sangre.")
        for (e in ldl) if (near(e, x, y, 8f * d)) return Info("Lipoproteína (LDL)", "Partícula que transporta colesterol y grasas por la sangre, formada por lípidos y proteínas. Cuando hay demasiadas, pueden depositarse en la pared arterial y formar placas de ateroma.")
        if (x < wall || x > w - wall) return Info("Endotelio y pared del vaso", "El vaso está revestido por el endotelio, una capa de células planas con núcleo abultado. Regula el paso de nutrientes, libera óxido nítrico para dilatar el vaso, impide que la sangre coagule sin necesidad y deja salir a los leucocitos hacia los tejidos. Debajo hay músculo liso y fibras elásticas.")
        return Info("Plasma", "Es la parte líquida de la sangre (≈ 55 %): agua con proteínas (albúmina, globulinas, fibrinógeno), nutrientes, hormonas, iones, desechos y lipoproteínas. Los puntos que se ven son proteínas y partículas disueltas.")
    }

    private fun step(list: List<Ent>, k: Float, dt: Float, beat: Float) {
        val cxx = cx; val hww = hw; val rad = 120f * d
        for (e in list) {
            if (touching && !overlay.isOpen) {
                val dx = e.x - tx; val dy = e.y - ty
                val dist = max(sqrt(dx * dx + dy * dy), 1f)
                if (dist < rad) {
                    val push = (1f - dist / rad) * 1400f * d
                    e.vx += dx / dist * push * dt; e.vy += dy / dist * push * dt; e.spin += (dx / dist) * 4f * dt
                }
            }
            val damp = max(0f, 1f - 2.5f * dt)
            e.vx *= damp; e.vy *= damp
            val u = ((e.x - cxx) / hww).coerceIn(-1f, 1f)
            val flow = 110f * d * k * e.depth * beat * max(0.2f, 1f - u * u)
            e.x += (e.vx + sin(time * 0.8f + e.phase) * 6f * d) * dt
            e.y += (flow + e.vy) * dt
            e.ang += e.spin * dt
            val minX = cxx - hww + e.r; val maxX = cxx + hww - e.r
            if (e.x < minX) { e.x = minX; e.vx = abs(e.vx) } else if (e.x > maxX) { e.x = maxX; e.vx = -abs(e.vx) }
            if (e.y > h + e.r * 2f || e.y < -h * 0.5f) { e.y = -e.r * 2f; e.x = rndX(); e.vx = 0f; e.vy = 0f }
        }
    }

    override fun update(dt: Float) {
        overlay.update(dt)
        if (w == 0f) return
        val t = min(dt, 0.05f)
        time += t
        val beat = 1f + 0.35f * max(0f, sin(time * 6.5f))
        step(dots, 1.15f, t, beat); step(ldl, 1.05f, t, beat); step(abs_, 1f, t, beat); step(plt, 1f, t, beat)
        step(rbc, 1f, t, beat); step(wbc, 0.55f, t, beat)
        for (b in bact) { step(listOf(b), 0.9f, t, beat) }

        // El primer neutrófilo persigue y engulle bacterias
        val n = wbc[0]
        var best: Bact? = null; var bd = 1e12f
        for (b in bact) if (!b.inside) { val dd = (b.x - n.x) * (b.x - n.x) + (b.y - n.y) * (b.y - n.y); if (dd < bd) { bd = dd; best = b } }
        best?.let { b ->
            val dist = sqrt(bd)
            if (dist < 420f * d) {
                n.vx += (b.x - n.x) / max(dist, 1f) * 260f * d * t; n.vy += (b.y - n.y) / max(dist, 1f) * 260f * d * t
                if (dist < n.r * 0.9f) { b.inside = true; b.eaten = 0f }
            }
        }
        for (b in bact) if (b.inside) {
            b.eaten += t
            b.x += (n.x - b.x) * min(1f, t * 3f); b.y += (n.y - b.y) * min(1f, t * 3f)
            if (b.eaten > 4f) { b.inside = false; b.x = rndX(); b.y = -20f * d; b.vx = 0f; b.vy = 0f }
        }

        // Oxígeno cedido por los glóbulos rojos
        o2Timer += t
        if (o2Timer > 0.12f && o2.size < 45) {
            o2Timer = 0f
            val e = rbc[Random.nextInt(rbc.size)]
            if (e.y in 0f..h) o2.add(O2(e.x, e.y, (if (e.x < cx) -1f else 1f) * (18f + Random.nextFloat() * 20f) * d, 0f, 3f))
        }
        val it = o2.iterator()
        while (it.hasNext()) { val o = it.next(); o.x += o.vx * t; o.y += (110f * d * 0.5f + o.vy) * t; o.life -= t; if (o.life <= 0f || o.x < wall * 0.6f || o.x > w - wall * 0.6f) it.remove() }
    }

    private fun drawRbc(canvas: Canvas, e: Ent) {
        val sp = sprite ?: return
        val sx = max(0.32f, abs(cos(e.ang)))
        val sc = e.r / 46f
        mat.reset(); mat.postTranslate(-48f, -48f); mat.postScale(sc, sc * sx); mat.postRotate(e.phase * 20f); mat.postTranslate(e.x, e.y)
        bmpPaint.alpha = (if (e.depth < 0.9f) 150 else if (e.depth > 1.2f) 235 else 255)
        canvas.drawBitmap(sp, mat, bmpPaint)
    }

    private fun blobPath(kit: Kit, x: Float, y: Float, r: Float, ph: Float, wob: Float) {
        kit.path.reset()
        val n = 18
        for (i in 0..n) {
            val a = i.toFloat() / n * TAU
            val rr = r * (1f + wob * sin(time * 1.6f + i * 1.7f + ph))
            val px = x + cos(a) * rr; val py = y + sin(a) * rr
            if (i == 0) kit.path.moveTo(px, py) else kit.path.lineTo(px, py)
        }
        kit.path.close()
    }

    override fun draw(canvas: Canvas) {
        kit.cv = canvas
        canvas.drawRect(0f, 0f, w, h, bg ?: return)
        with(kit) {
            // Pared: células endoteliales con núcleo abultado, glucocáliz y músculo liso
            val period = 84f * d
            var y = -period + (time * 5f * d) % period
            while (y < h) {
                for (side in 0..1) {
                    val x0 = if (side == 0) wall * 0.10f else w - wall * 0.90f
                    fill(col(0x66240408L)); oval(x0, y, x0 + wall * 0.8f, y + period * 0.88f)
                    fill(col(0x88120204L)); oval(x0 + wall * 0.15f, y + period * 0.3f, x0 + wall * 0.65f, y + period * 0.62f)
                    stroke(col(0x33FFB0A0L), 1f * d)
                    val gx = if (side == 0) wall * 0.92f else w - wall * 0.92f
                    for (k in 0..6) { val yy = y + k * period / 7f; line(gx, yy, gx + (if (side == 0) 5f else -5f) * d, yy + 2f * d) }
                }
                y += period
            }
            stroke(col(0x22000000L), 1.5f * d)
            var yy = 0f
            while (yy < h) { line(0f, yy, wall * 0.06f, yy + 6f * d); line(w, yy, w - wall * 0.06f, yy + 6f * d); yy += 18f * d }

            // Plasma
            fill(col(0x88E8A890L))
            for (e in dots) circle(e.x, e.y, e.r)
            fill(col(0xCCF0C860L))
            for (e in ldl) circle(e.x, e.y, e.r)
            stroke(col(0xCCB8E0FFL), 1.3f * d)
            for (e in abs_) {
                val ca = cos(e.ang); val sa = sin(e.ang); val l = e.r
                val jx = e.x + ca * l * 0.2f; val jy = e.y + sa * l * 0.2f
                line(e.x - ca * l, e.y - sa * l, jx, jy)
                line(jx, jy, jx + cos(e.ang + 0.6f) * l, jy + sin(e.ang + 0.6f) * l)
                line(jx, jy, jx + cos(e.ang - 0.6f) * l, jy + sin(e.ang - 0.6f) * l)
            }
            // Plaquetas
            for (e in plt) {
                cv.save(); cv.rotate(Math.toDegrees(e.ang.toDouble()).toFloat(), e.x, e.y)
                fill(col(0xFFD9A3D0L)); oval(e.x - e.r * 1.4f, e.y - e.r * 0.8f, e.x + e.r * 1.4f, e.y + e.r * 0.8f)
                fill(col(0xFF8E4A8AL)); circle(e.x - e.r * 0.4f, e.y, e.r * 0.25f); circle(e.x + e.r * 0.4f, e.y, e.r * 0.25f)
                cv.restore()
            }
            // Bacterias
            for (b in bact) {
                val sc = if (b.inside) max(0.1f, 1f - b.eaten / 4f) else 1f
                cv.save(); cv.rotate(Math.toDegrees((b.phase + time * 0.5).toDouble()).toFloat(), b.x, b.y)
                fill(col(0xFF7AD05AL)); rrect(b.x - 9f * d * sc, b.y - 3.6f * d * sc, b.x + 9f * d * sc, b.y + 3.6f * d * sc, 3.6f * d * sc)
                stroke(col(0xFFB8F090L), 1f * d); for (k in 0..2) { val a2 = time * 6f + k; line(b.x - 9f * d * sc, b.y, b.x - 15f * d * sc, b.y + sin(a2) * 5f * d) }
                cv.restore()
            }
            // Glóbulos rojos por planos de profundidad: lejos -> cerca
            for (dep in floatArrayOf(0.6f, 1f, 1.35f)) {
                for (e in rbc) if (e.depth == dep) drawRbc(canvas, e)
                if (dep == 1f) {
                    for (o in o2) { fill((min(1f, o.life) * 200).toInt().shl(24).or(0xBFEFFF)); circle(o.x, o.y, 2.6f * d) }
                    for (e in wbc) drawWbc(e)
                }
            }
            for (o in bact) if (o.inside) { fill(col(0xAA7AD05AL)); circle(o.x, o.y, 4f * d * max(0.2f, 1f - o.eaten / 4f)) }
        }
        canvas.drawRect(0f, 0f, w, h, vignette ?: return)
        if (!overlay.isOpen && time < 10f) { kit.text("Toca una célula para saber qué es", w / 2, h * 0.95f, 13f * d, col(0x88FFFFFFL)) }
        overlay.draw(canvas, w, h)
    }

    private fun Kit.drawWbc(e: Wbc) {
        blobPath(this, e.x, e.y, e.r, e.phase, 0.06f)
        fill(col(0xF0E9DDCBL)); drawPath()
        stroke(col(0xAAFFF5E4L), 1.5f * d); drawPath()
        fill(col(0xFFB58FC9L))
        if (e.type == 0) for (i in 0 until 8) { val a = e.phase * 3f + i * 0.8f + e.ang; circle(e.x + cos(a) * e.r * 0.62f, e.y + sin(a) * e.r * 0.62f, e.r * 0.05f) }
        fill(col(0xFF6D4C9FL))
        when (e.type) {
            0 -> for (i in 0 until 4) { val a = e.ang + i * 1.6f; circle(e.x + cos(a) * e.r * 0.33f, e.y + sin(a) * e.r * 0.33f, e.r * 0.26f) }
            1 -> circle(e.x, e.y, e.r * 0.72f)
            else -> { stroke(col(0xFF6D4C9FL), e.r * 0.42f); path.reset(); path.addArc(e.x - e.r * 0.5f, e.y - e.r * 0.5f, e.x + e.r * 0.5f, e.y + e.r * 0.5f, e.ang * 40f, 240f); drawPath() }
        }
    }
}
