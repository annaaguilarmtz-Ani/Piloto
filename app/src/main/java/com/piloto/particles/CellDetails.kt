package com.piloto.particles

import android.graphics.Paint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Animaciones ampliadas (caja lógica 300x240) de cada orgánulo. */
object CellDetails {
    private const val L = 11f
    private val WHITE = col(0xFFFFFFFFL)
    private val CYAN = col(0xFF58E6FFL)
    private val GOLD = col(0xFFFFD84AL)
    private val PURPLE = col(0xFFB58CE8L)

    private fun fr(x: Float) = x - kotlin.math.floor(x)

    private val beadColors = intArrayOf(col(0xFFFF7A7AL), col(0xFFFFC857L), col(0xFF7AE582L), col(0xFF6EC8FFL), col(0xFFC792FFL))

    val mitochondrion = Info(
        "Mitocondria",
        "Es la central energética de la célula. Los nutrientes se oxidan en la matriz (ciclo de Krebs) y liberan electrones. " +
            "En la membrana interna, plegada en crestas, la cadena respiratoria bombea protones (H⁺) hacia el espacio intermembrana. " +
            "Al regresar a la matriz a través de la ATP sintasa, que gira como una turbina, se fabrica ATP: la moneda de energía celular. " +
            "Tiene su propio ADN y ribosomas (herencia de una bacteria ancestral)."
    ) { k, t -> with(k) {
        fill(col(0xFF2A130CL)); rrect(20f, 40f, 280f, 200f, 80f)
        stroke(col(0xFFE89A5AL), 4f); rrect(20f, 40f, 280f, 200f, 80f)
        stroke(col(0xFFFFC98AL), 2.5f); rrect(30f, 50f, 270f, 190f, 72f)
        for (i in 0..4) {
            val x = 70f + i * 40f
            val top = i % 2 == 0
            val y0 = if (top) 50f else 190f
            val y1 = if (top) 135f else 105f
            stroke(col(0xFFFFC98AL), 11f); line(x, y0, x, y1)
            stroke(col(0xFF3A1C12L), 6f); line(x, y0, x, y1)
            val sy = if (top) 92f else 148f
            fill(col(0xFFFFD86BL)); circle(x + 8f, sy, 5f)
            stroke(col(0xFFFFF2C0L), 1.5f)
            val a = t * 9f + i
            line(x + 8f, sy, x + 8f + cos(a) * 5f, sy + sin(a) * 5f)
            for (j in 0 until 3) {
                val f = fr(t * 0.45f + i * 0.21f + j / 3f)
                val hy = if (top) 60f + f * 40f else 130f - f * 25f
                fill(CYAN and 0x00FFFFFF or ((1f - f * f) * 255).toInt().shl(24))
                circle(x + sin(f * 9f + j) * 2.5f, hy, 2.4f)
                val f2 = fr(t * 0.4f + i * 0.13f + j / 3f)
                fill(GOLD and 0x00FFFFFF or ((1f - f2) * 255).toInt().shl(24))
                circle(x + 12f + f2 * 26f, sy + (if (top) 1f else -1f) * f2 * 18f, 3f)
            }
        }
        stroke(col(0xFFFFE08AL), 1.5f); oval(215f, 150f, 245f, 175f)
        label("Membrana externa", 150f, 30f, L)
        label("Membrana interna con crestas", 150f, 20f, L)
        label("Matriz (ciclo de Krebs)", 120f, 178f, L)
        label("ATP sintasa", 215f, 121f, L, Paint.Align.LEFT)
        label("ADN mitocondrial", 232f, 190f, L)
        fill(CYAN); circle(30f, 222f, 3f); text("H⁺", 38f, 226f, L, WHITE, Paint.Align.LEFT)
        fill(GOLD); circle(75f, 222f, 3f); text("ATP", 83f, 226f, L, WHITE, Paint.Align.LEFT)
    } }

    val nucleus = Info(
        "Núcleo",
        "Guarda el ADN y controla la actividad de la célula. Una doble membrana (envoltura nuclear) lo separa del citoplasma; " +
            "sus poros regulan qué entra y qué sale. Dentro, la ARN polimerasa copia un gen a ARN mensajero, que sale por un poro " +
            "hasta un ribosoma. En el nucléolo se fabrican las subunidades de los ribosomas. Aquí también se duplica el ADN antes de dividirse."
    ) { k, t -> with(k) {
        fill(col(0xFF1A2166L)); circle(105f, 120f, 84f)
        stroke(col(0xFF8F9BFFL), 3f); circle(105f, 120f, 84f)
        stroke(col(0xFF5E6BE0L), 2f); circle(105f, 120f, 77f)
        for (i in 0 until 14) {
            val a = i * TAU / 14f
            fill(col(0xFFFFD27FL)); circle(105f + cos(a) * 80f, 120f + sin(a) * 80f, 3.6f)
        }
        fill(col(0xFF5A3A9AL)); circle(72f, 158f, 15f)
        stroke(col(0x66A6B0FFL), 2f)
        for (s in 0..2) {
            curve(40) { f, o -> o[0] = 40f + f * 100f; o[1] = 168f + s * 12f + sin(f * 12f + t * 0.5f + s) * 6f }
            drawPath()
        }
        helix(45f, 160f, 92f, 12f, 3f, t * 1.5f, col(0xFF7FE3FFL), col(0xFFFF9EC7L), 2.5f)
        // ARN polimerasa y ARNm
        val prog = min(1f, (t % 9f) / 4f)
        val px = 55f + prog * 95f
        fill(col(0xFFFFB347L)); oval(px - 9f, 78f, px + 9f, 106f)
        val exit = ((t % 9f) - 4f) / 4.5f
        stroke(col(0xFFFF6FA5L), 3f)
        path.reset(); path.moveTo(55f, 106f)
        path.quadTo(90f, 125f, px, 106f)
        if (exit <= 0f) drawPath()
        else {
            val e = min(1f, exit)
            val hx = 150f + e * 130f
            val hy = 110f + e * 26f
            curve(30) { f, o -> o[0] = 100f + (hx - 100f) * f; o[1] = 112f + (hy - 112f) * f + sin(f * 6f) * 4f }
            drawPath()
            fill(PURPLE); oval(268f, 128f, 292f, 146f)
        }
        label("Envoltura nuclear (doble membrana)", 105f, 20f, L)
        label("Poro nuclear", 200f, 176f, L, Paint.Align.LEFT)
        label("Nucléolo", 30f, 200f, L, Paint.Align.LEFT)
        label("ADN → ARNm", 105f, 66f, L)
        label("ARNm sale", 205f, 100f, L, Paint.Align.LEFT)
        label("Ribosoma", 262f, 168f, L)
    } }

    fun ribosome(prok: Boolean) = Info(
        if (prok) "Ribosoma 70S" else "Ribosoma 80S",
        "Traduce el mensaje del ARNm a proteínas. Lee el código de tres en tres letras (codones); por cada codón llega un ARN de " +
            "transferencia (ARNt) con su aminoácido, que se une a la cadena en crecimiento. " +
            (if (prok) "En bacterias el ribosoma es más pequeño (70S) y flota libre en el citoplasma; los antibióticos como la estreptomicina lo bloquean sin dañar los ribosomas humanos. Puede traducir mientras el ADN aún se transcribe."
            else "Puede flotar libre (proteínas para el citoplasma) o estar pegado al retículo rugoso (proteínas para exportar o para membranas).")
    ) { k, t -> with(k) {
        val off = (t * 22f) % 22f
        stroke(col(0xFFFF6FA5L), 4f); line(10f, 130f, 290f, 130f)
        for (i in -1..13) {
            val x = 10f + i * 22f + off
            if (x < 10f || x > 290f) continue
            stroke(beadColors[abs(i) % 5], 5f); line(x - 4f, 122f, x + 4f, 122f)
        }
        fill(col(0xFF8A62C8L)); oval(75f, 138f, 205f, 178f)
        fill(col(0xFFB58CE8L)); oval(70f, 70f, 210f, 138f)
        fill(col(0xFF201030L)); oval(120f, 118f, 150f, 130f)
        // ARNt entrante
        val f = fr(t * 0.45f)
        val ty = 225f - f * 75f
        val tx = 175f - f * 8f
        stroke(col(0xFF7AE582L), 3f); line(tx, ty, tx, ty + 22f)
        fill(col(0xFF7AE582L)); circle(tx, ty + 24f, 6f)
        fill(beadColors[(t * 0.45f).toInt() % 5]); circle(tx, ty - 4f, 5f)
        // Cadena polipeptídica
        val n = 4 + (t * 0.45f).toInt() % 6
        for (i in 0 until n) {
            fill(beadColors[i % 5]); circle(130f + i * 9f, 105f - i * 12f + sin(t * 2f + i) * 2f, 5f)
        }
        label("Subunidad mayor", 140f, 60f, L)
        label("Subunidad menor", 150f, 198f, L)
        label("ARNm (codones)", 60f, 152f, L)
        label("ARNt + aminoácido", 238f, 232f, L)
        label("Proteína naciente", 270f, 40f, L)
    } }

    val rer = Info(
        "Retículo endoplasmático rugoso",
        "Red de sacos aplanados (cisternas) continuos con la envoltura nuclear y cubiertos de ribosomas. " +
            "Las proteínas que se fabrican aquí entran directamente a la luz del retículo, donde se pliegan y reciben azúcares. " +
            "Después viajan en vesículas hacia el aparato de Golgi. Es abundante en células que secretan muchas proteínas, como las que producen anticuerpos o insulina."
    ) { k, t -> with(k) {
        for (b in 0..3) {
            val y = 45f + b * 48f
            for (s in 0..1) {
                val yy = y + s * 20f
                curve(50) { f, o -> o[0] = 20f + f * 230f; o[1] = yy + sin(f * 14f + b) * 5f }
                stroke(col(0xFF3E9AA8L), 4f); drawPath()
            }
            fill(col(0xFF16323AL))
            for (i in 0 until 16) {
                val f = i / 16f
                val x = 28f + f * 220f
                fill(PURPLE); circle(x, y - 6f + sin(f * 14f * 1f + b) * 5f + sin(x / 230f * 14f) * 0f, 2.6f)
            }
            for (j in 0 until 3) {
                val f = fr(t * 0.18f + j / 3f + b * 0.2f)
                val x = 20f + f * 230f
                fill(GOLD and 0x00FFFFFF or ((1f - f * f) * 255).toInt().shl(24)); circle(x, y + 10f + sin(x / 230f * 14f + b) * 5f, 3.4f)
            }
            if (b % 2 == 0) {
                val vf = fr(t * 0.2f + b * 0.3f)
                fill(col(0xFF7FD8E6L)); circle(255f + vf * 35f, y + 10f, 7f * min(1f, vf * 4f))
                fill(GOLD); circle(255f + vf * 35f, y + 10f, 3f * min(1f, vf * 4f))
            }
        }
        label("Ribosomas", 60f, 20f, L)
        label("Luz de la cisterna (proteínas)", 110f, 232f, L)
        label("Vesícula hacia el Golgi", 245f, 20f, L)
    } }

    val ser = Info(
        "Retículo endoplasmático liso",
        "Red de túbulos sin ribosomas. Fabrica lípidos (fosfolípidos, colesterol y hormonas esteroideas), " +
            "detoxifica fármacos y venenos (sobre todo en el hígado) y almacena calcio, que libera para que el músculo se contraiga."
    ) { k, t -> with(k) {
        val curves = arrayOf(
            floatArrayOf(20f, 160f, 80f, 40f, 140f, 220f, 200f, 100f),
            floatArrayOf(40f, 60f, 110f, 150f, 170f, 40f, 260f, 150f),
            floatArrayOf(60f, 210f, 130f, 110f, 210f, 200f, 280f, 60f)
        )
        for ((ci, c) in curves.withIndex()) {
            path.reset(); path.moveTo(c[0], c[1]); path.cubicTo(c[2], c[3], c[4], c[5], c[6], c[7])
            stroke(col(0xFF4FB0C0L), 20f); drawPath()
            stroke(col(0xFF17434DL), 14f); drawPath()
            for (j in 0 until 6) {
                val f = fr(t * 0.12f + j / 6f + ci * 0.17f)
                val q = onPath(f)
                fill(if (j % 2 == 0) col(0xFFFFB347L) else col(0xFFFF6FA5L)); circle(q[0], q[1], if (j % 2 == 0) 3.5f else 2.5f)
            }
            val q2 = onPath(0.5f)
            fill(col(0xFF8DF0B0L)); circle(q2[0], q2[1] + sin(t * 3f + ci) * 1.5f, 4.5f)
        }
        label("Túbulos lisos", 150f, 20f, L)
        label("Síntesis de lípidos", 80f, 232f, L)
        label("Depósito de Ca²⁺ y detoxificación", 190f, 232f, L, Paint.Align.LEFT)
    } }

    val golgi = Info(
        "Aparato de Golgi",
        "Es el centro de correos de la célula: recibe por su cara cis las vesículas del retículo, y las proteínas van pasando de cisterna en cisterna " +
            "mientras se modifican (se les añaden o quitan azúcares), se clasifican y se etiquetan. Por la cara trans salen las vesículas de secreción " +
            "hacia la membrana, y otras hacia los lisosomas."
    ) { k, t -> with(k) {
        val colors = intArrayOf(col(0xFF7FD69AL), col(0xFF9AD98AL), col(0xFFC3D77AL), col(0xFFE3B95AL), col(0xFFE8935AL))
        for (i in 0..4) {
            val y = 70f + i * 26f
            path.reset(); path.moveTo(75f + i * 3f, y + 10f); path.quadTo(150f, y - 26f + sin(t + i) * 2f, 225f - i * 3f, y + 10f)
            stroke(colors[i], 11f); drawPath()
        }
        for (j in 0 until 3) {
            val f = fr(t * 0.2f + j / 3f)
            fill(col(0xFF7FD8E6L)); circle(15f + f * 60f, 30f + f * 60f, 6f)
            fill(colors[0]); circle(15f + f * 60f, 30f + f * 60f, 3f)
        }
        for (j in 0 until 4) {
            val f = fr(t * 0.15f + j / 4f)
            val lvl = min(4f, f * 5f)
            val idx = lvl.toInt().coerceIn(0, 4)
            fill(colors[idx]); circle(if (idx % 2 == 0) 226f else 72f, 80f + f * 100f, 4f)
        }
        for (j in 0 until 3) {
            val f = fr(t * 0.22f + j / 3f)
            fill(col(0xFFE8935AL)); circle(225f + f * 65f, 190f + f * 40f, 7f * min(1f, f * 3f))
            fill(GOLD); circle(225f + f * 65f, 190f + f * 40f, 3f * min(1f, f * 3f))
        }
        label("Cara cis (recibe)", 80f, 20f, L)
        label("Cisternas: modifican y clasifican", 150f, 232f, L)
        label("Cara trans (envía)", 240f, 170f, L)
        label("Vesículas del RE", 30f, 118f, L, Paint.Align.LEFT)
        label("Secreción", 245f, 232f, L)
    } }

    val lysosome = Info(
        "Lisosoma",
        "Es el estómago y el reciclador de la célula. Su interior es muy ácido (pH ≈ 4,5) gracias a bombas de protones, y contiene enzimas " +
            "hidrolíticas que digieren proteínas, lípidos, azúcares y ácidos nucleicos. Se fusiona con vesículas que traen bacterias o orgánulos viejos, " +
            "los degrada y devuelve las moléculas sencillas (aminoácidos, azúcares) al citoplasma para reutilizarlas."
    ) { k, t -> with(k) {
        val u = fr(t / 9f)
        fill(col(0xFF4A2B0EL)); circle(160f, 125f, 72f)
        stroke(col(0xFFE0A04AL), 3.5f); circle(160f, 125f, 72f)
        for (i in 0 until 6) {
            val a = i * TAU / 6f + 0.3f
            fill(CYAN); circle(160f + cos(a) * 72f, 125f + sin(a) * 72f, 3.8f)
        }
        val digest = if (u < 0.35f) 1f else max(0f, 1f - (u - 0.35f) / 0.5f)
        if (u < 0.3f) {
            val f = u / 0.3f
            val vx = 15f + f * 85f
            stroke(col(0xFF9AD8FFL), 2.5f); circle(vx, 125f, 26f)
            fill(col(0xFF6FA84AL)); rrect(vx - 14f, 118f, vx + 14f, 132f, 7f)
        } else if (u < 0.4f) {
            fill(col(0x559AD8FFL)); circle(100f + (u - 0.3f) * 300f, 125f, 26f)
            fill(col(0xFF6FA84AL)); rrect(146f - 14f, 118f, 174f, 132f, 7f)
        } else {
            fill(col(0xFF6FA84AL))
            val s = digest
            rrect(160f - 14f * s, 125f - 7f * s, 160f + 14f * s, 125f + 7f * s, 7f * s)
            for (i in 0 until 10) {
                val a = i * 0.9f + t * 0.5f
                val rr = 22f + 12f * sin(t * 1.6f + i)
                fill(col(0xFFFF9D3AL)); circle(160f + cos(a) * rr, 125f + sin(a) * rr * 0.7f, 4f)
            }
            fill(col(0xFF7AE582L))
            for (i in 0 until 4) {
                val f = fr(t * 0.3f + i / 4f)
                if (digest < 0.9f) circle(160f + cos(i * 1.7f) * (10f + f * 75f), 125f + sin(i * 1.7f) * (10f + f * 75f), 3f)
            }
        }
        label("Enzimas hidrolíticas", 160f, 40f, L)
        label("Bomba de H⁺ (pH ácido)", 160f, 222f, L)
        label("Vesícula con residuos", 80f, 80f, L)
        label("Monómeros reciclados", 245f, 210f, L)
    } }

    val centrosome = Info(
        "Centrosoma y citoesqueleto",
        "El centrosoma contiene dos centriolos perpendiculares, formados por nueve tripletes de microtúbulos. Organiza los microtúbulos, " +
            "tubos de tubulina que crecen y se acortan continuamente y sirven de carreteras para transportar vesículas. " +
            "Al dividirse la célula se duplica y forma el huso que reparte los cromosomas."
    ) { k, t -> with(k) {
        for (i in 0 until 14) {
            val a = i * TAU / 14f
            val len = 70f + 55f * (0.5f + 0.5f * sin(t * 0.9f + i * 1.9f))
            stroke(col(0x88FFC24DL), 3f)
            line(110f, 120f, 110f + cos(a) * len, 120f + sin(a) * len)
            fill(col(0xFFFFE08AL)); circle(110f + cos(a) * len, 120f + sin(a) * len, 3f)
        }
        val rot = t * 0.4f
        stroke(col(0xFF8FA8FFL), 3f); circle(110f, 120f, 30f)
        for (i in 0 until 9) {
            val a = i * TAU / 9f + rot
            val cx = 110f + cos(a) * 24f
            val cy = 120f + sin(a) * 24f
            fill(col(0xFFC7D2FFL))
            for (s in 0..2) circle(cx + cos(a + 1.57f) * (s - 1) * 5.5f, cy + sin(a + 1.57f) * (s - 1) * 5.5f, 2.6f)
        }
        fill(col(0xFF20286EL)); rrect(200f, 175f, 285f, 205f, 8f)
        stroke(col(0xFFC7D2FFL), 2f)
        for (i in 0..5) line(205f + i * 14f, 175f, 205f + i * 14f, 205f)
        label("Centriolo (vista frontal)", 110f, 60f, L)
        label("Centriolo (lateral)", 242f, 225f, L)
        label("Microtúbulos", 110f, 232f, L)
        label("9 tripletes", 110f, 168f, L)
    } }

    private fun sm(x: Float) = x * x * (3f - 2f * x)
    private fun seg(u: Float, a: Float, b: Float) = ((u - a) / (b - a)).coerceIn(0f, 1f)
    private fun mix(a: Float, b: Float, t: Float) = a + (b - a) * t

    private fun ion(k: Kit, x: Float, y: Float, c: Int, s: String, a: Float = 1f, r: Float = 4.8f) {
        val al = (a.coerceIn(0f, 1f) * 255).toInt()
        if (al < 4) return
        with(k) {
            fill((al shl 24) or (c and 0xFFFFFF)); circle(x, y, r)
            stroke((al / 2 shl 24) or 0xFFFFFF, 0.6f); circle(x, y, r)
            text(s, x, y + 2.3f, 6.2f, (al shl 24) or 0xFFFFFF)
        }
    }

    private val NA = 0x2FB04A; private val KK = 0x8A66FF; private val ATPC = 0xFFC933; private val GLU = 0xFF5FA8; private val H2O = 0x5FC8FF

    fun membrane(prok: Boolean) = Info(
        "Membrana plasmática: bombas y transportadores",
        "La membrana es una doble capa de fosfolípidos (cabezas hidrófilas hacia fuera, colas hidrófobas dentro) con proteínas incrustadas. " +
            "Transporte pasivo: los canales y las acuaporinas dejan pasar iones y agua a favor de su gradiente, sin gastar energía. " +
            "Cotransporte: el transportador Na⁺–glucosa usa el Na⁺ que quiere entrar para arrastrar glucosa. " +
            "Transporte activo: la bomba Na⁺/K⁺-ATPasa gasta un ATP para sacar 3 Na⁺ e introducir 2 K⁺, en contra de sus gradientes; así la célula mantiene su voltaje y el gradiente de Na⁺ que impulsa otros transportes." +
            (if (prok) " En bacterias la cadena respiratoria de esta membrana bombea H⁺ y crea el gradiente que mueve la ATP sintasa." else "")
    ) { k, t -> with(k) {
        Sprites.drawRect(cv, "membrane", 0f, 25f, 300f, 215f)
        // ---- Bomba Na+/K+ ATPasa (x≈59) ----
        val u = (t / 8f) % 1f
        val px = 58.8f
        for (i in 0..2) {
            val sx = 38f + i * 20f; val sy = 214f
            val cav = 150f - i * 11f
            val a = sm(seg(u, 0f, 0.05f)) * (1f - seg(u, 0.93f, 1f))
            val s1 = sm(seg(u, 0.02f, 0.25f)); val s3 = sm(seg(u, 0.40f, 0.72f))
            var x = mix(sx, px, s1); var y = mix(sy, cav, s1)
            if (u >= 0.40f) {
                if (s3 < 0.5f) { x = px; y = mix(cav, 98f, s3 * 2f) } else { val q = (s3 - 0.5f) * 2f; x = mix(px, 34f + i * 24f, q); y = mix(98f, 46f - i * 4f, q) }
            }
            ion(this, x, y, NA, "Na⁺", a)
        }
        for (i in 0..1) {
            val a = sm(seg(u, 0.30f, 0.40f)) * (1f - seg(u, 0.95f, 1f))
            val s2 = sm(seg(u, 0.42f, 0.62f)); val s4 = sm(seg(u, 0.66f, 0.95f))
            var x = mix(40f + i * 36f, px, s2); var y = mix(40f, 100f, s2)
            if (u >= 0.66f) { x = mix(px, 44f + i * 26f, s4); y = mix(100f + 0f, 214f, s4) }
            ion(this, x, y, KK, "K⁺", a)
        }
        // ATP -> ADP + Pi
        val atpIn = sm(seg(u, 0f, 0.25f))
        val ax = mix(104f, 66f, atpIn); val ay = mix(214f, 156f, atpIn)
        val aa = sm(seg(u, 0f, 0.06f)) * (1f - seg(u, 0.85f, 0.98f))
        val conv = seg(u, 0.26f, 0.40f)
        val axx = if (u > 0.75f) mix(66f, 108f, seg(u, 0.75f, 0.98f)) else ax
        val ayy = if (u > 0.75f) mix(156f, 216f, seg(u, 0.75f, 0.98f)) else ay
        ion(this, axx, ayy, if (conv > 0.5f) 0x9A8A55 else ATPC, if (conv > 0.5f) "ADP" else "ATP", aa, 5.6f)
        if (u in 0.26f..0.55f) { val q = seg(u, 0.26f, 0.55f); ion(this, mix(72f, 100f, q), mix(150f, 196f, q), 0xFF8A2B, "Pi", 1f - q, 3.6f) }
        // ---- Canal de K+ (x≈120): difusión a favor del gradiente ----
        for (i in 0..2) {
            val f = ((t / 5f) + i / 3f) % 1f
            val y = mix(206f, 34f, f)
            val x = 119.6f + (if (y > 150f || y < 92f) sin(f * 20f + i) * 10f else 0f)
            ion(this, x, y, KK, "K⁺", sm(seg(f, 0f, 0.08f)) * (1f - seg(f, 0.92f, 1f)))
        }
        // ---- Cotransportador Na+-glucosa (x≈182) ----
        val g = (t / 7f) % 1f
        val gy = mix(36f, 210f, sm(g))
        val gxo = if (g < 0.3f) mix(160f, 184f, sm(seg(g, 0f, 0.3f))) else if (g < 0.75f) 184f else mix(184f, 200f, seg(g, 0.75f, 1f))
        val ga = sm(seg(g, 0f, 0.06f)) * (1f - seg(g, 0.94f, 1f))
        ion(this, gxo + 6f, gy + 0f, GLU, "Glc", ga, 5.8f)
        ion(this, gxo - 5f, gy - 4f, NA, "Na⁺", ga)
        // ---- Acuaporina (x≈232 / 253): agua en fila india ----
        for (bar in 0..1) for (i in 0..2) {
            val f = ((t / 4f) + i / 3f + bar * 0.17f) % 1f
            val x = (if (bar == 0) 232.4f else 253f)
            val y = mix(40f, 206f, f)
            val al = sm(seg(f, 0f, 0.08f)) * (1f - seg(f, 0.92f, 1f))
            ion(this, x + (if (y < 90f || y > 156f) sin(f * 12f) * 5f else 0f), y, H2O, "H₂O", al, 4.4f)
        }
        // ---- Rótulos ----
        text("EXTERIOR · Na⁺ alto", 4f, 11f, 8f, col(0xFFBFEFFFL), Paint.Align.LEFT, true)
        text("INTERIOR · K⁺ alto", 4f, 236f, 8f, col(0xFFFFE0A0L), Paint.Align.LEFT, true)
        val step = when { u < 0.25f -> "1 · entran 3 Na⁺ y se une ATP"; u < 0.40f -> "2 · fosforilación: cambia de forma"; u < 0.72f -> "3 · salen 3 Na⁺ · entran 2 K⁺ del exterior"; else -> "4 · K⁺ al interior · sale ADP" }
        label(step, 150f, 26f, 7.6f)
        label("Bomba Na⁺/K⁺", 59f, 232f, 7.6f); label("Canal K⁺", 120f, 232f, 7.6f)
        label("Na⁺–glucosa", 184f, 232f, 7.6f); label("Acuaporina", 243f, 232f, 7.6f)
        label("Glucoproteína", 280f, 66f, 7f, Paint.Align.CENTER)
        label("Colesterol", 96f, 120f, 6.6f)
    } }

    // ---------------- Procariota ----------------

    val nucleoid = Info(
        "Nucleoide (ADN bacteriano)",
        "La bacteria no tiene núcleo: su único cromosoma, circular y muy largo, se pliega y superenrolla en una región llamada nucleoide. " +
            "La replicación empieza en un punto (oriC); dos horquillas avanzan en sentidos opuestos, con la ADN polimerasa copiando ambas hebras, " +
            "hasta encontrarse y formar dos cromosomas hijos. Con buenas condiciones una bacteria se divide cada 20 minutos."
    ) { k, t -> with(k) {
        val f = fr(t / 10f)
        curve(180, true) { u, o ->
            val a = u * TAU
            val r = 78f + 5f * sin(11f * a + t)
            o[0] = 150f + cos(a) * r; o[1] = 120f + sin(a) * r * 0.95f
        }
        stroke(col(0x55FFCF66L), 8f); drawPath()
        stroke(col(0xFFFFE08AL), 2.2f); drawPath()
        val a0 = -PI.toFloat() / 2f
        for (s in intArrayOf(-1, 1)) {
            curve(60) { u, o ->
                val a = a0 + s * u * f * PI.toFloat()
                o[0] = 150f + cos(a) * 62f; o[1] = 120f + sin(a) * 62f * 0.95f
            }
            stroke(col(0xFFFF9A3CL), 2.4f); drawPath()
            val ae = a0 + s * f * PI.toFloat()
            fill(col(0xFFFF6FA5L)); circle(150f + cos(ae) * 62f, 120f + sin(ae) * 62f * 0.95f, 6f + sin(t * 6f) * 1.2f)
        }
        label("ADN circular superenrollado", 150f, 20f, L)
        label("oriC (origen)", 150f, 52f, L)
        label("ADN polimerasa", 235f, 226f, L)
        label("Hebras nuevas", 60f, 226f, L)
    } }

    val plasmid = Info(
        "Plásmidos y conjugación",
        "Son pequeños anillos de ADN extra que se replican por su cuenta. Suelen llevar genes útiles, como la resistencia a antibióticos. " +
            "Las bacterias pueden intercambiarlos: a través de un pilus sexual pasan una copia del plásmido a otra bacteria (conjugación), " +
            "así la resistencia se propaga rápidamente, incluso entre especies distintas."
    ) { k, t -> with(k) {
        val u = fr(t / 8f)
        for (side in 0..1) {
            val cx = if (side == 0) 60f else 240f
            fill(col(0xFF16343AL)); rrect(cx - 45f, 90f, cx + 45f, 150f, 30f)
            stroke(col(0xFF6FAE7AL), 3f); rrect(cx - 45f, 90f, cx + 45f, 150f, 30f)
        }
        stroke(col(0xFF8FD0A8L), 6f); line(105f, 120f, 195f, 120f)
        stroke(col(0xFF0B161CL), 3f); line(105f, 120f, 195f, 120f)
        stroke(col(0xFFFF8FA3L), 2.5f); circle(52f, 122f, 11f)
        fill(col(0xFFFF8FA3L)); circle(52f, 111f, 2.5f)
        if (u in 0.2f..0.7f) {
            val f = (u - 0.2f) / 0.5f
            stroke(col(0xFFFF8FA3L), 2.5f); circle(105f + f * 90f, 120f, 4f + 2f * sin(f * 20f))
        }
        if (u >= 0.7f) {
            val a = min(1f, (u - 0.7f) * 6f)
            stroke(col((a * 255).toInt().shl(24).toLong() or 0xFF8FA3L), 2.5f); circle(248f, 122f, 11f)
        }
        label("Pilus sexual", 150f, 96f, L)
        label("Bacteria donante", 60f, 176f, L)
        label("Bacteria receptora", 240f, 176f, L)
        label("Plásmido (gen de resistencia)", 150f, 215f, L)
    } }

    val granule = Info(
        "Gránulos de reserva",
        "Son depósitos donde la bacteria guarda lo que le sobra: glucógeno o almidón (energía), poli-β-hidroxibutirato " +
            "(carbono y energía) o polifosfato (fósforo). Cuando abunda el alimento crecen añadiendo unidades; cuando escasea, se degradan para sobrevivir."
    ) { k, t -> with(k) {
        val g = 40f + 12f * sin(t * 0.7f)
        fill(col(0xFFE8EEF0L)); circle(150f, 120f, g)
        stroke(col(0xFFB0C0C8L), 2f); circle(150f, 120f, g)
        stroke(col(0xFFB0C0C8L), 1.5f)
        for (i in 0 until 16) {
            val a = i * TAU / 16f
            val rr = g * (0.4f + 0.3f * sin(t * 1.5f + i))
            line(150f, 120f, 150f + cos(a) * rr, 120f + sin(a) * rr)
        }
        for (j in 0 until 8) {
            val f = fr(t * 0.2f + j / 8f)
            val a = j * 0.79f
            val r0 = 120f * (1f - f) + g
            fill(col(0xFFFFB347L)); circle(150f + cos(a) * r0, 120f + sin(a) * r0 * 0.8f, 3.5f)
        }
        label("Gránulo (glucógeno, PHB, polifosfato)", 150f, 215f, L)
        label("Nutrientes que se acumulan", 150f, 25f, L)
    } }

    val flagellum = Info(
        "Flagelo",
        "Es una hélice giratoria para nadar. En la envoltura se ancla un motor molecular: el rotor gira empujado por protones (H⁺) que fluyen " +
            "a través de las unidades estator, a más de 1000 vueltas por segundo. El gancho transmite el giro al filamento de flagelina, " +
            "que actúa como hélice. Invirtiendo el giro, la bacteria cambia de dirección y persigue nutrientes (quimiotaxis)."
    ) { k, t -> with(k) {
        for ((i, x) in floatArrayOf(45f, 62f, 79f).withIndex()) {
            stroke(intArrayOf(col(0xFF6FAE7AL), col(0xFFD6B25EL), col(0xFFE0B45AL))[i], 5f); line(x, 10f, x, 230f)
        }
        stroke(col(0xFFB0B8C0L), 8f); line(35f, 120f, 130f, 120f)
        val rot = t * 5f
        for (i in 0 until 2) {
            val yy = if (i == 0) 95f else 145f
            fill(col(0xFF9AA6B0L)); rrect(52f, yy - 4f, 90f, yy + 4f, 4f)
        }
        stroke(col(0xFFFF9A3CL), 4f)
        for (i in 0 until 6) {
            val a = rot + i * TAU / 6f
            line(100f, 120f + cos(a) * 26f, 100f, 120f + cos(a) * 26f)
        }
        fill(col(0xFF20286EL)); oval(92f, 92f, 108f, 148f)
        stroke(col(0xFFFF9A3CL), 3f)
        for (i in 0 until 4) {
            val a = rot + i * 1.57f
            line(100f, 120f, 100f + cos(a) * 3f, 120f + sin(a) * 26f)
        }
        for (s in intArrayOf(-1, 1)) {
            fill(col(0xFF3FA37AL)); rrect(95f, 120f + s * 40f - 10f, 116f, 120f + s * 40f + 10f, 4f)
            for (j in 0 until 3) {
                val f = fr(t * 0.5f + j / 3f)
                fill(CYAN); circle(105f, 120f + s * (85f - f * 65f), 3f)
            }
        }
        stroke(col(0xFFB0B8C0L), 6f)
        path.reset(); path.moveTo(130f, 120f); path.quadTo(150f, 120f, 158f, 100f)
        drawPath()
        stroke(col(0xFF8FD0A8L), 6f)
        curve(60) { f, o ->
            o[0] = 158f + f * 135f
            o[1] = 100f - f * 30f + 16f * f * sin(f * 12f - t * 8f)
        }
        drawPath()
        label("Membrana externa", 60f, 232f, L)
        label("Estator", 145f, 66f, L)
        label("Rotor", 150f, 178f, L)
        label("Gancho", 155f, 132f, L)
        label("Filamento de flagelina", 225f, 40f, L)
        label("H⁺", 78f, 225f, L)
    } }

    val pili = Info(
        "Pili y fimbrias",
        "Son filamentos finos de proteína (pilina) que salen de la superficie. Las fimbrias sirven para adherirse a superficies y tejidos, " +
            "primer paso de muchas infecciones. Los pili tipo IV se extienden, se agarran y se retraen como un gancho, arrastrando a la bacteria " +
            "(movimiento espasmódico). El pilus sexual permite la conjugación."
    ) { k, t -> with(k) {
        val u = fr(t / 6f)
        val ext = if (u < 0.45f) u / 0.45f else 1f
        val pull = if (u > 0.55f) (u - 0.55f) / 0.45f else 0f
        val cx = 60f + pull * 90f
        fill(col(0xFF16343AL)); rrect(cx - 45f, 90f, cx + 45f, 150f, 30f)
        stroke(col(0xFF6FAE7AL), 3f); rrect(cx - 45f, 90f, cx + 45f, 150f, 30f)
        fill(col(0xFF3A2A50L)); rrect(255f, 20f, 290f, 220f, 8f)
        for (i in 0..5) { fill(col(0xFFE05A8AL)); circle(255f, 40f + i * 32f, 4f) }
        val tip = min(255f, cx + 45f + ext * 170f)
        for (j in 0..3) {
            val y = 100f + j * 14f
            stroke(col(0xFF8FD0A8L), 2f)
            curve(30) { f, o -> o[0] = cx + 40f + f * (tip - cx - 40f); o[1] = y + (j - 1.5f) * f * 8f + sin(f * 9f + t * 2f + j) * 2f }
            drawPath()
        }
        label("Pili (adhesión)", 150f, 70f, L)
        label("Receptores de la célula huésped", 205f, 232f, L)
        label(if (u < 0.45f) "Se extiende" else if (u < 0.55f) "Se adhiere" else "Se retrae y tira", 60f, 186f, L)
    } }

    val wall = Info(
        "Cápsula y pared celular",
        "La cápsula es una capa viscosa de polisacáridos que protege de la desecación y de los glóbulos blancos. " +
            "Bajo ella, la pared de peptidoglucano es una malla de cadenas de azúcares unidas por puentes de péptidos: da forma y resiste la presión interna. " +
            "Antibióticos como la penicilina bloquean la construcción de los puentes; la pared se debilita y la bacteria estalla."
    ) { k, t -> with(k) {
        stroke(col(0x668FD0A8L), 2f)
        for (i in 0 until 26) {
            val x = 10f + i * 11f
            curve(12) { f, o -> o[0] = x + sin(f * 6f + t * 1.5f + i) * 4f; o[1] = 20f + f * 55f }
            drawPath()
        }
        for (r in 0..3) {
            val y = 95f + r * 18f
            stroke(col(0xFF6FAE7AL), 4f); line(10f, y, 290f, y)
            stroke(col(0xFFBFE8C8L), 1.6f)
            for (c in 0 until 14) {
                val x = 22f + c * 20f + (r % 2) * 10f
                if (r < 3) line(x, y, x, y + 18f)
            }
        }
        val f = fr(t * 0.3f)
        stroke(col(0xFFFF6FA5L), 5f); line(10f + f * 280f - 30f, 149f, 10f + f * 280f, 149f)
        stroke(col(0xFF3FA37AL), 4f); line(10f, 178f, 290f, 178f)
        stroke(col(0xFFF2C46BL), 4f); line(10f, 190f, 290f, 190f)
        fill(col(0xFF1E4C4AL)); rrect(10f, 195f, 290f, 232f, 4f)
        for (i in 0 until 3) {
            val g = fr(t * 0.25f + i / 3f)
            fill(GOLD); circle(40f + i * 100f, 100f + g * 45f, 4f)
        }
        label("Cápsula", 150f, 14f, L)
        label("Peptidoglucano (malla)", 150f, 88f, L)
        label("Enzima insertando puentes", 150f, 168f, L)
        label("Membrana plasmática", 150f, 174f + 26f, L)
    } }

    val respiration = Info(
        "Cadena respiratoria y ATP sintasa",
        "En la membrana plasmática de la bacteria, los electrones de los nutrientes pasan por una cadena de complejos que bombean protones (H⁺) fuera de la célula. " +
            "La acumulación de protones crea una fuerza que los empuja de vuelta a través de la ATP sintasa, una turbina molecular " +
            "que gira y fabrica ATP a partir de ADP y fosfato. Es el mismo principio que usan las mitocondrias."
    ) { k, t -> with(k) {
        fill(col(0xFF1E4C4AL)); rect.set(0f, 130f, 300f, 240f); cv.drawRect(rect, p)
        fill(col(0xFF3A2A18L)); rect.set(0f, 0f, 300f, 90f); cv.drawRect(rect, p)
        fill(col(0xFFF2C46BL)); rect.set(0f, 90f, 300f, 130f); cv.drawRect(rect, p)
        fill(col(0xFFC98F3AL)); rect.set(0f, 100f, 300f, 120f); cv.drawRect(rect, p)
        for (i in 0..2) {
            val x = 30f + i * 60f
            fill(col(0xFF3FA37AL)); rrect(x - 20f, 84f, x + 20f, 136f, 12f)
            for (j in 0 until 2) {
                val f = fr(t * 0.35f + j * 0.5f + i * 0.2f)
                fill(CYAN); circle(x - 6f + j * 12f, 130f - f * 90f, 3f)
            }
        }
        for (j in 0 until 4) {
            val f = fr(t * 0.6f + j / 4f)
            fill(col(0xFFFFC857L)); circle(30f + f * 120f, 110f, 3f)
        }
        // ATP sintasa
        fill(col(0xFFE05A8AL)); rrect(220f, 84f, 250f, 136f, 8f)
        val a = t * 6f
        fill(col(0xFFFFD86BL)); rrect(214f, 138f, 256f, 172f, 12f)
        stroke(col(0xFFFFF2C0L), 2.5f); line(235f, 155f, 235f + cos(a) * 16f, 155f + sin(a) * 8f)
        for (j in 0 until 3) {
            val f = fr(t * 0.5f + j / 3f)
            fill(CYAN); circle(235f, 40f + f * 100f, 3f)
            fill(GOLD); circle(235f + f * 30f, 175f + f * 40f, 4f)
        }
        label("Exterior (periplasma) – rico en H⁺", 150f, 16f, L)
        label("Membrana", 100f, 108f, L)
        label("Complejos (bombas de H⁺)", 90f, 160f, L)
        label("ATP sintasa", 235f, 76f, L)
        label("ATP", 268f, 225f, L)
        label("Citoplasma", 60f, 225f, L)
        text("e⁻ →", 20f, 96f, L, GOLD, Paint.Align.LEFT)
    } }

    // ---------------- Vegetal ----------------

    val chloroplast = Info(
        "Cloroplasto",
        "Es donde la planta hace la fotosíntesis. En las membranas de los tilacoides (apilados en grana) la clorofila capta la luz, rompe el agua y libera oxígeno; " +
            "la energía se guarda en ATP y NADPH gracias a un flujo de protones a través de la ATP sintasa. En el estroma, el ciclo de Calvin usa ese ATP y NADPH para fijar el CO₂ " +
            "y construir azúcares, que se guardan como almidón. Tiene doble membrana, ADN y ribosomas propios, como la mitocondria."
    ) { k, t -> with(k) {
        fill(col(0xFF12361EL)); oval(15f, 40f, 285f, 200f)
        stroke(col(0xFF5FBF6AL), 3f); oval(15f, 40f, 285f, 200f)
        stroke(col(0xFF8FE09AL), 1.5f); oval(22f, 47f, 278f, 193f)
        for (g in 0..3) {
            val gx = 62f + g * 50f; val gy = 92f + (g % 2) * 28f
            for (q in 0..4) {
                fill(col(0xFF3E9E4EL)); oval(gx - 18f, gy + q * 7f, gx + 18f, gy + q * 7f + 5f)
                fill(CYAN and 0x00FFFFFF or (0x88 shl 24)); circle(gx - 8f + sin(t * 2f + q + g) * 6f, gy + q * 7f + 2.5f, 1.6f)
            }
            if (g < 3) { stroke(col(0x885FBF6AL), 1.2f); line(gx + 18f, gy + 14f, gx + 32f, gy + 14f + ((g + 1) % 2 * 2 - 1) * -14f) }
            val f = fr(t * 0.3f + g * 0.25f)
            stroke(col(0xCCFFFFFFL), 1.2f); circle(gx + 10f + sin(t * 3f + g) * 4f, gy - f * 55f, 3f)
        }
        for (i in 0..5) {
            val f = fr(t * 0.5f + i / 6f)
            fill((1f - f * f).let { (it * 255).toInt().shl(24) or 0xFFE066 }); circle(30f + i * 42f + f * 22f, 6f + f * 90f, 3.5f)
        }
        stroke(col(0xFFFF9A3CL), 3f)
        curve(40) { u, o -> val a = u * 5.2f - 0.6f; o[0] = 218f + cos(a) * 26f; o[1] = 152f + sin(a) * 26f }
        drawPath()
        val q = onPath(fr(t * 0.2f))
        fill(GOLD); circle(q[0], q[1], 4f)
        for (i in 0..2) { val f = fr(t * 0.25f + i / 3f); fill(col(0xFFB8C4CCL)); circle(292f - f * 55f, 196f - f * 30f, 3f) }
        fill(col(0xFFFFE8A0L)); val e = fr(t * 0.2f + 0.5f); circle(240f + e * 30f, 130f - e * 50f, 3.5f)
        fill(col(0xFFEDEDE0L)); oval(235f, 70f, 262f, 84f)
        label("Membrana doble", 150f, 32f, L)
        label("Grana (tilacoides con clorofila)", 100f, 226f, L)
        label("Luz", 25f, 30f, L)
        label("O₂", 60f, 78f, L)
        label("Ciclo de Calvin", 218f, 118f, L)
        label("CO₂ →", 262f, 224f, L)
        label("Almidón", 250f, 62f, L)
    } }

    val vacuole = Info(
        "Vacuola central",
        "Ocupa hasta el 90 % de la célula vegetal. Almacena agua, azúcares, iones, pigmentos (como las antocianinas, que dan el color a pétalos y frutos) y desechos. " +
            "El agua entra por ósmosis a través de acuaporinas del tonoplasto y la vacuola empuja el citoplasma contra la pared: es la presión de turgencia, que mantiene rígida la planta. " +
            "Si falta agua, la vacuola se encoge y la planta se marchita."
    ) { k, t -> with(k) {
        val ph = 0.5f + 0.5f * sin(t * 0.8f)
        val rx = 95f + 30f * ph; val ry = 70f + 30f * ph
        fill(col(0xFF1F4A36L)); rrect(20f, 20f, 280f, 220f, 40f)
        stroke(col(0xFF7A9A3CL), 8f); rrect(20f, 20f, 280f, 220f, 40f)
        fill(col(0xFF1E5E7EL)); oval(150f - rx, 120f - ry, 150f + rx, 120f + ry)
        stroke(col(0xFF7FC4E0L), 2.5f); oval(150f - rx, 120f - ry, 150f + rx, 120f + ry)
        for (i in 0 until 6) {
            val a = i * TAU / 6f + 0.3f
            val px = 150f + cos(a) * rx; val py = 120f + sin(a) * ry
            fill(col(0xFF3FA37AL)); circle(px, py, 4f)
            val f = fr(t * 0.4f + i * 0.17f)
            val dir = if (cos(t * 0.8f) > 0f) 1f else -1f
            fill(col(0xFF9AE0FFL)); circle(px + cos(a) * (14f - 28f * f * dir), py + sin(a) * (14f - 28f * f * dir), 2.4f)
        }
        for (i in 0 until 10) {
            fill(if (i % 3 == 0) col(0xFFB05AD8L) else col(0xFFE8D06AL))
            circle(150f + cos(i * 2.1f + t * 0.2f) * rx * 0.55f, 120f + sin(i * 1.7f + t * 0.25f) * ry * 0.5f, if (i % 3 == 0) 6f else 2.6f)
        }
        stroke(col(0xFFFFC857L), 2.5f)
        val push = 8f + 8f * ph
        line(150f, 120f - ry - 2f, 150f, 120f - ry - push); line(150f, 120f + ry + 2f, 150f, 120f + ry + push)
        label("Tonoplasto con acuaporinas", 150f, 14f, L)
        label("Agua (ósmosis)", 232f, 232f, L)
        label("Pigmentos y azúcares", 150f, 122f, L)
        label(if (cos(t * 0.8f) > 0f) "Turgente" else "Perdiendo agua", 55f, 232f, L)
        label("Pared celular", 42f, 40f, L)
    } }

    val plantWall = Info(
        "Pared celular y plasmodesmos",
        "Rodea la célula vegetal y le da forma y resistencia. Está hecha de microfibrillas de celulosa entrecruzadas con hemicelulosa y pectina; entre células vecinas hay una lámina media de pectina que las pega. " +
            "Los plasmodesmos son túneles que atraviesan la pared y unen los citoplasmas de células contiguas: por ellos pasan agua, azúcares, señales y hasta ARN."
    ) { k, t -> with(k) {
        fill(col(0xFF1F4A36L)); rect.set(0f, 0f, 120f, 240f); cv.drawRect(rect, p)
        rect.set(180f, 0f, 300f, 240f); cv.drawRect(rect, p)
        fill(col(0xFFE8E0A0L)); rect.set(147f, 0f, 153f, 240f); cv.drawRect(rect, p)
        for (side in 0..1) {
            val x0 = if (side == 0) 122f else 156f
            fill(col(0xFF7A9A3CL)); rect.set(x0, 0f, x0 + 22f, 240f); cv.drawRect(rect, p)
            stroke(col(0xFFD4EC8AL), 1.6f)
            for (i in 0 until 24) {
                val y = i * 10f
                if (i % 2 == 0) line(x0 + 1f, y, x0 + 21f, y + 6f) else line(x0 + 1f, y + 6f, x0 + 21f, y)
                line(x0 + 4f, y + 4f, x0 + 4f, y + 9f)
            }
        }
        fill(col(0xFF1F4A36L)); rect.set(122f, 108f, 178f, 132f); cv.drawRect(rect, p)
        stroke(col(0xFFE0B45AL), 2f); line(122f, 108f, 178f, 108f); line(122f, 132f, 178f, 132f)
        stroke(col(0xFF4FB0C0L), 5f); line(100f, 120f, 200f, 120f)
        stroke(col(0xFF17434DL), 2f); line(100f, 120f, 200f, 120f)
        for (i in 0..4) {
            val f = fr(t * 0.25f + i / 5f)
            fill(if (i % 2 == 0) GOLD else CYAN); circle(60f + f * 180f, 100f + (i % 3) * 6f, 3.4f)
        }
        label("Célula A", 60f, 24f, L); label("Célula B", 240f, 24f, L)
        label("Pared primaria (celulosa)", 150f, 226f, L)
        label("Lámina media (pectina)", 150f, 14f, L)
        label("Plasmodesmo", 150f, 92f, L)
        label("Azúcares, agua, señales", 150f, 160f, L)
    } }
}
