package com.piloto.particles

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Simulación de partículas diminutas y multicolor.
 * Las partículas base son permanentes; las "hijas" (creadas al tocar) viven unos segundos.
 */
class ParticleSystem(private val density: Float) {

    companion object {
        const val BASE_COUNT = 450
        const val MAX_COUNT = 1800
        private const val SPLIT_RADIUS_DP = 55f
        private const val SPLIT_RATE = 1.6f // divisiones por segundo por partícula tocada
    }

    private val x = FloatArray(MAX_COUNT)
    private val y = FloatArray(MAX_COUNT)
    private val vx = FloatArray(MAX_COUNT)
    private val vy = FloatArray(MAX_COUNT)
    private val size = FloatArray(MAX_COUNT)
    private val hue = FloatArray(MAX_COUNT)
    private val phase = FloatArray(MAX_COUNT)
    private val life = FloatArray(MAX_COUNT) // -1 = permanente
    private var count = 0

    private var w = 0f
    private var h = 0f
    private var time = 0f

    private var touching = false
    private var tx = 0f
    private var ty = 0f
    private var burst = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hsv = floatArrayOf(0f, 0.85f, 1f)

    fun resize(width: Int, height: Int) {
        val first = w == 0f
        w = width.toFloat()
        h = height.toFloat()
        if (first) repeat(BASE_COUNT) { spawn(Random.nextFloat() * w, Random.nextFloat() * h, -1f) }
    }

    fun touchDown(px: Float, py: Float) {
        touching = true; tx = px; ty = py; burst = true
    }

    fun touchMove(px: Float, py: Float) {
        tx = px; ty = py
    }

    fun touchUp() {
        touching = false
    }

    private fun spawn(px: Float, py: Float, lifeSec: Float, h0: Float = Random.nextFloat() * 360f) {
        if (count >= MAX_COUNT) return
        val i = count++
        x[i] = px; y[i] = py
        val a = Random.nextFloat() * (2 * PI).toFloat()
        val s = (10f + Random.nextFloat() * 25f) * density
        vx[i] = kotlin.math.cos(a) * s
        vy[i] = sin(a) * s
        size[i] = (0.45f + Random.nextFloat() * 0.65f) * density // radio ~0.5–1.1 dp
        hue[i] = h0
        phase[i] = Random.nextFloat() * 6.28f
        life[i] = lifeSec
    }

    fun update(dtRaw: Float) {
        if (w == 0f) return
        val dt = min(dtRaw, 0.05f)
        time += dt
        val n = count

        for (i in 0 until n) {
            var ax = 0f
            var ay = 0f
            if (touching) {
                val dx = tx - x[i]
                val dy = ty - y[i]
                val d = max(sqrt(dx * dx + dy * dy), 1f)
                val nx = dx / d
                val ny = dy / d
                // Atracción hacia el dedo, más suave cerca para formar una nube que gira
                val pull = (600f * density) * min(1f, d / (90f * density)) + 120f * density
                ax += nx * pull
                ay += ny * pull
                // Remolino tangencial
                ax += -ny * 260f * density
                ay += nx * 260f * density
            } else {
                // Deriva suave en reposo
                val t = time * 0.4f + phase[i]
                ax += sin(t * 1.3f + i) * 14f * density
                ay += kotlin.math.cos(t * 1.1f + i * 0.7f) * 14f * density
            }
            val damp = if (touching) 2.4f else 0.7f
            vx[i] += (ax - vx[i] * damp) * dt
            vy[i] += (ay - vy[i] * damp) * dt
            x[i] += vx[i] * dt
            y[i] += vy[i] * dt

            if (x[i] < 0f) { x[i] = 0f; vx[i] = -vx[i] * 0.6f } else if (x[i] > w) { x[i] = w; vx[i] = -vx[i] * 0.6f }
            if (y[i] < 0f) { y[i] = 0f; vy[i] = -vy[i] * 0.6f } else if (y[i] > h) { y[i] = h; vy[i] = -vy[i] * 0.6f }
        }

        // División / multiplicación de las partículas tocadas
        if (touching) {
            val r = SPLIT_RADIUS_DP * density
            val r2 = r * r
            val chance = if (burst) 0.8f else SPLIT_RATE * dt
            for (i in 0 until n) {
                val dx = tx - x[i]
                val dy = ty - y[i]
                if (dx * dx + dy * dy < r2 && Random.nextFloat() < chance) {
                    spawn(x[i], y[i], 14f + Random.nextFloat() * 10f, (hue[i] + Random.nextFloat() * 50f - 25f + 360f) % 360f)
                }
            }
            burst = false
        }

        // Caducidad de las hijas
        var i = 0
        while (i < count) {
            if (life[i] >= 0f) {
                life[i] -= dt
                if (life[i] <= 0f) { remove(i); continue }
            }
            i++
        }
    }

    private fun remove(i: Int) {
        val last = --count
        if (i == last) return
        x[i] = x[last]; y[i] = y[last]; vx[i] = vx[last]; vy[i] = vy[last]
        size[i] = size[last]; hue[i] = hue[last]; phase[i] = phase[last]; life[i] = life[last]
    }

    fun draw(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)
        for (i in 0 until count) {
            hsv[0] = hue[i]
            val rgb = Color.HSVToColor(hsv)
            var a = 0.65f + 0.35f * sin(time * 2f + phase[i])
            if (life[i] in 0f..2f) a *= life[i] / 2f
            paint.color = (rgb and 0x00FFFFFF) or ((a.coerceIn(0f, 1f) * 255).toInt() shl 24)
            canvas.drawCircle(x[i], y[i], size[i], paint)
        }
    }
}
